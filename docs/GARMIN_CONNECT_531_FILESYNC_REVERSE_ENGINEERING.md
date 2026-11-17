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

Die ausführliche vom Benutzer gelieferte OpenCode-Datei identifiziert `classes12.dex:defpackage.v5d` (`DeviceSyncManager`) als konkrete `l860`-Implementierung. `C0375m860` ruft den in `p860.f` gehaltenen Listener auf; **die konkrete Registrierung der v5d-Instanz bleibt offen**. `v5d.h` prüft initialisierte Sync-Bibliothek, Credentials, DeviceProfile/Verbindung, abgeschlossenen GFDI-Handshake, App-Sync-Zustand und Internet. Erst danach wird die `hqi`-FULL_SYNC-Strategie gestartet.

Die FULL_SYNC-Komponenten sind laut Analyse geordnet: `x6d` (Zeit-Sync), `a7d` (Gerät-zu-Host-Pending-Uploads) und `p5d` (Download-Queue). `a7d` verlangt globale Upload-Freigabe und aktivierten Auto-Upload. Der belegte **Legacy-Verzeichnisweg** ist dann `DeviceManager.listFiles(filter=3)` → `PENDING_UPLOADS_ONLY` → **GFDI 5007 Directory Filter** → gegebenenfalls **5031 Supported File Types** → **5002 Download File (Directory-Index 0)** → eingehende **5004 File Data**. Der Parser überspringt 16 Headerbytes und liest 16-Byte-Einträge. Dies ist eine Connect-Codebeobachtung, keine verifizierte CIRQA-Übertragung.

**Korrektur:** Der Protobuf-`GDIFileAccess.ItemListRequest` gehört zu einem separaten `CoreTransferManager → AgentQueueHelper → FileTransferManager`-Pfad, nicht zu dem nachgewiesenen `v5d/hqi/a7d`-Pending-Upload-Weg. Sein Builder verwendet `GDISmartProto.Smart` mit Extension `GDIFileAccessExtension.fileAccessService` und `GDIFileAccess.Service.itemListRequest`; die äußeren Wire-Feldnummern sind nicht verifiziert. Nicht als ersten CIRQA-Historienrequest voraussetzen.

PulsLibre zählt eingehende, CRC-geprüfte GFDI-Nachrichten 5007/5031/5002/5004 passiv über `CirqaLegacyDirectoryObserver`; es werden weder Payloads veröffentlicht noch Nachrichten gesendet. Offline existieren inzwischen Parser für 5031 und Directory-Einträge sowie Decoder/Reassembler/Session-Koordinator für synthetische 5002/5004-Daten. Sie sind nicht an BLE angeschlossen; die reale CRC-Implementierung und Requestcodierung sind weiterhin nicht verifiziert. Den früheren `0x8A39`-Kandidaten nicht wiederholen.

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

`HostConfiguration` ist ein Interface, das `zxl` injiziert bekommt. Die konkreten Laufzeitimplementierungen für `getAppVersion()` und `getFriendlyName()` wurden in den verfügbaren DEX-Ausgaben nicht gefunden. `a860.a()` hat zwar einen separaten App-`versionName`-Getter mit Cache und Fallback `"1.0"`, aber dessen Gleichheit mit `HostConfiguration.getAppVersion()` ist nicht belegt. `friendlyName`-Provenienz bleibt ebenfalls unbekannt. `T/U/V` sind kleine Binary-Writer-Helfer; solange die tatsächlichen `T/U`-Primitivebreiten nicht aus Bytecode/Implementierung belegt sind, die obige Folge nicht als konkreten Encoder implementieren.

### 5050, Bit-Indizes und 5030

`zxl.k()` interpretiert die empfangene Capabilitybitmap LSB-first: gesetztes Bit an Byte `n`, Bitposition `b` wird Index `8*n+b`. Connect bildet daraus ein effektives Set und fügt Bit 4 hinzu, wenn **Gerätebitmap** Bit 3 gesetzt ist, Bits 4 und 90 aber fehlen. Diese Normalisierung ist Connect-spezifisch; CIRQA-Bitsemantik unbekannt.

`yb60` sendet Nachricht 5030 mit Body exakt `[eventByte, 00]`; `HANDSHAKE_COMPLETE=8`. Die Completion prüft jedoch Bit 3 im **an `yb60` übergebenen `HostConfiguration`-Set** und stößt bei Vorhandensein `p860.f()` an. Dieser Host-Set-Test ist nicht das oben beschriebene Gerätebitmap-Bit. `p860.f()` kann `VISIBLE_AS_NEEDED` an den nicht-null `l860`-Listener melden, wenn Optionen initialisiert sind und die zugehörige Gerätebedingung Bit 4 nicht ausschließt. Der Listener selbst wird nur aufgerufen, falls `p860.f`-AtomicReference nicht null ist.

`p860.requestSync` hat separat diese Bedingungen: bei Geräte-Capability 5 sendet es `GDICore.SyncRequest` und setzt, sofern möglich, `useThisConnection=false`; ohne Bit 5 führt Bit 90 zur Ablehnung als nicht anwendbar für Sync 2; ansonsten sendet Gerätebit 4 `HOST_DID_ENTER_FOREGROUND`, andernfalls wird `f()` über einen einmaligen Gate aufgerufen. Diese Pfade sind nicht mit einem Host-FileRequest gleichzusetzen.

`v5d` implementiert `l860`, jedoch ist die Runtime-Zuweisung an `p860.f` nicht gefunden und bleibt **unbekannt**. Wenn `v5d` den Callback erhält, verlangt `FULL_SYNC` initialisierte Sync-Library, gültige Credentials, verbundenes DeviceProfile, abgeschlossenen GFDI-Handshake, Internet, keinen app-eigenen und keinen konkurrierenden Sync. CIRQA erfüllt diese Gates nicht nachweislich; seine Capability-Konfiguration bleibt unbekannt.

### Directory-Felder, 5004-Korrekturen, Dateiablage

Der Entry-Constructor `k7h` bezeichnet seine Werte als `index`, `dataType`, 3-byte `identifier`, `flags`, `size`, `timestamp`, `xmlDataType`. Damit sind Offset 8 `u32LE size` und Offset 12 `u32LE timestamp` besser belegt als die älteren unbenannten `fieldA/B`-Bezeichnungen. Identifierbyte 0 dient als Subtype; die weiteren zwei Bytes werden als u16 aus dem Identifier gelesen, ihre weitergehende Semantik ist nicht gesichert. Offset 6 wird im Directoryparser ignoriert. Der 16-byte Header bleibt opak.

Bei unkomprimiertem 5004-Offsetfehler sendet `yw40.e(4)` fünf Bytes (`result + u32 currentOffset`), nicht sechs. Das komprimierte Parserformat hat einen separaten Sequence-Bytezähler. Die Connect-CRC-Helfer sind im verfügbaren JADX-Quellbaum nicht so aufgelöst, dass PulsLibre einen produktiven Prüfer aus dieser Analyse allein portieren darf; aktuelle Offline-Reassembly verlangt eine injizierte Prüffunktion.

`DeviceManager.extractFile(index)` ruft den Legacy-Reader für einzelne Directory-Indizes. Connects `a7d` lädt/archiviert aber nach cloudseitiger Verarbeitung: Callback `archiveFile(fileIndex)` → `DeviceManager.archiveFile` → `FileManagerCompat.archive`. Das ist ein potenziell geräteverändernder Schritt. PulsLibre darf den Archivierungs-, Erase-, Write- oder Flagänderungspfad nicht benutzen.

### Separate Protobuf-Route und offene Schemafelder

`CoreTransferManager → AgentQueueHelper → FileTransferManager` baut `GDISmartProto.Smart` mit Extension `GDIFileAccessExtension.fileAccessService` und `GDIFileAccess.Service`. Pull-Builder setzt Item/UID, Priority, SupportedTransports, Offset sowie optional RequestedCompressionWindow; Response erwartet Result, Transport und TransferHandle. Die generated Schema-Descriptoren fehlen in den verfügbaren JADX-Quellen: Extensionnummer, Service-oneof-Nummern und konkrete serialized Bytes sind **unbekannt**. Kein nachgewiesener Aufruf von `v5d`/Legacy nach `ItemListRequest`; keine Vermischung mit Legacy-Dateiindex oder Chunkformat.

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

**Wichtige Unsicherheit:** Die vorhandene JADX-Rekonstruktion setzt in der Payload-Schleife `i = this.e` unmittelbar vor `this.e = u45.a(byte,i)`; nach der Schleife vergleicht sie `i` (also den vor dem letzten Byte gelesenen Stand) mit dem übertragenen CRC, nicht `this.e` (den aktualisierten Stand). Bei leerem Payload wird der aktuelle Akkumulator verglichen. Das sieht nach einem Connect-Parserfehler oder einer ungewöhnlichen Wire-Konvention aus. Für echte Bytecode-Verhaltensgleichheit muss die Registerfolge in DEX/smali noch einmal bestätigt werden. Deshalb darf der normale CRC-16/ARC Endwert nicht ohne diesen Vorbehalt als nachgewiesene CIRQA-Chunkakzeptanz ausgegeben werden.

Standardalgorithmus-Chunkvektor (nicht automatisch Passvektor des obigen dekompilierten Vergleichs): `00 3D BB 00 00 00 00 31 32 33 34 35 36 37 38 39` bedeutet unkomprimierter Byte0 `00`, CRC `0xBB3D` als LE `3D BB`, Offset 0 und ASCII-Payload `123456789`.

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


**Umsetzung in PulsLibre:** Reine Offline-Primitive `CirqaLegacyCrc16` und `CirqaLegacy5002Codec`, mit JVM-Vektoren. Keine 5004-Produktivakzeptanz vor DEX-Kontrollflussprüfung, keine BLE-Sendefunktion.
