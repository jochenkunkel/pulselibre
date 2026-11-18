import importlib.util
import sqlite3
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("fit_inventory.py")
spec = importlib.util.spec_from_file_location("fit_inventory", MODULE_PATH)
inventory = importlib.util.module_from_spec(spec)
spec.loader.exec_module(inventory)


def fit_file(data):
    header = bytearray(14)
    header[0] = 14
    header[1] = 0x20
    header[2:4] = b"\x64\x00"
    header[4:8] = len(data).to_bytes(4, "little")
    header[8:12] = b".FIT"
    header[12:14] = inventory.crc16(header[:12]).to_bytes(2, "little")
    body = bytes(header) + data
    return body + inventory.crc16(body).to_bytes(2, "little")


def profile():
    return {"version": {"major": 21, "minor": 222, "patch": 0},
            "messages": {
                20: {"name": "record", "fields": {
                    3: {"name": "heart_rate", "scale": [1], "offset": [0], "units": "bpm"},
                    253: {"name": "timestamp", "scale": [1], "offset": [0], "units": "s"},
                }},
            }}


class FitInventoryTests(unittest.TestCase):
    def test_all_fit_base_type_ids_widths_and_names(self):
        expected = {
            0: ("enum", 1), 1: ("sint8", 1), 2: ("uint8", 1),
            3: ("sint16", 2), 4: ("uint16", 2), 5: ("sint32", 4),
            6: ("uint32", 4), 7: ("string", 1), 8: ("float32", 4),
            9: ("float64", 8), 10: ("uint8z", 1), 11: ("uint16z", 2),
            12: ("uint32z", 4), 13: ("byte", 1), 14: ("sint64", 8),
            15: ("uint64", 8), 16: ("uint64z", 8),
        }
        for base, (name, width) in expected.items():
            self.assertEqual(inventory.BASE_TYPES[base], name)
            self.assertEqual(inventory.BASE_TYPE_SIZES[base], width)

    def test_fit_invalid_sentinels_follow_base_type(self):
        examples = {
            0: b"\xff", 1: b"\x7f", 2: b"\xff", 3: b"\xff\x7f",
            4: b"\xff\xff", 5: b"\xff\xff\xff\x7f",
            6: b"\xff\xff\xff\xff", 8: b"\xff" * 4,
            9: b"\xff" * 8, 10: b"\0", 11: b"\0\0",
            12: b"\0" * 4, 13: b"\xff", 14: b"\xff" * 7 + b"\x7f",
            15: b"\xff" * 8, 16: b"\0" * 8,
        }
        for base, raw in examples.items():
            with self.subTest(base=base):
                self.assertEqual(inventory._value_counts(raw, base, False), (0, 1))
        self.assertEqual(inventory._value_counts(b"ok\0", 7, False), (1, 0))
        self.assertEqual(inventory._value_counts(b"\0", 7, False), (0, 1))

    def test_compressed_timestamps_and_developer_fields_are_counted(self):
        data = bytes([
            0x60, 0, 0, 20, 0, 2,       # definition local 0, global record
            253, 4, 0x86, 3, 1, 2,     # timestamp + heart rate
            1, 1, 2, 2,                # count 1; field 1, size 2, dev index 2
            0,                          # normal record
        ]) + (100_000).to_bytes(4, "little") + bytes([70, 0x34, 0x12]) + bytes([
            0x80, 71, 0x78, 0x56,      # compressed timestamp record
        ])
        result = inventory.inventory_fit(Path("private.fit"), fit_file(data), profile())
        self.assertEqual(result["record_count"], 2)
        rows = result["fields"]
        hr = next(r for r in rows if r["field_number"] == 3 and r["developer_data_index"] == "")
        self.assertEqual(hr["valid_value_count"], 2)
        self.assertEqual(hr["timestamped_record_count"], 2)
        self.assertEqual(hr["missing_timestamp_record_count"], 0)
        self.assertEqual(hr["first_timestamp"], hr["last_timestamp"])
        developer = next(r for r in rows if r["developer_data_index"] == 2)
        self.assertEqual(developer["developer_field_number"], 1)
        self.assertEqual(developer["record_count"], 2)

    def test_array_counts_valid_and_sentinel_elements_without_values(self):
        data = bytes([
            0x40, 0, 0, 20, 0, 1,
            3, 2, 2,                    # heart rate field as two uint8 elements
            0, 70, 0xFF,
        ])
        result = inventory.inventory_fit(Path("private.fit"), fit_file(data), profile())
        row = next(r for r in result["fields"] if r["field_number"] == 3)
        self.assertEqual(row["valid_value_count"], 1)
        self.assertEqual(row["invalid_or_missing_count"], 1)

    def test_invalid_crc_is_rejected(self):
        data = bytes([0x40, 0, 0, 20, 0, 0])
        raw = bytearray(fit_file(data))
        raw[-3] ^= 1
        with self.assertRaises(inventory.FitError):
            inventory.inventory_fit(Path("private.fit"), bytes(raw), profile())

    def test_big_endian_definition_and_data_are_counted(self):
        data = bytes([
            0x40, 0, 1, 0, 20, 2,       # big-endian global message 20
            253, 4, 0x86, 3, 1, 2,
            0,
        ]) + (100_000_000).to_bytes(4, "big") + bytes([72])
        result = inventory.inventory_fit(Path("private.fit"), fit_file(data), profile())
        hr = next(r for r in result["fields"] if r["field_number"] == 3)
        self.assertEqual(hr["valid_value_count"], 1)
        self.assertEqual(hr["first_timestamp"], "1993-03")

    def test_missing_timestamp_is_explicit(self):
        data = bytes([0x40, 0, 0, 20, 0, 1, 3, 1, 2, 0, 72])
        result = inventory.inventory_fit(Path("private.fit"), fit_file(data), profile())
        hr = next(r for r in result["fields"] if r["field_number"] == 3)
        self.assertEqual(hr["timestamped_record_count"], 0)
        self.assertEqual(hr["missing_timestamp_record_count"], 1)
        self.assertEqual(hr["first_timestamp"], "")

    def test_unknown_message_and_field_are_retained_without_guessed_names(self):
        data = bytes([
            0x40, 0, 0, 0x58, 0x02, 1,  # unknown global message 600
            99, 1, 2,                   # unknown field 99, uint8
            0, 42,
        ])
        result = inventory.inventory_fit(Path("private.fit"), fit_file(data), profile())
        self.assertEqual(result["message_counts"][0]["global"], 600)
        self.assertIsNone(result["message_counts"][0]["name"])
        field = result["fields"][0]
        self.assertEqual(field["field_number"], 99)
        self.assertIsNone(field["field_name"])
        self.assertEqual(field["base_type"], "uint8")

    def test_nonfit_xml_inventory_contains_no_text_or_attribute_values(self):
        raw = b'<?xml version="1.0"?><Device><Name>private-name</Name><File id="secret"/></Device>'
        result = inventory.inventory_nonfit(Path("private.xml"), raw)
        self.assertEqual(result["format"], "XML")
        self.assertEqual(result["xml_element_count"], 3)
        self.assertNotIn("private-name", repr(result))
        self.assertNotIn("secret", repr(result))

    def test_profile_is_read_as_literal_without_execution(self):
        with tempfile.TemporaryDirectory() as temp:
            source = Path(temp) / "profile.py"
            marker = Path(temp) / "executed"
            source.write_text(
                f"from pathlib import Path\nPath({str(marker)!r}).touch()\n"
                "Profile = {'messages': {}, 'version': {}}\n",
                encoding="utf-8",
            )
            self.assertEqual(inventory.profile_from_python(source)["messages"], {})
            self.assertFalse(marker.exists())

    def test_sqlite_duplicate_key_audit_is_read_only_and_reports_duplicates(self):
        with tempfile.TemporaryDirectory() as temp:
            db = Path(temp) / "readings.db"
            connection = sqlite3.connect(db)
            connection.execute(
                """CREATE TABLE readings (
                    device_id TEXT, metric TEXT, measured_at_ms INTEGER,
                    sample_id TEXT, value REAL, unit TEXT, received_at_ms INTEGER
                )"""
            )
            row = ("device", "heart_rate", 1, "candidate")
            connection.executemany(
                "INSERT INTO readings(device_id,metric,measured_at_ms,sample_id) VALUES (?,?,?,?)",
                (row, row),
            )
            connection.commit()
            connection.close()
            summary, _ = inventory.sqlite_imported(db)
            self.assertEqual(summary["duplicate_primary_keys"], 1)
            self.assertEqual(summary["reading_count"], 2)


if __name__ == "__main__":
    unittest.main()
