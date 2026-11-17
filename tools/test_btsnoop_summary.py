"""Synthetic, privacy-safe tests for the offline btsnoop validator."""
import importlib.util
import struct
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("btsnoop_summary.py")
spec = importlib.util.spec_from_file_location("btsnoop_summary", MODULE)
snoop = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snoop)


def capture(records, datalink=1002):
    header = b"btsnoop\x00" + struct.pack(">II", 1, datalink)
    for record in records:
        header += struct.pack(">IIIIQ", len(record), len(record), 0, 0, 0) + record
    return header


class SnoopTests(unittest.TestCase):
    def check(self, data):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "synthetic.log"
            path.write_bytes(data)
            return snoop.summarize(path)

    def test_zero_acl_explicitly_insufficient(self):
        result = self.check(capture([b"\x04\x0e", b"\x01\x01"]))
        self.assertEqual(result["acl_records"], 0)
        self.assertFalse(result["sufficient_for_att_analysis"])
        self.assertIn("No ACL packets", result["warning"])
        self.assertEqual(result["types"], {"command": 1, "event": 1})

    def test_complete_att_acl(self):
        # H4 ACL, start fragment, L2CAP CID 4, one-byte ATT payload.
        acl = b"\x02" + struct.pack("<HHHH", 1 | (2 << 12), 5, 1, 4) + b"\x0a"
        result = self.check(capture([acl]))
        self.assertEqual(result["complete_att_candidates"], 1)
        self.assertTrue(result["sufficient_for_att_analysis"])
        self.assertIn("Complete ATT candidate", result["warning"])

    def test_truncated_and_oversized_rejected(self):
        with self.assertRaises(snoop.InvalidSnoop):
            self.check(capture([b"\x04"])[:-1])
        oversized = b"btsnoop\x00" + struct.pack(">II", 1, 1002)
        oversized += struct.pack(">IIIIQ", 20000000, 20000000, 0, 0, 0)
        with self.assertRaises(snoop.InvalidSnoop):
            self.check(oversized)

    def test_acl_header_and_length_mismatch_rejected(self):
        # A valid btsnoop record may still contain malformed HCI ACL framing.
        with self.assertRaises(snoop.InvalidSnoop):
            self.check(capture([b"\x02\x01\x00"]))
        # Declared ACL payload has five bytes, but only four follow.
        short = b"\x02" + struct.pack("<HH", 1 | (2 << 12), 5) + b"\x01\x00\x04\x00"
        with self.assertRaises(snoop.InvalidSnoop):
            self.check(capture([short]))
        # Trailing bytes must not be silently accepted either.
        extra = b"\x02" + struct.pack("<HH", 1 | (2 << 12), 0) + b"\x00"
        with self.assertRaises(snoop.InvalidSnoop):
            self.check(capture([extra]))

    def test_l2cap_trailing_bytes_not_counted_as_complete_att(self):
        # A six-byte ACL payload contains an L2CAP header, one ATT byte,
        # and one trailing byte that is not part of the declared L2CAP PDU.
        acl = b"\x02" + struct.pack("<HHHH", 1 | (2 << 12), 6, 1, 4) + b"\x0a\xff"
        result = self.check(capture([acl]))
        self.assertEqual(result["acl_records"], 1)
        self.assertEqual(result["complete_att_candidates"], 0)
        self.assertFalse(result["sufficient_for_att_analysis"])
        self.assertIn("no complete ATT candidate", result["warning"])

    def test_truncated_record_not_counted_as_complete_att(self):
        # A capture may store fewer bytes than the original on-wire packet.
        acl = b"\x02" + struct.pack("<HHHH", 1 | (2 << 12), 5, 1, 4) + b"\x0a"
        data = b"btsnoop\x00" + struct.pack(">II", 1, 1002)
        data += struct.pack(">IIIIQ", len(acl) + 8, len(acl), 0, 0, 0) + acl
        result = self.check(data)
        self.assertEqual(result["acl_records"], 1)
        self.assertEqual(result["complete_att_candidates"], 0)
        self.assertFalse(result["sufficient_for_att_analysis"])

    def test_acl_continuation_not_misclassified_as_complete_att(self):
        continuation = b"\x02" + struct.pack("<HH", 1 | (1 << 12), 5) + b"\x01\x00\x04\x00\x0a"
        result = self.check(capture([continuation]))
        self.assertEqual(result["acl_records"], 1)
        self.assertEqual(result["complete_att_candidates"], 0)
        self.assertFalse(result["sufficient_for_att_analysis"])

    def test_unsupported_datalink_rejected(self):
        with self.assertRaises(snoop.InvalidSnoop):
            self.check(capture([b"\x04"], 1001))


if __name__ == "__main__":
    unittest.main()
