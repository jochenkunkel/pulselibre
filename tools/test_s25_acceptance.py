import tempfile
import unittest
from pathlib import Path

import s25_acceptance as s


def xml(*nodes):
    body = "".join(
        f'<node text="{t}" content-desc="{d}" clickable="{str(c).lower()}" bounds="[{b[0]},{b[1]}][{b[2]},{b[3]}]"/>'
        for t, d, b, c in nodes)
    return f'<?xml version="1.0"?><hierarchy rotation="0">{body}</hierarchy>UI hierarchy dumped to: /dev/tty'


class FakeAdb(s.Adb):
    """Replays scripted outputs; records every command that passed the allow-list."""

    def __init__(self, screens=(), shells=None):
        self.commands, self.screens, self.shells = [], list(screens), shells or {}
        super().__init__("TESTSERIAL", runner=self._run, sleep=lambda _: None)

    def _run(self, args, timeout):
        command = args[1]
        self.commands.append(command)
        if command.startswith("uiautomator dump /dev/tty"):
            return (self.screens.pop(0) if len(self.screens) > 1 else self.screens[0]).encode()
        if command == "screencap -p":
            return b"PNG"
        return self.shells.get(command, "").encode()


class AllowListTest(unittest.TestCase):
    def test_dangerous_commands_are_refused(self):
        adb = FakeAdb(["x"])
        for bad in ("pm clear de.pulselibre", "pm uninstall de.pulselibre", "settings put secure enabled_accessibility_services x",
                    "am instrument -w de.pulselibre.test/x", "am start -n de.pulselibre/.CirqaTestLaboratoryActivity",
                    "svc bluetooth disable", "rm -rf /sdcard", "input text hello", "wm size 100x100",
                    "settings put system font_scale 1.0; pm clear de.pulselibre"):
            with self.assertRaises(s.AdbError, msg=bad):
                adb.shell(bad)
        self.assertEqual([], adb.commands)

    def test_needed_commands_pass(self):
        adb = FakeAdb(["x"])
        for good in ("input tap 10 20", "input swipe 1 2 3 4 300", "input keyevent 4", "cmd uimode night no", "wm density 540",
                     "wm density reset", "settings put system font_scale 1.3", "dumpsys package de.pulselibre",
                     "am start -n de.pulselibre/.MainActivity", "logcat -d -b crash"):
            adb.shell(good)

    def test_serial_is_mandatory(self):
        with self.assertRaises(s.AdbError):
            s.Adb("")


class ParsingTest(unittest.TestCase):
    def test_parse_and_find(self):
        nodes = s.parse_nodes(xml(("Übersicht", "", (0, 2000, 200, 2100), True), ("", "Hinweis", (0, 0, 10, 10), False)))
        self.assertEqual(2, len(nodes))
        self.assertEqual((100, 2050), s.find(nodes, "Übersicht").center)
        self.assertEqual("Hinweis", s.find(nodes, "Hinweis").label)
        self.assertIsNone(s.find(nodes, "Nirgends"))

    def test_garbage_gives_no_nodes(self):
        self.assertEqual([], s.parse_nodes("nothing"))

    def test_overlap_heuristic(self):
        a = s.Node("Ruhepuls Fitnessentwicklung", "", (0, 0, 400, 100))
        b = s.Node("Teilweise", "", (200, 10, 700, 90))
        c = s.Node("Weit weg", "", (0, 500, 400, 600))
        self.assertEqual([("Ruhepuls Fitnessentwicklung", "Teilweise")], s.overlapping_text_nodes([a, b, c]))
        self.assertEqual([], s.overlapping_text_nodes([a, c]))

    def test_axis_regex_requires_dates(self):
        self.assertTrue(s.AXIS_WITH_DATES.search("x Horizontale Achse 19.11. 16:28 bis 20.11. 16:28. y"))
        self.assertFalse(s.AXIS_WITH_DATES.search("Horizontale Achse 15:24 bis 15:24."))

    def test_coverage_agreement(self):
        ok = s.parse_nodes(xml(("", "Abdeckung: 0,5 %", (0, 0, 9, 9), False), ("Abdeckung 0,5 % unter 70 %.", "", (0, 0, 9, 9), False)))
        bad = s.parse_nodes(xml(("", "Abdeckung: 0,5 %", (0, 0, 9, 9), False), ("Abdeckung 1 % unter 70 %.", "", (0, 0, 9, 9), False)))
        self.assertTrue(s.coverage_texts_agree(ok)[0])
        self.assertFalse(s.coverage_texts_agree(bad)[0])  # the original #312 bug: tile 0,5 % vs reason 1 %
        worse = s.parse_nodes(xml(("", "Abdeckung: 0,5 %", (0, 0, 9, 9), False), ("Abdeckung 0,9 % unter 70 %.", "", (0, 0, 9, 9), False)))
        self.assertFalse(s.coverage_texts_agree(worse)[0])


class RunnerTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)

    def run_with(self, adb):
        return s.Run(adb, Path(self.tmp.name))

    def test_risky_labels_are_never_tapped(self):
        screen = xml(("Historie herunterladen", "", (0, 0, 100, 100), True), ("Synchronisieren", "", (0, 0, 100, 100), True),
                     ("Diagnosepaket teilen …", "", (0, 0, 100, 100), True))
        adb = FakeAdb([screen])
        run = self.run_with(adb)
        for label in ("Historie herunterladen", "Synchronisieren", "Diagnosepaket teilen"):
            with self.assertRaises(s.AdbError):
                run.tap_text(label)
        self.assertFalse(any(c.startswith("input tap") for c in adb.commands))

    def test_navigation_taps_the_bottom_tab_not_the_heading(self):
        screen = xml(("Verlauf", "", (0, 500, 200, 560), True), ("Verlauf", "", (0, 2150, 200, 2250), True))
        adb = FakeAdb([screen])
        run = self.run_with(adb)
        self.assertTrue(run.tab("Verlauf"))
        self.assertIn("input tap 100 2200", adb.commands)

    def test_navigation_survives_whitespace_in_content_desc(self):
        screen = xml(("", "  Gerät\n", (0, 2150, 200, 2250), True))
        run = self.run_with(FakeAdb([screen]))
        self.assertTrue(run.tab("Gerät"))

    def test_navigation_prefers_resource_id_in_any_language(self):
        screen = ('<?xml version="1.0"?><hierarchy><node text="" content-desc="Device" clickable="true" '
                  'resource-id="de.pulselibre:id/navigation_item_Gerät" bounds="[0,2150][200,2250]"/></hierarchy>')
        adb = FakeAdb([screen])
        self.assertTrue(self.run_with(adb).tab("Gerät"))
        self.assertIn("input tap 100 2200", adb.commands)

    def test_empty_dump_is_retried_then_raises_instead_of_reusing_old_data(self):
        good = xml(("Gerät", "", (0, 0, 100, 100), True))
        adb = FakeAdb(["ERROR: could not get idle state.", "ERROR: could not get idle state.", good])
        # two failed attempts (tty and file fallback empty), the third succeeds
        self.assertEqual(self.run_with(adb).dump()[0].text, "Gerät")
        failing = FakeAdb(["ERROR: could not get idle state."])
        with self.assertRaises(s.AdbError):
            self.run_with(failing).dump(attempts=2)

    def test_restore_resets_density_font_and_night_even_if_a_command_fails(self):
        adb = FakeAdb(["x"], shells={"cmd uimode night": "Night mode: yes", "settings get system font_scale": "1.0\n",
                                     "wm density": "Physical density: 480"})
        run = self.run_with(adb)
        run.remember_and_restore_setup()
        run.restore()
        self.assertIn("wm density reset", adb.commands)
        self.assertIn("settings put system font_scale 1.0", adb.commands)
        self.assertIn("cmd uimode night yes", adb.commands)
        self.assertEqual(s.PASS, run.checks[-1].status)

    def test_report_marks_manual_items_and_counts(self):
        run = self.run_with(FakeAdb(["x"]))
        run.add("T", "ok", s.PASS)
        run.add("T", "kaputt", s.FAIL, "a|b")
        run.checks += s.manual_items()
        path = Path(self.tmp.name) / "report.md"
        import datetime
        s.write_report(run, datetime.datetime(2026, 11, 20, 18, 0), path)
        text = path.read_text()
        self.assertIn("PASS 1 · FAIL 1", text)
        self.assertIn("nicht in das Repository einchecken", text)
        self.assertIn("MANUAL", text)

    def test_dry_run_and_repo_guard(self):
        self.assertEqual(0, s.main(["--serial", "X", "--dry-run"]))
        with self.assertRaises(SystemExit):
            s.main(["--serial", "X", "--out-dir", str(Path(__file__).resolve().parent), "--phases", "preflight"])
        with self.assertRaises(SystemExit):
            s.main(["--serial", "X", "--phases", "gibtsnicht"])


class ScanPageTest(unittest.TestCase):
    def test_scan_collects_nodes_from_every_scroll_position_and_stops_at_the_bottom(self):
        top = xml(("Herzfrequenz", "", (0, 300, 400, 400), False))
        middle = xml(("Schritte heute", "", (0, 300, 400, 400), False), ("", "Abdeckung: 0,5 %", (0, 500, 400, 560), False))
        bottom = xml(("Startseite anpassen", "", (0, 300, 400, 400), False))
        adb = FakeAdb([top, middle, bottom, bottom], {"wm size": "Physical size: 1080x2340"})
        with tempfile.TemporaryDirectory() as tmp:
            page = s.Run(adb, Path(tmp)).scan_page()
        titles = {n.text for n in page}
        self.assertTrue({"Herzfrequenz", "Schritte heute", "Startseite anpassen"} <= titles)
        self.assertTrue(any(n.desc == "Abdeckung: 0,5 %" for n in page))

    def test_swipes_are_slow_and_short_to_avoid_flings(self):
        adb = FakeAdb(["x"])
        with tempfile.TemporaryDirectory() as tmp:
            s.Run(adb, Path(tmp)).swipe(True)
        command = adb.commands[-1].split()
        self.assertEqual("900", command[-1])
        self.assertLess(int(command[3]) - int(command[5]), 0.35 * 2340)


class FullRunTest(unittest.TestCase):
    """A scripted phone: every phase runs end to end; nothing outside the allow-list is sent; settings are restored."""

    SCREEN = xml(
        ("Übersicht", "", (0, 2150, 200, 2250), True), ("Verlauf", "", (200, 2150, 400, 2250), True),
        ("Analysen", "", (400, 2150, 600, 2250), True), ("Gerät", "", (600, 2150, 800, 2250), True),
        ("Daten", "", (800, 2150, 1000, 2250), True),
        ("Herzfrequenz", "", (0, 600, 500, 700), False), ("SpO₂", "", (500, 600, 900, 700), False),
        ("Schlaf", "", (0, 800, 500, 900), False), ("Nacht-Insights", "", (0, 1000, 500, 1100), False),
        ("Schritte heute", "", (0, 1200, 500, 1300), False), ("24 Stunden", "", (0, 300, 200, 400), True),
        ("7 Tage", "", (200, 300, 400, 400), True), ("30 Tage", "", (400, 300, 600, 400), True),
        ("Verteilung", "", (0, 450, 200, 500), True), ("Punkte", "", (200, 450, 400, 500), True),
        ("", "Horizontale Achse 19.11. 16:28 bis 20.11. 16:28.", (0, 500, 900, 560), False),
        ("Startseite anpassen", "", (0, 1500, 900, 1600), True), ("Favorit", "", (300, 700, 450, 760), True),
        ("Hoch", "", (500, 700, 650, 760), True), ("Speichern", "", (800, 2100, 1000, 2200), True),
        ("Zurücksetzen", "", (0, 2100, 300, 2200), True), ("Gerät · CIRQA", "", (0, 700, 700, 800), False),
        ("Verbunden", "", (0, 900, 400, 1000), False), ("Testlabor", "", (0, 200, 400, 300), False),
        ("Noch nicht verfügbar", "", (0, 1300, 400, 1400), False), ("Analysen", "", (0, 100, 400, 200), False),
    )
    SHELLS = {
        "dumpsys package de.pulselibre": "versionName=2.0.0_build1502\n    flags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ]\n"
            "    requested permissions:\n      android.permission.POST_NOTIFICATIONS\n    install permissions:\n",
        "dumpsys power": "mWakefulness=Awake", "dumpsys window": "", "dumpsys battery": "level: 80\n USB powered: true",
        "wm size": "Physical size: 1080x2340", "cmd uimode night": "Night mode: yes",
        "settings get system font_scale": "1.0\n", "wm density": "Physical density: 480", "logcat -d -b crash": "",
    }

    def test_all_phases_run_clean_and_restore(self):
        with tempfile.TemporaryDirectory() as tmp:
            adb = FakeAdb([self.SCREEN], self.SHELLS)
            code = s.main(["--serial", "TESTSERIAL", "--out-dir", tmp, "--background-seconds", "1"], adb=adb)
            report = (Path(tmp) / "report.md").read_text()
            self.assertNotIn("abgebrochen", report)          # no phase died with an exception
            self.assertIn("Systemeinstellungen wiederhergestellt", report)
            self.assertIn("MANUAL", report)
            self.assertTrue(any(f.suffix == ".png" for f in Path(tmp).iterdir()))
            for risky in ("Historie herunterladen", "install", "instrument", "pm "):
                self.assertFalse(any(risky in c for c in adb.commands), risky)
            self.assertIn("wm density reset", adb.commands[-6:])   # restored at the very end
            self.assertIsInstance(code, int)


if __name__ == "__main__":
    unittest.main()
