# CI-Agent – S10-Nachbereitung, Provenienz-Regression und 157er-Auflösung

- Agent/Umgebung: CI-Agent (OpenCode, CI/Tests/Dokumentation)
- Beginn: 2026-11-22 13:40 UTC
- Letzte Aktualisierung: 2026-11-22 14:10 UTC
- Status: abgeschlossen
- Rolle/Zuständigkeit: CI-/Runner-Status, S10-Berichte, redaktierte Evidenz, JVM-Regressionen
- Erledigte Aufträge: Watchdog-Meldung 22.11. – aktive Prüfung der Abstimmungsordner; S10-Sammelbericht nach der GUI-Abnahme mit Build 1828 korrigieren (PR #485); P1-Regression zur Provenienz-Weitergabe (PR #486); offene FIT-Bitte zur 157er CSV/DB-Differenz beantworten (PR #488).
- Jetzt in Bearbeitung: nichts – diese Sitzung ist abgeschlossen. Die Coverage-Semantik läuft in der separaten Sitzung `2026-11-22_1405_ci-agent_coverage-semantik.md` (PR #489).
- Zeitnah geplant: keine exklusive Reservierung. Backup-v7-Manifest-/Prüfsummen-Tampering bleibt als nächste Lücke (Emulator oder Produkt-Extraktion, vorher ankündigen).
- Betroffene Bereiche/Dateien: `docs/protocol/device-tests/CIRQA_S10_BERICHTE_ANALYSE_INPUT_2026-11-22.md`, `docs/protocol/device-tests/CIRQA_S10_CSV_DB_157_DIFFERENZ_AUFGELOEST_2026-11-22.md`, `app/src/test/java/de/pulselibre/CirqaFitBackfillTest.kt` (nur Test), `docs/collaboration/agent-sync/`, `docs/collaboration/agent-chat/messages/`, eigene Sitzungsdatei. Keine GUI-/FIT-Quelldateien, kein Produktcode.
- Branch/Worktrees: `ci/bounded-db-import-execution-20261122` (PR #486, gemergt), `ci/s10-csv-157-docs-20261122` (PR #488).
- Abstimmungsbedarf: 157er CSV/DB-Differenz war FIT-Agent-Auftrag; ich habe nur die von FIT erbetene redaktierte Grundlage geliefert und die Antwort per `agent-sync` und PR-Kommentar (#487) übergeben. Keine Geräteaktion ohne neue Freigabe.
- Agent-sync-Nachrichten: `2026-11-22_1345_ci-agent-an-alle_s10-nachtrag-build1828.md`, `2026-11-22_1346_ci-agent-an-alle_provenienz-regression.md`, `2026-11-22_1400_ci-agent-an-opencode_s10-csv-157-aufgeloest.md`.
- Nachweise: PR #482/#483 (Berichte/Analyse), PR #485 (`a770aad`, Build-1828-Abnahme), PR #486 (`5b10b89`, JVM-Provenienz-Test 4/4 lokal grün, CLOUD/FULL `45030213221` inkl. Emulator API 35 und dependency-review `45030213195` grün, FULL-Lauf auf resultierendem `main` `45031197501` gestartet), PR #488 (157er Auflösung: 27.736/27.736 Schlüssel plus Wert/Einheit/Zeit exakt gleich; 157 spätere CIRQA-Live-Ereignisse).
- Stop-Grenzen: keine BLE-/GATT-/FileSync-Kommandos, kein Reset/Entkoppeln/`CLOSE_ALL`, keine Löschung realer Daten, keine Instrumentationstests auf physischen Geräten, keine Rohdaten/Gesundheitswerte/Gerätekennungen in GitHub. CSV nur lokal 0700/0600.
- Übergabe: FIT-Agent kann 157 als erledigt schließen; GUI-Agent benötigt keine Rückfrage; ChatGPT bitte PR #488 zusammen mit Abschnitt 9 des S10-Sammelberichts lesen.
