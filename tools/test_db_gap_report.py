import io, json, unittest, zipfile
from datetime import datetime, timezone
from zoneinfo import ZoneInfo
import db_gap_report as g

def ms(h, m=0): return int(datetime(2026, 11, 1, h, m, tzinfo=timezone.utc).timestamp() * 1000)

def archive(events, observations=(), provenance=()):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as zf:
        zf.writestr("cirqa_events.jsonl", "\n".join(json.dumps(e) for e in events))
        zf.writestr("canonical_observations.jsonl", "\n".join(json.dumps(o) for o in observations))
        zf.writestr("canonical_fit_observation_provenance.jsonl", "\n".join(json.dumps(p) for p in provenance))
    buf.seek(0)
    return zipfile.ZipFile(buf)

def ev(service, h, m): return {"type": "reading", "schema": 1, "service": service,
                                "received_at_utc": datetime.fromtimestamp(ms(h, m) / 1000, timezone.utc).isoformat()}

class GapTest(unittest.TestCase):
    def test_gaps_use_strict_threshold_and_ignore_edges(self):
        t = [ms(10, 0), ms(10, 5), ms(10, 11), ms(10, 12)]
        self.assertEqual(g.gaps(t, 300000), [(ms(10, 5), ms(10, 11))])  # exactly 5 min is not a gap

    def test_live_and_fit_are_separate_series_and_fit_needs_provenance(self):
        zf = archive([ev(6, 10, 0), ev(6, 10, 20), ev(19, 10, 0)],
                     [{"sample_id": "a", "metric": "heart_rate", "measured_at_ms": ms(8, 0)},
                      {"sample_id": "b", "metric": "heart_rate", "measured_at_ms": ms(8, 1)},
                      {"sample_id": "c", "metric": "heart_rate", "measured_at_ms": ms(9, 0)}],  # no FIT provenance
                     [{"sample_id": "a"}, {"sample_id": "b"}])
        s = g.collect(zf)
        self.assertEqual(len(s["Live Herzfrequenz (Telefonzeit)"]), 2)
        self.assertEqual(len(s["Band-Verlauf Herzfrequenz (FIT, Messzeit)"]), 2)

    def test_report_lists_longest_first_in_local_time(self):
        text = g.report({"X": [ms(10, 0), ms(10, 30), ms(10, 32), ms(12, 0)]}, 5, ZoneInfo("Europe/Berlin"))
        self.assertIn("Luecken: 2", text)
        self.assertLess(text.index("(1 h 28 min)"), text.index("(30 min)"))
        self.assertIn("01.11. 11:32 -> 01.11. 13:00", text)  # UTC 10:32 -> 12:00 is CET 11:32 -> 13:00

if __name__ == "__main__":
    unittest.main()
