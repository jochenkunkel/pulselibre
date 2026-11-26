"""Synthetic-only tests for the bounded, redacted btsnoop replay parser."""
import importlib.util
import io
import struct
import sys
import tempfile
import unittest
from collections import deque
from pathlib import Path

MODULE = Path(__file__).with_name("garmin_hci_replay.py")
SPEC = importlib.util.spec_from_file_location("garmin_hci_replay", MODULE)
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
    params = bytes((0x01, 0, handle & 0xff, handle >> 8)) + bytes(15)
    return b"\x04\x3e" + bytes((len(params),)) + params


def hci_disconnection(handle):
    params = bytes((0, handle & 0xff, handle >> 8, 0x13))
    return b"\x04\x05\x04" + params


def acl(handle, pb, l2cap_payload, flags):
    packet = b"\x02" + struct.pack("<HH", handle | (pb << 12), len(l2cap_payload)) + l2cap_payload
    return packet, flags, 0, len(packet)


def acl_complete_l2cap(handle, l2cap_payload, flags):
    packet = b"\x02" + struct.pack("<HH", handle | (3 << 12), len(l2cap_payload)) + l2cap_payload
    return packet, flags, 0, len(packet)


def l2cap(cid, payload):
    return struct.pack("<HH", len(payload), cid) + payload


class ReplayTests(unittest.TestCase):
    def parse(self, records):
        _, iterator = replay_module.iter_records(io.BytesIO(snoop(records)))
        parser = replay_module.Replay()
        return parser, parser.run(iterator)

    def test_acl_split_into_two_fragments_is_reassembled_and_value_is_redacted(self):
        att = b"\x1b\x1c\x00" + b"private-value"
        l2 = l2cap(4, att)
        split = 5
        first_payload = l2[:split]
        second_payload = l2[split:]
        records = [
            (hci_connection(0x40), 1, 0, 1 + 2 + 19),
            acl(0x40, 2, first_payload, 1),
            acl(0x40, 1, second_payload, 1),
        ]
        parser, summary = self.parse(records)
        self.assertEqual(1, summary["att_pdus"])
        self.assertEqual({"notification": 1}, summary["att_opcodes"])
        self.assertEqual(0, summary["unanchored_att_pdus"])
        serialized = repr(parser.events)
        self.assertNotIn("private-value", serialized)
        self.assertEqual(len(b"private-value"), parser.events[-1]["value_length"])
        self.assertEqual([2, 3], parser.events[-1]["record_range"])

    def test_acl_split_into_three_fragments_is_reassembled(self):
        att = b"\x1b\x01\x00" + b"abcdefgh"
        l2 = l2cap(4, att)
        cuts = (4, 9)
        records = [
            acl(0x4A, 2, l2[:cuts[0]], 1),
            acl(0x4A, 1, l2[cuts[0]:cuts[1]], 1),
            acl(0x4A, 1, l2[cuts[1]:], 1),
        ]
        _parser, summary = self.parse(records)
        self.assertEqual(1, summary["att_pdus"])

    def test_acl_interleaving_and_handle_reuse_reset_connection_epoch(self):
        handle = 0x41
        # Leave a partial L2CAP PDU, disconnect, then reuse the same HCI handle.
        partial = l2cap(4, b"\x1b\x10\x00abc")[:5]
        records = [
            (hci_connection(handle), 1, 0, 22),
            acl(handle, 2, partial, 1),
            (hci_disconnection(handle), 1, 0, 1 + 2 + 4),
            (hci_connection(handle), 1, 0, 22),
            acl(handle, 1, b"continuation", 1),
        ]
        _parser, summary = self.parse(records)
        self.assertEqual(2, summary["connection_epochs"])
        self.assertEqual(1, summary["errors"]["orphan_acl_continuation"])
        self.assertEqual(0, summary["att_pdus"])

    def test_two_connections_can_have_interleaved_fragment_assemblies(self):
        first = 0x46
        second = 0x47
        att_first = b"\x1b\x01\x00first"
        att_second = b"\x1b\x02\x00second"
        l2_first, l2_second = l2cap(4, att_first), l2cap(4, att_second)
        records = [
            (hci_connection(first), 1, 0, 22),
            (hci_connection(second), 1, 0, 22),
            acl(first, 2, l2_first[:5], 0),
            acl(second, 2, l2_second[:6], 0),
            acl(first, 1, l2_first[5:], 0),
            acl(second, 1, l2_second[6:], 0),
        ]
        parser, summary = self.parse(records)
        self.assertEqual(2, summary["att_pdus"])
        self.assertEqual(2, summary["att_connection_epochs"])
        self.assertEqual(0, len(parser.pending))

    def test_controller_complete_l2cap_packet_pb3_is_supported(self):
        complete = acl_complete_l2cap(0x48, l2cap(4, b"\x1b\x01\x00x"), 1)
        _parser, summary = self.parse([complete])
        self.assertEqual(1, summary["att_pdus"])
        self.assertNotIn("reserved_pb_flag", summary["errors"])

    def test_host_to_controller_pb3_is_rejected_as_reserved(self):
        complete = acl_complete_l2cap(0x49, l2cap(4, b"\x1b\x01\x00x"), 0)
        _parser, summary = self.parse([complete])
        self.assertEqual(0, summary["att_pdus"])
        self.assertEqual(1, summary["errors"]["reserved_pb_flag"])

    def test_overlapping_service_ranges_within_one_server_withhold_attribution(self):
        parser = replay_module.Replay()
        epoch = "c-test"
        parser.services[(epoch, "controller_to_host")] = [
            (1, 10, "00001800-0000-1000-8000-00805f9b34fb"),
            (5, 20, "6a4e2800-667b-11e3-949a-0800200c9a66"),
        ]
        parser.read_by_type_requests[(epoch, "host_to_controller")] = deque([
            "00002803-0000-1000-8000-00805f9b34fb"
        ])
        record = replay_module.Record(1, 0, 0, 0, 1, 0, BT_EPOCH, 2, b"")
        # declaration handle 5 is inside two conflicting ranges
        parser._read_by_type(
            record, epoch, "controller_to_host",
            b"\x09\x15\x05\x00\x0c\x06\x00"
            + bytes.fromhex("6a4e2810667b11e3949a0800200c9a66")[::-1],
        )
        self.assertIsNone(parser.characteristic_services[(epoch, "controller_to_host", 6)])
        self.assertEqual(1, parser.error_counts["ambiguous_overlapping_service_ranges"])

    def test_opposite_server_handle_ranges_do_not_conflict_or_cross_attribute(self):
        parser = replay_module.Replay()
        epoch = "c-dual-role"
        generic_access = "00001800-0000-1000-8000-00805f9b34fb"
        generic_attribute = "00001801-0000-1000-8000-00805f9b34fb"
        parser.services[(epoch, "controller_to_host")] = [(1, 10, generic_access)]
        parser.services[(epoch, "host_to_controller")] = [(1, 10, generic_attribute)]
        parser.read_by_type_requests[(epoch, "host_to_controller")] = deque([
            "00002803-0000-1000-8000-00805f9b34fb"
        ])
        parser.read_by_type_requests[(epoch, "controller_to_host")] = deque([
            "00002803-0000-1000-8000-00805f9b34fb"
        ])
        record = replay_module.Record(1, 0, 0, 0, 1, 0, BT_EPOCH, 2, b"")
        declaration = b"\x09\x07\x02\x00\x02\x03\x00\x00\x2a"

        parser._read_by_type(record, epoch, "controller_to_host", declaration)
        parser._read_by_type(
            record, epoch, "host_to_controller",
            b"\x09\x07\x02\x00\x02\x03\x00\x01\x2a",
        )

        self.assertEqual(
            generic_access,
            parser.characteristic_services[(epoch, "controller_to_host", 3)],
        )
        self.assertEqual(
            generic_attribute,
            parser.characteristic_services[(epoch, "host_to_controller", 3)],
        )
        self.assertNotEqual(
            parser.characteristics[(epoch, "controller_to_host", 3)],
            parser.characteristics[(epoch, "host_to_controller", 3)],
        )
        self.assertNotIn("ambiguous_overlapping_service_ranges", parser.error_counts)
        parser._att(record, epoch, 1, "host_to_controller", b"\x12\x03\x00x", 1, 0)
        self.assertEqual("00002a00-0000-1000-8000-00805f9b34fb", parser.events[-1]["characteristic_uuid"])
        parser._att(record, epoch, 1, "host_to_controller", b"\x1b\x03\x00x", 1, 0)
        self.assertEqual("00002a01-0000-1000-8000-00805f9b34fb", parser.events[-1]["characteristic_uuid"])

    def test_overlong_l2cap_start_is_rejected_and_counted(self):
        malformed_l2cap = struct.pack("<HH", 2, 4) + b"abc"
        _parser, summary = self.parse([acl(0x4B, 2, malformed_l2cap, 1)])
        self.assertEqual(1, summary["errors"]["overlong_l2cap_start"])
        self.assertEqual(0, summary["l2cap_complete"])

    def test_reserved_hci_handle_is_not_interpreted_as_l2cap(self):
        malformed_l2cap = struct.pack("<HH", 4, 4) + b"x" * 900
        records = [
            acl(0x0FF1, 2, malformed_l2cap, 1),
            acl(0x0FF1, 2, malformed_l2cap, 1),
        ]
        parser, summary = self.parse(records)
        self.assertEqual(2, summary["errors"]["invalid_acl_connection_handle"])
        self.assertNotIn("overlong_l2cap_start", summary["errors"])
        self.assertNotIn("second_start_before_completion", summary["errors"])
        self.assertEqual(0, summary["l2cap_complete"])
        self.assertEqual(0, summary["att_pdus"])
        self.assertEqual(0, len(parser.pending))

    def test_second_start_discards_incomplete_l2cap_without_mixing_payloads(self):
        handle = 0x4C
        first_start = struct.pack("<HH", 8, 4) + b"\x1b"
        complete_notification = l2cap(4, b"\x1b\x01\x00x")
        parser, summary = self.parse([
            acl(handle, 2, first_start, 1),
            acl(handle, 2, complete_notification, 1),
        ])

        self.assertEqual(1, summary["errors"]["second_start_before_completion"])
        self.assertEqual(1, summary["att_pdus"])
        self.assertEqual(0, summary["incomplete_acl_assemblies_at_eof"])
        self.assertEqual(0, len(parser.pending))
        self.assertEqual([2, 2], parser.events[-1]["record_range"])

    def test_write_command_is_counted_separately_from_write_request(self):
        handle = 0x42
        records = [
            (hci_connection(handle), 1, 0, 22),
            acl(handle, 2, l2cap(4, b"\x52\x1f\x00payload"), 0),
            acl(handle, 2, l2cap(4, b"\x12\x1f\x00payload"), 0),
        ]
        parser, summary = self.parse(records)
        att_events = [event for event in parser.events if event["layer"] == "att"]
        self.assertEqual(1, summary["att_opcodes"]["write_command"])
        self.assertEqual(1, summary["att_opcodes"]["write_request"])
        self.assertEqual(2, summary["att_pdus"])
        self.assertEqual(7, att_events[0]["value_length"])
        self.assertEqual(10, att_events[0]["att_pdu_length"])
        self.assertEqual(7, att_events[1]["value_length"])
        self.assertEqual(10, att_events[1]["att_pdu_length"])

    def test_truncated_att_opcode_is_counted_and_not_decoded(self):
        records = [acl(0x44, 2, l2cap(4, b"\x52\x1f"), 0)]
        parser, summary = self.parse(records)
        self.assertEqual(1, summary["errors"]["truncated_att_opcode"])
        self.assertEqual("truncated", parser.events[-1]["parse_status"])
        self.assertIsNone(parser.events[-1]["att_handle"])

    def test_gatt_ml_mapping_is_connection_scoped_and_values_are_not_retained(self):
        handle = 0x45
        service_uuid = bytes.fromhex("6a4e2800667b11e3949a0800200c9a66")[::-1]
        characteristic_uuid = bytes.fromhex("6a4e2810667b11e3949a0800200c9a66")[::-1]
        control_value = (
            b"\x00\x01" + (2).to_bytes(8, "little") +
            (1).to_bytes(2, "little") + b"\x00\x7b"
        )
        control_request = (
            b"\x00\x00\x02" + bytes(7) + (1).to_bytes(2, "little") + b"\x00"
        )
        records = [
            (hci_connection(handle), 1, 0, 22),
            acl(handle, 2, l2cap(4, b"\x10\x01\x00\xff\xff\x00\x28"), 0),
            acl(handle, 2, l2cap(4, b"\x11\x14\x0e\x00\x1f\x00" + service_uuid), 1),
            acl(handle, 2, l2cap(4, b"\x08\x0e\x00\x1f\x00\x03\x28"), 0),
            acl(handle, 2, l2cap(
                4, b"\x09\x15\x0f\x00\x20\x12\x00" + characteristic_uuid
                + b"\x14\x00\x0c\x15\x00"
                + bytes.fromhex("6a4e2820667b11e3949a0800200c9a66")[::-1]
            ), 1),
            acl(handle, 2, l2cap(4, b"\x52\x15\x00" + control_request), 0),
            acl(handle, 2, l2cap(4, b"\x1b\x12\x00" + control_value), 1),
        ]
        parser, summary = self.parse(records)
        self.assertEqual(
            1, summary["strict_ml_control_response_counts"][0]["count"],
        )
        self.assertEqual(1, summary["strict_ml_control_response_counts"][0]["service_id"])
        self.assertNotIn(control_value.hex(), repr(parser.events))
        self.assertFalse(hasattr(parser, "ml_values"))

    def test_non_att_l2cap_is_counted_but_not_parsed_as_att(self):
        records = [acl(0x43, 2, l2cap(0x0006, b"\x1b\x01\x00"), 1)]
        _parser, summary = self.parse(records)
        self.assertEqual(1, summary["l2cap_complete"])
        self.assertEqual(0, summary["att_pdus"])
        self.assertEqual(1, summary["non_att_l2cap"])

    def test_partial_capture_record_is_not_used_as_acl_evidence(self):
        packet = b"\x02" + struct.pack("<HHHH", 0x2001, 5, 1, 4) + b"\x0a"
        _parser, summary = self.parse([(packet, 1, 0, len(packet) + 2)])
        self.assertEqual(0, summary["att_pdus"])
        self.assertEqual(1, summary["errors"]["incomplete_acl_records"])

    def test_drop_count_and_non_monotonic_time_are_explicit(self):
        data = bytearray(b"btsnoop\x00" + struct.pack(">II", 1, 1002))
        packet = b"\x01\x03\x0c\x00"
        for timestamp, drops in ((BT_EPOCH + 2_000_000, 1), (BT_EPOCH + 1_000_000, 0)):
            data.extend(struct.pack(">IIIIQ", len(packet), len(packet), 0, drops, timestamp))
            data.extend(packet)
        _, iterator = replay_module.iter_records(io.BytesIO(data))
        result = replay_module.Replay().run(iterator)
        self.assertEqual(1, result["errors"]["records_with_drops"])
        self.assertEqual(1, result["errors"]["non_monotonic_btsnoop_time"])

    def test_drop_counter_marks_gap_but_does_not_discard_complete_current_packet(self):
        packet, flags, _drops, original = acl(
            0x4C, 2, l2cap(4, b"\x1b\x01\x00ok"), 1
        )
        parser, result = self.parse([(packet, flags, 2, original)])
        self.assertEqual(1, result["att_pdus"])
        self.assertEqual(1, result["errors"]["records_with_drops"])
        self.assertNotIn("truncated_records", result["errors"])
        self.assertEqual([], [e for e in parser.events if e.get("parse_status") == "truncated"])

    def test_hundred_thousand_records_are_processed_without_retaining_each_record(self):
        data = bytearray(b"btsnoop\x00" + struct.pack(">II", 1, 1002))
        packet = b"\x01\x03\x0c\x00"
        one_record = struct.pack(">IIIIQ", len(packet), len(packet), 0, 0, BT_EPOCH)
        for _ in range(100_000):
            data.extend(one_record)
            data.extend(packet)
        _, iterator = replay_module.iter_records(io.BytesIO(data))
        parser = replay_module.Replay()
        result = parser.run(iterator)
        self.assertEqual(100_000, result["records"])
        self.assertEqual([], parser.events)

    def test_malformed_capture_header_is_rejected(self):
        with self.assertRaises(replay_module.ReplayError):
            replay_module.iter_records(io.BytesIO(b"not-btsnoop"))

    def test_btsnoop_epoch_conversion_uses_known_unix_epoch_vector(self):
        record = replay_module.Record(1, 0, 0, 0, 0, 0, BT_EPOCH, None, b"")
        self.assertEqual("1970-01-01T00:00:00Z", record.timestamp_utc)

    def test_optional_logcat_summary_keeps_only_clock_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            capture = root / "sample.log"
            capture.write_bytes(snoop([]))
            logcat = root / "logcat.txt"
            logcat.write_bytes(
                b"11-22 17:50:17.413  100  100 I Test: private-token-value\n"
                b"not a threadtime line\n"
            )
            result = replay_module.replay(capture, logcat_path=logcat)
            encoded = repr(result)
            self.assertNotIn("private-token-value", encoded)
            self.assertEqual(2, result["logcat"]["lines"])
            self.assertEqual(1, result["logcat"]["threadtime_lines"])
            self.assertEqual("11-22 17:50:17.413", result["logcat"]["first_threadtime"])


if __name__ == "__main__":
    unittest.main()
