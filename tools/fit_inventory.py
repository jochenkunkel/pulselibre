#!/usr/bin/env python3
"""Read-only, privacy-preserving structural inventory for a local FIT archive.

The input files are never modified. Output contains schema and aggregate counts,
not sample values or raw file contents. Garmin's profile module is loaded as a
literal AST assignment and is never executed.
"""
from __future__ import annotations

import argparse
import ast
import csv
import hashlib
import json
import sqlite3
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

FIT_EPOCH = 631_065_600
BASE_TYPES = {
    0: "enum", 1: "sint8", 2: "uint8", 3: "sint16", 4: "uint16",
    5: "sint32", 6: "uint32", 7: "string", 8: "float32",
    9: "float64", 10: "uint8z", 11: "uint16z", 12: "uint32z",
    13: "byte", 14: "sint64", 15: "uint64", 16: "uint64z",
}
INVALIDS = {
    0: {0xFF}, 1: {0x7F}, 2: {0xFF}, 3: {0x7FFF}, 4: {0xFFFF},
    5: {0x7FFFFFFF}, 6: {0xFFFFFFFF}, 8: {0xFFFFFFFF},
    9: {0xFFFFFFFFFFFFFFFF}, 10: {0}, 11: {0}, 12: {0},
    13: {0xFF}, 14: {0x7FFFFFFFFFFFFFFF},
    15: {0xFFFFFFFFFFFFFFFF}, 16: {0},
}
BASE_TYPE_SIZES = {
    0: 1, 1: 1, 2: 1, 3: 2, 4: 2, 5: 4, 6: 4, 7: 1,
    8: 4, 9: 8, 10: 1, 11: 2, 12: 4, 13: 1, 14: 8, 15: 8, 16: 8,
}


class FitError(ValueError):
    pass


def crc16(data: bytes) -> int:
    crc = 0
    for byte in data:
        crc ^= byte
        for _ in range(8):
            crc = (crc >> 1) ^ (0xA001 if crc & 1 else 0)
    return crc & 0xFFFF


def profile_from_python(path: Path) -> dict[str, Any]:
    """Read only the literal `Profile = {...}` assignment; execute no SDK code."""
    tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
    for node in tree.body:
        if isinstance(node, ast.Assign) and any(
            isinstance(target, ast.Name) and target.id == "Profile"
            for target in node.targets
        ):
            value = ast.literal_eval(node.value)
            if not isinstance(value, dict) or not isinstance(value.get("messages"), dict):
                break
            return value
    raise FitError("Profile literal assignment not found")


def _unsigned(raw: bytes, big: bool) -> int | None:
    if not raw:
        return None
    return int.from_bytes(raw, "big" if big else "little")


def _month(raw: bytes, big: bool) -> str | None:
    seconds = _unsigned(raw, big)
    if seconds is None or seconds == 0xFFFFFFFF:
        return None
    # Keep time resolution at month-level in reports.
    import datetime
    try:
        return datetime.datetime.fromtimestamp(
            seconds + FIT_EPOCH, datetime.timezone.utc
        ).strftime("%Y-%m")
    except (OverflowError, OSError, ValueError):
        return None


def _decode_scalar(raw: bytes, base: int, big: bool) -> int | None:
    if not raw or base not in (0, 1, 2, 3, 4, 5, 6, 10, 11, 12, 14, 15, 16):
        return None
    width = BASE_TYPE_SIZES[base]
    if len(raw) % width:
        return None
    # Counts refer to records with a fully readable, non-sentinel value.
    vals = []
    for pos in range(0, len(raw), width):
        chunk = raw[pos:pos + width]
        val = int.from_bytes(chunk, "big" if big else "little",
                             signed=base in (1, 3, 5, 14))
        invalid = INVALIDS.get(base, set())
        if val in invalid:
            continue
        vals.append(val)
    return len(vals) if vals else None


def _value_counts(raw: bytes, base: int, big: bool) -> tuple[int, int]:
    """Return valid and invalid scalar-element counts without retaining values."""
    if base == 7:
        text = raw.split(b"\0", 1)[0]
        return (1, 0) if text else (0, 1)
    if base == 13:
        if not raw or all(byte == 0xFF for byte in raw):
            return 0, 1
        return len(raw), 0
    width = BASE_TYPE_SIZES.get(base)
    if width is None or not raw or len(raw) % width:
        return 0, 1
    valid = invalid = 0
    for pos in range(0, len(raw), width):
        chunk = raw[pos:pos + width]
        if base in (8, 9):
            invalid += int(all(byte == 0xFF for byte in chunk))
            valid += int(not all(byte == 0xFF for byte in chunk))
            continue
        val = int.from_bytes(chunk, "big" if big else "little",
                             signed=base in (1, 3, 5, 14))
        if val in INVALIDS.get(base, set()):
            invalid += 1
        else:
            valid += 1
    return valid, invalid


def _profile_field(profile: dict[str, Any], global_num: int, field_num: int):
    msg = profile.get("messages", {}).get(global_num)
    if msg is None:
        return None, None
    field = msg.get("fields", {}).get(field_num)
    return msg, field


def inventory_fit(path: Path, raw: bytes, profile: dict[str, Any]):
    if len(raw) < 14 or raw[8:12] != b".FIT":
        raise FitError("FIT signature/header absent")
    header_size = raw[0]
    if header_size not in (12, 14) or len(raw) < header_size + 2:
        raise FitError("unsupported FIT header size")
    data_size = int.from_bytes(raw[4:8], "little")
    data_end = header_size + data_size
    if data_end + 2 != len(raw):
        raise FitError("FIT declared length mismatch")
    if header_size == 14 and crc16(raw[:14]) != 0:
        raise FitError("FIT header CRC mismatch")
    if crc16(raw) != 0:
        raise FitError("FIT file CRC mismatch")

    definitions: list[dict[str, Any] | None] = [None] * 16
    records: Counter[int] = Counter()
    field_stats: dict[tuple[int, int, int | None], dict[str, Any]] = {}
    developer_descriptions: dict[tuple[int, int], dict[str, Any]] = {}
    record_ordinal = 0
    observation_ordinal = 0
    previous_timestamp: int | None = None
    months: list[str] = []
    monitoring_hr_timestamp16_records = 0
    monitoring_hr_full_timestamp_records = 0
    imported_candidate_keys: dict[tuple[int, int], set[str]] = defaultdict(set)
    cursor = header_size

    def need(n: int):
        nonlocal cursor
        if n < 0 or cursor + n > data_end:
            raise FitError("truncated FIT record")
        start = cursor
        cursor += n
        return start

    while cursor < data_end:
        tag = raw[need(1)]
        compressed = bool(tag & 0x80)
        if not compressed and tag & 0x40:
            if tag & 0x10:
                raise FitError("reserved definition header bit set")
            local = tag & 0x0F
            reserved = raw[need(1)]
            architecture = raw[need(1)]
            if reserved != 0 or architecture not in (0, 1):
                raise FitError("invalid definition header")
            endian = "big" if architecture else "little"
            global_num = int.from_bytes(raw[need(2):cursor], endian)
            count = raw[need(1)]
            fields = []
            for _ in range(count):
                start = need(3)
                fnum, size, base = raw[start:start + 3]
                if size == 0:
                    raise FitError("zero-size FIT field")
                fields.append((fnum, size, base & 0x1F, base))
            dev_fields = []
            if tag & 0x20:
                dev_count = raw[need(1)]
                for _ in range(dev_count):
                    start = need(3)
                    fnum, size, dev_index = raw[start:start + 3]
                    if size == 0:
                        raise FitError("zero-size developer field")
                    dev_fields.append((fnum, size, dev_index))
            definitions[local] = {
                "global": global_num, "big": architecture == 1,
                "fields": fields, "developer": dev_fields,
            }
            for fnum, _size, _base, _base_raw in fields:
                field_stats.setdefault((global_num, fnum, None), {
                    "definitions": 0, "records": 0, "valid": 0, "invalid": 0,
                    "timestamped_records": 0,
                    "sizes": set(), "bases": set(), "timestamps": []
                })["definitions"] += 1
            for fnum, _size, dev_index in dev_fields:
                field_stats.setdefault((global_num, fnum, dev_index), {
                    "definitions": 0, "records": 0, "valid": 0, "invalid": 0,
                    "timestamped_records": 0,
                    "sizes": set(), "bases": set(), "timestamps": []
                })["definitions"] += 1
            continue

        if not compressed and tag & 0x30:
            raise FitError("reserved data record header bits set")
        local = ((tag >> 5) & 0x03) if compressed else (tag & 0x0F)
        definition = definitions[local]
        if definition is None:
            raise FitError("data record uses undefined local message")
        global_num = definition["global"]
        record_ordinal += 1
        if global_num in (20, 55):
            observation_ordinal += 1
        records[global_num] += 1
        raw_fields: dict[int, bytes] = {}
        for fnum, size, _base, _base_raw in definition["fields"]:
            if compressed and fnum == 253:
                continue
            raw_fields[fnum] = raw[need(size):cursor]
        raw_dev: dict[tuple[int, int], bytes] = {}
        for fnum, size, dev_index in definition["developer"]:
            raw_dev[(dev_index, fnum)] = raw[need(size):cursor]
        if global_num == 55 and 27 in raw_fields:
            monitoring_hr_timestamp16_records += int(26 in raw_fields)
            monitoring_hr_full_timestamp_records += int(253 in raw_fields)

        timestamp_raw = raw_fields.get(253)
        timestamp = _unsigned(timestamp_raw, definition["big"]) if timestamp_raw else None
        if compressed:
            if previous_timestamp is None:
                raise FitError("compressed timestamp without previous timestamp")
            delta = tag & 0x1F
            timestamp = (previous_timestamp & ~31) | delta
            if timestamp < previous_timestamp:
                timestamp += 32
            raw_fields[253] = timestamp.to_bytes(
                4, "big" if definition["big"] else "little"
            )
        if timestamp is not None and timestamp != 0xFFFFFFFF:
            previous_timestamp = timestamp
            month = _month(timestamp.to_bytes(4, "big" if definition["big"] else "little"),
                           definition["big"])
            if month:
                months.append(month)

        for fnum, size, base, base_raw in definition["fields"]:
            key = (global_num, fnum, None)
            stat = field_stats.setdefault(key, {"definitions": 0, "records": 0,
                                                  "valid": 0, "invalid": 0,
                                                  "timestamped_records": 0,
                                                  "sizes": set(), "bases": set(),
                                                  "timestamps": []})
            stat["records"] += 1
            stat["sizes"].add(size)
            stat["bases"].add(base)
            field_raw = raw_fields[fnum]
            valid_values, invalid_values = _value_counts(field_raw, base, definition["big"])
            stat["valid"] += valid_values
            stat["invalid"] += invalid_values
            if timestamp is not None and timestamp != 0xFFFFFFFF:
                stat["timestamped_records"] += 1
                month = _month(timestamp.to_bytes(4, "little"), False)
                if month:
                    stat["timestamps"].append(month)
            if global_num == 206:
                desc = _developer_description(raw_fields, definition["big"])
                if desc is not None:
                    developer_descriptions[(desc["developer_data_index"],
                                            desc["field_definition_number"])] = desc
            if ((global_num == 20 and fnum in (3, 4, 5, 6, 7, 73)) or
                    (global_num == 55 and fnum in (9, 27))):
                value = _decode_single(field_raw, base, definition["big"])
                if (value is not None and value not in INVALIDS.get(base, set()) and
                        timestamp is not None and timestamp != 0xFFFFFFFF and
                        (global_num != 20 or fnum != 3 or 1 <= value <= 240)):
                    source_sha = hashlib.sha256(raw).hexdigest()
                    material = f"{source_sha}:{observation_ordinal}:{global_num}:{fnum}"
                    imported_candidate_keys[(global_num, fnum)].add(
                        hashlib.sha256(material.encode()).hexdigest()
                    )

        for (dev_index, fnum), field_raw in raw_dev.items():
            key = (global_num, fnum, dev_index)
            stat = field_stats.setdefault(key, {"definitions": 0, "records": 0,
                                                  "valid": 0, "invalid": 0,
                                                  "timestamped_records": 0,
                                                  "sizes": set(), "bases": set(),
                                                  "timestamps": []})
            stat["records"] += 1
            stat["sizes"].add(len(field_raw))
            desc = developer_descriptions.get((dev_index, fnum))
            base = desc.get("fit_base_type_id", 13) if desc else 13
            stat["bases"].add(base & 0x1F)
            valid_values, invalid_values = _value_counts(
                field_raw, base & 0x1F, definition["big"]
            )
            stat["valid"] += valid_values
            stat["invalid"] += invalid_values
            if timestamp is not None and timestamp != 0xFFFFFFFF:
                stat["timestamped_records"] += 1
                month = _month(timestamp.to_bytes(4, "little"), False)
                if month:
                    stat["timestamps"].append(month)

    alias = hashlib.sha256(raw).hexdigest()[:12]
    messages = []
    for number, count in sorted(records.items()):
        msg = profile.get("messages", {}).get(number)
        messages.append({"global": number, "name": msg.get("name") if msg else None,
                         "record_count": count})
    fields_out = []
    for (global_num, field_num, dev_index), stat in sorted(
        field_stats.items(), key=lambda item: (item[0][0], item[0][2] is not None,
                                                item[0][2] or -1, item[0][1])
    ):
        msg, field = _profile_field(profile, global_num, field_num)
        dev_desc = developer_descriptions.get((dev_index, field_num)) if dev_index is not None else None
        field_name = field.get("name") if field else None
        base_type = next(iter(stat["bases"])) if len(stat["bases"]) == 1 else None
        field_size = sorted(stat["sizes"])
        timestamps = stat["timestamps"]
        fields_out.append({
            "file_alias": alias,
            "format": "FIT",
            "sha256_prefix_or_local_alias": alias,
            "file_size": len(raw),
            "fit_global_message_number": global_num,
            "fit_message_name": msg.get("name") if msg else None,
            "field_number": field_num,
            "field_name": (dev_desc.get("name") if dev_desc else field_name),
            "developer_field_number": field_num if dev_index is not None else "",
            "developer_data_index": dev_index if dev_index is not None else "",
            "base_type": BASE_TYPES.get(base_type, "unknown") if base_type is not None else "mixed",
            "field_size": "|".join(map(str, field_size)),
            "scale": _profile_attr(field, "scale"),
            "offset": _profile_attr(field, "offset"),
            "unit": _profile_attr(field, "units"),
            "definition_count": stat["definitions"],
            "record_count": stat["records"],
            "timestamped_record_count": stat["timestamped_records"],
            "missing_timestamp_record_count": (
                stat["records"] - stat["timestamped_records"]
            ),
            "valid_value_count": stat["valid"],
            "invalid_or_missing_count": (
                stat["invalid"] + max(0, records[global_num] - stat["records"])
            ),
            "first_timestamp": min(timestamps) if timestamps else "",
            "last_timestamp": max(timestamps) if timestamps else "",
            "mapping_source": "Garmin FIT Profile 21.222.0" if field else "unmapped",
            "evidence_level": "observed_in_local_file",
            "currently_imported": False,
            "notes": "; ".join(part for part in (
                _dev_note(dev_desc) if dev_index is not None else "",
                "value counts are scalar elements; invalid/missing combines sentinels " +
                "with global-message records where the field is absent",
            ) if part),
        })
    return {
        "file_alias": alias, "format": "FIT", "size": len(raw),
        "sha256": hashlib.sha256(raw).hexdigest(),
        "message_counts": messages, "fields": fields_out,
        "record_count": sum(records.values()),
        "first_month": min(months) if months else "",
        "last_month": max(months) if months else "",
        "monitoring_hr_timestamp16_records": monitoring_hr_timestamp16_records,
        "monitoring_hr_full_timestamp_records": monitoring_hr_full_timestamp_records,
        "import_candidate_keys": dict(imported_candidate_keys),
    }


def _profile_attr(field: dict[str, Any] | None, key: str) -> str:
    if not field:
        return ""
    value = field.get(key, "")
    if isinstance(value, (list, tuple)):
        return "|".join(map(str, value))
    return str(value)


def _decode_single(raw: bytes, base: int, big: bool):
    width = BASE_TYPE_SIZES.get(base)
    if width is None or len(raw) != width:
        return None
    return int.from_bytes(raw, "big" if big else "little",
                          signed=base in (1, 3, 5, 14))


def _developer_description(fields: dict[int, bytes], big: bool):
    # FIT global message 206 (field_description); parse profile-standard fields.
    idx = _decode_scalar(fields.get(0, b""), 2, big)
    field_num = _decode_scalar(fields.get(1, b""), 2, big)
    base = _decode_scalar(fields.get(2, b""), 2, big)
    if idx is None or field_num is None:
        return None
    def text(field: int):
        raw = fields.get(field, b"")
        return raw.split(b"\0", 1)[0].decode("utf-8", errors="replace") or None
    scale = _decode_scalar(fields.get(6, b""), 2, big)
    offset = _decode_scalar(fields.get(7, b""), 1, big)
    return {
        "developer_data_index": idx, "field_definition_number": field_num,
        "fit_base_type_id": base, "name": text(3), "units": text(8),
        "scale": scale, "offset": offset,
    }


def _dev_note(desc: dict[str, Any] | None) -> str:
    if not desc:
        return "developer field definition unavailable"
    return "developer field; metadata observed in global message 206"


def inventory_nonfit(path: Path, raw: bytes):
    alias = hashlib.sha256(raw).hexdigest()[:12]
    result = {
        "file_alias": alias, "format": "unknown_non_fit", "size": len(raw),
        "sha256": hashlib.sha256(raw).hexdigest(),
        "message_counts": [], "fields": [], "record_count": 0,
        "first_month": "", "last_month": "",
    }
    if raw.startswith(b"<?xml"):
        try:
            root = ET.fromstring(raw)
            tags = Counter(element.tag.rsplit("}", 1)[-1] for element in root.iter())
            result["format"] = "XML"
            result["xml_root_tag"] = root.tag.rsplit("}", 1)[-1]
            result["xml_element_counts"] = dict(sorted(tags.items()))
            result["xml_element_count"] = sum(tags.values())
            result["xml_attribute_names"] = sorted({
                key.rsplit("}", 1)[-1] for element in root.iter() for key in element.attrib
            })
        except ET.ParseError:
            result["format"] = "XML_malformed"
    elif raw and raw[0] >> 3 in range(1, 16):
        shape = _protobuf_shape(raw)
        if shape is not None:
            result["format"] = "protobuf_schema_unknown"
            result["protobuf_fields"] = shape
        else:
            result["format"] = "binary_unknown"
    return result


def _protobuf_shape(data: bytes):
    """Validate wire framing and return field numbers/wire types only."""
    def varint(offset: int):
        value = 0
        for shift in range(0, 70, 7):
            if offset >= len(data):
                raise ValueError("truncated varint")
            byte = data[offset]
            offset += 1
            value |= (byte & 0x7F) << shift
            if byte < 0x80:
                return value, offset
        raise ValueError("oversized varint")

    def parse(blob: bytes, depth: int, path: str, output: list[dict[str, Any]]):
        pos = 0
        fields: Counter[tuple[int, int]] = Counter()
        lengths: dict[tuple[int, int], list[int]] = defaultdict(list)
        nested: list[tuple[int, bytes]] = []
        while pos < len(blob):
            key, pos = varint(pos)
            number, wire = key >> 3, key & 7
            if number == 0 or wire in (3, 4) or wire not in (0, 1, 2, 5):
                raise ValueError("invalid protobuf key")
            fields[(number, wire)] += 1
            if wire == 0:
                _, pos = varint(pos)
            elif wire in (1, 5):
                size = 8 if wire == 1 else 4
                if pos + size > len(blob):
                    raise ValueError("truncated fixed field")
                pos += size
            else:
                size, pos = varint(pos)
                if pos + size > len(blob):
                    raise ValueError("truncated length-delimited field")
                value = blob[pos:pos + size]
                pos += size
                lengths[(number, wire)].append(size)
                if depth < 8 and value:
                    nested.append((number, value))
        output.append({
            "path": path or "root",
            "field_shapes": [
                {"field_number": number, "wire_type": wire, "occurrences": count,
                 "lengths_bytes": sorted(lengths.get((number, wire), []))}
                for (number, wire), count in sorted(fields.items())
            ],
        })
        for number, value in nested:
            try:
                parse(value, depth + 1, f"{path}.{number}" if path else str(number), output)
            except ValueError:
                # Opaque length-delimited bytes are not assumed to be submessages.
                continue

    summaries: list[dict[str, Any]] = []
    try:
        parse(data, 0, "", summaries)
    except ValueError:
        return None
    return summaries


def sqlite_imported(path: Path | None):
    if path is None:
        return {}, {}
    uri = f"file:{path.resolve()}?mode=ro"
    con = sqlite3.connect(uri, uri=True)
    try:
        integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
        counts = dict(con.execute(
            "SELECT metric, COUNT(*) FROM readings GROUP BY metric"
        ).fetchall())
        duplicate_keys = con.execute(
            """SELECT COUNT(*) FROM (
                SELECT device_id, metric, measured_at_ms, sample_id, COUNT(*) AS n
                FROM readings GROUP BY device_id, metric, measured_at_ms, sample_id
                HAVING n > 1
            )"""
        ).fetchone()[0]
        sample_ids = {r[0] for r in con.execute(
            "SELECT sample_id FROM readings WHERE sample_id != ''"
        )}
        return {"integrity_check": integrity, "metric_counts": counts,
                "reading_count": sum(counts.values()),
                "duplicate_primary_keys": duplicate_keys}, sample_ids
    finally:
        con.close()


def inventory(input_dir: Path, profile_path: Path, database: Path | None):
    profile = profile_from_python(profile_path)
    files = []
    raw_dir = input_dir / "verified_fit"
    for path in sorted(raw_dir.glob("*.fit")):
        raw = path.read_bytes()
        files.append(inventory_fit(path, raw, profile))
    for path in sorted((input_dir / "verified_raw").glob("*")):
        if path.is_file():
            files.append(inventory_nonfit(path, path.read_bytes()))
    if not files:
        raise FitError("no files found under verified_fit/ or verified_raw/")

    _db_summary, sample_ids = sqlite_imported(database)
    candidate_total = 0
    matched_total = 0
    for file in files:
        candidate_keys = file.get("import_candidate_keys", {})
        candidate_total += sum(len(keys) for keys in candidate_keys.values())
        matched_total += sum(len(keys & sample_ids) for keys in candidate_keys.values())
        for row in file["fields"]:
            global_num, fnum = row["fit_global_message_number"], row["field_number"]
            keys = candidate_keys.get((global_num, fnum), set())
            if keys:
                matched = len(keys & sample_ids)
                row["currently_imported"] = matched > 0
                row["imported_value_count"] = matched
                row["notes"] = (row["notes"] + "; " if row["notes"] else "") + \
                    f"SQLite persisted-key matches: {matched}/{len(keys)}"
        file.pop("import_candidate_keys", None)
    if database is not None:
        _db_summary["decoder_candidate_keys"] = candidate_total
        _db_summary["decoder_candidate_keys_in_sqlite"] = matched_total
    return files, _db_summary, profile["version"]


CSV_COLUMNS = [
    "file_alias", "format", "sha256_prefix_or_local_alias", "file_size",
    "fit_global_message_number", "fit_message_name", "field_number",
    "field_name", "developer_field_number", "developer_data_index", "base_type",
    "field_size", "scale", "offset", "unit", "definition_count", "record_count",
    "timestamped_record_count", "missing_timestamp_record_count",
    "valid_value_count", "invalid_or_missing_count", "first_timestamp",
    "last_timestamp", "mapping_source", "evidence_level", "currently_imported",
    "imported_value_count", "notes",
]


def write_outputs(files, db_summary, profile_version, out: Path):
    out.mkdir(parents=True, exist_ok=True)
    flattened = [row for file in files for row in file["fields"]]
    for file in files:
        if file["format"] == "FIT":
            continue
        notes = "content not decoded or copied into inventory"
        if file["format"] == "XML":
            notes += "; root=" + file["xml_root_tag"]
            notes += "; element_counts=" + json.dumps(
                file["xml_element_counts"], sort_keys=True, separators=(",", ":")
            )
            notes += "; attribute_names=" + ",".join(file["xml_attribute_names"])
        elif file["format"] == "protobuf_schema_unknown":
            notes += "; protobuf framing valid; schema unknown; field numbers/wire types only in JSON"
        flattened.append({
            "file_alias": file["file_alias"],
            "format": file["format"],
            "sha256_prefix_or_local_alias": file["file_alias"],
            "file_size": file["size"],
            "mapping_source": "local byte-structure inspection",
            "evidence_level": "observed_in_local_file",
            "currently_imported": False,
            "imported_value_count": 0,
            "notes": notes,
        })
    with (out / "fit-field-inventory.csv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_COLUMNS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(flattened)
    safe_files = []
    for file in files:
        safe_files.append({
            "file_alias": file["file_alias"],
            "format": file["format"],
            "size": file["size"],
            "message_counts": file["message_counts"],
            "field_row_count": len(file["fields"]),
            "record_count": file["record_count"],
            "first_month": file["first_month"],
            "last_month": file["last_month"],
            "monitoring_hr_timestamp16_records": file.get("monitoring_hr_timestamp16_records", 0),
            "monitoring_hr_full_timestamp_records": file.get("monitoring_hr_full_timestamp_records", 0),
            **({"xml_root_tag": file["xml_root_tag"],
                "xml_element_counts": file["xml_element_counts"],
                "xml_element_count": file["xml_element_count"],
                "xml_attribute_names": file["xml_attribute_names"]}
               if file["format"] == "XML" else {}),
            **({"protobuf_fields": file["protobuf_fields"]}
               if file["format"] == "protobuf_schema_unknown" else {}),
        })
    with (out / "inventory.json").open("w", encoding="utf-8") as f:
        json.dump({"profile_version": profile_version, "files": safe_files,
                   "sqlite_summary": db_summary}, f, ensure_ascii=False, indent=2)
    _write_markdown_matrices(files, db_summary, profile_version, out / "inventory-matrices.md")


def _md_cell(value: Any) -> str:
    text = str(value if value not in (None, "") else "—")
    return text.replace("|", "\\|").replace("\n", " ")


def _write_markdown_matrices(files, db_summary, profile_version, destination: Path):
    lines = [
        "# CIRQA offline inventory matrices",
        "",
        f"FIT Profile reference: {profile_version.get('major')}.{profile_version.get('minor')}."
        f"{profile_version.get('patch')}. This is a schema reference, not decoded value output.",
        "",
        "No measurement values or full timestamps are included. File aliases are local "
        "12-character SHA-256 prefixes. `valid` and `invalid/missing` are encoded-element "
        "counts; absence counts are based on message records, as described in the main report.",
        "",
        "## File matrix",
        "",
        "| Alias | Format | Bytes | FIT data records | Observed messages / records | Month range | Quality / note |",
        "| --- | --- | ---: | ---: | --- | --- | --- |",
    ]
    for file in files:
        messages = ", ".join(
            f"{m['global']}:{m['name'] or 'unknown'} ({m['record_count']})"
            for m in file["message_counts"]
        ) or "—"
        if file["format"] == "FIT":
            quality = "CRC-validated in private source set"
            month = (file["first_month"] + "–" + file["last_month"]
                     if file["first_month"] else "no complete timestamp")
        elif file["format"] == "XML":
            quality = (f"well-formed XML; root={file['xml_root_tag']}; "
                       f"{file['xml_element_count']} elements; values omitted")
            month = "—"
        else:
            quality = "protobuf wire framing valid; schema/semantics unknown"
            month = "—"
        lines.append(
            f"| {file['file_alias']} | {file['format']} | {file['size']} | "
            f"{file['record_count']} | {_md_cell(messages)} | {month} | {quality} |"
        )

    signatures: dict[tuple, dict[str, Any]] = {}
    for file in files:
        for row in file["fields"]:
            key = tuple(row.get(k, "") for k in (
                "fit_global_message_number", "fit_message_name", "field_number",
                "field_name", "developer_field_number", "developer_data_index",
                "base_type", "field_size", "scale", "offset", "unit", "mapping_source",
            ))
            aggregate = signatures.setdefault(key, {
                "files": set(), "definitions": 0, "records": 0, "valid": 0,
                "invalid": 0, "timestamped": 0, "missing_timestamp": 0,
                "imported": 0,
            })
            aggregate["files"].add(file["file_alias"])
            for output_key, input_key in (
                ("definitions", "definition_count"), ("records", "record_count"),
                ("valid", "valid_value_count"), ("invalid", "invalid_or_missing_count"),
                ("timestamped", "timestamped_record_count"),
                ("missing_timestamp", "missing_timestamp_record_count"),
                ("imported", "imported_value_count"),
            ):
                aggregate[output_key] += int(row.get(input_key) or 0)

    lines.extend([
        "",
        "## FIT message/field matrix",
        "",
        "One row per observed message/field signature. Unmapped profile entries retain "
        "their observed numeric ID and base type. Repeated/variable definitions are "
        "represented as separate rows when size, scale, offset, or unit differs.",
        "",
        "| Global ID | Message (Profile) | Field ID | Field (Profile) | Dev field / index | Observed base type | Size B | Scale | Offset | Unit | Files | Definitions | Records | Valid elements | Invalid/missing | Timestamped records | Missing timestamp | SQLite imported | Mapping / decoder status |",
        "| ---: | --- | ---: | --- | --- | --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
    ])
    for key, agg in sorted(signatures.items(), key=lambda item: (
        item[0][0], item[0][5] != "", item[0][5] or "", item[0][2], item[0][6]
    )):
        (global_num, message, field_num, field_name, dev_field, dev_index,
         base, size, scale, offset, unit, source) = key
        import_status = (
            f"{agg['imported']}/{agg['valid']} matched"
            if agg["imported"] else "not imported in observed SQLite rows"
        )
        lines.append("| " + " | ".join(_md_cell(x) for x in (
            global_num, message, field_num, field_name,
            (f"{dev_field}/{dev_index}" if dev_index != "" else "—"),
            base, size, scale, offset, unit, len(agg["files"]),
            agg["definitions"], agg["records"], agg["valid"], agg["invalid"],
            agg["timestamped"], agg["missing_timestamp"], agg["imported"],
            f"{source}; {import_status}",
        )) + " |")

    lines.extend([
        "",
        "## SQLite import coverage",
        "",
        f"Read-only database check: `{db_summary.get('integrity_check', 'not run')}`; "
        f"{db_summary.get('reading_count', 0)} rows; "
        f"{db_summary.get('duplicate_primary_keys', 'not checked')} duplicate primary keys.",
        "",
        f"Current decoder candidate keys: {db_summary.get('decoder_candidate_keys', 0)}; "
        f"matching keys in SQLite: {db_summary.get('decoder_candidate_keys_in_sqlite', 0)}. "
        "Counts and key matches are shown without exporting values or exact timestamps.",
        "",
    ])
    destination.write_text("\n".join(lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, required=True,
                        help="private extracted directory containing verified_fit/ and verified_raw/")
    parser.add_argument("--profile", type=Path, required=True,
                        help="trusted Garmin FIT Profile.py literal data file; never executed")
    parser.add_argument("--sqlite", type=Path,
                        help="optional read-only copy of pulselibre_readings.db")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    files, db_summary, profile_version = inventory(args.input_dir, args.profile, args.sqlite)
    write_outputs(files, db_summary, profile_version, args.output)
    print(json.dumps({
        "files": len(files),
        "profile_version": profile_version,
        "formats": dict(Counter(file["format"] for file in files)),
        "fit_files": sum(file["format"] == "FIT" for file in files),
        "fit_bytes": sum(file["size"] for file in files if file["format"] == "FIT"),
        "raw_bytes": sum(file["size"] for file in files if file["format"] != "FIT"),
        "db": db_summary,
        "output": str(args.output),
    }, ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except (FitError, OSError, sqlite3.Error) as exc:
        print(f"inventory failed: {exc}", file=sys.stderr)
        raise SystemExit(2)
