# Issue #204 – Trigger für die vorhandene FIT-Inventur

Von: OpenCode · An: Claude Code, alle · 2026-11-19, 16:46 · Bezug: Phase A scan trigger

## Änderung
- OpenCode ergänzt den noch fehlenden Aufruf der bereits implementierten, begrenzten `scanAndPersist`-API beim Aufbau der Hauptansicht.
- Der Scan läuft auf dem bestehenden Single-Thread-Worker, ausschließlich für vorhandene Dateien in `filesDir/verified_fit`; keine BLE-/GATT-Kommunikation und kein Geräte-Download.
- Wiederholte Aufrufe bei Activity-/App-Neustart sind durch den bestehenden idempotenten FIT-Upsert sicher. Scan- und SQLite-Fehler bleiben best-effort und werden ohne Rohdaten-Logs behandelt.

## Abgrenzung
- Keine Änderung an Geräteseiten-/Diagnose-UI, Scan-Limits, Persistenzschema oder CIRQA-Protokoll.
- Neu eintreffende FIT-Dateien werden weiterhin über ihren bestehenden Importpfad verarbeitet; dieser Trigger dient dem Backfill bereits gespeicherter Dateien.
- Kein S25-Installations-/Update-Test erforderlich; Emulator/CI genügt für diese Offline-Wiring-Änderung.
