#!/usr/bin/env python3
"""Read-only battery baseline for the phone (issue #235).

Two commands, both only *read* from the phone via adb (no install, no settings
change, no BLE action, nothing is sent to the band):

  snapshot  record battery level, charge counter, charging state, screen state
            and the app's own Bluetooth/wakelock lines into a local JSON file
  report    compare two snapshots: drain in %/h and mAh/h for the window

Snapshots may contain device details. Keep them OUT of the repository
(default output directory: ~/pulselibre-measurements, not tracked).
The band's battery is NOT measured here; note it by hand from a validated
source. Percent values are coarse indicators, not an energy balance.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path

PACKAGE = "de.pulselibre"
MIN_WINDOW_MIN = 60
KEEP = re.compile(r"(bluetooth|wake ?lock|foreground|scan|Estimated power use)", re.I)


def adb(*args: str, serial: str | None = None) -> str:
    cmd = ["adb"] + (["-s", serial] if serial else []) + list(args)
    return subprocess.run(cmd, capture_output=True, text=True, check=True, timeout=60).stdout


def parse_battery(text: str) -> dict:
    """Parse `dumpsys battery` into the few fields that matter."""
    def num(key: str):
        m = re.search(rf"^\s*{key}:\s*(-?\d+)", text, re.M)
        return int(m.group(1)) if m else None
    plugged = any(re.search(rf"{k} powered:\s*true", text) for k in ("AC", "USB", "Wireless", "Dock"))
    return {"level": num("level"), "scale": num("scale"), "charge_counter_uah": num("Charge counter"),
            "plugged": plugged, "temperature_dc": num("temperature")}


def parse_screen(text: str) -> str:
    m = re.search(r"mWakefulness=(\w+)", text)
    return m.group(1) if m else "unknown"


def drain(a: dict, b: dict) -> dict:
    """Drain between two snapshots (a earlier). Returns values plus warnings."""
    hours = (b["epoch"] - a["epoch"]) / 3600
    warns = []
    if hours <= 0:
        raise ValueError("second snapshot must be later than the first")
    if hours * 60 < MIN_WINDOW_MIN:
        warns.append(f"window shorter than {MIN_WINDOW_MIN} min: percent steps dominate, not reliable")
    if a["battery"]["plugged"] or b["battery"]["plugged"]:
        warns.append("device was plugged in at start or end: result invalid")
    if a.get("build") != b.get("build"):
        warns.append("app build changed between snapshots: result invalid")
    la, lb = a["battery"]["level"], b["battery"]["level"]
    if la is not None and lb is not None and lb > la:
        warns.append("level rose (charged in between): result invalid")
    out = {"hours": round(hours, 2), "percent_per_hour": None, "mah_per_hour": None, "warnings": warns}
    if la is not None and lb is not None:
        out["percent_per_hour"] = round((la - lb) / hours, 2)
    ca, cb = a["battery"]["charge_counter_uah"], b["battery"]["charge_counter_uah"]
    if ca and cb:
        out["mah_per_hour"] = round((ca - cb) / 1000 / hours, 1)
    return out


def cmd_snapshot(args: argparse.Namespace) -> int:
    s = args.serial
    app = adb("shell", "dumpsys", "package", PACKAGE, serial=s)
    build = re.search(r"versionName=(\S+)", app)
    stats = adb("shell", "dumpsys", "batterystats", PACKAGE, serial=s)
    snap = {
        "label": args.label, "note": args.note, "epoch": int(time.time()),
        "build": build.group(1) if build else None,
        "battery": parse_battery(adb("shell", "dumpsys", "battery", serial=s)),
        "screen": parse_screen(adb("shell", "dumpsys", "power", serial=s)),
        "app_stats_lines": [l.strip() for l in stats.splitlines() if KEEP.search(l)][:60],
    }
    out = Path(args.out_dir).expanduser()
    out.mkdir(parents=True, exist_ok=True)
    path = out / f"{time.strftime('%Y%m%d-%H%M%S')}-{re.sub(r'[^A-Za-z0-9_-]', '_', args.label)}.json"
    path.write_text(json.dumps(snap, indent=2, ensure_ascii=False))
    print(f"{path}\nlevel={snap['battery']['level']}% plugged={snap['battery']['plugged']} screen={snap['screen']}")
    return 0


def cmd_report(args: argparse.Namespace) -> int:
    a, b = (json.loads(Path(p).read_text()) for p in (args.first, args.second))
    r = drain(a, b)
    print(f"{a['label']} -> {b['label']}: {r['hours']} h, {r['percent_per_hour']} %/h, {r['mah_per_hour']} mAh/h")
    for w in r["warnings"]:
        print("WARNUNG:", w)
    print("Hinweis: Handy-Gesamtverbrauch, nicht nur PulsLibre; Band separat von Hand erfassen.")
    return 1 if r["warnings"] else 0


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("snapshot")
    s.add_argument("--label", required=True, help="e.g. uebersicht-sichtbar-start")
    s.add_argument("--note", default="", help="mode, band battery %% (by hand), screen use")
    s.add_argument("--out-dir", default="~/pulselibre-measurements")
    s.add_argument("--serial")
    s.set_defaults(fn=cmd_snapshot)
    r = sub.add_parser("report")
    r.add_argument("first"); r.add_argument("second")
    r.set_defaults(fn=cmd_report)
    a = p.parse_args(argv)
    return a.fn(a)


if __name__ == "__main__":
    sys.exit(main())
