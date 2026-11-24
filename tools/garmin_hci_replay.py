#!/usr/bin/env python3
"""Bounded, metadata-only replay of Android H4 btsnoop captures.

The replay reconstructs HCI ACL -> L2CAP -> ATT. It intentionally does not
print, hash, or persist ATT values. Proprietary ML/GFDI parsing is not inferred
from a UUID or from an unverified handle; this tool reports only what its
transport parser can prove.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import struct
from collections import deque
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import BinaryIO, Iterator

MAGIC = b"btsnoop\x00"
BT_EPOCH_UNIX_MICROS = 0x00DC_DDB3_0F2F_8000
MAX_RECORD_SIZE = 16 * 1024 * 1024
MAX_L2CAP_PDU = 1024 * 1024
MAX_OPEN_ASSEMBLIES = 256
MAX_REDACTED_EVENTS = 200_000
MAX_FRAGMENT_AGE_MICROS = 30_000_000
PARSER_VERSION = "2"
LOGCAT_THREADTIME = re.compile(rb"^\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d+")
ATT_NAMES = {
    0x01: "error_response", 0x02: "exchange_mtu_request",
    0x03: "exchange_mtu_response", 0x04: "find_information_request",
    0x05: "find_information_response", 0x06: "find_by_type_value_request",
    0x07: "find_by_type_value_response", 0x08: "read_by_type_request",
    0x09: "read_by_type_response", 0x0A: "read_request",
    0x0B: "read_response", 0x0C: "read_blob_request",
    0x0D: "read_blob_response", 0x0E: "read_multiple_request",
    0x0F: "read_multiple_response", 0x10: "read_by_group_type_request",
    0x11: "read_by_group_type_response", 0x12: "write_request",
    0x13: "write_response", 0x16: "prepare_write_request",
    0x17: "prepare_write_response", 0x18: "execute_write_request",
    0x19: "execute_write_response", 0x1B: "notification",
    0x1D: "indication", 0x1E: "confirmation", 0x52: "write_command",
}
ATT_MIN_LENGTHS = {
    0x01: 5, 0x02: 3, 0x03: 3, 0x04: 5, 0x05: 2, 0x06: 7, 0x07: 1,
    0x08: 7, 0x09: 2, 0x0A: 3, 0x0B: 1, 0x0C: 5, 0x0D: 1,
    0x0E: 5, 0x0F: 1, 0x10: 7, 0x11: 2, 0x12: 3, 0x13: 1,
    0x16: 5, 0x17: 5, 0x18: 2, 0x19: 1, 0x1B: 3, 0x1D: 3,
    0x1E: 1, 0x52: 3,
}


class ReplayError(ValueError):
    pass


@dataclass(frozen=True)
class Record:
    index: int
    offset: int
    original_len: int
    included_len: int
    flags: int
    drops: int
    timestamp_micros: int
    packet_type: int | None
    packet: bytes

    @property
    def direction(self) -> str:
        # btsnoop HCI flag bit 0: 0 = sent by host, 1 = received by host.
        return "host_to_controller" if self.flags & 1 == 0 else "controller_to_host"

    @property
    def timestamp_utc(self) -> str | None:
        unix_micros = self.timestamp_micros - BT_EPOCH_UNIX_MICROS
        if unix_micros < 0:
            return None
        seconds, micros = divmod(unix_micros, 1_000_000)
        return datetime.fromtimestamp(seconds, timezone.utc).replace(
            microsecond=micros
        ).isoformat().replace("+00:00", "Z")

    @property
    def complete(self) -> bool:
        return self.original_len == self.included_len


def iter_records(stream: BinaryIO) -> tuple[int, Iterator[Record]]:
    header = stream.read(16)
    if len(header) != 16 or header[:8] != MAGIC:
        raise ReplayError("invalid or truncated btsnoop header")
    version, datalink = struct.unpack(">II", header[8:])
    if version != 1 or datalink != 1002:
        raise ReplayError("only btsnoop v1 H4 datalink 1002 is supported")

    def records() -> Iterator[Record]:
        index = 0
        while True:
            offset = stream.tell()
            raw_header = stream.read(24)
            if not raw_header:
                return
            if len(raw_header) != 24:
                raise ReplayError("truncated btsnoop record header")
            original, included, flags, drops, timestamp = struct.unpack(
                ">IIIIQ", raw_header
            )
            if included > original or included > MAX_RECORD_SIZE:
                raise ReplayError("invalid or oversized btsnoop record length")
            payload = stream.read(included)
            if len(payload) != included:
                raise ReplayError("truncated btsnoop record payload")
            packet_type = payload[0] if payload else None
            index += 1
            yield Record(
                index, offset, original, included, flags, drops, timestamp,
                packet_type, payload[1:] if payload else b"",
            )
    return datalink, records()


def uuid_from_att(raw: bytes) -> str:
    if len(raw) == 2:
        return f"0000{int.from_bytes(raw, 'little'):04x}-0000-1000-8000-00805f9b34fb"
    if len(raw) == 16:
        # Bluetooth ATT transmits the 128-bit UUID in little-endian byte order.
        b = raw[::-1].hex()
        return f"{b[:8]}-{b[8:12]}-{b[12:16]}-{b[16:20]}-{b[20:]}"
    return "unresolved"


def _u16(data: bytes, offset: int) -> int:
    return struct.unpack_from("<H", data, offset)[0]


class Replay:
    def __init__(self) -> None:
        self.counts: Counter[str] = Counter()
        self.packet_counts: Counter[str] = Counter()
        self.att_counts: Counter[str] = Counter()
        self.error_counts: Counter[str] = Counter()
        self.direction_opcodes: dict[str, Counter[str]] = defaultdict(Counter)
        self.acl_boundary_flags: dict[str, Counter[str]] = defaultdict(Counter)
        self.events: list[dict[str, object]] = []
        self.active_epochs: dict[int, str] = {}
        self.epoch_serial = 0
        self.att_epochs: set[str] = set()
        self.unanchored_att_pdus = 0
        self.dropped_events = 0
        self.pending: dict[tuple[str, int, str], dict[str, object]] = {}
        # Handles are local to a GATT server database. Both peers may perform
        # discovery on one HCI connection, so scope the map by the direction
        # of server -> client discovery responses as well as link epoch.
        self.characteristics: dict[tuple[str, str, int], str] = {}
        self.characteristic_services: dict[tuple[str, str, int], str | None] = {}
        self.services: dict[tuple[str, str], list[tuple[int, int, str]]] = defaultdict(list)
        self.read_by_group_requests: dict[tuple[str, str], deque[str]] = defaultdict(deque)
        self.read_by_type_requests: dict[tuple[str, str], deque[str]] = defaultdict(deque)
        self.att_error_response_request_opcodes: Counter[int] = Counter()
        self.att_error_response_queue_consumed: Counter[str] = Counter()
        self.att_error_response_queue_unmatched: Counter[str] = Counter()
        self.last_timestamp: int | None = None
        self.first_record: Record | None = None
        self.last_record: Record | None = None
        self.ml_characteristic_value_counts: Counter[tuple[str, str, str]] = Counter()
        self.ml_registration_requests: dict[str, deque[int]] = defaultdict(deque)
        self.ml_control_responses: Counter[tuple[int, int, str]] = Counter()
        self.unmatched_ml_control_candidates = 0

    def run(self, records: Iterator[Record]) -> dict[str, object]:
        for record in records:
            self.counts["records"] += 1
            if self.first_record is None:
                self.first_record = record
            self.last_record = record
            if self.last_timestamp is not None and record.timestamp_micros < self.last_timestamp:
                self.error_counts["non_monotonic_btsnoop_time"] += 1
            self._expire_fragments(record.timestamp_micros)
            self.last_timestamp = record.timestamp_micros
            if record.drops:
                self.error_counts["records_with_drops"] += 1
                if self.pending:
                    self.error_counts["acl_assemblies_interrupted_by_drops"] += len(self.pending)
                    self.pending.clear()
            if not record.complete:
                self.error_counts["truncated_records"] += 1
            packet_name = {1: "command", 2: "acl", 3: "sco", 4: "event", 5: "iso"}.get(
                record.packet_type, "empty" if record.packet_type is None else "unknown"
            )
            self.packet_counts[packet_name] += 1
            if record.packet_type == 4 and record.complete:
                self._hci_event(record)
            elif record.packet_type == 2:
                self._acl(record)
        self.counts["att_pdus"] = sum(self.att_counts.values())
        self.counts["complete_l2cap_pdus"] = self.counts["l2cap_complete"]
        return {
            "parser_version": PARSER_VERSION,
            "records": self.counts["records"],
            "first_record_utc": self.first_record.timestamp_utc if self.first_record else None,
            "last_record_utc": self.last_record.timestamp_utc if self.last_record else None,
            "packet_types": dict(sorted(self.packet_counts.items())),
            "att_opcodes": dict(sorted(self.att_counts.items())),
            "att_direction_opcodes": {
                direction: dict(sorted(counter.items()))
                for direction, counter in sorted(self.direction_opcodes.items())
            },
            "acl_packet_boundary_flags": {
                direction: dict(sorted(counter.items()))
                for direction, counter in sorted(self.acl_boundary_flags.items())
            },
            "connection_epochs": self.epoch_serial,
            "att_connection_epochs": len(self.att_epochs),
            "unanchored_att_pdus": self.unanchored_att_pdus,
            "redacted_events_written": len(self.events),
            "redacted_events_dropped_at_limit": self.dropped_events,
            "active_connections_at_eof": len(self.active_epochs),
            "incomplete_acl_assemblies_at_eof": len(self.pending),
            "errors": dict(sorted(self.error_counts.items())),
            "att_error_response_request_opcodes": {
                f"0x{opcode:02x}": count
                for opcode, count in sorted(self.att_error_response_request_opcodes.items())
            },
            "att_error_response_queue_consumed": dict(
                sorted(self.att_error_response_queue_consumed.items())
            ),
            "att_error_response_queue_unmatched": dict(
                sorted(self.att_error_response_queue_unmatched.items())
            ),
            "l2cap_complete": self.counts["l2cap_complete"],
            "non_att_l2cap": self.counts["non_att_l2cap"],
            "att_pdus": self.counts["att_pdus"],
            "gatt_service_ranges_observed": sum(len(v) for v in self.services.values()),
            "garmin_ml_characteristics": sum(
                1 for uuid in self.characteristics.values()
                if uuid.startswith("6a4e") and uuid.endswith("-667b-11e3-949a-0800200c9a66")
            ),
            "garmin_ml_characteristic_value_counts": [
                {"epoch": epoch, "uuid": uuid, "direction": direction, "count": count}
                for (epoch, uuid, direction), count in sorted(self.ml_characteristic_value_counts.items())
            ],
            "strict_ml_control_response_counts": [
                {"service_id": service_id, "status": status, "classification": classification, "count": count}
                for (service_id, status, classification), count in sorted(self.ml_control_responses.items())
            ],
            "unmatched_ml_control_candidates": self.unmatched_ml_control_candidates,
            "direction_definition": "btsnoop flag bit0: 0 host-sent, 1 host-received; validate via ATT opcodes",
            "timestamp_definition": "btsnoop microseconds since 1900-01-01 UTC; converted by subtracting 0x00dcddb30f2f8000",
            "timestamp_confidence": "unvalidated_absolute_offset: converted btsnoop time and Android logcat threadtime differ from the README UTC window by approximately one hour; values are retained as source-clock conversions, not asserted capture UTC",
            "evidence_limit": "ATT transport is reconstructed; Garmin attribution requires discovered UUID/handle mapping; MLR/GFDI/FileAccess/FIT are not inferred by this layer.",
        }

    def _new_epoch(self, handle: int) -> str:
        self.epoch_serial += 1
        epoch = f"c{self.epoch_serial}"
        self.active_epochs[handle] = epoch
        # A reused HCI handle starts with no inherited L2CAP state or GATT map.
        for key in [k for k in self.pending if k[1] == handle]:
            del self.pending[key]
        for key in [k for k in self.characteristics if k[0] == epoch]:
            del self.characteristics[key]
        return epoch

    def _hci_event(self, record: Record) -> None:
        p = record.packet
        if len(p) < 2:
            self.error_counts["short_hci_event"] += 1
            return
        event_code, parameter_len = p[0], p[1]
        params = p[2:]
        if len(params) != parameter_len:
            self.error_counts["hci_event_length_mismatch"] += 1
            return
        if event_code == 0x05 and len(params) >= 4:  # Disconnection Complete
            status, raw_handle = params[0], _u16(params, 1)
            handle = raw_handle & 0x0FFF
            if status == 0:
                epoch = self.active_epochs.pop(handle, None)
                for key in [k for k in self.pending if k[1] == handle]:
                    del self.pending[key]
                self._emit({
                    "record": record.index, "timestamp_utc": record.timestamp_utc,
                    "epoch": epoch, "layer": "hci",
                    "event": "disconnection_complete", "handle": handle,
                    "status": status, "direction": record.direction,
                })
        elif event_code == 0x3E and len(params) >= 4:
            subevent = params[0]
            # LE Connection Complete (0x01), Enhanced (0x0a), and
            # LE Enhanced Connection Complete V2 (0x29) share status+handle.
            if subevent in (0x01, 0x0A, 0x29):
                status, raw_handle = params[1], _u16(params, 2)
                if status == 0:
                    handle = raw_handle & 0x0FFF
                    epoch = self._new_epoch(handle)
                    self._emit({
                        "record": record.index, "timestamp_utc": record.timestamp_utc,
                        "epoch": epoch, "layer": "hci",
                        "event": "le_connection_complete", "handle": handle,
                        "status": status, "direction": record.direction,
                    })

    def _acl(self, record: Record) -> None:
        p = record.packet
        self.counts["acl_records"] += 1
        if not record.complete:
            self.error_counts["incomplete_acl_records"] += 1
            return
        if len(p) < 4:
            self.error_counts["short_acl_header"] += 1
            return
        raw_handle, acl_len = struct.unpack_from("<HH", p)
        handle, pb = raw_handle & 0x0FFF, (raw_handle >> 12) & 3
        self.acl_boundary_flags[record.direction][f"pb_{pb}"] += 1
        payload = p[4:]
        if len(payload) != acl_len:
            self.error_counts["acl_length_mismatch"] += 1
            return
        # HCI connection handles 0xF00..0xFFE are reserved and 0xFFF is
        # the broadcast handle; neither identifies an LE ACL connection.
        # Do not interpret controller traffic on these values as L2CAP.
        if handle > 0x0EFF:
            self.error_counts["invalid_acl_connection_handle"] += 1
            return
        epoch = self.active_epochs.get(handle)
        if epoch is None:
            # Keep an explicit unanchored epoch; never claim device attribution.
            epoch = f"unanchored-{handle:03x}"
        key = (epoch, handle, record.direction)
        if pb in (0, 2):  # first fragment; 0=non-flushable, 2=flushable
            if len(payload) < 4:
                self.error_counts["short_l2cap_start"] += 1
                self.pending.pop(key, None)
                return
            expected, cid = struct.unpack_from("<HH", payload)
            if expected > MAX_L2CAP_PDU:
                self.error_counts["oversized_l2cap_pdu"] += 1
                self.pending.pop(key, None)
                return
            body = payload[4:]
            previous = self.pending.pop(key, None)
            if previous is not None:
                self.error_counts["second_start_before_completion"] += 1
            if len(body) > expected:
                self.error_counts["overlong_l2cap_start"] += 1
                return
            if len(body) == expected:
                self._l2cap(record, epoch, handle, record.direction, cid, body)
                return
            if len(self.pending) >= MAX_OPEN_ASSEMBLIES:
                self.error_counts["assembly_table_full"] += 1
                return
            self.pending[key] = {
                "expected": expected, "cid": cid, "data": bytearray(body),
                "start_record": record.index, "start_offset": record.offset,
                "start_timestamp": record.timestamp_micros, "pb": pb,
            }
        elif pb == 3 and record.direction == "controller_to_host":
            # HCI permits a controller-to-host ACL packet to carry one complete
            # L2CAP PDU. Host-to-controller PB=3 is reserved and rejected.
            if len(payload) < 4:
                self.error_counts["short_complete_l2cap_packet"] += 1
                return
            expected, cid = struct.unpack_from("<HH", payload)
            body = payload[4:]
            if expected != len(body):
                self.error_counts["complete_l2cap_length_mismatch"] += 1
                return
            self._l2cap(record, epoch, handle, record.direction, cid, body)
        elif pb == 1:
            state = self.pending.get(key)
            if state is None:
                self.error_counts["orphan_acl_continuation"] += 1
                return
            data = state["data"]
            assert isinstance(data, bytearray)
            expected = int(state["expected"])
            if len(data) + len(payload) > expected:
                self.error_counts["overlong_l2cap_continuation"] += 1
                del self.pending[key]
                return
            data.extend(payload)
            if len(data) == expected:
                del self.pending[key]
                self._l2cap(record, epoch, handle, record.direction, int(state["cid"]),
                            bytes(data), int(state["start_record"]), int(state["start_offset"]))
        else:  # Host-to-controller PB=3 is reserved.
            self.error_counts["reserved_pb_flag"] += 1

    def _l2cap(self, record: Record, epoch: str, handle: int,
               direction: str, cid: int, body: bytes,
               start_record: int | None = None, start_offset: int | None = None) -> None:
        self.counts["l2cap_complete"] += 1
        if cid != 0x0004:
            self.counts["non_att_l2cap"] += 1
            return
        if not body:
            self.error_counts["empty_att_pdu"] += 1
            return
        self._att(
            record, epoch, handle, direction, body,
            start_record or record.index,
            start_offset if start_offset is not None else record.offset,
        )

    def _att(self, record: Record, epoch: str, handle: int,
             direction: str, pdu: bytes, start_record: int,
             start_offset: int) -> None:
        opcode = pdu[0]
        name = ATT_NAMES.get(opcode, f"unknown_0x{opcode:02x}")
        self.att_counts[name] += 1
        self.att_epochs.add(epoch)
        if epoch.startswith("unanchored-"):
            self.unanchored_att_pdus += 1
        self.direction_opcodes[direction][name] += 1
        att_handle: int | None = None
        uuid = None
        if opcode in (0x0A, 0x0C, 0x12, 0x52, 0x1B, 0x1D, 0x16) and len(pdu) >= 3:
            att_handle = _u16(pdu, 1)
            # ATT values belong to the server's database: notifications and
            # indications are sent by the server; writes are sent by its peer.
            server_direction = (
                direction if opcode in (0x1B, 0x1D)
                else "controller_to_host" if direction == "host_to_controller"
                else "host_to_controller"
            )
            uuid = self.characteristics.get((epoch, server_direction, att_handle))
        event: dict[str, object] = {
            "record": record.index, "record_start": start_record,
            "record_offset_start": start_offset, "epoch": epoch, "hci_handle": handle,
            "direction": record.direction, "layer": "att", "opcode": f"0x{opcode:02x}",
            "name": name, "att_handle": att_handle, "characteristic_uuid": uuid,
            "att_pdu_length": len(pdu),
            "value_length": self._att_value_length(opcode, pdu),
            "record_complete": True,
        }
        if opcode == 0x01 and len(pdu) >= ATT_MIN_LENGTHS[0x01]:
            event["request_opcode_in_error"] = f"0x{pdu[1]:02x}"
            event["attribute_handle_in_error"] = _u16(pdu, 2)
            event["error_code"] = f"0x{pdu[4]:02x}"
        min_length = ATT_MIN_LENGTHS.get(opcode)
        if min_length is not None and len(pdu) < min_length:
            self.error_counts["truncated_att_opcode"] += 1
            event["parse_status"] = "truncated"
            event["value_length"] = None
            event["timestamp_utc"] = record.timestamp_utc
            self._emit(event)
            return
        event["parse_status"] = "structural"
        event["timestamp_utc"] = record.timestamp_utc
        self._emit(event)
        if opcode == 0x01:
            self._consume_error_response_discovery_request(epoch, direction, pdu)
        if opcode in (0x12, 0x52, 0x1B, 0x1D) and att_handle is not None and uuid is not None:
            service_uuid = self.characteristic_services.get(
                (epoch, server_direction, att_handle)
            )
            if (
                service_uuid == "6a4e2800-667b-11e3-949a-0800200c9a66"
                and uuid.startswith("6a4e")
                and uuid.endswith("-667b-11e3-949a-0800200c9a66")
            ):
                value = pdu[3:]
                self.ml_characteristic_value_counts[(epoch, uuid, direction)] += 1
                suffix = uuid[4:8]
                if direction == "host_to_controller" and suffix in {
                    f"{i:04x}" for i in range(0x2820, 0x282A)
                }:
                    # GarminMlHandshake.registerRequest(): 13-byte control request.
                    if (len(value) == 13 and value[:10] == b"\x00\x00\x02" + bytes(7)
                            and value[12] == 0):
                        self.ml_registration_requests[epoch].append(_u16(value, 10))
                elif direction == "controller_to_host" and suffix in {
                    f"{i:04x}" for i in range(0x2810, 0x281A)
                }:
                    # Mirror GarminMlOffline.inspectRegisterResponse and require
                    # a matching locally queued request; candidates alone are
                    # not registration evidence.
                    if (14 <= len(value) <= 64 and value[:2] == b"\x00\x01"
                            and value[2] == 2 and value[3:10] == bytes(7)):
                        service_id = _u16(value, 10)
                        status, ml_handle = value[12], value[13]
                        pending = self.ml_registration_requests[epoch]
                        if pending and pending[0] == service_id:
                            pending.popleft()
                            classification = "accepted" if status == 0 and 1 <= ml_handle <= 127 else "rejected"
                            self.ml_control_responses[(service_id, status, classification)] += 1
                        else:
                            self.unmatched_ml_control_candidates += 1
        if opcode == 0x10 and len(pdu) >= 7:
            self.read_by_group_requests[(epoch, direction)].append(uuid_from_att(pdu[5:]))
        elif opcode == 0x08 and len(pdu) >= 7:
            self.read_by_type_requests[(epoch, direction)].append(uuid_from_att(pdu[5:]))
        if opcode == 0x11:
            self._read_by_group_type(record, epoch, direction, pdu, start_record, start_offset)
        elif opcode == 0x09:
            self._read_by_type(record, epoch, direction, pdu, start_record, start_offset)
        elif opcode == 0x05:
            self._find_information(record, epoch, direction, pdu, start_record, start_offset)

    def _consume_error_response_discovery_request(
        self, epoch: str, response_direction: str, pdu: bytes
    ) -> None:
        """Retire the failed discovery request named by an ATT Error Response.

        ATT Error Response carries the original request opcode. Discovery
        requests are queued by connection epoch and direction; consume only
        the oldest request of the matching opcode in the opposite direction.
        """
        if len(pdu) < ATT_MIN_LENGTHS[0x01]:
            return
        request_opcode = pdu[1]
        self.att_error_response_request_opcodes[request_opcode] += 1
        request_direction = (
            "controller_to_host"
            if response_direction == "host_to_controller"
            else "host_to_controller"
        )
        if request_opcode == 0x08:
            queue_name = "read_by_type"
            requests = self.read_by_type_requests.get((epoch, request_direction))
        elif request_opcode == 0x10:
            queue_name = "read_by_group"
            requests = self.read_by_group_requests.get((epoch, request_direction))
        else:
            return
        if requests:
            requests.popleft()
            self.att_error_response_queue_consumed[queue_name] += 1
        else:
            self.att_error_response_queue_unmatched[f"unmatched_{queue_name}"] += 1

    @staticmethod
    def _att_value_length(opcode: int, pdu: bytes) -> int | None:
        if opcode in (0x0B, 0x0D):  # Read / Read Blob Response
            return len(pdu) - 1
        if opcode in (0x12, 0x52, 0x1B, 0x1D):  # Write / notification / indication
            return len(pdu) - 3
        if opcode in (0x16, 0x17):  # Prepare Write Request / Response
            return len(pdu) - 5
        return None

    def _read_by_group_type(
        self, record: Record, epoch: str, direction: str, pdu: bytes,
        start_record: int | None = None, start_offset: int | None = None,
    ) -> None:
        if len(pdu) < 2:
            self.error_counts["short_read_by_group_type_response"] += 1
            return
        width = pdu[1]
        if width not in (6, 20) or (len(pdu) - 2) % width:
            self.error_counts["malformed_read_by_group_type_response"] += 1
            return
        request_direction = (
            "controller_to_host" if direction == "host_to_controller" else "host_to_controller"
        )
        requests = self.read_by_group_requests[(epoch, request_direction)]
        requested_uuid = requests.popleft() if requests else None
        if requested_uuid != "00002800-0000-1000-8000-00805f9b34fb":
            return
        for off in range(2, len(pdu), width):
            start, end = _u16(pdu, off), _u16(pdu, off + 2)
            service_uuid = uuid_from_att(pdu[off + 4:off + width])
            self.services[(epoch, direction)].append((start, end, service_uuid))
            self._emit({
                "record": record.index, "record_start": start_record or record.index,
                "record_offset_start": start_offset if start_offset is not None else record.offset,
                "timestamp_utc": record.timestamp_utc,
                "epoch": epoch, "direction": direction,
                "layer": "gatt_discovery", "kind": "service",
                "start_handle": start, "end_handle": end, "uuid": service_uuid,
                "evidence": "att_read_by_group_type_response",
            })

    def _read_by_type(
        self, record: Record, epoch: str, direction: str, pdu: bytes,
        start_record: int | None = None, start_offset: int | None = None,
    ) -> None:
        if len(pdu) < 2:
            self.error_counts["short_read_by_type_response"] += 1
            return
        width = pdu[1]
        if width < 5 or (len(pdu) - 2) % width:
            self.error_counts["malformed_read_by_type_response"] += 1
            return
        request_direction = (
            "controller_to_host" if direction == "host_to_controller" else "host_to_controller"
        )
        requests = self.read_by_type_requests[(epoch, request_direction)]
        requested_uuid = requests.popleft() if requests else None
        if requested_uuid != "00002803-0000-1000-8000-00805f9b34fb":
            return
        for off in range(2, len(pdu), width):
            value_handle = _u16(pdu, off)
            value = pdu[off + 2:off + width]
            if len(value) < 5 or len(value) not in (5, 19):
                self.error_counts["malformed_characteristic_declaration"] += 1
                continue
            properties, char_handle = value[0], _u16(value, 1)
            char_uuid = uuid_from_att(value[3:])
            self.characteristics[(epoch, direction, char_handle)] = char_uuid
            services = [s for s in self.services.get((epoch, direction), [])
                        if s[0] <= value_handle <= s[1]]
            service_uuids = {s[2] for s in services}
            service_uuid = next(iter(service_uuids)) if len(service_uuids) == 1 else None
            if len(service_uuids) > 1:
                self.error_counts["ambiguous_overlapping_service_ranges"] += 1
            self.characteristic_services[(epoch, direction, char_handle)] = service_uuid
            self._emit({
                "record": record.index, "record_start": start_record or record.index,
                "record_offset_start": start_offset if start_offset is not None else record.offset,
                "timestamp_utc": record.timestamp_utc,
                "epoch": epoch, "direction": direction,
                "layer": "gatt_discovery", "kind": "characteristic",
                "declaration_handle": value_handle, "value_handle": char_handle,
                "properties": properties, "uuid": char_uuid,
                "service_uuid": service_uuid,
                "evidence": "att_read_by_type_response",
            })

    def _find_information(
        self, record: Record, epoch: str, direction: str, pdu: bytes,
        start_record: int | None = None, start_offset: int | None = None,
    ) -> None:
        if len(pdu) < 2:
            self.error_counts["short_find_information_response"] += 1
            return
        width = {1: 4, 2: 18}.get(pdu[1])
        if width is None or (len(pdu) - 2) % width:
            self.error_counts["malformed_find_information_response"] += 1
            return
        for off in range(2, len(pdu), width):
            attr_handle = _u16(pdu, off)
            uuid = uuid_from_att(pdu[off + 2:off + width])
            self._emit({
                "record": record.index, "record_start": start_record or record.index,
                "record_offset_start": start_offset if start_offset is not None else record.offset,
                "timestamp_utc": record.timestamp_utc,
                "epoch": epoch, "direction": direction,
                "layer": "gatt_discovery", "kind": "descriptor",
                "handle": attr_handle, "uuid": uuid,
                "evidence": "att_find_information_response",
            })

    def _emit(self, event: dict[str, object]) -> None:
        if len(self.events) >= MAX_REDACTED_EVENTS:
            self.dropped_events += 1
            return
        event["connection_epoch"] = event.pop("epoch", None)
        record = event.get("record")
        record_start = event.get("record_start", record)
        event.setdefault("record_range", [record_start, record] if record is not None else None)
        offset_end = (
            self.last_record.offset
            if self.last_record and record == self.last_record.index
            else None
        )
        offset_start = event.get("record_offset_start", offset_end)
        event.setdefault(
            "record_offset_range",
            [offset_start, offset_end],
        )
        event.setdefault("evidence_level", "derived" if event.get("layer") == "gatt_discovery" else "observed")
        if event.get("timestamp_utc") is not None:
            event.setdefault("time_confidence", "hci_btsnoop_epoch_unvalidated")
        event.setdefault("errors", [])
        event.setdefault("parser_version", PARSER_VERSION)
        for transient_key in ("record", "record_start", "record_offset_start"):
            event.pop(transient_key, None)
        self.events.append(event)

    def _expire_fragments(self, timestamp_micros: int) -> None:
        expired = []
        for key, state in self.pending.items():
            started = int(state["start_timestamp"])
            if timestamp_micros >= started and timestamp_micros - started > MAX_FRAGMENT_AGE_MICROS:
                expired.append(key)
        for key in expired:
            del self.pending[key]
        if expired:
            self.error_counts["acl_fragment_timeout"] += len(expired)


def replay(
    path: Path,
    events_path: Path | None = None,
    logcat_path: Path | None = None,
    summary_path: Path | None = None,
) -> dict[str, object]:
    capture_hash = hashlib.sha256()
    with path.open("rb") as stream:
        # Hash without retaining the capture in memory.
        while chunk := stream.read(1024 * 1024):
            capture_hash.update(chunk)
    with path.open("rb") as stream:
        _datalink, records = iter_records(stream)
        parser = Replay()
        summary = parser.run(records)
    summary["capture_sha256"] = capture_hash.hexdigest()
    summary["capture_bytes"] = path.stat().st_size
    if logcat_path:
        logcat_hash = hashlib.sha256()
        lines = 0
        timestamped = 0
        first = last = None
        with logcat_path.open("rb") as source:
            for line in source:
                logcat_hash.update(line)
                lines += 1
                match = LOGCAT_THREADTIME.match(line)
                if match:
                    stamp = match.group().decode("ascii")
                    first = first or stamp
                    last = stamp
                    timestamped += 1
        summary["logcat"] = {
            "sha256": logcat_hash.hexdigest(),
            "bytes": logcat_path.stat().st_size,
            "lines": lines,
            "threadtime_lines": timestamped,
            "first_threadtime": first,
            "last_threadtime": last,
            "clock_zone": "not encoded; local-vs-UTC offset unresolved",
        }
    if events_path:
        events_path.parent.mkdir(parents=True, exist_ok=True)
        with events_path.open("w", encoding="utf-8") as out:
            for event in parser.events:
                event = {
                    "schema": "pulselibre.garmin_wire_event.v1",
                    "capture_sha256": capture_hash.hexdigest(),
                    **event,
                    "parser_version": PARSER_VERSION,
                }
                out.write(json.dumps(event, sort_keys=True, separators=(",", ":")) + "\n")
    if summary_path:
        summary_path.parent.mkdir(parents=True, exist_ok=True)
        summary_path.write_text(
            json.dumps(summary, sort_keys=True, indent=2) + "\n",
            encoding="utf-8",
        )
    return summary


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path)
    parser.add_argument("--events", type=Path, help="write redacted transport events")
    parser.add_argument("--logcat", type=Path, help="optional logcat used only for hash and clock metadata")
    parser.add_argument("--summary", type=Path, help="write the redacted JSON summary")
    args = parser.parse_args()
    try:
        result = replay(args.capture, args.events, args.logcat, args.summary)
    except (OSError, ReplayError, struct.error) as error:
        parser.exit(2, f"Invalid capture: {error}\n")
    print(json.dumps(result, sort_keys=True, indent=2))


if __name__ == "__main__":
    main()
