#!/usr/bin/env python3
"""Offline Garmin ML/GFDI/FileAccess replay of an Android H4 btsnoop capture (#506, #637, #148).

Layers: HCI ACL -> L2CAP -> ATT (write command / notification) -> Garmin ML/MLR (2-byte header)
-> COBS -> GFDI frame (CRC-16/ARC) -> 5043/5044 protobuf transport -> Smart / FileAccess messages,
plus MLR file transfers (config ``00 00 <u32 handle>`` -> data -> zlib) correlated to PullItem via
the transfer handle.

Output is redacted: counts, field numbers, schema-like type labels and reconstruction status only.
It never prints UIDs, flag UUIDs, tokens, hashes, payload values or FIT contents. Read-only: no device access.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import re
import struct
import sys
import zlib
from collections import Counter, defaultdict
from pathlib import Path

_SPEC = importlib.util.spec_from_file_location("garmin_hci_replay", Path(__file__).with_name("garmin_hci_replay.py"))
hci = importlib.util.module_from_spec(_SPEC)
sys.modules.setdefault(_SPEC.name, hci)
_SPEC.loader.exec_module(hci)

PARSER_VERSION = "1"
GFDI_PROTOBUF = (5043, 5044)
SAFE_LABEL = re.compile(r"^[A-Za-z][A-Za-z0-9_]{0,40}$")
MAX_MESSAGE = 1 << 20


def cobs_decode(data: bytes) -> bytes | None:
    out = bytearray()
    i = 0
    while i < len(data):
        code = data[i]
        i += 1
        if code == 0 or i + code - 1 > len(data):
            return None
        out += data[i:i + code - 1]
        i += code - 1
        if code < 0xFF and i < len(data):
            out.append(0)
    return bytes(out)


def crc16_arc(data: bytes) -> int:
    crc = 0
    for byte in data:
        crc ^= byte
        for _ in range(8):
            crc = (crc >> 1) ^ 0xA001 if crc & 1 else crc >> 1
    return crc


def gfdi_type(raw: int) -> int:
    """Compact GFDI types set bit 15; the low byte is the offset from 5000."""
    return (raw & 0xFF) + 5000 if raw & 0x8000 else raw


def _varint(data: bytes, i: int) -> tuple[int, int]:
    value = shift = 0
    while True:
        if i >= len(data) or shift > 63:
            raise ValueError("varint")
        b = data[i]
        i += 1
        value |= (b & 0x7F) << shift
        shift += 7
        if b < 0x80:
            return value, i


def pb_fields(data: bytes) -> list[tuple[int, int, object]] | None:
    """Canonical protobuf field list or None when the bytes are not a complete message."""
    out = []
    i = 0
    try:
        while i < len(data):
            key, i = _varint(data, i)
            number, wire = key >> 3, key & 7
            if number == 0:
                return None
            if wire == 0:
                value, i = _varint(data, i)
            elif wire == 2:
                length, i = _varint(data, i)
                if i + length > len(data):
                    return None
                value = data[i:i + length]
                i += length
            elif wire == 5:
                value = data[i:i + 4]
                i += 4
            elif wire == 1:
                value = data[i:i + 8]
                i += 8
            else:
                return None
            out.append((number, wire, value))
    except ValueError:
        return None
    return out if i == len(data) else None


def _get(fields, number):
    return [v for n, _, v in fields or [] if n == number]


class Replay:
    def __init__(self) -> None:
        self.connection = 0
        self.pending: dict = {}
        self.cobs: dict = defaultdict(bytearray)
        self.assembly: dict = {}
        self.transfers: list[dict] = []
        self.current_transfer: dict = {}
        self.pull_uid: dict = {}
        self.pull_handle: dict = {}
        self.items: dict = {}
        self.labels: dict = {}
        self.c = Counter()
        self.gfdi = Counter()
        self.smart = Counter()
        self.fa = Counter()
        self.list_types: Counter = Counter()
        self.push_types: Counter = Counter()
        self.pull_types: Counter = Counter()
        self.flag_types: Counter = Counter()
        self.list_request_shapes: Counter = Counter()
        self.ml_services: dict = defaultdict(Counter)
        self.att_pairs: dict = defaultdict(Counter)

    # ----- transport -----
    def record(self, r) -> None:
        p = r.packet
        if r.packet_type == 4 and len(p) > 3 and p[0] == 0x3E and p[2] in (0x01, 0x0A):
            self.connection += 1
            self.c["le_connections"] += 1
            return
        if r.packet_type != 2 or not r.complete or len(p) < 4:
            return
        raw, _ = struct.unpack_from("<HH", p)
        handle, pb, payload = raw & 0xFFF, (raw >> 12) & 3, p[4:]
        key = (handle, r.direction)
        if pb in (0, 2):
            if len(payload) < 4:
                return
            expected, cid = struct.unpack_from("<HH", payload)
            body = bytearray(payload[4:])
            if len(body) < expected:
                self.pending[key] = (expected, cid, body)
                return
        else:
            if key not in self.pending:
                return
            expected, cid, body = self.pending[key]
            body += payload
            if len(body) < expected:
                return
            del self.pending[key]
        if cid != 4 or len(body) < 4 or body[0] not in (0x52, 0x1B, 0x12):
            return
        att_handle = struct.unpack_from("<H", body, 1)[0]
        host = r.direction == "host_to_controller"
        self.att_pairs[self.connection]["write" if host else "notify", att_handle] += 1
        self.ml(host, bytes(body[3:]))

    def ml(self, host: bool, value: bytes) -> None:
        if not value:
            return
        if value[0] == 0:  # ML management
            if len(value) >= 13 and value[1] == 1:
                service = struct.unpack_from("<H", value, 10)[0]
                self.ml_services[self.connection][f"0x{service:04x}"] += 1
            self.c["ml_management"] += 1
            return
        if not value[0] & 0x80 or len(value) <= 2:
            self.c["mlr_control"] += 1
            return
        index, data = (value[0] >> 4) & 7, value[2:]
        tkey = (self.connection, index)
        if host and len(data) == 6 and data[0] == 0 and data[1] == 0:
            transfer = {"conn": self.connection, "handle": struct.unpack_from("<I", data, 2)[0],
                        "buf": bytearray(), "answered": False}
            self.current_transfer[tkey] = transfer
            self.transfers.append(transfer)
            return
        transfer = self.current_transfer.get(tkey)
        if transfer is not None and not host:
            if data[0] != 0:
                transfer["buf"] += data
                return
            if not transfer["answered"] and len(data) <= 4:
                transfer["answered"] = True
                return
        self.cobs_stream((self.connection, host, index), data)

    def cobs_stream(self, key, data: bytes) -> None:
        buf = self.cobs[key]
        buf += data
        while buf:
            if buf[0] != 0:
                j = buf.find(0)
                if j < 0:
                    buf.clear()
                    return
                del buf[:j]
                continue
            j = buf.find(0, 1)
            if j < 0:
                return
            encoded = bytes(buf[1:j])
            del buf[:j + 1]
            frame = cobs_decode(encoded) if encoded else None
            if frame and len(frame) >= 6:
                self.frame(key[1], frame)

    def frame(self, host: bool, frame: bytes) -> None:
        length, raw_type = struct.unpack_from("<HH", frame)
        if length != len(frame) or crc16_arc(frame[:-2]) != struct.unpack_from("<H", frame, len(frame) - 2)[0]:
            self.c["gfdi_crc_invalid"] += 1
            return
        kind = gfdi_type(raw_type)
        self.gfdi[("host" if host else "device", kind)] += 1
        if kind not in GFDI_PROTOBUF or len(frame) < 20:
            return
        body = frame[4:-2]
        rid, offset, total, chunk = struct.unpack_from("<HIII", body)
        data = body[14:14 + chunk]
        akey = (self.connection, host, kind, rid)
        if offset == 0:
            if total > MAX_MESSAGE:
                return
            self.assembly[akey] = bytearray()
        if akey not in self.assembly:
            return
        self.assembly[akey] += data
        if len(self.assembly[akey]) >= total:
            self.message(host, kind, rid, bytes(self.assembly.pop(akey)))

    # ----- protobuf -----
    def label(self, reference_fields) -> str:
        dtype = pb_fields((_get(reference_fields, 2) or [b""])[0]) or []
        label = _get(dtype, 2)
        key = _get(dtype, 3)
        if label:
            try:
                text = label[0].decode("ascii")
            except UnicodeDecodeError:
                text = ""
            if SAFE_LABEL.match(text):
                if key:
                    self.labels[(self.connection, key[0])] = text
                return text
        if key and (self.connection, key[0]) in self.labels:
            return self.labels[(self.connection, key[0])]
        fmt = _get(dtype, 1)
        return f"format_{fmt[0]}" if fmt else "unlabelled"

    def reference(self, ref: bytes) -> tuple[bytes | None, str]:
        f = pb_fields(ref) or []
        uid = _get(f, 1)
        label = self.label(f)
        if uid:
            self.items[uid[0]] = {"label": label, "size": (_get(f, 3) or [None])[0]}
        return (uid[0] if uid else None), label

    def message(self, host: bool, kind: int, rid: int, message: bytes) -> None:
        who = "host" if host else "device"
        for number, _, value in pb_fields(message) or []:
            if number != 43:
                self.smart[(who, kind, number)] += 1
                continue
            sub = pb_fields(value) or []
            if not sub:
                continue
            op, _, body = sub[0]
            self.fa[(who, kind, op)] += 1
            f = pb_fields(body) if isinstance(body, bytes) else None
            if op == 9 and host:
                self.list_request_shapes[",".join(str(n) for n, _, _ in f or [])] += 1
            elif op == 10 and not host:
                for ref in _get(f, 4):
                    self.list_types[self.reference(ref)[1]] += 1
            elif op == 12 and not host:
                for ref in _get(f, 1):
                    self.push_types[self.reference(ref)[1]] += 1
            elif op == 1 and host:
                item = pb_fields((_get(f, 1) or [b""])[0]) or []
                uid = _get(item, 1)
                if uid:
                    self.pull_uid[(self.connection, rid)] = uid[0]
                    self.pull_types[self.items.get(uid[0], {}).get("label", "unlisted")] += 1
            elif op == 2 and not host:
                self.pull_handle[(self.connection, rid)] = (_get(f, 3) or [None])[0]
            elif op == 15 and host:
                uid = _get(f, 1)
                self.flag_types[self.items.get(uid[0], {}).get("label", "unlisted") if uid else "?"] += 1

    # ----- summary -----
    def summary(self) -> dict:
        by_handle = {}
        for key, uid in self.pull_uid.items():
            if key in self.pull_handle:
                by_handle[(key[0], self.pull_handle[key])] = uid
        status = Counter()
        for t in self.transfers:
            uid = by_handle.get((t["conn"], t["handle"]))
            if uid is None:
                status["uncorrelated"] += 1
                continue
            size = self.items.get(uid, {}).get("size")
            content = None
            for bits in (15, -15, 47):
                try:
                    d = zlib.decompressobj(bits)
                    content = d.decompress(bytes(t["buf"]))
                    if d.eof:
                        break
                    content = None
                except zlib.error:
                    content = None
            if content is None:
                status["zlib_incomplete"] += 1
            elif size is not None and len(content) == size:
                status["size_exact" + ("_fit" if content[8:12] == b".FIT" else "_nonfit")] += 1
            else:
                status["size_mismatch"] += 1
        def fmt(counter: Counter) -> dict:
            return {"/".join(map(str, key)): value for key, value in sorted(counter.items(), key=str)}
        return {
            "schema": "pulselibre.garmin_fileaccess_replay.v1",
            "parser_version": PARSER_VERSION,
            "redaction": "counts, field numbers and schema labels only; no UIDs, UUIDs, hashes or values",
            "connections": self.connection,
            "att_pairs": {str(c): {f"{k[0]}:0x{k[1]:04x}": v for k, v in sorted(p.items())} for c, p in self.att_pairs.items()},
            "ml_registered_services": {str(c): dict(sorted(s.items())) for c, s in self.ml_services.items()},
            "gfdi_frames": fmt(self.gfdi),
            "gfdi_crc_invalid": self.c["gfdi_crc_invalid"],
            "smart_messages": fmt(self.smart),
            "fileaccess_messages": fmt(self.fa),
            "item_list_request_shapes": dict(self.list_request_shapes),
            "listed_types": dict(sorted(self.list_types.items())),
            "pushed_types": dict(sorted(self.push_types.items())),
            "pulled_types": dict(sorted(self.pull_types.items())),
            "set_flags_types": dict(sorted(self.flag_types.items())),
            "mlr_transfers": len(self.transfers),
            "mlr_transfer_status": dict(sorted(status.items())),
        }


def replay_file(path: Path) -> dict:
    with path.open("rb") as stream:
        _, records = hci.iter_records(stream)
        replay = Replay()
        for record in records:
            replay.record(record)
    return replay.summary()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("capture", type=Path)
    parser.add_argument("--summary", type=Path, help="write the redacted JSON summary here")
    args = parser.parse_args()
    result = replay_file(args.capture)
    text = json.dumps(result, indent=2, sort_keys=True)
    if args.summary:
        args.summary.write_text(text + "\n", encoding="utf-8")
    else:
        print(text)


if __name__ == "__main__":
    main()
