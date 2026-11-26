# ENDE – #142/#159 Live-Gerätemetadaten: Ergebnis, S25-Nachweise, Übergaben

- Zeit (UTC): 2026-11-22 20:45:00
- Von: OpenCode
- An: alle, besonders GUI-Agent, FIT-Agent, CI-Agent
- Bezug: [START](2026-11-22_190700_opencode_START_device-metadata-battery-live.md) · [Übernahme](../../agent-sync/2026-11-22_1907_opencode-an-gui-fit-agent_issue142-159-uebernahme.md) · [Register](../../agent-activity/sessions/2026-11-22_1907_opencode_device-metadata-battery-live.md)
- Branch: `opencode/issue142-159-device-metadata-20261122`, Basis `origin/main` `15da2cd7` inklusive #517; v8-Schemata/Mengen zusammengeführt, finale Commit-ID wird erst nach grünem FULL-CI und Push dokumentiert. #496 bleibt geparkt und unberührt.

## Ergebnis
- **Persistenz:** additives Schema v8 mit Tabelle `live_device_metadata` (`field_key`, `value_text`, `unit`, `source`, `observed_at_ms`, `stored_at_ms`), Migration, Backup v8 und Restore v7-kompatibel. Passiver Backfill übernimmt **nur tatsächlich vorhandene** Felder aus `fit_device_metadata`. Jeder beobachtete Wert behält Quelle und Zeitstempel; nichts wird erfunden oder aus anderen Werten abgeleitet.
- **Abfrage:** `DeviceMetadataProbe` führt **eine** begrenzte, read-only Abfrage pro Besuch des Reiters „Gerät“ aus (40 s Gesamtlimit, automatischer Disconnect): zuerst GATT `0x180F`/`0x2A19` sowie Device Information/Current Time, danach **nur falls kein GATT-Akkustand kam** Garmin `GDIDeviceStatus` über den ML-Kanal. ML-Registrierung erst nach bestätigter CCCD-Subscription; keine überlappenden GATT-Schreibvorgänge. Kein Pairing/Unpairing, kein Reset, kein `CLOSE_ALL`, keine Instrumentationstests auf dem S25.
- **Decoder (rein JVM-testbar):** `GarminDeviceStatusProtobuf`, `GarminDeviceStatusResponseDecoder` (COBS/GFDI, inkl. `extraValues` für zusätzlich angebotene ML-Werte) und `DeviceMetadataGattDecoder`.
- **Anzeige:** `DeviceOverviewView` zeigt Batterie prominent mit Balken/Frische sowie **alle** weiteren gespeicherten Werte als eigene Zeilen mit Quelle und Alter; Identifikatoren bleiben in der DB, werden aber maskiert angezeigt. Fehlende Werte bleiben „nicht verfügbar“; `37 %`/`NORMAL` sind ausschließlich das historische Einmalbeispiel vom 18.11.2026 und nirgends hart codiert.

## Tatsächliche Tests / Nachweise
- `gradle :app:testDebugUnitTest`: **grün** (952 Tests, u. a. Protobuf/COBS/GATT/Format/Backup).
- `gradle :app:assembleDebugAndroidTest`: **grün**; Instrumentierungstests für DB-Speicher, Backup v7/v8 und UI-Zeilen auf dem Emulator.
- `./tools/ci-local.sh --quick`: **grün**, inkl. R8-Release und APK-Diagnose.
- Voller lokaler Lauf `./tools/ci-local.sh --full` inkl. Emulator nach dem finalen Commit gestartet; Ergebnis wird hier nachgelesen, bevor ein PR gemergt wird. Ein erster Lauf scheiterte flakelig beim Emulator-Package-Install (`DELETE_FAILED_INTERNAL_ERROR`). Der zweite Lauf war **rot durch sechs echte Testfehler** der Schema-v8-Änderung: drei Migrationstests erwarteten noch `user_version = 7`, der Backup-Restore-Prüfer kannte den neuen v8-Eintrag noch nicht, und der FIT-Backfill setzte `fit_device_metadata` auch in synthtischen Legacy-DBs voraus. Alle drei Ursachen sind behoben (`extractArchive` nutzt jetzt die v8-Liste, der Backfill prüft die Tabellenexistenz, Versionserwartungen auf 8 korrigiert); die komplette Emulator-Suite lief danach **grün** (134 Tests, 0 Fehler).
- **Bounded S25-Läufe** (`<S10-Seriennummer>`, gehaltener `s25_lease.py run`-Wrapper, `ANDROID_SERIAL` explizit): GATT lieferte `device_name=CIRQA Smart Band` sowie `gatt_2a01=c100`, `gatt_2a04=1800240000009001`, `gatt_2aa6=01`, `gatt_2a54=0000`, `gatt_6a4e=00d2b57923` – alle mit Quelle und Zeitstempel in der DB nachgewiesen. `0x2A19` wurde **nicht** angeboten. Der Garmin-Status-Request wurde nach GFDI-Registrierung quittiert (Handles 109/110/111, 12 B, 1 Chunk), aber es kam **keine Batterie-Antwort** im 18-s-Fenster; die wiederholten ML-Nachrichten enthielten nur Geräteinformationen. Deshalb bleibt die Batterieanzeige korrekt „nicht verfügbar“. Details und Stop-Gates: `docs/protocol/device-tests/CIRQA_LIVE_DEVICE_METADATA_BOUNDED_TEST_2026-11-22.md`.
- Temporäre `Log`-Diagnose im Probe-Code wurde nach dem Nachweis **wieder entfernt**; `NoPlatformLoggingTest > mainSourcesDoNotUseLogcatOrStdout` ist grün. Keine Rohtraces, MACs oder Seriennummern im Git.

## Tickettexte
- #159: Titel/Text präzisiert – P2 UI, P1 Research bleibt #142; 37 %/NORMAL nur historisch; Freigabereihenfolge GATT → Garmin; bounded Scope; finaler S25-Nachweis und Grenzen.
- #142: Titel von P2 auf **P1 Research** korrigiert (P1 im Text, P2 nur UI in #159); „keine neuen Geräteversuche ohne Freigabe“ durch die **konkret erteilte, mehrfache begrenzte read-only Freigabe** ersetzt; historischer 37-%-Befund und finaler Nachweis/Grenzen ergänzt.

## Offene Abhängigkeiten / Übergaben
- **GUI-Agent:** `DeviceBattery.kt`, `DeviceOverviewView.kt` und die schmale `MainActivity`-Anbindung wurden für diesen Auftrag genutzt. Die Spalten/Quellen sind oben dokumentiert; weitere UI-Politur kann jetzt erfolgen, bitte nicht parallel im selben Branch.
- **FIT-Agent:** der passiven Backfill betrifft nur gelesene Daten aus `fit_device_metadata`; keine Änderung an Import- oder Decoder-Generationen.
- **CI-Agent:** bitte den vollen Lauf auf Branch-Head und anschließend auf `main` prüfen; Merge erst nach grünem, gelesenem Ergebnis (inkl. Emulator).
- Keine Bitte an den Nutzer: alle nötigen Freigaben lagen vor; keine Hardwareeskalation offen.
