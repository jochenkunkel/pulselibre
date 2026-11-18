# CIRQA Historical Sync – kanonischer Forschungsstand (18.11.2026)

**Zweck:** Aktueller, nach Quellenstatus getrennter Einstieg für alle Agenten. GitHub-`main` und die jeweils datierten Geräteberichte bleiben maßgeblich. Dies ist keine Freigabe für neue Hardwareaktionen.

**Visueller Überblick:** [Bestätigte BLE-Sequenz und offene Transportpfade](../../diagrams/README.md).

## Nachweisstand am CIRQA

| Schritt | Evidenz | Grenze |
| --- | --- | --- |
| Bootstrap 5024/5050 → 5030 → 5007 → 5031 | Phase B: 5030 outer/result 0, 5007 Status 0, 5031 Status 0 / 12 Typen | Keine FIT-Datei |
| 5002 Directory Index 0 nach vollständiger Kette | Phase C: Antwortgröße **8192 Bytes** | Größe allein ist kein gelesener Inhalt |
| 5004 Directory-Body über BLE | Nur ein 32-Byte-Chunk; strukturell Platzhalter-Eintrag idx=0, type=0; CRC/ACK geprüft | Kein echtes Dateiverzeichnis; kein FIT-Download |
| 5043 | Als `Smart.core_service.connection_ready_notification` interpretiert; Transport-ACK untersucht | Keine Begründung für spekulatives 5044 |
| Filter 3 versus 0 | V11: gleicher Platzhalter | Filterwechsel allein löst Blocker nicht |
| Bluetooth Classic / RFCOMM | Zwei gegatterte Läufe: V1 `io-error`, V2 präzise `accept-timeout` mit SDP-UUID-Liste `[00000000-0000-0000-0000-000000000000]` | **Hypothese für dieses Gerät widerlegt:** keine Gegenverbindung, SPP-UUID `DEAB91E4-…` wird nicht angeboten. Kein FIT-Transfer; beide Gates verbraucht, kein Reset |
| Protobuf FileAccess (5043/5044) | V2: 5043-Item-List gesendet, keine 5044-Antwort in 20 s | Kein FIT-Transfer; Gates verbraucht |
| Protobuf **sync_request** (5043/5044) | V21: GFDI-ACK Status 0, `sync_response`, Item-Liste mit 33 Einträgen, erster Pull und MLR-Transfer erfolgreich | **Eine** FIT-Datei mit exakter Größe und gültigen CRCs gespeichert; Vollständigkeit/weitere Dateien noch offen |

Primärnachweise: [Historical Sync V1](../experiments/CIRQA_HISTORICAL_SYNC_V1_2026-11-18.md), [bounded Directory/FIT](../experiments/CIRQA_BOUNDED_DIRECTORY_FIT_TRANSFER_2026-11-18.md), [RFCOMM-Vorbereitung](../experiments/CIRQA_RFCOMM_PREPARATION_2026-11-18.md). Frühe Berichte sind historische Snapshots, keine aktuellen Testanweisungen.

## Wichtige Korrektur (2026-11-18)

Die negativen Ergebnisse dürfen **nicht** als „CIRQA hat keine FIT-Daten" gelesen
werden. [Eigene Connect-Geräteevidenz](../../device-tests/CIRQA_GARMIN_CONNECT_SYNC_EVIDENCE_2026-11-16.md)
belegt echte Syncs dieses CIRQA (UPLOAD `MONITORING_B`, `SLEEP_DATA`,
`BIOMETRIC`; DOWNLOAD `TRUE_UP`, `BIOMETRIC`, teils ACKED). Das Gerät besitzt
also abrufbare Daten; unsere drei getesteten Wege waren unvollständig.

Der bisherige HCI-Mitschnitt eines Connect-Syncs enthielt **6990 HCI-Records,
aber null ACL-Datenpakete** – er kann den GFDI/ATT-Kommandopath nicht zeigen.
Führende Hypothese für den fehlenden Schritt: die in Connect vor der
Datei-Verarbeitung zwingend abgeschlossene **Garmin-Authentifizierung
(5101–5112, `AuthManager mr2`, 15-s-Gate vor `FileManager`-Setup)**, die unsere
Kette nicht ausführt. Alternativ ein anderer Transport/Pfad.

**Nächster entscheidender Schritt (nicht spekulativ):** genau **ein** vollständiger,
bidirektionaler ACL/ATT-HCI-Mitschnitt eines echten Garmin-Connect-Syncs mit
diesem gepaarten Gerät (lokal privat, keine Rohdaten in Git). Erst danach die
beobachteten Kommandos als eigene Regression-Fixtures nachbauen. Das ist die in
der Geräteevidenz priorisierte Aufgabe 3.

**Neuer Nutzerbefund (18.11., abends):** Garmin Connect war den ganzen Tag
deaktiviert; direkt nach dem Start wurden **alle Daten des Tages** synchronisiert.
Das belegt einen aktiven, host-initiierten Sync beim App-Start. In Connect
(`p860.SyncMessageManager.syncNow()`) geht dem Dateiabruf zwingend ein
`Smart{core_service{sync_request{use_this_connection=false}}}` über 5043 voraus;
danach erst antwortet das Gerät mit `sync_response` und beginnt Dateien zu
liefern. Unsere bisherigen 5043-Versuche waren `file_access_service`, nicht
`sync_request` – das ist die plausibelste Erklärung für die leeren Antworten.

`CirqaSyncRequestCandidate` baut diesen 4-Byte-Smart-Inhalt
(`Smart` Feld 13 → `CoreService` Feld 1, leeres `SyncRequest`, da proto3 den
Default-false-Bool auslässt) und kapselt ihn in einen gültigen 5043-Frame.

**Gerätebefund (Gate V1, Build 1049) – positive Reaktion:**
```
5000[to=5043,status=0,payloadLen=8]        ← GFDI-ACK, Status 0 = akzeptiert
5044 vollständig, len=6, shape=13:2[2:2]  ← Smart.core_service.sync_response
requestSent=true
```
Damit ist bestätigt: Das CIRQA akzeptiert den Connect-kompatiblen Sync-Request
und antwortet mit `sync_response`. Die früheren `file_access_service`-Requests
blieben ohne Antwort, weil der Sync nicht angestoßen wurde.

**Gate V6/V7 – weiterer Durchbruch:** Das ACK der eingehenden 5044 (wie Connect)
reduzierte die 3 Wiederholungen auf eine. Das anschließende
`file_access_service`-Item-List-Request (nach `sync_response`) wurde beantwortet:
```
accepted=true; itemListSent=true; 5044x4;
proto=[13:2[2:2]; 43:2[10:2];]
```
Die zweite Antwort ist **`Smart.file_access_service(43) → Service.item_list_response(10)`**
– das Gerät besitzt also sehr wohl abrufbare Dateien. Die 12×5024 sind
Device-Info (keine File-Chunks; unplausible Offsets).

Aus der Binary extrahierte Felder für den Download:
`ItemListResponse{status=1, session_id=2, next_transaction_id=3, items=4}`,
`ItemReference{uid=1, data_type=2, data_size=3, flags=4, transaction_id=5}`,
`Service.pull_item_request=1`, `PullItemDataRequest{item=1, priority=2, supported_transports=3, offset=4}`.
**Korrektur V23:** Die erste Feldnummer ist `status=1`, nicht der Erfolgs-Enumwert.
Der unabhängige Codec weist `ItemListStatus.SUCCESS=0` und
`FAIL_UNKNOWN_SESSION_ID=1` aus. V23 (Build 1087) erhielt am Gerät Status 0,
behandelte ihn irrtümlich als Fehler und stoppte vor jedem Pull. Keine Datei
wurde in diesem Lauf gespeichert oder importiert; das V23-Einmal-Gate ist
verbraucht. Der korrigierte nächste Lauf verwendet ein frisches V24-Gate.

Die UID muss nicht interpretiert werden; ihre rohen Bytes werden unverändert
in den Pull-Request übernommen. Parser und Pull-Builder sind offline getestet.

**Gate V8 (Build 1060) – Dateiliste real gelesen:**
```
items=31; sizes=4841,5115,2399,4230,3373,1500,3714,4210,456,19975,2028,4651,...
proto=[13:2[2:2]; 43:2[10:2]; 43:2[2:2];]
```
Die 19975 B entsprechen genau dem GDXml aus der Connect-Geräteevidenz. Der
Pull-Request für den ersten Eintrag wurde beantwortet (`Service.pull_item_response`,
Feld 2). Damit ist die Dateiverfügbarkeit am CIRQA zweifelsfrei belegt.

Connect verarbeitet die Pull-Antwort zu `(transferHandle, transport, compressionWindow)`
(`com.garmin.device.filetransfer.b`, `lh80`) und liest die Bytes anschließend
über den **MLR-Transport** (`com.garmin.device.filetransfer.mlr.MLRTransportPipe`,
MultiLink-Service-IDs `8216/0x2018, 16408, …`).

**Gate V12 (Build 1067) – Pull erfolgreich:**
```
pull[result=0, transport=0, handle=264, path=false, window=null]
```
`SUCCESS` + `MULTILINK_TRANSPORT_PIPE` + `transfer_handle=264`.

**Gate V16 (Build 1073) – MLR-Daten empfangen:**
Nach Registrierung von Service `8216/0x2018` (Handle 105) und Connect's
Konfigurationsframe `[0x00, DIRECTION_READ, u32LE transferHandle]` lieferte das
Gerät **35 Chunks / 4844 Bytes** – passend zur ersten Datei (4841 B + Overhead).
Die komplette Kette ist damit am echten CIRQA nachgebaut:
`sync_request → sync_response → item_list_request → item_list_response(31 Dateien)
→ pull_item_request → pull_item_response(SUCCESS, handle) → MLR-Service → Rohbytes`.

Gate V17 ergänzt Integritätsdiagnostik (Länge, Magic, FIT-Header, SHA-256),
aber noch keine Speicherung. Kein Archiv-/Flag-/Löschbefehl gesendet.

**Gate V21 (Build 1081, Commit `8092171`) – erste FIT-Datei vollständig gespeichert und importiert:**
```
accepted=true; items=33; pull[result=0,transport=0,handle=271];
mlrService=8216; configureResponse=success; chunks=34; bytes=4841;
FIT header/file CRC valid; privateFit=CREATED; observations=32;
importable=32; sqlite=ok:32
```
Die private Datei wurde unabhängig mit exakter Länge 4841, FIT-Magic,
Header-/Datei-CRC und SHA-256 gegen den content-addressierten Dateinamen
geprüft. SQLite enthält 32 idempotente Messwertzeilen (11 `distance`,
11 `heart_rate`, 10 `speed`; 11 Zeitpunkte). Die vorherige Android-
`AccessDeniedException` beim Hardlink-Publishing wurde durch atomisches Rename
im app-privaten Verzeichnis behoben. Unit-Tests/Build lokal erfolgreich;
CI-Run [44361690905](https://github.com/jochenkunkel/pulselibre/actions/runs/44361690905)
erfolgreich. Kein eigener Archiv-, Flag-, Lösch-, Reset- oder `CLOSE_ALL`-
Befehl. **Begrenzt:** nur ein Eintrag wurde abgerufen; die übrigen Einträge,
Paginierung und Vollständigkeit sind noch nicht belegt. V21-Gate ist verbraucht.

Wichtig: Der Connect-Enum-Wert `MULTILINK_TRANSPORT_PIPE` ist **0** (die DEX
MUTF-8-Bytes `C0 80` kodieren ein NUL-Byte, nicht 64) – aus der generierten
Klasse `GDIFileAccess$TransportProtocol` direkt bestätigt.

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

**Gerätebefund:** V1 scheiterte an einem zu frühen Trigger (kein GFDI-Handle);
nach Fix lief V2 mit `requestSent=true`, aber ohne jede 5044-Antwort
(`Protobuf-Übertragungen vollständig: 0; verworfen: 0`) im 20-s-Fenster.
Damit ist auch der FileAccess-Protobuf-Weg auf diesem CIRQA erfolglos.
Beide Gates sind verbraucht; kein Reset. Offen bleibt, ob das Gerät eine
5000-Ablehnung auf unsere 5043 sandte (bisher nicht protokolliert).

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
