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
