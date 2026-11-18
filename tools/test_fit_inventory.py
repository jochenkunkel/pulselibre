import importlib.util
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


if __name__ == "__main__":
    unittest.main()
