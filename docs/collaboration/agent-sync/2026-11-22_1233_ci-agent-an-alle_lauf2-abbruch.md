# Lauf 2: 5-Minuten-Frist abgelaufen, App abgebrochen – SAF-Vergleich offen

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:33 UTC

## Zeitachse

- Nutzer präzisierte 12:32 UTC: „Historie herunterladen" wurde schon gedrückt, als er „Ich tippe jetzt Herunterladen" schrieb. Konservativer Start spätestens 12:27:30 UTC; Fristende spätestens 12:32:30 UTC.
- Nur lesende Beobachtung während des Laufs: 12:27:30 UTC `MainActivity` im Vordergrund, UI-Dump 33 Byte (`could not get idle state`), kein Statustext.
- 12:32:55 UTC `am force-stop de.pulselibre`; danach kein App-Prozess, `versionCode=1806` unverändert. Kein Retry.
- Garmin weiterhin genau wie freigegeben nur temporär gestoppt (kein Prozess, nicht in `pm list packages -d`, `enabled=0` unverändert). Kein dauerhafter Eingriff.

## Ergebnis

Kein nachgewiesener Downloadabschluss, kein DB-Import nachgewiesen, kein innerer Zustand beim Abbruch beobachtet. Keine BLE-/GATT-/FileSync-Kommandos von mir, kein Reset, keine Entkopplung, keine Datenlöschung.

## Nächster Schritt / Bitte

- **Nutzer:** bitte jetzt den SAF-Export machen (Daten → Backup erstellen → Downloads → Speichern) und mir sagen, wenn die Datei im Downloadordner liegt. Ich kopiere sie lokal in den 0700-Ordner und vergleiche ausschließlich redaktierte Anzahlen/Prüfsummen mit der Baseline (alle 15 Tabellen 0 Datensätze).
- **FIT-Agent/Claude Code:** keine parallelen Geräteaktionen, bis der Vergleich abgeschlossen ist.

## Grenzen

`coverage=device-history-unverified` bleibt bis zum Nachweis von DB-Import **und** vollständiger Historie gültig. Keine Rohdaten, Gerätekennungen oder Gesundheitswerte in GitHub. Keine Instrumentationstests auf dem S10.
