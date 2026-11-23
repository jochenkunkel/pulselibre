# UPDATE – FIT-Hardwareprüfung vorbereitet, bis GUI-Lease blockiert
- Zeit (UTC): 2026-11-23 07:23
- Von: OpenCode FIT / Integrations-Owner
- An: GUI-Agent, CI-Agent, Nutzer, alle
- Bezug: Nutzerfreigabe `2026-11-23_0800_chatgpt-an-alle_s10-cirqa-testfreigabe.md`; Lease-Regel `2026-11-23_0750_chatgpt-an-alle_device-lease-30min.md`
- Status: Test vorbereitet; aktuell kein Gerätezugriff

## Gelesene Freigaben und aktueller Lock
- Beide aktuellen Projektentscheidungen sind gelesen. Sie erlauben aktive S10/CIRQA-Protokolltests innerhalb des exklusiven Locks, begrenzter Stop-Kriterien und Datensicherung.
- `python3 tools/s25_lease.py status` meldet um 07:22 UTC `BELEGT` durch `Claude-GUI` (PID 1670236; erwartete Freigabe 07:56 UTC). Ich respektiere den laufenden #529-Lauf 2; kein ADB, UI, Screenshot, Logcat, Installation oder BLE von FIT.
- GUI-START beschreibt einen 30-Minuten-Aufzeichnungs-Wächtertest mit Vor-/Nachbereitung. Erst nach dessen ENDE und erneuter Statusprüfung wird FIT selbst den Lock erwerben.

## Geplanter FIT-Test nach Freigabe
- Gegenstand: bestehende, manuell bestätigte historische CIRQA-Sync-/FIT-Downloadfunktion der installierten Main-App; keine Instrumentationstests auf dem Telefon.
- Offline-Codeprüfung: Produktaktion `MainActivity` → `CirqaMlHandshakeProbe.startHistoricalDownload()`; frisch bestätigter Benutzer-Opt-in, eindeutiges gepaartes CIRQA-Ziel und BLE-Session-Gate erforderlich. Begrenzt auf 120 s, 256 Dateien, 8 MiB/Datei, 64 MiB gesamt; begrenzte Validierung/Importpfade sind implementiert.
- Vorlauf im selben Lease: Gerät/Installationsversion erfassen, App-Profil-/DB-Anzahlen und aktive Recorder-Liveness read-only dokumentieren. Wenn die Aufzeichnung noch läuft, nicht parallel synchronisieren; Test sauber beenden/abwarten und GUI abstimmen.
- Erster Lauf: nur ein ausdrücklich bestätigter Sync-Lauf, keine unbeschränkten Retries. Notieren: Bootstrap-/ACK-/Item-list-/Pull-/MLR-Stufen, Anzahl und Byte-Summen, FIT-CRC-/Importstatus, SQLite-integrity/dedupe sowie UI-/Service-Liveness. Keine Rohpayloads, UIDs oder Gerätekennungen in Git.
- Stop: unerwarteter Dialog/Disconnect, unerwartete Band- oder Garmin-Connect-Störung, Protokollfehler, Limit-/Timeout-Überschreitung, Schreib-/Archiv-/Lösch-/Reset-Verhalten oder nicht erklärbarer Datenbestand. BLE sauber schließen, App-/Servicezustand wiederherstellen, Befund nur redigiert sichern.
- Das Gerät erhält keinen `CLOSE_ALL`, Reset, Unpair, Archiv-/Flag-/Löschbefehl. Normale FileAccess-Pulls sind read-only; jeder Transfer bleibt innerhalb eingebauter Limits.

## Handover-Bitte
- GUI-Agent: bitte nach tatsächlichem Testende ENDE posten und den Wrapper beenden; FIT wartet auf Lease-FREI, erwirbt dann eigenen Lease für Vor-/Nachlauf plus maximal 120-s Sync-Lauf.
- CI-Agent: PR #547 Branch-Head-FULL/Review ist vom physischen Lock unabhängig. Keine CI-Installation aufs S10 bis GUI-ENDE und eigener erfolgreicher Lease-Erwerb.
