# UPDATE – #547 integriert; Filterkontext-Beleg eingegrenzt
- Zeit (UTC): 2026-11-23 12:10
- Von: OpenCode FIT / technische Leitung
- An: Projektleitung, CI-Agent, GUI-Agent, alle
- Bezug: P0-Fortsetzung und Nutzerfreigabe neuer getrennter CIRQA-Sequenzen

## PR-/CI-Status
- #547 exakter Head `efaa08651a6d75abad1e431929b956e6aef14ba3`: alle Checks einschließlich dependency-review und API-35-Emulator bestanden (`45168593628`); gemergt als `8201c9260d96093bc7a7f8b47ad338471a97f3eb`.
- Post-merge Android FULL `45171100041` auf Merge-SHA läuft; Merge-Erfolg ist damit noch nicht als Main-FULL-Erfolg ausgewiesen.
- #556 aktueller Head `48fe07020eb9662484d95caf89cf319f14419fa9`; Build/Unit/Lint und dependency-review grün, API-35-Emulator in `45169856415` noch aktiv. Noch nicht gemergt.

## Pagination-/Filterkontext – Befund
- `CIRQA_S10_GARMIN_PURE_SYNC_HCI_2026-11-22.md` und `CIRQA_S10_THIRD_SYNC_HCI_CORRECTION_2026-11-21.md` berichten für Garmin-Connect-Folgelisten: Token-Treffer, `transaction=true`, `get_flags=1`, `types=1`. Anfangsrequest berichtet zusätzlich `exclude_flags=1`, `types=0`.
- Das sind nur redigierte Zähl-/Presence-Angaben. Das Connect-Schema zeigt `get_flags` als repeated `GDIDataTypes.UUID` und `included_data_types` als repeated `DataType`-Message, nicht als einfache Integer. Exakte UUID/DataType-Werte sind in den Berichten nicht enthalten.
- Folglich ist „fehlender Filterkontext verursacht CIRQA-Leerantwort“ weiterhin eine plausible, aber nicht ausreichend operationalisierte Hypothese. Keine Werte/UUIDs werden geraten; noch kein geänderter Request gesendet. Die bestehende NextTransaction-Gate-Zählung wird nicht zurückgesetzt oder umgangen.
- Read-only S10-Paketprüfung unter exklusiver Lease: Modell `SM-G970F`, installierte App `2.0.0_build1939`, MainActivity im Vordergrund, Garmin Connect Prozess nicht laufend, Bluetooth an; Lease ordnungsgemäß FREI. Kein BLE-Protokollrequest in dieser Fortsetzung.

## Migration-v9-/Backup-Befund aus Code
- Instrumentierungstest `CirqaDataModelV9InstrumentedTest.v8UpgradeBackfillsQualityWithoutTouchingStoredValuesAndIsIdempotent`: synthetische v8-Daten (2 Beobachtungen), nach Upgrade 2 Quality-Projektionen, unveränderte physiologische Werte und Timestamp-Herkunft, `integrity_check=ok`, `foreign_key_check=0`; Reopen bleibt 2 Zeilen. Separate Testmethoden `statusCodesAreEventsAndNeverBecomePhysiologicalValues` und `integrityConstraintsStayActiveAfterUpgrade`.
- `DatabaseBackup` bleibt Version 8; v9 `observation_status_events`, `import_runs`, `import_errors` und `observation_quality` sind nicht in Backupentries. Es gibt derzeit keinen Produktions-INSERT-Pfad zu status/import tables; Quality wird aus mitgesicherten v8-Generationen deterministisch rekonstruiert. Damit keine nachgewiesene aktuelle Persistenz-/Restore-Verlustfolge, aber v9-Status-/Importdaten wären bei künftigem Schreiben nicht in v8-Backups erhalten. Vor produktiver Nutzung dieser Tabellen braucht es Format-/Restore-Erweiterung und Tests.
- CI-Agent-Audit und konkrete signierte APK-Evidenz noch ausstehend.

## Nächster Schritt / Testentscheidung
Emulator für #556 und Post-merge Main-FULL abwarten. Für einen echten Filterkontext-Test zuerst Connect-Trace-/Protobuf-Requestwerte offline aus den zugriffsgeschützten lokalen Rohdaten rekonstruieren; nur mit exakt belegten Werten gezielt testen. Sonst höchstens separat freigegebenen, unveränderten token-only Baseline-Test erwägen – nicht als Pagination-Fix oder Historiennachweis. Kein Pull/MLR/Import und maximal ein Follow-up; `sync_request` kann Banddaten als übertragen markieren.
