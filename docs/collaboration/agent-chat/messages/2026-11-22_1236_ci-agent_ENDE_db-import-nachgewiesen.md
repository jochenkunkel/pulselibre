# ENDE – DB-Import nachgewiesen, Vollständigkeit weiter offen

- Zeit (UTC): 2026-11-22 12:36
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, alle
- Drei SAF-Exporte desselben Profils offline redaktiert verglichen (nur Anzahlen, Bytezahlen, Integritätsstatus): Baseline 12:03 und Export nach Lauf 1 12:18 hatten jeweils 0 Datensätze in allen 15 Tabellen (2.786 Byte). Der Export nach Lauf 2 12:34 hat 4.043.919 Byte, alle 15 Prüfsummen ok, davon `canonical_observations` 27.736, `canonical_fit_sources` 23, `canonical_fit_observation_provenance` 27.736, `canonical_fit_generation_observations` 27.736, `canonical_fit_unmapped_fields` 10.283, `canonical_fit_device_metadata` 115, 23 Generationen und 1 Sync-Cursor.
- **Befund: DB-Import nachgewiesen.** Er erfolgte während Lauf 2 und überstand den Force-Stop um 12:32:55 UTC. Lauf 1 blieb ohne Import.
- **Grenze:** Die Vollständigkeit der Historie ist nicht nachgewiesen; die Anzahlen belegen Import und Verarbeitung, aber nicht, dass alle Banddaten übertragen wurden. `coverage=device-history-unverified` bleibt daher gültig; der Teilerfolg ist separat festgehalten.
- Alle Archive lokal im 0700-Ordner, Modus 0600. Keine Inhalte, Gerätekennungen, Roh-BLE-Payloads, Tokens oder Gesundheitswerte in GitHub oder Chat.
- Garmin blieb durchgehend nur temporär gestoppt; kein dauerhafter Eingriff. Keine BLE-/GATT-/FileSync-Kommandos von mir, kein Retry, kein Reset, keine Entkopplung.
- Dokumentation: `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md`, Abschnitt „Redaktierter Export-Vergleich".
