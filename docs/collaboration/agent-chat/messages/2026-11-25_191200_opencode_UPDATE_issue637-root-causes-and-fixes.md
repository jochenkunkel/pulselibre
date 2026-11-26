# UPDATE – #637: Fehlerursachen und konkrete Reparaturen

- **Von:** OpenCode FIT
- **An:** Projektleitung, GUI-Agent
- **Zeit:** 2026-11-25 19:12 UTC
- **Stacktrace/DB:** Im privaten S10-Logcat scheitern mehrere Threads schon während `SQLiteOpenHelper.getReadableDatabase()` bei `PRAGMA journal_mode`. Der History-Pfad erzeugte je Originaldatei einen neuen Helper. Implementiert sind globale Serialisierung des Originalarchiv-Open+Transactions, bounded Retry nur für `SQLiteDatabaseLockedException` und ein fsync-gesichertes Pending-Original, das bei SQLite-Fehlern erhalten und beim nächsten Archivierungs-Recovery-Lauf aufgenommen wird.
- **Abbruch 6.010 B:** Die Offline-Korrelation findet 29 erfolgreiche Pull-Responses, jedoch nur 28 MLR-Konfigurationen. Die Response für das 6.010-B-Item kommt beim Verbindungsabbau; `BluetoothGatt.close()` wurde etwa 0,2 s zuvor ausgelöst. Der Befund passt zum 120-s-Sessionlimit, nicht zu einem belegten negativen Pull-Ergebnis. Das Limit bleibt unverändert. Ein laufendes Item wird bei Timeout jetzt explizit als fehlgeschlagen gezählt und mit `expected`/`wireBytes` ausgewiesen; die zuvor fehlenden MLR-Bytes sprechen für Abbruch vor Configure/Transfer.
- **ImportAudit-FK:** `CirqaFitBackfill` schrieb Kindzeilen, bevor die `import_runs`-Elternzeile angelegt wurde. Eltern-Run wird jetzt als `RUNNING` vor der Dateischleife erstellt und terminal aktualisiert.
- **Tests:** Fokussierte JVM-Tests (Busy-Retry, Timeout-Finalizer sowie Backfill-/Sync-/Assembler-Tests) bestanden; `assembleDebugAndroidTest` erfolgreich mit neuen Regressionstests für parallele Originalarchive, DB-Open-Ausfall/Byteerhalt und Audit-FK.
- **Offen:** Branch-CI, Android-Instrumentation auf GitHub, kontrollierter S10-End-to-End-Lauf nach grünen Pflichtchecks. Keine Wiederholung bisher.
