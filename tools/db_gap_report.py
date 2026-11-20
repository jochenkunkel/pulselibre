#!/usr/bin/env python3
"""Luecken-Bericht (> Schwelle ohne Wert) aus einem PulsLibre-Datenbank-Backup (ZIP).

Nur lokal verwenden: Die Ausgabe enthaelt echte Messzeiten und gehoert NICHT ins Repo, in Tickets oder Logs.
Das Backup entsteht in der App (Export > Datenbank sichern); dieses Skript liest nur und aendert nichts.

  python3 tools/db_gap_report.py backup.zip [--minutes 5] [--tz Europe/Berlin]
"""
import argparse
import json
import sys
import zipfile
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

LIVE = {6: "Live Herzfrequenz (Telefonzeit)", 19: "Live SpO2 (Telefonzeit)", 7: "Live Schritte (Telefonzeit)",
        12: "Live RR-Intervall (Telefonzeit)", 13: "Live Stress (Telefonzeit)", 21: "Live Atmung (Telefonzeit)"}


def lines(zf, name):
    if name not in zf.namelist():
        return
    with zf.open(name) as fh:
        for raw in fh:
            raw = raw.strip()
            if raw:
                try:
                    yield json.loads(raw)
                except ValueError:
                    continue


def collect(zf):
    """Return {series name: sorted list of epoch ms}; live series by reception time, FIT by measurement time."""
    series = {}
    for item in lines(zf, "cirqa_events.jsonl"):
        if item.get("type") != "reading" or item.get("schema") != 1:
            continue
        service = item.get("service")
        if service not in LIVE:
            continue
        try:
            t = datetime.fromisoformat(item["received_at_utc"].replace("Z", "+00:00"))
        except (KeyError, ValueError):
            continue
        series.setdefault(LIVE[service], []).append(int(t.timestamp() * 1000))
    fit = {p["sample_id"] for p in lines(zf, "canonical_fit_observation_provenance.jsonl")}
    for obs in lines(zf, "canonical_observations.jsonl"):
        if obs.get("sample_id") in fit and obs.get("metric") == "heart_rate":
            series.setdefault("Band-Verlauf Herzfrequenz (FIT, Messzeit)", []).append(int(obs["measured_at_ms"]))
    return {k: sorted(set(v)) for k, v in series.items()}


def gaps(times, threshold_ms):
    """(start, end) between neighbours further apart than the threshold; the edges are not gaps."""
    return [(a, b) for a, b in zip(times, times[1:]) if b - a > threshold_ms]


def fmt(ms, tz):
    return datetime.fromtimestamp(ms / 1000, timezone.utc).astimezone(tz).strftime("%d.%m. %H:%M")


def dur(ms):
    minutes = int(ms // 60000)
    if minutes >= 1440:
        return f"{minutes // 1440} d {minutes % 1440 // 60} h"
    return f"{minutes // 60} h {minutes % 60} min" if minutes >= 60 else f"{minutes} min"


def report(series, minutes, tz, top=15):
    threshold = minutes * 60000
    out = [f"Luecken > {minutes} min ohne Wert (Ortszeit {tz.key}); nur lokal verwenden.\n"]
    for name, times in sorted(series.items()):
        g = gaps(times, threshold)
        span = times[-1] - times[0] if len(times) > 1 else 0
        lost = sum(b - a for a, b in g)
        out.append(f"== {name}\n  Werte: {len(times)}  von {fmt(times[0], tz)} bis {fmt(times[-1], tz)}  (Spanne {dur(span)})")
        out.append(f"  Luecken: {len(g)}  zusammen {dur(lost)}" + (f"  ({100 * lost / span:.1f} % der Spanne)" if span else ""))
        for a, b in sorted(g, key=lambda x: x[0] - x[1])[:top]:
            out.append(f"    {fmt(a, tz)} -> {fmt(b, tz)}  ({dur(b - a)})")
        if len(g) > top:
            out.append(f"    ... {len(g) - top} weitere (nach Laenge sortiert, nur die laengsten gezeigt)")
        out.append("")
    return "\n".join(out)


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("backup")
    p.add_argument("--minutes", type=int, default=5)
    p.add_argument("--tz", default="Europe/Berlin")
    args = p.parse_args(argv)
    with zipfile.ZipFile(args.backup) as zf:
        series = collect(zf)
    if not series:
        print("Keine passenden Daten im Backup.")
        return 1
    print(report(series, args.minutes, ZoneInfo(args.tz)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
