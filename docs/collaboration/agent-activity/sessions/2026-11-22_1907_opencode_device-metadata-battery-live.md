# Aktivitätssitzung: Akku + Gerätemetadaten live erfassen, persistieren und im Reiter „Geräte“ anzeigen

- Agent/Umgebung: OpenCode (separater Mitwirkender, ausdrücklich vom Nutzer beauftragt)
- Rolle: BLE/Protokoll, Datenbank, Decoder; für diesen Auftrag auch DB-/Anzeigekette
- Aktueller Auftrag: #142/#159 – aktuellen Akkustand, Firmwareversion und weitere vom Band angebotene Metadaten in die Datenbank schreiben und den jeweils letzten Wert im Reiter „Geräte“ aktuell anzeigen; Tickettexte präzisieren
- Jetzt bearbeitet: #142, #159 (abgeschlossen)
- Zeitnah geplant: keine weitere Übernahme; #496 bleibt geparkt
- Betroffene Dateien: `CirqaDatabase.kt` (Schema v8), `DatabaseBackup.kt`, `DeviceMetadata.kt`, `DeviceMetadataGattDecoder.kt`, `DeviceMetadataProbe.kt`, `GarminDeviceStatusProtobuf.kt`, `GarminDeviceStatusResponseDecoder.kt`, `DeviceBattery.kt`, `DeviceOverviewView.kt`, `MainActivity.kt`, `strings.xml` (de/en/es/fr), JVM-/Instrumentierungstests, Issue-Texte, Testplan
- Branch: `opencode/issue142-159-device-metadata-20261122`, Head `dd1227a9`, Basis `origin/main` `15da2cd7` inkl. #517; PR #520 gemergt
- Abstimmungsbedarf: GUI-Agent besitzt historisch `DeviceBattery.kt`/`DeviceOverviewView.kt`; Übernahme per agent-sync angemeldet und im Abschluss bestätigt (Nutzer hat die Gesamtumsetzung direkt an OpenCode übertragen). Neu: PR #517 (#500) setzt ebenfalls DB-/Backup-Version 8; Integration beider v8-Mengen vor Push/Merge nötig.
- Agent-sync-Nachrichten: `2026-11-22_1907_opencode-an-gui-fit-agent_issue142-159-uebernahme.md`, `2026-11-22_2045_opencode-an-gui-fit-ci_issue142-159-abschluss.md`, `2026-11-22_2115_opencode-an-fit-gui-ci_issue142-159-db-v8-koordination.md`
- Nachweise:
  - `gradle :app:testDebugUnitTest` grün (952 Tests), `gradle :app:assembleDebugAndroidTest` grün, `./tools/ci-local.sh --quick` grün inkl. R8/APK-Diagnose.
  - FULL-Lauf inkl. Emulator: erster Lauf flakelig beim Emulator-Install; zweiter Lauf rot durch sechs echte Schema-v8-Testfehler (Versionserwartungen, v8-Backup-Eintrag, FIT-Backfill gegen fehlende Legacy-Tabelle). Nach Korrektur komplette Emulator-Suite grün (134 Tests, 0 Fehler). Erneuter voller Lauf nach finalem Commit gestartet; Ergebnis wird vor einem Merge gelesen.
  - Bounded S25-Läufe im gehaltenen Lease-Wrapper (`RF8M826126Z`): GATT `device_name=CIRQA Smart Band` + fünf weitere Characteristics mit Quelle/Zeit in `live_device_metadata`; kein `0x2A19`; Garmin-Status-Request quittiert, keine Batterie-Antwort im 18-s-Fenster. Siehe `docs/protocol/device-tests/CIRQA_LIVE_DEVICE_METADATA_BOUNDED_TEST_2026-11-22.md`.
  - Issue #159 (P2 UI) und #142 (P1 Research) korrigiert; 37 %/NORMAL nur als historischer Einmalbefund dokumentiert.
- Status: **abgeschlossen**; PR #520 nach grünem vollem Branch-Head-CI gemergt, anschließender voller `main`-Lauf `45074902617` grün. Alle Nachweise unten final.
- Nächster Schritt: keiner; #496 bleibt geparkt. Ein vorheriger FULL-Lauf auf dem Zwischenstand scheiterte nur an einem Emulator-Offline-Flake, nicht an einem Test.
