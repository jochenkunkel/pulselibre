# ENDE – CI-Agent: passive Mitschrift des vollständigen Garmin-Connect-Syncs am S10

- Zeit (UTC): 2026-11-22 16:57
- Von: CI-Agent (OpenCode) · An: ChatGPT, OpenCode/FIT-Agent, Claude Code, alle
- Bezug: START 16:51; Nutzer meldete „fertig" nach eigenem Sync in Garmin Connect; Rohdaten-Freigabe 16:55.

## Ergebnis

- Aufzeichnung 16:50:16–16:53:04 UTC: HCI-Snoop `0→1` (verifiziert), Logcat aller Puffer (11.562 Zeilen) + HCI-Log aus lokalem Bugreport (5.522.737 Bytes). Keine eigenen BLE-/GATT-/FileSync-Kommandos; Nutzer löste den Sync selbst aus.
- Logcat (nur Zählungen): 1.947 Bluetooth-/GATT-Zeilen, 338 Garmin-Zeilen, 726 `onWriteCharacteristic`-Callbacks, konzentriert auf 16:52 UTC – belegt rege GATT-Aktivität im Fenster, **keinen** PulsLibre-Importbeleg.
- HCI-Validator `tools/btsnoop_summary.py` (8/8 Tests grün): 85.154 Records, 69.470 ACL, 36.714 ATT-Kandidaten. Gerätezuordnung/Protokollsemantik daraus nicht belegt.
- **Rohdaten ausdrücklich vom Nutzer freigegeben** („es sind dummydate!!!!! ich gebe es frei"): beide Artefakte mit SHA-256-Manifest und ehrlichem Hinweis auf zählend geprüfte Token-/Passwort-Treffer unter `docs/protocol/device-tests/raw/2026-11-22-s10-garmin-full-sync/` bereitgestellt. Freigabe gilt nur für diesen Test.
- Dokumentation: `docs/protocol/device-tests/CIRQA_S10_GARMIN_FULL_SYNC_LOG_2026-11-22.md`.

## Tests / CI (gelesen)

- PR #504 gemergt (`68cb0257`); `main`-FULL CLOUD `45045764522` und AUTO `45045761461` beide `success`, inkl. Emulator API 35. `dependency-review` grün (Scorecard-Hinweis 2.5 < 3 nicht blockierend).
- Lokal: JVM-Suite 926/926 grün; `btsnoop_summary` 8/8; `git diff --check` grün.

## Offene Abhängigkeiten / Übergaben

- An FIT-Agenten: Abstimmungsdatei `2026-11-22_1651_ci-agent-an-fit-agent_garmin-sync-passive-mitschrift.md` – keine Gegenmeldung; passive Mitschrift abgeschlossen, keine Geräteüberschneidung beobachtet. Rohdaten jetzt für FIT-Deep-Dive verfügbar.
- `coverage=device-history-unverified` bleibt unverändert; keine Produktänderung aus diesem Mitschnitt abgeleitet. PR #499 / Issue #500 unangetastet. Keine Instrumentationstests auf physischen Geräten.
