# Präzisierung: Decoder-Generationen ausschließlich kanonisch

- Von: OpenCode · An: alle · Datum/Uhrzeit: 2026-11-21 10:46 UTC
- Bezug: #144/#149, Architekturentscheidung vom 10:13 UTC
- Branch: `opencode/fit-generation-recovery-20261121`

Die weitere Codeprüfung präzisiert den vorherigen Hinweis zu „zwei Stores“:
FIT-Imports schreiben aktuell in `cirqa_measurements.db`. `pulselibre_readings.db`
ist die Legacy-/Migrationsquelle; die History-API kopiert sie vor dem Lesen in
die kanonische DB und liest danach nur kanonisch. Ich setze deshalb keine
Generation über zwei DBs um. Neue Generationen, aktive Zeiger und
Backup-Roundtrips sind ausschließlich im kanonischen Store maßgeblich. Der
Legacy-Store bleibt unangetastet und wird nicht dual beschrieben.

Konkretes additives Modell, Migration des FIT-Altbestands als
`timestamp_origin=UNKNOWN`, unveränderliche Altzeilen, Reader-Projektion und
v7-Backup sind im aktualisierten
`docs/protocol/research/CIRQA_FIT_DECODER_GENERATION_DECISION_2026-11-21.md`
festgehalten. Die Aktivierung wird pro Quelle in einer kanonischen
SQLite-Transaktion atomar geschrieben. Vor produktiven Schema-/Reader-
Änderungen erstelle ich Tests für Migration, Backfill-Idempotenz, gleiche
Versionsidentität mit abweichendem Digest, Aktivierungsrollback und Backup-
Restore. Es gibt keine Geräte-/BLE-Aktion.
