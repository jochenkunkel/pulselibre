import unittest
import battery_baseline as bb

BAT = "AC powered: false\n USB powered: false\n  level: 80\n  Charge counter: 4000000\n  temperature: 250\n"


def snap(epoch, level, counter, plugged=False, build="1"):
    return {"label": "x", "epoch": epoch, "build": build,
            "battery": {"level": level, "charge_counter_uah": counter, "plugged": plugged}}


class BatteryBaselineTest(unittest.TestCase):
    def test_parse_battery(self):
        b = bb.parse_battery(BAT)
        self.assertEqual((b["level"], b["charge_counter_uah"], b["plugged"]), (80, 4000000, False))

    def test_parse_battery_plugged(self):
        self.assertTrue(bb.parse_battery("USB powered: true\n level: 5\n")["plugged"])

    def test_drain_rates(self):
        r = bb.drain(snap(0, 80, 4_000_000), snap(7200, 76, 3_800_000))
        self.assertEqual((r["percent_per_hour"], r["mah_per_hour"], r["warnings"]), (2.0, 100.0, []))

    def test_warnings(self):
        self.assertTrue(bb.drain(snap(0, 80, 1), snap(600, 79, 1))["warnings"])
        self.assertTrue(bb.drain(snap(0, 80, 1, plugged=True), snap(7200, 70, 1))["warnings"])
        self.assertTrue(bb.drain(snap(0, 50, 1), snap(7200, 60, 1))["warnings"])
        self.assertTrue(bb.drain(snap(0, 80, 1), snap(7200, 70, 1, build="2"))["warnings"])

    def test_order(self):
        with self.assertRaises(ValueError):
            bb.drain(snap(10, 1, 1), snap(10, 1, 1))

    def test_screen(self):
        self.assertEqual(bb.parse_screen("mWakefulness=Awake"), "Awake")


if __name__ == "__main__":
    unittest.main()
