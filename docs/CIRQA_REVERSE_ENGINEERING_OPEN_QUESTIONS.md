# CIRQA Reverse Engineering – offene Fragen und widerlegte Hypothesen

**Stand:** 2026-11-17. Diese Liste priorisiert Fakten, die einen sicheren historischen Abruf blockieren. Keine Frage in dieser Datei ist eine Erlaubnis für neue BLE-Kommandos.

## Aktueller Befund

### Nachgewiesen

* Garmin Connect 5.31 besitzt einen Legacy-Dateipfad aus 5007, optional 5031, 5002 und 5004. `DeviceManager.listPendingUploadFiles` verwendet Filterwert 3; Directoryindex 0 wird gelesen.
* Connect parst Directory nach 16 übersprungenen Headerbytes in 16-Byte-Einträge; `k7h` nennt Size und Timestamp.
* Connect liest ausgewählte Directory-Dateien separat durch `extractFile(index)` und `FileManager.readFile(index)`.
* Connects `a7d` kann nach Serververarbeitung ein Device-Item via `archiveFile(index)` archivieren. Diese Operation ist für PulsLibre tabu.
* CIRQA hat einen früheren kompakten `0x8A39`-Kandidaten mit `UNSUPPORTED` beantwortet.
* Die vorhandenen CIRQA-Berichte dokumentieren positives GFDI-5000 ACK für die aktive 5050-Antwort, aber danach kein 5037, keine FileListResponse und keine Protobuf-Übertragung im beobachteten Fenster.

### Unbekannt

* CIRQA-Produkt-/Modellzuordnung und Geräte-Capabilities; keine Maske aus fremden Garmin-Modellen übernehmen.
* Ob CIRQA Legacy-GFDI-Dateitransfer oder CoreTransfer-Protobuf verwendet.
* Ob CIRQA 5007, 5031, 5002, 5004 oder einen anderen Bootstrap unterstützt.
* `v5d`-Laufzeitregistrierung an `p860.f`; Implementierungsklasse gefunden, Setter/Injection nicht nachgewiesen.
* Exakte 5002 Request-Primitive-Breiten/Offsets und die Semantik seiner Null-/Konstantfelder.
* 5002 Erfolgsgrößenfelds exakte Wire-Endian/Primitivbreite, wenn Helperbytecode nicht verfügbar ist.
* 5031 CIRQA Typ-/Subtype-Map.
* 16-Byte Directoryheader und Subfelder byte 4–6; Gerätewirkungen des Filters/Flags.
* 5004 Outer-ACK-Frame in der tatsächlichen CIRQA/ML-Übertragung, peerseitiges Retry-Timing, Wiederaufnahme nach Trennung.
* FIT-/Monitoring-/Sleep-/Activity-/Biometric-Dateiverfügbarkeit und FIT-Profile/CRC-Variante.
* Protobuf Generated-Schema-Extension-/Service-Feldnummern, Transferhandle/Wire-Chunks und Routing-Capability-Switch in CIRQA.

### CoreTransfer – belegte Builderoberfläche, nicht Wire-Format

`FileTransferProtoRequests` (`classes12.dex`, `com.garmin.device.filetransfer.b`) packt `GDIFileAccess.Service` in `GDISmartProto.Smart` Extension `fileAccessService`. Pull-Request Builder setzt Item UID/Reference, priority, supported transports, offset, optional compression window; Response verlangt Result, transport, transfer handle und validiert optionales compression window. Die nicht vorhandenen generierten Descriptoren blockieren die Feldnummern/serialized bytes. Deshalb ist die Protobuf-Rekonstruktion strukturell, keine vollständige On-Wire-Spezifikation.

## Widerlegte oder nicht bestätigte Hypothesen

| Hypothese | Ergebnis | Beleg/Limit |
|---|---|---|
| `GDIFileAccess.ItemListRequest` ist sicher der erste Request nach Connect-Handshake | **Nicht belegt; trennen** | Erreichbarer `CoreTransferManager → AgentQueueHelper → FileTransferManager` ist separater Pfad. Legacy `a7d`-Pfad nutzt GFDI-Nachrichten. |
| SYNC-Bit 3 allein startet CIRQA FileSync | **Nicht bestätigt** | 5050 mit Nullmaske und 5050 ACK; kein nachfolgendes 5037 im aufgezeichneten Fenster. Bit-3-A/B am Gerät ist nicht als bestanden dokumentiert. |
| 5050 ACK bedeutet Historienfreigabe | **Widerlegt** | ACK quittiert die Nachricht; kein 5037/FileList in der zugehörigen Beobachtung. |
| 0x8A39 kann als CIRQA FileList wiederholt werden | **Widerlegt als Supportannahme** | CIRQA antwortete dreimal `UNSUPPORTED`; Ursache (Typ, Capability, fehlender Bootstrap) bleibt ununterscheidbar. |
| `HostConfiguration`-Bit 3 ist ein CIRQA-Gerätebit | **Widerlegt** | `yb60`-Completion prüft das HostConfiguration-Set; das ist nicht Gerätebitmapsemantik. |
| Connect 5007-Pending-Filter beweist „alle Historie“ | **Nicht belegt** | Enum sagt `PENDING_UPLOADS_ONLY`; Zweck/Statusfilter geräteseitig unbekannt. |
| Connect-Archivierung ist erforderlich, um Datei zu lesen | **Nicht belegt und gefährlich** | Archivierung erscheint nach Cloud-Serververarbeitung im `a7d`-Workflow, nicht als Voraussetzung des Read-Pfads. PulsLibre sendet sie nicht. |

## Priorisierte Untersuchung

### P0 – vor jedem aktiven Datei-Befehl

1. Recover `lbb.T/U` field definitions or DEX bytecode and pin exact 5002 request bytes; separately verify 5002 success size field.
2. Inspect FileManagerCompat coroutine and low-level GFDI responder to document the complete 5004 ACK outer response and whether incoming 5004 is inherently device-mutating.
3. Reconstruct Support subtype mapping without assuming the legacy uploadable set represents CIRQA history.
4. Review modern `origin/main` parser/test changes and keep them pure offline; do not add BLE writer.

### P1 – static protocol completion

1. Recover core generated Protobuf schema descriptors, extension numbers, message tags, complete Pull/transfer status response and `GDISmartProto` envelope. If descriptors are absent, label wire numbers unknown.
2. Inspect `p5d` download queue and compare its configured destination/operations with `a7d`; do not infer it uses GFDI path.
3. Verify FIT parser and allowed file size/CRC requirements independently, not by guessed subtype names.
4. Determine what `FileManagerCompat.archive` does at wire level and document the exact destructive boundary.

### P2 – CIRQA decision evidence

1. Find a CIRQA-specific firmware/profile record or authorized passive trace with validated GFDI request/response payloads.
2. Do not expose raw health data, device identifiers or diagnostic captures in public repository.
3. After a distinct evidence review and explicit user approval, design a one-command, read-only, bounded experiment; stop on first negative/unexpected frame.

## Go/no-go

**Current decision: NO-GO for a controlled CIRQA FileSync test.** Static Connect parser work is sufficient to write offline tests and a decoder model, not enough to select a CIRQA request. Current CIRQA evidence specifically has no file-list start signal and the previous compact request was rejected. Reconsider only after a new, discriminating, source-backed/CIRQA-specific hypothesis exists and all request bytes/stop gates are verified.
