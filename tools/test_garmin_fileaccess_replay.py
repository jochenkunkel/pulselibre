"""Synthetic-only tests for the redacted Garmin ML/GFDI/FileAccess replay (#506)."""
import importlib.util
import json
import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path

MODULE = Path(__file__).with_name("garmin_fileaccess_replay.py")
SPEC = importlib.util.spec_from_file_location("garmin_fileaccess_replay", MODULE)
fa = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = fa
SPEC.loader.exec_module(fa)
EPOCH = fa.hci.BT_EPOCH_UNIX_MICROS


def varint(v):
    out = bytearray()
    while True:
        b = v & 0x7F
        v >>= 7
        out.append(b | (0x80 if v else 0))
        if not v:
            return bytes(out)


def ld(n, b):
    return varint(n << 3 | 2) + varint(len(b)) + b


def vi(n, v):
    return varint(n << 3) + varint(v)


def cobs(data):
    out, block = bytearray(), bytearray()
    for byte in data:
        if byte == 0:
            out.append(len(block) + 1); out += block; block = bytearray()
        else:
            block.append(byte)
            if len(block) == 254:
                out.append(255); out += block; block = bytearray()
    out.append(len(block) + 1); out += block
    return b"\x00" + bytes(out) + b"\x00"


def gfdi(kind, rid, smart):
    body = struct.pack("<HIII", rid, 0, len(smart), len(smart)) + smart
    frame = struct.pack("<HH", len(body) + 6, kind) + body
    return frame + struct.pack("<H", fa.crc16_arc(frame))


class Capture:
    def __init__(self):
        self.records = []

    def add(self, packet, flags):
        self.records.append((packet, flags))

    def connect(self, handle=64):
        params = bytes((0x01, 0, handle, 0)) + bytes(15)
        self.add(b"\x04\x3e" + bytes((len(params),)) + params, 1)

    def att(self, host, att_handle, value, handle=64):
        opcode = 0x52 if host else 0x1B
        l2 = struct.pack("<HH", 3 + len(value), 4) + bytes((opcode,)) + struct.pack("<H", att_handle) + value
        self.add(b"\x02" + struct.pack("<HH", handle, len(l2)) + l2, 0 if host else 1)

    def ml(self, host, index, payload):
        self.att(host, 0x15 if host else 0x12, bytes((0x80 | index << 4, 0)) + payload)

    def bytes(self):
        data = bytearray(b"btsnoop\x00" + struct.pack(">II", 1, 1002))
        for packet, flags in self.records:
            data += struct.pack(">IIIIQ", len(packet), len(packet), flags, 0, EPOCH + 1_000_000) + packet
        return bytes(data)


def scenario():
    uid = b"\x11" * 8
    ref = ld(1, uid) + ld(2, vi(1, 0) + ld(2, b"FIT_TYPE_32") + vi(3, 0)) + vi(3, 20) + vi(5, 7) + vi(6, 1)
    content = bytes(8) + b".FIT" + bytes(8)
    c = Capture()
    c.connect()
    c.att(True, 0x15, bytes((0, 0)) + bytes(8) + struct.pack("<H", 0x2018) + b"\x00")  # register MLR (mgmt)
    c.att(False, 0x12, bytes((0, 1)) + bytes(8) + struct.pack("<H", 0x2018) + b"\x00\x86")
    c.ml(True, 7, cobs(gfdi(5043, 11, ld(47, ld(3, vi(1, 0))))))                  # Smart 47.3 (as Connect)
    c.ml(False, 7, cobs(gfdi(5044, 11, ld(47, ld(4, vi(1, 0))))))                 # answer 47.4
    c.ml(True, 7, cobs(gfdi(5043, 1, ld(43, ld(9, vi(2, 5) + ld(4, b"\x09" + bytes(8)))))))
    c.ml(False, 7, cobs(gfdi(5044, 1, ld(43, ld(10, vi(1, 0) + ld(4, ref))))))
    c.ml(False, 7, cobs(gfdi(5043, 9, ld(43, ld(12, ld(1, ref))))))
    c.ml(True, 7, cobs(gfdi(5043, 2, ld(43, ld(1, ld(1, ld(1, uid)) + vi(5, 15))))))
    c.ml(False, 7, cobs(gfdi(5044, 2, ld(43, ld(2, vi(1, 0) + vi(3, 4242))))))
    c.ml(True, 6, b"\x00\x00" + struct.pack("<I", 4242))       # MLR transfer config
    c.ml(False, 6, b"\x00\x00\x00")                             # config answer
    c.ml(False, 6, zlib.compress(content))                      # data (zlib header 0x78 != 0)
    c.ml(True, 7, cobs(gfdi(5043, 3, ld(43, ld(15, ld(1, uid) + ld(2, b"\x09" + bytes(8)))))))
    return c.bytes(), uid


class FileAccessReplayTest(unittest.TestCase):
    def run_capture(self, data):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "synthetic.log"
            path.write_bytes(data)
            return fa.replay_file(path)

    def test_full_chain_is_reconstructed_and_counted(self):
        data, _ = scenario()
        s = self.run_capture(data)
        self.assertEqual(1, s["connections"])
        self.assertEqual({"0x2018": 1}, s["ml_registered_services"]["1"])
        self.assertEqual({"2,4": 1}, s["item_list_request_shapes"])
        self.assertEqual({"FIT_TYPE_32": 1}, s["listed_types"])
        self.assertEqual({"FIT_TYPE_32": 1}, s["pushed_types"])
        self.assertEqual({"FIT_TYPE_32": 1}, s["pulled_types"])
        self.assertEqual({"FIT_TYPE_32": 1}, s["set_flags_types"])
        self.assertEqual({"size_exact_fit": 1}, s["mlr_transfer_status"])
        self.assertEqual(0, s["gfdi_crc_invalid"])
        self.assertEqual(1, s["fileaccess_messages"]["device/5043/12"])
        self.assertEqual({"answer": 1, "push_after_answer:FIT_TYPE_32": 1, "request": 1}, s["monitoring_flush"])

    def test_output_never_contains_identifiers(self):
        data, uid = scenario()
        text = json.dumps(self.run_capture(data))
        self.assertNotIn(uid.hex(), text)
        self.assertNotIn("1111111111111111", text)

    def test_corrupt_crc_is_counted_not_parsed(self):
        c = Capture()
        c.connect()
        frame = bytearray(gfdi(5043, 1, ld(43, ld(9, b""))))
        frame[-1] ^= 0xFF
        c.ml(True, 7, cobs(bytes(frame)))
        s = self.run_capture(c.bytes())
        self.assertEqual(1, s["gfdi_crc_invalid"])
        self.assertEqual({}, s["fileaccess_messages"])

    def test_compact_gfdi_type_and_cobs_roundtrip(self):
        self.assertEqual(5043, fa.gfdi_type(0x802B))
        self.assertEqual(5000, fa.gfdi_type(5000))
        payload = bytes(range(0, 7)) + b"\x00" * 3 + bytes(300)
        self.assertEqual(payload, fa.cobs_decode(cobs(payload)[1:-1]))

    def test_protobuf_parser_rejects_truncated_messages(self):
        self.assertIsNone(fa.pb_fields(b"\x0a\x05ab"))
        self.assertEqual([(1, 0, 150)], fa.pb_fields(b"\x08\x96\x01"))


if __name__ == "__main__":
    unittest.main()
