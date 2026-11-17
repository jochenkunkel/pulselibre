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

PulsLibre zählt nun eingehende, CRC-geprüfte GFDI-Nachrichten 5007/5031/5002/5004 passiv über `CirqaLegacyDirectoryObserver`; es werden weder Payloads veröffentlicht noch Nachrichten gesendet. **Noch offen:** `v5d`-Registrierung, CIRQA-Capabilities, die für CIRQA tatsächlich benutzte Dateitransferroute, 5007-/5002-Requestbytes, GATT/ML-Transport und erfolgreiche Geräteantworten. Den früheren `0x8A39`-Kandidaten nicht wiederholen.
