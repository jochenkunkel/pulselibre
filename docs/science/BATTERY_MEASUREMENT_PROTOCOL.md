# Akku-Messprotokoll (Ticket #235) – Handy-Seite

Werkzeug: `tools/battery_baseline.py` (nur lesend per adb; kein Install, keine Einstellung, keine BLE-Aktion). Unit-Tests: `tools/test_battery_baseline.py`.

## Ablauf je Messfenster (Betriebsart)
1. Handy **nicht am Ladekabel**, gleicher Build, gleiche Einstellungen. Betriebsart festlegen: App geschlossen · Hintergrund · Übersicht sichtbar · Nachtaufzeichnung · manueller Sync · PulsLibre deaktiviert (Vergleich).
2. Start: `python3 tools/battery_baseline.py snapshot --label <modus>-start --note "Band-Akku xx % (von Hand)"`
3. Mindestens **60 Minuten** (besser mehrere Stunden) normal laufen lassen.
4. Ende: `snapshot --label <modus>-ende`, dann `python3 tools/battery_baseline.py report <start.json> <ende.json>`.
5. Je Betriebsart **mindestens drei** vergleichbare Fenster; Ergebnis als %/h und mAh/h eintragen.

Das Werkzeug warnt bei Fenstern unter 60 min, Laden, Build-Wechsel oder steigendem Pegel; das Ergebnis ist dann ungültig.

## Grenzen
- Gemessen wird der **Gesamtverbrauch des Handys**, nicht nur PulsLibre. Die App-Zeilen (Bluetooth, Wakelock, Foreground) werden mitgespeichert, damit sie danach ausgewertet werden können.
- Der **Band-Akku** wird nicht gemessen; nur eine validierte Quelle oder die manuelle Anzeige eintragen, nichts schätzen.
- Prozentwerte sind grobe Indikatoren, keine Energiebilanz.
- Snapshots enthalten Gerätedaten: Standardordner `~/pulselibre-measurements` (außerhalb des Repos), nie committen.

## Messwerte (hier eintragen, mit Datum, Build, Modus)
| Datum | Build | Modus | Dauer | %/h | mAh/h | Band-Akku (Hand) | Bemerkung |
|---|---|---|---|---|---|---|---|
| – | – | – | – | – | – | – | Noch keine belastbare Messung (Stand 2026-11-20). |

Das Ticket #235 bleibt offen, bis ein Vorher/Nachher-Vergleich vorliegt.
