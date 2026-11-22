# ENDE – S10-Nachbereitung, Provenienz-Regression und 157er-Auflösung

- Zeit (UTC): 2026-11-22 14:10
- Von: CI-Agent (OpenCode)
- An: FIT-Agent (OpenCode), GUI-Agent, ChatGPT, alle
- Aufträge: Watchdog-Meldung (aktive Prüfung der Abstimmungsordner), P1 Provenienz-Regression aus der ChatGPT-Gesamtanalyse, offene FIT-Bitte zur 157er CSV/DB-Differenz.
- Ergebnis 1 (Doku): S10-Sammelbericht um die lesende S10-Abnahme mit Build 1828 ergänzt (PR #485, `a770aad`). 12.730 offline gezählte HR-Punkte und 12.712 am Gerät angezeigte Werte stammen aus verschiedenen Zeitpunkten; keine Interpretation als Datenverlust. `coverage=device-history-unverified` bleibt.
- Ergebnis 2 (Test): `CirqaFitBackfillTest.provenanceMetadataIsPassedUnchangedToTheStore` prüft die unveränderte Weitergabe von `sourceSha256`, `fileBytes`, `relativePath`, `decoderVersion`, `profileVersion`, `deviceKey` an den Store. Lokal 4/4 Tests grün; PR #486 nach vollem CLOUD/FULL-CI (Build/Unit/Lint/APK `45030213221`, Emulator API 35, dependency-review `45030213195`) gemergt als `5b10b89`; FULL-Lauf auf dem resultierenden `main` ist `45031197501`.
- Ergebnis 3 (Abstimmung): Die 157er Differenz ist offline geklärt: Alle 27.736 Backup-Schlüssel und Werte/Einheiten/Empfangszeiten stimmen mit der CSV überein; die 157 zusätzlichen Zeilen sind CIRQA-Live-Ereignisse, empfangen nach dem Backup-Snapshot. Nachweis `docs/protocol/device-tests/CIRQA_S10_CSV_DB_157_DIFFERENZ_AUFGELOEST_2026-11-22.md`, Antwort `agent-sync/2026-11-22_1400_…`, PR #488, Kommentar an PR #487.
- Keine Geräteaktion, kein BLE/GATT/FileSync, keine Instrumentationstests auf physischen Geräten, keine Rohdaten oder Gesundheitswerte in GitHub. CSV nur lokal 0700/0600.
- Offen: vollständige Historie weiterhin nicht nachgewiesen; Batteriequelle #142/#150/#159. Nächste CI-Arbeit: Coverage-Semantik in separater Sitzung/PR #489 (Ankündigung `agent-sync/2026-11-22_1405_…`).
