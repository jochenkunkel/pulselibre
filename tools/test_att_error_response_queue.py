#!/usr/bin/env python3
"""Synthetic-only regression tests for ATT Error Response (0x01) queue handling.

The records below are generated placeholders. They drive the *production*
Replay through H4 btsnoop -> HCI ACL -> L2CAP -> ATT; no real capture, device
address, token, UID, payload or health value is used.

Error responses consume only the matching request opcode's oldest outstanding
discovery request in the same connection epoch and opposite direction.
"""
import importlib.util
import io
import struct
import sys
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("garmin_hci_replay.py")
SPEC = importlib.util.spec_from_file_location("garmin_hci_replay_att_error", MODULE)
replay_module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = replay_module
SPEC.loader.exec_module(replay_module)

BT_EPOCH = replay_module.BT_EPOCH_UNIX_MICROS


def snoop(records):
    data = bytearray(b"btsnoop\x00" + struct.pack(">II", 1, 1002))
    for packet, flags, drops, original_len in records:
        data.extend(struct.pack(">IIIIQ", original_len, len(packet), flags, drops, BT_EPOCH + 1_000_000))
        data.extend(packet)
    return bytes(data)


def hci_connection(handle):
    params = bytes((0x01, 0, handle & 0xFF, handle >> 8)) + bytes(15)
    return b"\x04\x3e" + bytes((len(params),)) + params


def hci_disconnection(handle):
    params = bytes((0, handle & 0xFF, handle >> 8, 0x13))
    return b"\x04\x05\x04" + params


def acl(handle, pb, l2cap_payload, flags):
    packet = b"\x02" + struct.pack("<HH", handle | (pb << 12), len(l2cap_payload)) + l2cap_payload
    return packet, flags, 0, len(packet)


def acl_complete_l2cap(handle, l2cap_payload, flags):
    return acl(handle, 3, l2cap_payload, flags)


def l2cap(cid, payload):
    return struct.pack("<HH", len(payload), cid) + payload


def read_by_type_request(type_uuid_le):
    return b"\x08\x01\x00\xff\xff" + type_uuid_le


def error_response(request_opcode=0x08, attribute_handle=0x0001, error_code=0x0A):
    return bytes((0x01, request_opcode)) + struct.pack("<H", attribute_handle) + bytes((error_code,))


def characteristic_response(char_uuid_le):
    # width=7 = 2-byte value handle + 5-byte declaration (props, handle, uuid)
    return b"\x09\x07" + struct.pack("<H", 0x0002) + b"\x03" + struct.pack("<H", 0x0003) + char_uuid_le


class AttErrorResponseQueueTests(unittest.TestCase):
    def parse(self, records):
        _, iterator = replay_module.iter_records(io.BytesIO(snoop(records)))
        parser = replay_module.Replay()
        return parser, parser.run(iterator)

    def test_error_response_consumes_failed_request_before_next_discovery_response(self):
        # Request A asks for UUID 0x2800 and fails. Request B asks for 0x2803
        # and succeeds. The error for A must consume A, so B's response can be
        # associated with its own request.
        records = [
            (hci_connection(0x40), 1, 0, 22),
            (acl(0x40, 0, l2cap(4, read_by_type_request(b"\x00\x28")), 0)),
            (acl_complete_l2cap(0x40, l2cap(4, error_response()), 1)),
            (acl(0x40, 0, l2cap(4, read_by_type_request(b"\x03\x28")), 0)),
            (acl_complete_l2cap(0x40, l2cap(4, characteristic_response(b"\x10\x28")), 1)),
        ]
        parser, summary = self.parse(records)
        self.assertIn(("c1", 0x0003), parser.characteristics)
        self.assertEqual(0, len(parser.read_by_type_requests[("c1", "host_to_controller")]))
        self.assertEqual(1, summary["att_opcodes"].get("error_response"))
        self.assertEqual(2, summary["att_opcodes"].get("read_by_type_request"))
        self.assertEqual({"0x08": 1}, summary["att_error_response_request_opcodes"])
        self.assertEqual({"read_by_type": 1}, summary["att_error_response_queue_consumed"])

    def test_error_response_consumes_only_queue_for_its_request_opcode(self):
        records = [
            (hci_connection(0x42), 1, 0, 22),
            (acl(0x42, 0, l2cap(4, read_by_type_request(b"\x00\x28")), 0)),
            (acl(0x42, 0, l2cap(4, b"\x10\x01\x00\xff\xff\x00\x28"), 0)),
            (acl_complete_l2cap(0x42, l2cap(4, error_response(request_opcode=0x10)), 1)),
        ]
        parser, summary = self.parse(records)
        self.assertEqual(1, len(parser.read_by_type_requests[("c1", "host_to_controller")]))
        self.assertEqual(0, len(parser.read_by_group_requests[("c1", "host_to_controller")]))
        self.assertEqual({"0x10": 1}, summary["att_error_response_request_opcodes"])
        self.assertEqual({"read_by_group": 1}, summary["att_error_response_queue_consumed"])

    def test_error_response_in_opposite_direction_does_not_consume_queue(self):
        records = [
            (hci_connection(0x43), 1, 0, 22),
            (acl(0x43, 0, l2cap(4, read_by_type_request(b"\x00\x28")), 0)),
            (acl(0x43, 0, l2cap(4, error_response(request_opcode=0x08)), 0)),
        ]
        parser, summary = self.parse(records)
        self.assertEqual(1, len(parser.read_by_type_requests[("c1", "host_to_controller")]))
        self.assertEqual({"0x08": 1}, summary["att_error_response_request_opcodes"])
        self.assertEqual({"unmatched_read_by_type": 1}, summary["att_error_response_queue_unmatched"])

    def test_request_queues_are_epoch_scoped_and_not_mixed_after_reconnect(self):
        # A pending request in c1 must not be consumed by a response in c2,
        # even when the same HCI handle is reused. This guards against
        # cross-connection attribution, independent of the 0x01 defect.
        records = [
            (hci_connection(0x41), 1, 0, 22),
            (acl(0x41, 0, l2cap(4, read_by_type_request(b"\x00\x28")), 0)),
            (hci_disconnection(0x41), 1, 0, 7),
            (hci_connection(0x41), 1, 0, 22),
            (acl_complete_l2cap(0x41, l2cap(4, characteristic_response(b"\x10\x28")), 1)),
        ]
        parser, summary = self.parse(records)
        self.assertEqual(2, summary["connection_epochs"])
        self.assertEqual(0, summary["garmin_ml_characteristics"])
        self.assertEqual(1, len(parser.read_by_type_requests[("c1", "host_to_controller")]))
        self.assertEqual(0, len(parser.read_by_type_requests[("c2", "host_to_controller")]))


if __name__ == "__main__":
    unittest.main()
