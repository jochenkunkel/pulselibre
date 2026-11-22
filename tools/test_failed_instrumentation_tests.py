import importlib.util
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("fit_tests", Path(__file__).parent / "ci" / "failed_instrumentation_tests.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)

XML = """<?xml version="1.0"?>
<testsuite name="s" tests="3" failures="1" errors="1">
  <testcase classname="de.pulselibre.A" name="ok"/>
  <testcase classname="de.pulselibre.B" name="broken"><failure message="expected:&lt;1&gt; but was:&lt;2&gt;">trace</failure></testcase>
  <testcase classname="de.pulselibre.C" name="crashed"><error>java.lang.IllegalStateException: boom
at x</error></testcase>
</testsuite>"""


class FailedInstrumentationTests(unittest.TestCase):
    def test_names_failures_and_errors(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d) / "TEST-a.xml").write_text(XML, encoding="utf-8")
            found = MODULE.failures(list(Path(d).rglob("TEST-*.xml")))
        self.assertEqual([("de.pulselibre.B", "broken", "expected:<1> but was:<2>"),
                          ("de.pulselibre.C", "crashed", "java.lang.IllegalStateException: boom")], found)

    def test_green_run_reports_nothing(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d) / "TEST-a.xml").write_text('<testsuite><testcase classname="x" name="y"/></testsuite>', encoding="utf-8")
            self.assertEqual([], MODULE.failures(list(Path(d).rglob("TEST-*.xml"))))

    def test_unreadable_file_is_listed_not_fatal(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d) / "TEST-a.xml").write_text("<kaputt", encoding="utf-8")
            self.assertEqual(1, len(MODULE.failures(list(Path(d).rglob("TEST-*.xml")))))

    def test_missing_directory_is_handled(self):
        self.assertEqual(0, MODULE.main(["x", "/gibt/es/nicht"]))


if __name__ == "__main__":
    unittest.main()
