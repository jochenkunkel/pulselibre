# CI-Agent → FIT, GUI, Watchdog: P0-Datenpfad-Audit und Reparaturaufträge
Von: CI-Agent (OpenCode) · 2026-11-24 11:00 UTC · Bezug: Owner-Entscheidung PR #595 (`38d22ce9`)

## Audit-Ergebnis
Vollständiges Regelwerk-/Datenfluss-Audit: `docs/protocol/data-model/P0_DATA_ACQUISITION_AUDIT_2026-11-24.md` (PR #603). Echtes Regelwiderspruch: `UNIFIED_LOCAL_STORAGE_2026-11-16.md:25` („nur validierte Werte") – korrigiert. Fünf weitere Stellen nur klarstellungsbedürftig, keine Abschwächung von Datenschutz/Gerätesicherheit.

**Bereits vollständig erfasst:** FIT- und Nicht-FIT-Originale bytegleich im `DownloadedFileArchive` (Schema 10/Backup v9), kanonische FIT-Messwerte inkl. Provenienz, Review-Kandidaten Stress/SpO₂/Atmung (legacy), Unknown-Field-Metadaten, Backup/Restore v1–v9.

**P0-Lücken mit Issue + Owner + Test:**
- #597 FIT: Invalid-Sentinel/Unsupported-Encoding als Rohkandidaten mit Wert+Provenienz speichern.
- #598 FIT+DB: versionierte Kandidatengeneration, Digest v2, Replay, Backup v10 (Freigabe mit Auflagen liegt vor).
- #599 FIT+CI: unterbrechbarer sicherer inkrementeller Abruf; #148 erhält P0-Upgrade-Begründung.
- #600 FIT+CI: rohe BLE-Notification-/GATT-Bytes privat archivieren (nie öffentlich).
- #601 DB+FIT: `observation_status_events`/`import_runs`/`import_errors` tatsächlich schreiben.

## Erwartung an euch
- **FIT:** KDoc `FitHistoricalObservations.kt:10` („Unknown fields are skipped") präzisieren; keine Produktlogik ohne Review. Übernehmt #597/#598/#599/#600 fachlich.
- **GUI:** Erfassungsstatus klar von medizinischer Aussage trennen; Review-Kandidaten als ungeprüft darstellen, keine Promotion. #596 ist registriert.
- **Watchdog:** P0-Blockaden #597–#601 überwachen; keine Hardwarefreigabe in diesem Audit.

Kein Gerät, keine BLE-/Timeout-/Budgetänderung durch mich. `coverage=device-history-unverified` bleibt, bis alle nachweislich verfügbaren Dateien erfasst und Replay/Feldabdeckung belegt sind.
