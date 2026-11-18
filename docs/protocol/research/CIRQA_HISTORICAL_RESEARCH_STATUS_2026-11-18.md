# CIRQA Historical Sync – kanonischer Forschungsstand (18.11.2026)

**Zweck:** Aktueller, nach Quellenstatus getrennter Einstieg für alle Agenten. GitHub-`main` und die jeweils datierten Geräteberichte bleiben maßgeblich. Dies ist keine Freigabe für neue Hardwareaktionen.

## Nachweisstand am CIRQA

| Schritt | Evidenz | Grenze |
| --- | --- | --- |
| Bootstrap 5024/5050 → 5030 → 5007 → 5031 | Phase B: 5030 outer/result 0, 5007 Status 0, 5031 Status 0 / 12 Typen | Keine FIT-Datei |
| 5002 Directory Index 0 nach vollständiger Kette | Phase C: Antwortgröße **8192 Bytes** | Größe allein ist kein gelesener Inhalt |
| 5004 Directory-Body über BLE | Nur ein 32-Byte-Chunk; strukturell Platzhalter-Eintrag idx=0, type=0; CRC/ACK geprüft | Kein echtes Dateiverzeichnis; kein FIT-Download |
| 5043 | Als `Smart.core_service.connection_ready_notification` interpretiert; Transport-ACK untersucht | Keine Begründung für spekulatives 5044 |
| Filter 3 versus 0 | V11: gleicher Platzhalter | Filterwechsel allein löst Blocker nicht |
| Bluetooth Classic / RFCOMM | Zwei gegatterte Läufe: V1 `io-error`, V2 präzise `accept-timeout` mit SDP-UUID-Liste `[00000000-0000-0000-0000-000000000000]` | **Hypothese für dieses Gerät widerlegt:** keine Gegenverbindung, SPP-UUID `DEAB91E4-…` wird nicht angeboten. Kein FIT-Transfer; beide Gates verbraucht, kein Reset |

Primärnachweise: [Historical Sync V1](../experiments/CIRQA_HISTORICAL_SYNC_V1_2026-11-18.md), [bounded Directory/FIT](../experiments/CIRQA_BOUNDED_DIRECTORY_FIT_TRANSFER_2026-11-18.md), [RFCOMM-Vorbereitung](../experiments/CIRQA_RFCOMM_PREPARATION_2026-11-18.md). Frühe Berichte sind historische Snapshots, keine aktuellen Testanweisungen.

## Wichtigste Transporthypothese

Die dokumentierte Connect-5.31-Analyse zeigt einen Bluetooth-Classic-Server mit Garmin-SPP-UUID `DEAB91E4-670F-11E1-9ACE-30BC4824019B`, `listenUsingRfcommWithServiceRecord` und GFDI/COBS auf Streams. Der externe Gadgetbridge-Vergleich berichtet ebenfalls 8192-Byte-5002-Antwort bei 32-Byte-BLE-Platzhalter. **Hypothese:** Die echten Dateien werden auf dem getesteten CIRQA erst über Classic-RFCOMM angeboten. Das ist noch nicht am eigenen Gerät bewiesen: Der erste begrenzte Lauf sah trotz SDP-Query keine Gegenverbindung. Gate V2 hat das geklärt: Die SDP-Antwort enthält nur die Null-UUID, nicht `DEAB91E4-…`; die CIRQA bietet den Garmin-SPP-Dienst nicht an. RFCOMM ist damit kein gangbarer Weg zu den FIT-Dateien dieses Geräts. Ein verbleibender, separater Forschungsweg wäre der Protobuf-FileAccess-Kanal (`0x2018`/`GDIFileAccess`), der früher als kompakter `0x8A39`-Kandidat `UNSUPPORTED` lieferte.

### Protobuf-FileAccess-Kandidat (offline, nicht gesendet)

Connect 5.31 (`com.garmin.device.filetransfer.b.i`) packt
`GDIFileAccess.Service.item_list_request` (Feld 9) in
`GDISmartProto.Smart.file_access_service` (Feld 43) und sendet es über den
Protobuf-Messenger (GFDI 5043), **nicht** über den kompakten Transport. Aus der
Binary extrahierte Feldnummern: `file_access_service = 43`, `item_list_request = 9`.
Der frühere `0x8A39`-Fehlschlag nutzte genau diese Protobuf-Struktur, aber einen
kompakten Rahmen mit fremden HRM600-Sequenz-/Counter-Werten – das ist eine
plausible, bislang nicht getestete Fehlerquelle.

`CirqaFileAccessListRequest` baut jetzt denselben `Smart`-Inhalt in einem
ordnungsgemäßen 5043-Transportframe (requestId, offset=0, total/chunk, CRC).
Rein lesende Item-List-Anfrage; keine Archiv-/Flag-/Lösch-/Reset-Operation.
**Offline getestet, nicht an BLE angebunden, nicht gesendet.** Ein Gerätelauf
braucht eine neue, ausdrückliche Freigabe und ein neues Gate.

Vorhandener Code: `CirqaRfcommTransport`, `CirqaRfcommRunner`, `CirqaRfcommDirectorySession`, `CirqaRfcommStreamFramer`, `CirqaDirectoryReadV2Session`. Vor Hardwarelauf prüfen: terminaler Abbruch bei ungültiger 5002-Größe, ungültigen Frames/Sessionfehlern, vollständige Timeout-/Cancel-Semantik und Race-Tests; keine automatische Verbindung oder ungefragte Kommandos. Nutzerbestätigung und persistenter Einmal-Gate bleiben erforderlich.

**Garmin-Connect-Binary-Analyse:** [Zentraler Befund- und Interoperabilitätsbericht](GARMIN_CONNECT_BINARY_ANALYSIS.md) bündelt DEX-/RFCOMM-Erkenntnisse, offene Herkunftsmetadaten und rechtliche Leitplanken; keine dekompilierten Garmin-Dateien ins Repository übernehmen.

## Externe Quellen – Nutzen und Grenzen

1. [gwerneckp/garmin-ble](https://github.com/gwerneckp/garmin-ble): unabhängige GFDI-/COBS-/CRC-/Protobuf-Referenz, Simulator/Replay und [FileSync-Proto](https://github.com/gwerneckp/garmin-ble/blob/main/proto/gdi_file_sync_service.proto). **Dateitransfer FIT/GPX laut Roadmap v0.6 geplant, nicht als funktionierend belegt.** AGPL-3.0; keine ungeprüfte Codeübernahme.
2. [Garmin FIT SDK](https://developer.garmin.com/fit/): Referenz für FIT-Profile, Felddefinitionen und Decoder-Vergleich. Im Projekt bereits `FitFileHeader`, `FitRecordStructure`, `FitHistoricalObservations`, `CirqaVerifiedFitStore`; der Kotlin-Decoder ist ausdrücklich **kein vollständiges FIT SDK**. Referenztests mit nichtpersönlichen Fixtures und unbekannten Feldern durchführen; FIT-CRC beweist Integrität, nicht Herkunft.
3. [Garmin Connect Personal API Python](https://github.com/garmin/connect-personal-api-python): historische Cloud-Datenmodelle/optionaler Vergleich nach vorhandenem lokalen Download; **kein CIRQA-BLE-/RFCOMM-Dateitransport**, erfordert Internet/API-Authentifizierung und ist keine Offline-first-Abhängigkeit.
4. [GarminHRM600](https://github.com/mskerwiderski/GarminHRM600): GFDI V2/FileSync-Referenz für andere Geräte; fremde Geräteidentitäten, Counter und `CLOSE_ALL` nicht auf CIRQA übertragen.

## Nächste sichere Arbeitsreihenfolge

1. RFCOMM-Code und Offline-Fixtures reviewen; Fehler- und Timeoutbehandlung härten, ohne BLE-Transport zu verändern.
2. Aktuellen `main`-Build/CI und App-Signatur verifizieren; keine DB-Downgrade-/Datenverlust-Risiken.
3. **Erst nach ausdrücklicher Gerätefreigabe** genau einen bounded, read-only RFCOMM-Directory-Lauf; nur redigierte Metadaten dokumentieren.
4. Nur bei echten lesbaren Dateieinträgen: getrennt freigegebene FIT-Übertragung, vollständige Größen-/CRC-/Offset-Prüfung, Original unverändert speichern. Kein Löschen, Archivieren, SetFlags, Reset, `CLOSE_ALL` oder automatischer Retry.
5. Erst danach FIT-Decoder gegen offizielle SDK-Fixtures validieren und idempotenten SQLite-Import mit Schema-/Export-/Backup-Regressionen planen.

## Dokumentationsordnung

- **Dieser Text:** aktueller Forschungsindex und Entscheidungsstand.
- [Protokollindex](../README.md): Ordner- und Quellenwegweiser.
- [Protokollspezifikation](../CIRQA_PROTOCOL_SPEC.md): Formatverträge, jeweils mit Nachweisstatus.
- [Experimente](../experiments/): datierte Versuchsberichte, einschließlich Hardwareevidenz.
- [Gerätetests](../device-tests/): datierte Feldtests, ältere Dateien als Archiv betrachten.
- [Sync-Plan](../sync/CIRQA_HISTORY_SYNC_PLAN.md): Zielarchitektur, nicht als bereits realisierte Funktion lesen.
- [Projektstatus](../../PROJECT_STATUS.md) und [Chat-Einstieg](../../CHAT_START_HERE.md): teamweite Übergabe.

**Keine Dateien allein zur kosmetischen Umordnung verschieben:** Bestehende kanonische Pfade und Links erhalten; veraltete Aussagen mit datiertem Hinweis kennzeichnen statt historische Evidenz zu überschreiben.
