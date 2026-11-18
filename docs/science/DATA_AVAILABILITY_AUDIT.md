# Datenverfügbarkeit und Qualitätsmodell (Issue #40)

Stand 2026-11-18, aus dem Code abgeleitet (`DashboardMeasurement`, `DashboardDayReader`, `DayDataQuality`, `MetricAvailability`). Keine klinische Validierung.

## Messgrößen

| Messgröße | GFDI-Service | Einheit | Plausibilitätsgrenze | Status |
|---|---|---|---|---|
| Herzfrequenz | 6 | bpm | 1–240 | verfügbar |
| Ruhepuls | 6 | bpm | 1–240 | experimentell (Gerätefeld, Messbedingungen ungeprüft) |
| Schritte (Tageszähler), Schrittziel | 7 | Schritte | ≥ 0 / > 0 | verfügbar (kumulativ je Tag) |
| SpO₂ | 19 | % | 1–100 | experimentell (Sensor nicht validiert, Gate #39) |
| RR-Intervall | 12 | ms | > 0 | verfügbar; ist **kein** RMSSD/HRV |
| Stress, Body Battery | 13 / 20 | Geräteskala 0–100 | 0–100 | experimentell (proprietärer Index) |
| Atemfrequenz | 21 | Atemzüge/min | 1–100 | experimentell |
| Kalorien gesamt/aktiv | 8 | Geräteeinheit ungeprüft | ≥ 0 | experimentell |
| Intensitätsminuten | 10 | min | ≥ 0 | Decoder vorhanden, kein Dashboard-Verlauf |
| Schlaf, HRV | – | – | – | nicht verfügbar (keine verifizierte Quelle) |

## Zeitachse
- Zeitstempel: Empfangszeit `received_at_utc` (UTC), keine Gerätemesszeit. Historische Garmin-Daten fehlen (OpenCode-Thema).
- Tag = lokaler Kalendertag der Telefon-Zeitzone mit echter Dauer (23/25 h bei Zeitumstellung); `DashboardDay`.
- Duplikate: identische Zeilen (SHA-256) und gleiche Werte zum selben Zeitpunkt werden gezählt, nicht doppelt gewertet.
- Lücken werden nie interpoliert; `DayDataQuality` weist Abdeckung, Anzahl und längste Lücke aus (Schwelle 5 min).

## Zustände (`MetricAvailability`)
`nicht verfügbar` (keine gültige Beobachtung) · `unzureichende Daten` (Abdeckung < 50 %) · `experimentell` (Quelle ungeprüft) · `verfügbar`.

## Offen
- Repository-Schicht für Nachtwerte und mehrtägige Aggregate (Voraussetzung für #41).
- Nachtgrenzen ohne verifizierte Schlafdaten nicht definiert.
- Anzeige des Zustands in den Kacheln noch nicht verdrahtet.
