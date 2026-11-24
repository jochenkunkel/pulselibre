# Garmin Connect 5.31 – FileSync-Reverse-Engineering: Projektübernahme

Stand: 2026-11-17. Quelle: vom Benutzer bereitgestellter OpenCode-Analysebericht mit diesem Dateinamen; proprietäre APK und Dekompilate verbleiben außerhalb des Repositories.

> **Hinweis:** Dies ist eine eigenständig formulierte, verdichtete Übernahme der im Chat gelieferten Analyse. Die ausführliche OpenCode-Originaldatei war bei Übernahme noch nicht auf GitHub veröffentlicht. Sie soll später unverändert als eigenständiger Bericht ergänzt/abgeglichen werden.

## Evidenzstatus

**Nachgewiesen im untersuchten Garmin-Connect-5.31-Code, nicht als CIRQA-Kompatibilität bewiesen:**

- `zxl` verarbeitet 5024/5050/5027. 5024 erhält ACK plus Hostfelder. 5050-Hostantwort muss abgeschlossen sein; GFDI-ACK allein bedeutet nicht Handshake COMPLETE.
- `yb60.a` sendet GFDI **5030** mit Body `[eventCode, 0]`. `HANDSHAKE_COMPLETE=8`; `HOST_DID_ENTER_FOREGROUND=6`. Bei Geräte-Capability-Bit **3** stößt die Completion des Handshake-Events `p860.f()` an (auch Fehlerpfad). Das ist eine **Sync-Aktivierung**, keine bewiesene Dateiliste.
- `p860` verarbeitet eingehende **5037** (Sync-Anforderung) und **5009** (file ready); 5027 ist ebenfalls eingehend. Diese Nachrichten sind nicht als vom Host zu sendende Startnachrichten belegt.
- `p860.c` verwendet bei Capability 5 einen `GDICore.SyncRequest` im Protobuf-Messenger. Andere Capability-Kombinationen steuern `5030 HOST_DID_ENTER_FOREGROUND` oder `p860.f()`. Bit 90 kann Sync 2 ausschließen. Die CIRQA-Bitmaske ist unbekannt.
- In `classes12.dex` verwendet `FileTransferProtoRequests.sendListFiles` einen Protobuf-`GDIFileAccess.ItemListRequest` über `GDIFileAccess.Service`/`GDISmartProto`. Pagination nutzt Response-Session-/Transaktionskontext; es gibt keinen belegten festen initialen Compact-GFDI-Counter. `PullItemDataRequest` dient dem Abruf per UID.
- Die konkreten CIRQA-Datentypen, der Listener-Call-Graph vom Handshake zur ersten Historienliste, Transfer-Reassembly, Retries und Deduplizierung sind **unbekannt**.

## Konsequenzen für PulsLibre

1. **5030 ist jetzt ein belegter Connect-Codepfad** und darf nicht pauschal ausgeschlossen werden. `CirqaConnect531SystemEvent` bildet offline die zwei Bodybytes ab. Es ist **kein BLE-Sendepfad**.
2. Die bisherige `0x8A39`-FileList-Hypothese ist keine nachgewiesene Connect-`ItemListRequest`-Nachricht; CIRQA antwortete bereits dreimal mit `UNSUPPORTED`. Nicht erneut senden.
3. Die Session-ID und `nextTransactionId` des Protobuf-FileAccess-Dienstes dürfen nicht mit dem Compact-GFDI-Headercounter verwechselt werden.
4. Keine fremden Capabilitymasken oder Geräteidentitäten übernehmen. 5030 nicht vor belegter Host-5050-Completion senden.
5. Keine Annahme, dass `5037`, `5011` oder eine Protobuf-FileList-Anfrage zwingend direkt auf 5050 folgen.

## Nächster OpenCode-Fokus

Den `SyncMessageListener` (`l860`), dessen Registrierung in `p860`/`C0375m860`, und die Call-Sites zu `AgentQueueHelper.requestDeviceFileList` (`yc1`/`C1021rd1`) bis zur ersten historischen `ItemListRequest`-Auslösung verfolgen. Parallel den Protobuf-Request-Envelope und die vom Device zurückgegebenen Session-/Transaction-IDs verifizieren. Keine angenommenen Startcounter.

## Analysequellen

Garmin Connect 5.31, versionCode 11761; APK-Bundle SHA-256 `a7cbd2bdde76d4fb804c2a07cee908adb0f9e4846a05f082e92b01f8dea1e4a5`. Lokale JADX-Referenzen: `classes.dex` `zxl`, `C0253ayl`, `yb60`, `ub60`, `p860`, `C0375m860`; `classes12.dex` `com.garmin.device.filetransfer.a`, `com.garmin.device.filetransfer.b`. Keine dekompilierten Quellen eingecheckt.


## 2026-11-17: Listener gefunden; Korrektur der Sync-Pfadzuordnung

Die ausführliche vom Benutzer gelieferte OpenCode-Datei identifiziert `classes12.dex:defpackage.v5d` (`DeviceSyncManager`) als konkrete `l860`-Implementierung. `DeviceSyncService.onCreate()` ruft `vlj.c(...).a.b(v5d.f())` auf; `vlj.a` ist `e68`, und `e68.b(ConnectionCallback)` trägt `v5d` in das Connection-Callback-Set ein. Das belegt die Registrierung als **ConnectionCallback**, nicht als `l860`-Listener in `p860.f`. `C0375m860` ruft einen dort gehaltenen Listener nur auf, wenn die AtomicReference nicht null ist; eine Zuweisung `v5d` → `p860.f` ist nicht gefunden und bleibt unbelegt. `v5d.h` prüft initialisierte Sync-Bibliothek, Credentials, DeviceProfile/Verbindung, abgeschlossenen GFDI-Handshake, App-Sync-Zustand und Internet. Erst danach wird die `hqi`-FULL_SYNC-Strategie gestartet.

Die FULL_SYNC-Komponenten sind laut Analyse geordnet: `x6d` (Zeit-Sync), `a7d` (Gerät-zu-Host-Pending-Uploads) und `p5d` (Download-Queue). `a7d` verlangt globale Upload-Freigabe und aktivierten Auto-Upload. Der belegte **Legacy-Verzeichnisweg** ist dann `DeviceManager.listFiles(filter=3)` → `PENDING_UPLOADS_ONLY` → **GFDI 5007 Directory Filter** → gegebenenfalls **5031 Supported File Types** → **5002 Download File (Directory-Index 0)** → eingehende **5004 File Data**. Der Parser überspringt 16 Headerbytes und liest 16-Byte-Einträge. Dies ist eine Connect-Codebeobachtung, keine verifizierte CIRQA-Übertragung.

**Korrektur:** Der Protobuf-`GDIFileAccess.ItemListRequest` gehört zu einem separaten `CoreTransferManager → AgentQueueHelper → FileTransferManager`-Pfad, nicht zu dem nachgewiesenen `v5d/hqi/a7d`-Pending-Upload-Weg. Sein Builder verwendet `GDISmartProto.Smart` mit Extension `GDIFileAccessExtension.fileAccessService` und `GDIFileAccess.Service.itemListRequest`; die äußeren Wire-Feldnummern sind nicht verifiziert. Nicht als ersten CIRQA-Historienrequest voraussetzen.

PulsLibre zählt eingehende GFDI-Nachrichten 5007/5031/5002/5004 passiv über `CirqaLegacyDirectoryObserver`; es werden weder Payloads veröffentlicht noch Nachrichten gesendet. Offline existieren Parser/Assembler für synthetische Transfers, nicht an BLE angeschlossen. CRC-Algorithmus und 5002-Requestcodierung sind nun aus Connect statisch rekonstruiert; **offen bleiben** die `yw40`-Vergleichsbesonderheit, CIRQA-Capabilities, konkrete CIRQA-Transferroute, GATT/ML-Transport und erfolgreiche Geräteantworten. Den früheren `0x8A39`-Kandidaten nicht wiederholen.

## 2026-11-17: CRC-16 und vollständige 5002-Serialisierung

### Quellen und Methoden

* `classes10.dex:defpackage.u45.a(byte,int)` – nibbleweise CRC-Update.
* `classes.dex:defpackage.yw40.a(byte[])` – unkomprimierter 5004-Parser und CRC-Aufruf.
* `classes3.dex:defpackage.lbb.T(ByteArrayOutputStream,int)` und `.U(ByteArrayOutputStream,long)` – 16-/32-bit Writer.
* `classes14.dex:defpackage.a60.E(int,byte[])` und `.F(int,byte[])` – unsigned LE Reader.
* `classes.dex:defpackage.wtz.c(int)` – 5002 Requestbau, Statusmapping und Retry.

Die lokalen JADX-Einzelklassen wurden nur zur statischen Prüfung verwendet und bleiben außerhalb des Repositories.

### CRC-16/ARC Update und Testvektoren

`u45.a` enthält die reflected nibble table
`0000 CC01 D801 1400 F001 3C00 2800 E401 A001 6C00 7800 B401 5000 9C01 8801 4400`.
Das entspricht CRC-16/ARC (CRC-16/IBM): `poly=0xA001` reflected (`0x8005` normal), `init=0x0000`, `refin/refout=true`, `xorout=0x0000`. Der Routineaufruf ist ein Update und nimmt den laufenden Wert entgegen; es gibt keinen final-XOR-Schritt. Beispielwerte:

| Input bytes | Result | Little-endian CRC bytes |
|---|---:|---|
| empty | `0x0000` | `00 00` |
| ASCII `123456789` (`31 32 33 34 35 36 37 38 39`) | `0xBB3D` | `3D BB` |
| `00` | `0x0000` | `00 00` |
| `01 02 03` | `0xA110` | `10 A1` |
| ASCII `FIT` (`46 49 54`) | `0xBAD7` | `D7 BA` |

`yw40` erwartet 5004-Body mindestens 7 Bytes, liest empfangenen CRC als `a60.E(1,body)` (u16 LE), Offset als `a60.F(3,body)` (u32 LE), und ruft `u45.a` ausschließlich für Payloadbytes ab Offset 7 auf. Das CRC-Feld ist damit little endian. Der `yw40.e` Akkumulator beginnt als JVM-default 0 und wird am Parserobjekt fortgeführt.

Dieselbe `u45.a`-Routine berechnet in `classes.dex:defpackage.vns.a` auch den äußeren GFDI-Frame-CRC: Startwert 0, alle Framebytes außer den finalen zwei CRC-Bytes; `a60.O` serialisiert das Ergebnis u16 LE. Das äußere Frame-CRC und das innere 5004-Payload-CRC verwenden also dieselbe Routine, aber unterschiedliche Bytebereiche.

**DEX-Kontrollfluss verifiziert:** `classes.dex` wurde mit Android SDK `dexdump -c` checksum-validiert. In `yw40.a([B)Ljow;` laden die Code-Unit-Offsets `0x0032–0x0033` `this.e` in `v7`; `0x0037–0x003d` aktualisieren und speichern `v7` pro Payloadbyte; Offset `0x0042` vergleicht `if-eq v7,v4`. Der Vergleich erfolgt nach allen Payloadupdates. DEX-Codeadresse des CRC-Aufrufs `0x4f834a`, Vergleich `0x4f835c`. Die JADX-Variable war irreführend rekonstruiert. Dies belegt Connect-Verhalten, nicht CIRQA-Kompatibilität.

Standardalgorithmus-Chunkvektor und Connect-Endvergleich: `00 3D BB 00 00 00 00 31 32 33 34 35 36 37 38 39` bedeutet unkomprimierter Byte0 `00`, CRC `0xBB3D` als LE `3D BB`, Offset 0 und ASCII-Payload `123456789`.

### 5002-Request vollständig

`lbb.T` schreibt genau u16 LE; `lbb.U` schreibt genau u32 LE, jeweils low byte zuerst. `wtz.c` schreibt:

| Offset | Bytes | Quelle/Wert |
|---:|---:|---|
| 0–1 | `T(fileIndex)` | u16 LE Index |
| 2–5 | `U(0)` | u32 LE Konstante |
| 6 | `01` | konstantes Byte |
| 7–8 | `T(0)` | u16 LE Konstante |
| 9–12 | `U(0)` | u32 LE Konstante |
| 13 | `01` oder `00` | Kompression ein/aus |

Gesamtlänge sind **14 Bytes**. `ByteArrayOutputStream(13)` im Aufrufer setzt nur Anfangskapazität und wächst bei Bedarf; es ist kein Wire-Length-Feld. Konstante Felder sind binär belegt, ihre semantischen Namen/Bedeutung jedoch nicht.

Synthetischer Request: fileIndex `0x1234`, compressed:
`34 12 00 00 00 00 01 00 00 00 00 00 00 01`.
Für Index 0, uncompressed: `00 00 00 00 00 00 01 00 00 00 00 00 00 00`.

### 5002-Response und vollständig decodierbarer Beispielwert

`a60.F(offset,bytes)` liest vier Bytes unsigned Little Endian in `long` (jedes Byte mit `0xff` maskiert; Shifts 0, 8, 16, 24). `wtz.c` interpretiert Statusbyte 0; bei Status 0 muss Länge mindestens 5 sein und es liest `F(1,body)`, castet dann zu `int`. Somit ist das Response-Bodylayout:

```text
offset 0: u8 status
offset 1: u32 LE value (bei Status 0: erwartete Dateigröße/-länge)
```

Vektor `00 78 56 34 12` → Status 0, Wert `0x12345678` = 305419896. Werte über `Int.MAX_VALUE` sind im Java-`int`-Cast negativ, obwohl `F` selbst unsigned `long` liefert; Größenlimit/Überlauf muss der PulsLibre-Decoder explizit behandeln. `NOT_READY` (Status 3) wird einmal nach 1000 ms wiederholt, zweites Status 3 schlägt fehl.

### Geltungsbereich

Das bestätigt die **Garmin Connect 5.31 Legacy-GFDI-Implementierung**, einschließlich Directoryindex 0 und Einzeldateiindex-Reads. Es beweist nicht, dass CIRQA diese Requests, Optionen oder CRC-Konvention akzeptiert. CIRQA-Capabilities, 5007/5031/5002/5004 Antworten, verwendete Kompressionsvariante und eine tatsächliche Historienübertragung bleiben unbelegt. Es wurden keine Gerätekommandos gesendet und keine Gerätedateien verändert.

## 2026-11-17: Handshake-Konfiguration und vollständiger Transfer-Call-Graph

### 5024-Hostantwort und Konfigurationsherkunft

`classes.dex:defpackage.zxl.h(protocolVersion,responder)` sendet Status-ACK mit Erweiterung in dieser Reihenfolge:

```text
T(protocol = pxk.b && protocolVersion >= 150 ? 150 : 113)
T(0xffff)
U(0xffffffff)
T(HostConfiguration.getAppVersion())
T(0xffff)
V(HostConfiguration.getFriendlyName())
V(Build.MANUFACTURER ?: "")
V(Build.MODEL ?: "")
byte 0x01
```

`pxk.b` ist im untersuchten Connect-Build **konstant true**, nicht ein unbekannter dynamischer Feature-Flag. Deren Wirkung an dieser Stelle: Protocolwert 150 für DeviceProtocol >=150, sonst 113. Die Methode hängt den payload-ACK an die Antwort für 5024. Das ist Connect-Code, keine Bestätigung, dass CIRQA dieses Layout akzeptiert.

`HostConfiguration` ist ein Interface, das `zxl` injiziert bekommt. Die konkreten Laufzeitimplementierungen für `getAppVersion()` und `getFriendlyName()` wurden in den verfügbaren DEX-Ausgaben nicht gefunden. `a860.a()` hat zwar einen separaten App-`versionName`-Getter mit Cache und Fallback `"1.0"`, aber dessen Gleichheit mit `HostConfiguration.getAppVersion()` ist nicht belegt. `friendlyName`-Provenienz bleibt ebenfalls unbekannt. `T/U/V` sind kleine Binary-Writer-Helfer; `T/U` sind inzwischen als u16LE/u32LE aufgelöst; `V` ist length-prefixed UTF-8. Die Herkunft der Konfigurationswerte bleibt offen.

### 5050, Bit-Indizes und 5030

`zxl.k()` interpretiert die empfangene Capabilitybitmap LSB-first: gesetztes Bit an Byte `n`, Bitposition `b` wird Index `8*n+b`. Connect bildet daraus ein effektives Set und fügt Bit 4 hinzu, wenn **Gerätebitmap** Bit 3 gesetzt ist, Bits 4 und 90 aber fehlen. Diese Normalisierung ist Connect-spezifisch; CIRQA-Bitsemantik unbekannt.

`yb60` sendet Nachricht 5030 mit Body exakt `[eventByte, 00]`; `HANDSHAKE_COMPLETE=8`. Die Completion prüft jedoch Bit 3 im **an `yb60` übergebenen `HostConfiguration`-Set** und stößt bei Vorhandensein `p860.f()` an. Dieser Host-Set-Test ist nicht das oben beschriebene Gerätebitmap-Bit. `p860.f()` kann `VISIBLE_AS_NEEDED` an den nicht-null `l860`-Listener melden, wenn Optionen initialisiert sind und die zugehörige Gerätebedingung Bit 4 nicht ausschließt. Der Listener selbst wird nur aufgerufen, falls `p860.f`-AtomicReference nicht null ist.

`p860.requestSync` hat separat diese Bedingungen: bei Geräte-Capability 5 sendet es `GDICore.SyncRequest` und setzt, sofern möglich, `useThisConnection=false`; ohne Bit 5 führt Bit 90 zur Ablehnung als nicht anwendbar für Sync 2; ansonsten sendet Gerätebit 4 `HOST_DID_ENTER_FOREGROUND`, andernfalls wird `f()` über einen einmaligen Gate aufgerufen. Diese Pfade sind nicht mit einem Host-FileRequest gleichzusetzen.

`v5d` implementiert `l860`, ist aber separat nachweislich als `ConnectionCallback` in `e68` registriert. Die Runtime-Zuweisung an `p860.f` ist nicht gefunden und bleibt **unbekannt**. Wenn `v5d` den Sync-Callback erhält, verlangt `FULL_SYNC` initialisierte Sync-Library, gültige Credentials, verbundenes DeviceProfile, abgeschlossenen GFDI-Handshake, Internet, keinen app-eigenen und keinen konkurrierenden Sync. CIRQA erfüllt diese Gates nicht nachweislich; seine Capability-Konfiguration bleibt unbekannt.

### Directory-Felder, 5004-Korrekturen, Dateiablage

Der Entry-Constructor `k7h` bezeichnet seine Werte als `index`, `dataType`, 3-byte `identifier`, `flags`, `size`, `timestamp`, `xmlDataType`. Damit sind Offset 8 `u32LE size` und Offset 12 `u32LE timestamp` besser belegt als die älteren unbenannten `fieldA/B`-Bezeichnungen. Identifierbyte 0 dient als Subtype; die weiteren zwei Bytes werden als u16 aus dem Identifier gelesen, ihre weitergehende Semantik ist nicht gesichert. Offset 6 wird im Directoryparser ignoriert. Der 16-byte Header bleibt opak.

Bei unkomprimiertem 5004-Offsetfehler sendet `yw40.e(4)` fünf Bytes (`result + u32 currentOffset`), nicht sechs. Das komprimierte Parserformat hat einen separaten Sequence-Bytezähler. Die CRC-Updatefunktion ist statisch als CRC-16/ARC aufgelöst; die genaue `yw40`-Vergleichsvariable muss noch im DEX-Bytecode verifiziert werden. Die Offline-Reassembly verlangt weiterhin eine injizierte Prüffunktion und ist nicht an BLE angeschlossen.

`DeviceManager.extractFile(index)` ruft den Legacy-Reader für einzelne Directory-Indizes. Connects `a7d` lädt/archiviert aber nach cloudseitiger Verarbeitung: Callback `archiveFile(fileIndex)` → `DeviceManager.archiveFile` → `FileManagerCompat.archive`. Das ist ein potenziell geräteverändernder Schritt. PulsLibre darf den Archivierungs-, Erase-, Write- oder Flagänderungspfad nicht benutzen.

### Separate Protobuf-Route und offene Schemafelder

`CoreTransferManager → AgentQueueHelper → FileTransferManager` baut `GDISmartProto.Smart` mit Extension `GDIFileAccessExtension.fileAccessService` und `GDIFileAccess.Service`. Pull-Builder setzt Item/UID, Priority, SupportedTransports, Offset sowie optional RequestedCompressionWindow; Response erwartet Result, Transport und TransferHandle. Der zum lokalen Forschungsbestand gehörende `classes12.dex`-JADX-Export enthält die generierte `GDIFileAccess`-Schemaquelle; die konkreten serialized Bytes des vollständigen Smart-Envelopes sind damit noch nicht durch einen CIRQA-Transportmitschnitt bestätigt. Kein nachgewiesener Aufruf von `v5d`/Legacy nach `ItemListRequest`; keine Vermischung mit Legacy-Dateiindex oder Chunkformat.

### 2026-11-21: statischer `ItemList`-Transaktionspfad in Connect 5.31

Der lokale JADX-Export (proprietäre APK und dekompilierte Quellen verbleiben
außerhalb des Repositories) erlaubt jetzt eine genauere Call-Graph-Prüfung:

- Generated `GDIFileAccess.ItemListRequest`: `session_id=1`,
  `transaction_id=2`, `max_item_count=3`, danach Filter/Metadatenfelder.
- Generated `ItemListResponse`: `status=1`, `session_id=2`,
  `next_transaction_id=3`, `items=4`, `error_code=5`,
  `abandoned_session_id=6`, `malformed_data_type=7`.
  `ItemListResponse.Status.SUCCESS` ist numerisch **0**.
- `ItemReference` besitzt zusätzlich ein eigenes `transaction_id=5`;
  dieses darf nicht mit der Response `next_transaction_id` verwechselt werden.
- `FileTransferProtoRequests.sendListFiles` (JADX-Klasse
  `com.garmin.device.filetransfer.b`, Methode `e`; der normale Restructure-
  Decompiler übersprang sie, Simple-Modus zeigte den Kontrollfluss) folgt
  `session_id` intern für Listenseiten. Wenn die Antwort keine Session-ID,
  aber eine `next_transaction_id` enthält, gibt die Methode stattdessen
  `ListFilesResult(files, token)` an den Aufrufer zurück. Beim folgenden
  Aufruf wird der Token im Filter als `transaction_id` in Requestfeld 2
  eingesetzt. Das ist eine getrennte Transaktion, keine Seite derselben
  Session.
- `AgentQueueHelper.requestDeviceFileList` reicht den Filter an den
  `FileTransferHandler` weiter und erhält dieses Ergebnis. `AgentQueueHelper`
  speichert den Ergebnis-Token agentbezogen für spätere Filter. In den
  geprüften Methoden ist kein automatisches, unbegrenztes Durchlaufen aller
  Transaktionen belegt; wer und unter welchen Stop-/Policy-Bedingungen den
  nächsten Aufruf auslöst, bleibt separat zu verfolgen.

#### Einordnung der leeren S25-Folgeantwort

- `nextTransaction=true` in der App-Diagnose ist eine **Presence-Anzeige** für
  Responsefeld `next_transaction_id=3`; es ist kein boolesches Protobuf-Feld.
  Der Wert `status=0` bedeutet `SUCCESS`, auch wenn `items` leer ist.
- Der S25-Lauf belegt: Nach `sync_request` war eine initiale Liste mit 28 Items
  erreichbar; eine nachfolgende Anfrage mit dem Token allein lieferte eine
  erfolgreiche leere Liste und wieder einen Token. Das belegt weder einen
  zusätzlichen Dateibestand noch einen Fehlerzustand.
- Der Connect-5.31-Builder stellt eine wichtige Vergleichsgröße bereit:
  `ItemListRequest` setzt nicht nur `transaction_id`, sondern kann im selben
  Request außerdem `exclude_all_flags`, `get_flags` und `included_data_types`
  aus dem agentbezogenen `Filter` mitführen. Die begrenzte S25-Folgeprobe
  übermittelte nur den Transaktionstoken. Ein abweichender Filterkontext ist
  daher eine **plausible, aber unbestätigte** Erklärung für abweichende
  Ergebnislisten; der Reverse-build beweist nicht, dass gerade dieser
  Unterschied die leere Antwort verursacht hat. Auch ein leerer/erschöpfter
  Gerätebestand bleibt möglich.
- Connects Aufrufer fragt die Liste je `UploadDestinationAgent` an und filtert
  anschließend die Referenzen über die Agent-Regeln, bevor `TransferItem`s
  vorbereitet werden. `ItemList` ist somit nicht gleichbedeutend mit „alle
  Dateien werden geladen“; tatsächliche Pulls sind eine getrennte Stufe.
- Die aktuelle CIRQA-Integration bildet `session_id`-Seiten ab. Wenn die
  Session-ID fehlt und `next_transaction_id` vorhanden ist, muss der Lauf als
  **unvollständig / weitere Transaktion angekündigt** ausgewiesen werden.
  Ohne neue aktive Gerätefreigabe wird der Token nicht automatisch erneut
  angefragt. Das ist ein konservativer Produktzustand, kein Beweis dafür, dass
  weitere Dateien existieren.

Abgleich mit dem einzelnen S25-Lauf: Die Folgeantwort mit 0 Items und
`status=0` ist protokollseitig ein erfolgreicher, aber leerer
`ListFilesResult`; das weiterhin vorhandene `next_transaction_id` ist ein
Token für eine mögliche weitere Transaktionsanfrage. Der S25-Test stoppte nach
der vereinbarten einen Folgeanfrage. Er beweist weder, dass die nächste
Transaktion Dateien enthält, noch, dass Connect sie unter CIRQA-Bedingungen
automatisch abruft.

### CIRQA-Testentscheidung

Die CIRQA-Berichte dokumentieren ein positives frisches 5000-ACK für die 5050-Antwort, aber danach im beobachteten Fenster **kein** 5037, FileListResponse, 5011 oder Protobuf-Transfer. `0x8A39` wurde mit `UNSUPPORTED` zurückgewiesen. Diese Befunde plus unbekannte Capabilitymaske/Requestcodierung reichen **nicht** für einen kontrollierten aktiven FileSync-Test. Die aktuelle Implementierungsvorbereitung bleibt offline; keine BLE-Sendefunktion für diese Legacy-Kommandos wurde ergänzt.

Vollständige Protokollbeschreibung, reiner Implementierungsplan und offene Fragen: `CIRQA_LEGACY_FILESYNC_PROTOCOL.md`, `CIRQA_FILE_TRANSFER_IMPLEMENTATION_PLAN.md`, `CIRQA_REVERSE_ENGINEERING_OPEN_QUESTIONS.md`.


## 2026-11-17: CRC-16 und vollständige 5002-Serialisierung

### Quellen und Methoden

* `classes10.dex:defpackage.u45.a(byte,int)` – nibbleweise CRC-Update.
* `classes.dex:defpackage.yw40.a(byte[])` – unkomprimierter 5004-Parser und CRC-Aufruf.
* `classes3.dex:defpackage.lbb.T(ByteArrayOutputStream,int)` und `.U(ByteArrayOutputStream,long)` – 16-/32-bit Writer.
* `classes14.dex:defpackage.a60.E(int,byte[])` und `.F(int,byte[])` – unsigned LE Reader.
* `classes.dex:defpackage.wtz.c(int)` – 5002 Requestbau, Statusmapping und Retry.

Die lokalen JADX-Einzelklassen wurden nur zur statischen Prüfung verwendet und bleiben außerhalb des Repositories.

### CRC-16/ARC Update und Testvektoren

`u45.a` enthält die reflected nibble table
`0000 CC01 D801 1400 F001 3C00 2800 E401 A001 6C00 7800 B401 5000 9C01 8801 4400`.
Das entspricht CRC-16/ARC (CRC-16/IBM): `poly=0xA001` reflected (`0x8005` normal), `init=0x0000`, `refin/refout=true`, `xorout=0x0000`. Der Routineaufruf ist ein Update und nimmt den laufenden Wert entgegen; es gibt keinen final-XOR-Schritt. Beispielwerte:

| Input bytes | Result | Little-endian CRC bytes |
|---|---:|---|
| empty | `0x0000` | `00 00` |
| ASCII `123456789` (`31 32 33 34 35 36 37 38 39`) | `0xBB3D` | `3D BB` |
| `00` | `0x0000` | `00 00` |
| `01 02 03` | `0xA110` | `10 A1` |
| ASCII `FIT` (`46 49 54`) | `0xBAD7` | `D7 BA` |

`yw40` erwartet 5004-Body mindestens 7 Bytes, liest empfangenen CRC als `a60.E(1,body)` (u16 LE), Offset als `a60.F(3,body)` (u32 LE), und ruft `u45.a` ausschließlich für Payloadbytes ab Offset 7 auf. Das CRC-Feld ist damit little endian. Der `yw40.e` Akkumulator beginnt als JVM-default 0 und wird am Parserobjekt fortgeführt.

Dieselbe `u45.a`-Routine berechnet in `classes.dex:defpackage.vns.a` auch den äußeren GFDI-Frame-CRC: Startwert 0, alle Framebytes außer den finalen zwei CRC-Bytes; `a60.O` serialisiert das Ergebnis u16 LE. Das äußere Frame-CRC und das innere 5004-Payload-CRC verwenden also dieselbe Routine, aber unterschiedliche Bytebereiche.

**DEX-Kontrollfluss verifiziert:** `classes.dex` wurde mit Android SDK `dexdump -c` checksum-validiert. In `yw40.a([B)Ljow;` laden die Code-Unit-Offsets `0x0032–0x0033` `this.e` in `v7`; `0x0037–0x003d` aktualisieren und speichern `v7` pro Payloadbyte; Offset `0x0042` vergleicht `if-eq v7,v4`. Der Vergleich erfolgt nach allen Payloadupdates. DEX-Codeadresse des CRC-Aufrufs `0x4f834a`, Vergleich `0x4f835c`. Die JADX-Variable war irreführend rekonstruiert. Dies belegt Connect-Verhalten, nicht CIRQA-Kompatibilität.

Standardalgorithmus-Chunkvektor und Connect-Endvergleich: `00 3D BB 00 00 00 00 31 32 33 34 35 36 37 38 39` bedeutet unkomprimierter Byte0 `00`, CRC `0xBB3D` als LE `3D BB`, Offset 0 und ASCII-Payload `123456789`.

### 5002-Request vollständig

`lbb.T` schreibt genau u16 LE; `lbb.U` schreibt genau u32 LE, jeweils low byte zuerst. `wtz.c` schreibt:

| Offset | Bytes | Quelle/Wert |
|---:|---:|---|
| 0–1 | `T(fileIndex)` | u16 LE Index |
| 2–5 | `U(0)` | u32 LE Konstante |
| 6 | `01` | konstantes Byte |
| 7–8 | `T(0)` | u16 LE Konstante |
| 9–12 | `U(0)` | u32 LE Konstante |
| 13 | `01` oder `00` | Kompression ein/aus |

Gesamtlänge sind **14 Bytes**. `ByteArrayOutputStream(13)` im Aufrufer setzt nur Anfangskapazität und wächst bei Bedarf; es ist kein Wire-Length-Feld. Konstante Felder sind binär belegt, ihre semantischen Namen/Bedeutung jedoch nicht.

Synthetischer Request: fileIndex `0x1234`, compressed:
`34 12 00 00 00 00 01 00 00 00 00 00 00 01`.
Für Index 0, uncompressed: `00 00 00 00 00 00 01 00 00 00 00 00 00 00`.

### 5002-Response und vollständig decodierbarer Beispielwert

`a60.F(offset,bytes)` liest vier Bytes unsigned Little Endian in `long` (jedes Byte mit `0xff` maskiert; Shifts 0, 8, 16, 24). `wtz.c` interpretiert Statusbyte 0; bei Status 0 muss Länge mindestens 5 sein und es liest `F(1,body)`, castet dann zu `int`. Somit ist das Response-Bodylayout:

```text
offset 0: u8 status
offset 1: u32 LE value (bei Status 0: erwartete Dateigröße/-länge)
```

Vektor `00 78 56 34 12` → Status 0, Wert `0x12345678` = 305419896. Werte über `Int.MAX_VALUE` sind im Java-`int`-Cast negativ, obwohl `F` selbst unsigned `long` liefert; Größenlimit/Überlauf muss der PulsLibre-Decoder explizit behandeln. `NOT_READY` (Status 3) wird einmal nach 1000 ms wiederholt, zweites Status 3 schlägt fehl.

### Geltungsbereich

Das bestätigt die **Garmin Connect 5.31 Legacy-GFDI-Implementierung**, einschließlich Directoryindex 0 und Einzeldateiindex-Reads. Es beweist nicht, dass CIRQA diese Requests, Optionen oder CRC-Konvention akzeptiert. CIRQA-Capabilities, 5007/5031/5002/5004 Antworten, verwendete Kompressionsvariante und eine tatsächliche Historienübertragung bleiben unbelegt. Es wurden keine Gerätekommandos gesendet und keine Gerätedateien verändert.


**Umsetzung in PulsLibre:** Reine Offline-Primitive `CirqaLegacyCrc16` und `CirqaLegacy5002Codec`, mit JVM-Vektoren. Keine BLE-Sendefunktion; CIRQA-Kompatibilität bleibt unbelegt.


## 2026-11-24: Externes Protokollreview – GarminHRM600, Gadgetbridge und garmin-proto-lab

**Zweck:** Öffentliche Vergleichsimplementierungen auf prüfbare Hinweise zu #637 (Daily-History-FIT), #506 (MLR/GFDI), #599 (History Download) und #148 (Fortsetzung) auswerten. Dieses Review ist **kein** Nachweis der CIRQA-Wire-Semantik und autorisiert keine neuen aktiven BLE-Kommandos.

### Direkt geprüfte Quellen und Evidenzgrenzen

1. [mskerwiderski/GarminHRM600 – README](https://github.com/mskerwiderski/GarminHRM600/blob/main/README.md) und [docs/gfdi-filetransfer.md](https://github.com/mskerwiderski/GarminHRM600/blob/main/docs/gfdi-filetransfer.md), per GitHub-Dateiinhalt geprüft am 24.11.2026. Das Projekt berichtet hardwarevalidierte HRM-600-Transfers, nicht CIRQA-Transfers.
2. [seofernando25/garmin-proto-lab – README](https://github.com/seofernando25/garmin-proto-lab/blob/main/README.md) und [spec/PROTOCOL.md](https://github.com/seofernando25/garmin-proto-lab/blob/main/spec/PROTOCOL.md), per GitHub-Dateiinhalt geprüft am 24.11.2026. Dort wird die Hardwareverifikation ausdrücklich als ausstehend bezeichnet.
3. [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) ist als ursprüngliche Protokollreferenz in GarminHRM600 dokumentiert; **Gadgetbridge-Quellcode wurde für dieses Review nicht eigenständig dateiweise geprüft**. Der GitHub-Mirror verweist auf Codeberg. Aussagen über Gadgetbridge sind daher Sekundärangaben aus GarminHRM600, keine direkte Codeverifikation.

### Konkrete externe Befunde

| Thema | Externe Aussage | Übertragbarkeit auf CIRQA |
|---|---|---|
| GFDI/ML | GarminHRM600 nutzt Garmin Multi-Link, GFDI und Protobuf FileSyncService; GFDI service 1. | Architekturvergleich sinnvoll; CIRQA-Wire-Bestätigung erforderlich. |
| Dateiinhalt | Beim HRM 600 folgt auf FileRequest/FileResponse mit Transfer-Handle eine Registrierung des ML-Dienstes `0x2018` und ein separater Rohdatenstrom; das Gerät sendet die Bytes dort unkomprimiert. | **Hypothese** für die noch ungeklärte CIRQA-PullItem→FIT-Korrelation. Nicht als bereits bewiesener CIRQA-Transport implementieren. |
| Zuordnung | GarminHRM600 beschreibt eine File-Grant-Nachricht (FileSyncService Feld 5) mit Zuordnung `handle → FileId`; Zuordnung nach bloßer Reihenfolge ist fehleranfällig. | Im privaten HCI-Trace nach korrespondierenden Feldern und eindeutigen File-IDs suchen. |
| ACKs | GarminHRM600 beschreibt ACKs für fragmentierte und vollständige `5044`-Antworten, um Retransmits/Fehlzuordnungen zu vermeiden. | Mit CIRQA-Trace und PulsLibre-ACK-State-Machine **offline** vergleichen. |
| Listen | GarminHRM600 dokumentiert `FileListRequest` (Servicefeld 9), `FileListResponse` (10), Cursor/Pagination sowie Flags, die bereits synchronisierte Dateien ausschließen können. | `included_data_types` und übrige Garmin-Connect-Felder aus #637 **nicht** ohne Evidenz mit diesen Flags gleichsetzen. |
| Re-List | HRM 600 kann während Sync neue Dateien finalisieren; Referenz beschreibt erneute Listenabfrage nach leerer Download-Queue. | Mögliche Lückenursache, aber nur als Testhypothese für CIRQA. |
| MLR | garmin-proto-lab dokumentiert Sequenzraum, kumulative ACKs, Sendefenster und Retransmissionsmechanik. | Hilfreiche Offline-Referenz, Hardwarevalidierung im Referenzprojekt noch offen. |
| FIT | GarminHRM600 nutzt FIT-CRC und Dateivalidierung; andere Geräte verwenden andere FIT-Message-Typen. | Keine automatische Semantik für CIRQA Global Messages 397/398 daraus ableiten. |

### Abgleich mit unserem neuen Beleg (#637)

FIT hat im Garmin-Connect-HCI-Mitschnitt vom 24.11.2026 eine bytegenaue, CRC-valide FIT-Datei mit `skin_temp_overnight` (Global Message 398) und 1.405 nicht zugeordneten Global-397-Records nachgewiesen; keine üblichen Aktivitäts-Record-/Session-/Activity-Nachrichten. Das beweist **Daily-History-bezogenen Inhalt im Capture**, nicht vollständige Tageshistorie. Ein entsprechender Quelldatensatz fehlt in den **untersuchten PR-seitigen SQLite-Vergleichsexporten**, was allein noch keinen Importfehler in jeder Datenbankinstanz beweist. Der separate 680-Zeilen-Aktivitätstest ist nicht diesem Capture zuzuordnen.

**P0-Folgeuntersuchungen für FIT (offline, nach Belegstärke priorisiert):**

1. Die **konkrete** CRC-gültige FIT-Datei per privaten SHA-256-/Byte-Offset-Metadaten von HCI→MLR→FileAccess/Transfer-Handle→Archiv nachverfolgen. Bei fehlender MLR-Rekonstruktion den Beweisstatus ausdrücklich offen lassen.
2. Auf FileAccess-Grant-/Handle→FileId-Äquivalente, fragmentierte `5044`-Antworten und korrespondierende ACKs prüfen. Nur auf Capture belegte Semantik übernehmen.
3. Garmin-Connect-`ItemListRequest` einschließlich `included_data_types` und weiterer Felder gegen PulsLibres leeren Initialrequest **byte- und feldgenau** vergleichen; Pagination, Wiederholungen und Antwort-Item-IDs erfassen. Kein unbelegter Request in Produktivcode.
4. Für die konkrete Datei getrennte Zählung **archiviert → vom Decoder gelesen → strukturierte Records → interpretierbare Beobachtungen → SQLite-Quellzeile → CSV**. Insbesondere PR-Export-Scope und bereits bekannte SQLite-Commit-Probleme berücksichtigen. Unbekannte 397-Felder nicht als erfundene Gesundheitsmetriken importieren.
5. Vor jedem Fix eine kleine synthetische Offline-Regression anlegen (ACK/Chunk/Handle-Bindung bzw. Importstatus). Action-History-Verhalten erhalten. GitHub-CI bei Codeänderungen verbindlich; keine lokale Voll-CI-Emulation erforderlich.

**Entscheidung:** Dieses Review ist dokumentarisch. **Keine** neue CIRQA-Transportfunktion, kein aktiver BLE-Test und keine automatische Umdeutung von `included_data_types` sind dadurch gerechtfertigt. Alle Aussagen sind zu unterscheiden in *extern dokumentiert*, *im CIRQA-Capture bewiesen* und *Hypothese*.
