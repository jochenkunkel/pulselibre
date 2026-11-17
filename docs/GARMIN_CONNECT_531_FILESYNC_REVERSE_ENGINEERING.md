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
