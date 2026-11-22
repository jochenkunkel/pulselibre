#!/usr/bin/env python3
"""Print the failing instrumentation tests from Gradle's JUnit XML so a red emulator job names them in the log.

The Android emulator prints harmless GPU lines ("bad color buffer handle", "Failed to find ColorBuffer") in green
runs too; the log otherwise only says "There were failing tests" and the names sit in the HTML report artifact.
Exit code is always 0: this is a diagnostic helper, the caller keeps the original Gradle exit status.
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def failures(xml_files):
    found = []
    for path in sorted(xml_files):
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError):
            found.append(("(unlesbare Ergebnisdatei)", str(path), ""))
            continue
        for case in root.iter("testcase"):
            bad = case.find("failure")
            if bad is None:
                bad = case.find("error")
            if bad is not None:
                message = (bad.get("message") or bad.text or "").strip().splitlines()
                found.append((case.get("classname", "?"), case.get("name", "?"), message[0] if message else ""))
    return found


def main(argv):
    root = Path(argv[1]) if len(argv) > 1 else Path("app/build/outputs/androidTest-results")
    files = list(root.rglob("TEST-*.xml")) if root.exists() else []
    bad = failures(files)
    if not bad:
        print(f"[failed-instrumentation-tests] keine fehlgeschlagenen Testfälle in {len(files)} Ergebnisdatei(en) gefunden")
        return 0
    print(f"[failed-instrumentation-tests] {len(bad)} fehlgeschlagene(r) Testfall/-fälle:")
    for cls, name, message in bad:
        print(f"  FAILED {cls}#{name}: {message}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
