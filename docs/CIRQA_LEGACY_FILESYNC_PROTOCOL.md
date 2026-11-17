# CIRQA / Garmin Connect 5.31 – Legacy-GFDI-Dateitransfer

Stand 2026-11-17. Grundlage: vom Projektinhaber bereitgestellter vollständiger Bericht `GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md` (410 Zeilen) und `GARMIN_CONNECT_531_HANDSHAKE_TEST_NOTES.md` (151 Zeilen). Diese Beschreibung ist **Connect-Code-Evidenz**, keine nachgewiesene CIRQA-Kompatibilität.

## Zwei getrennte Wege

**Legacy FULL_SYNC (Connect):** 5050-Hostresponse-Completion → 5030 `HANDSHAKE_COMPLETE` (8) → unter Bedingungen `p860.f()` → `l860` (Implementierung `v5d`, tatsächliche Registrierung unbekannt) → `hqi` FULL_SYNC → Zeit-Sync `x6d` → Upload-Komponente `a7d` → `FileManager` → 5007 → ggf. 5031 → 5002 Index 0 → 5004 Directory-Daten → einzelne Dateiindizes per 5002/5004.

**Separater CoreTransfer:** `CoreTransferManager → AgentQueueHelper → FileTransferManager → GDIFileAccess.ItemListRequest` in `GDISmartProto.Smart`. Kein nachgewiesener Übergang vom `l860`-Legacy-Pfad. Nicht mit Legacy-GFDI vermischen.

Der 5030-Completion-Aufruf von `p860.f()` hängt vom **an yb60 übergebenen HostConfiguration-Set Bit 3** ab, nicht nachweislich von CIRQA-Gerätebit 3. `p860.f()` benötigt initialisierte Sync-Optionen und fehlendes Gerätebit 4. Ein eingehendes gültiges 5037 ist ein alternativer Sync-Einstieg. `v5d` verlangt Sync-Bibliothek, Credentials, Gerät/Verbindung, abgeschlossenen Handshake, Internet und keinen konkurrierenden Sync.

## GFDI 5007 Directory Filter

Requestbody: exakt ein Byte Filterenum: `NO_FILTER=00`, `DEFAULT=01`, `PENDING_UPLOADS_ONLY=03`. Response mindestens ein Byte; `00` bedeutet unterstützt, sonst nicht unterstützt. Ein Nichtnullstatus wird von Connect protokolliert, führt nicht allein zum Fehler. Leere Response ist malformed.

## GFDI 5031 Supported File Types

Leerer Requestbody. Response: `u8 count`, dann je Eintrag `u8 type | u8 subtype | u8 nameLength | UTF8(nameLength)`. Bei fehlendem Support-Type-Cache wird 5031 abgefragt. Der Connect-Fallback bei `UNKNOWN_OR_NOT_SUPPORTED` enthält `(128,4,FIT_TYPE_4)` und `(128,36,FIT_TYPE_36)`; das beweist keine CIRQA-Unterstützung. Der PulsLibre-Offlineparser lehnt abgeschnittene oder ungültig UTF-8-kodierte Daten kontrolliert ab.

## GFDI 5002 Download File

`wtz.c(i)` verwendet `ByteArrayOutputStream(13)` (initiale Kapazität, **nicht Beweis der serialisierten Bodylänge**) und schreibt in der beobachteten Reihenfolge `T(fileIndex) || U(0) || 01 || T(0) || U(0) || compressionFlag`. Die Primitive `lbb.T/U` sind noch nicht verifiziert; **keine Feldbreiten/Offsets raten oder einen aktiven Request daraus konstruieren**. Verzeichnisindex 0 wird bevorzugt komprimiert gelesen; bei `CANCELLED_BY_DEVICE` wird einmal unkomprimiert neu versucht.

Response Status an Byte 0: `0=SUCCESS` (mindestens 5 Bytes, `a60.F(1,body)` liefert erwartete Größe/Parameter), `1=INDEX_MISSING`, `2=NOT_READABLE`, `3=NOT_READY`, `4=INVALID_REQUEST`, `5=CRC_MISMATCH`, `6=EXCEEDS_FILE_SIZE`. Bei Status 3 genau **ein** Retry nach 1000 ms; erneutes 3 beendet den Versuch.

## GFDI 5004 File Data

Unkomprimiert: `u8 status | u16LE chunkCRC | u32LE offset | payload` (mindestens 7 Bytes). CRC `u45.a` ausschließlich über Payloadbytes, Algorithmus noch zu verifizieren. Offset muss dem erwarteten Gesamtbytezähler entsprechen; exaktes Duplicate des zuletzt akzeptierten Offsets wird nicht erneut angehängt. Antwortbody `u8 result | u32LE currentOffset`: `0=OK/duplicate`, `2=malformed`, `3=CRC mismatch`, `4=offset mismatch`. Connects `e(4)` erzeugt fünf Bytes (Result plus aktuellem u32-Offset); ein sechstes Byte gibt es nicht. Drei aufeinanderfolgende ungültige Chunks brechen ab.

Komprimiert: `u8 sequence | u8 flags | u32LE uncompressedOffset | u16LE expectedCRC | deflateBytes` (mindestens 8 Bytes). Sequenz 8-bit mit Wrap; Flag `0x02` beendet den aktuellen Deflate-Stream; CRC über **dekomprimierte** Bytes. ACK/NACK: `[sequence,00]` OK, `01` malformed, `02` CRC, `03` Deflate, `04` Sequenz-/Offsetabweichung. Ohne verifizierte CRC-Routine kein produktiver Reader.

## Directory-Datei (Index 0)

16 Bytes Header werden übersprungen, Bedeutung unbekannt. Danach 16-Byte-Einträge (little endian):

| Offset | Länge | Bedeutung |
|---:|---:|---|
| 0 | 2 | u16 LE fileIndex |
| 2 | 1 | u8 type |
| 3 | 3 | `identifier[3]`; Byte 0 als Subtype, Bytes 1–2 vom `k7h`-Modell als u16 Identifier weitergelesen (genauere Semantik unbekannt) |
| 6 | 1 | specific/reserved byte, Listingparser überspringt |
| 7 | 1 | Flags: READ=0x80, WRITE=0x40, ERASE=0x20, ARCHIVE=0x10, APPEND=0x08, CRYPTO=0x04 |
| 8 | 4 | u32 LE `size` (Kotlin-Metadaten des `k7h`-Konstruktors) |
| 12 | 4 | u32 LE `timestamp` (Kotlin-Metadaten des `k7h`-Konstruktors) |

Connect ignoriert trailing 1–15 Bytes; PulsLibre lehnt unvollständige Einträge defensiv ab. `a7d` filtert nach type=0x80 und Upload-/Supported-Subtypes und liest ausgewählte Indizes über `DeviceManager.extractFile → FileManager.readFile → 5002/5004`. Die CIRQA-Subtype-Menge ist unbekannt. Die ersten 16 Headerbytes werden übersprungen und bleiben vollständig unbekannt; das Directory-Format zeigt im geparsten Eintrag kein explizites CRC-Feld.

## Umsetzung und Grenzen

Offline implementiert: `CirqaLegacyDirectoryParser`, `CirqaLegacyFileTypesParser` mit JVM-Tests; passiver GFDI-Zähler `CirqaLegacyDirectoryObserver`. Kein neuer BLE-Sendepfad. Offene Punkte: CRC `u45.a`, `lbb.T/U`, `a60.F`, reale CIRQA-Capabilities, v5d-Registrierung, Dateiauswahl, persistente Speicherung und Geräteantworten. Der abgelehnte `0x8A39`-Kandidat bleibt deaktiviert. Kein Upload-Flag setzen, kein Löschen, kein CLOSE_ALL.

**Mutationswarnung:** Connects `a7d` ruft nach erfolgreichem Server-Upload `archiveFile(fileIndex)` auf. `DeviceManager.archiveFile` delegiert an asynchrones `FileManagerCompat.archive`; das ist geräteseitige Mutation und für PulsLibre verboten. Auch ERASE/WRITE/Flagänderungen dürfen im ersten Abrufpfad nicht auftreten. Die genaue Archive-Wire-Nachricht und deren Geräteseiteneffekt wurden nicht vollständig dekodiert.

## Implementierungsfortschritt (2026-11-17)

`CirqaLegacyTransferDecoder.kt` decodiert **offline** 5002-Response-Status (0–6), validiert die Mindestlänge der Success-Response und bewahrt die vier Bytes ab Offset 1 ausdrücklich als rohe Bytes auf, bis `a60.F` verifiziert ist. Für 5004 extrahiert er aus unkomprimierten Chunks Status, CRC16, u32LE Offset und Payload; für komprimierte Chunks Sequenz, Flags, u32LE Offset, CRC16 und Deflate-Rohdaten. JVM-Tests enthalten synthetische Grenzwerte. Der Decoder **validiert keine CRC**, reassembliert keine Daten und sendet keine ACKs. Nicht für eine produktive Dateiübertragung verwenden, bis `u45.a`, `a60.F`, `lbb.T/U` und die Transferzustandsmaschine belegt und getestet sind.

## Offline-Reassembly (2026-11-17)

`CirqaLegacyPlainFileAssembler` implementiert ausschließlich die **unkomprimierte** 5004-Reassembly im JVM-Code. Eine injizierte CRC-Prüffunktion ist zwingend; ohne nachgewiesene `u45.a`-Implementierung ist keine Verbindung zum Live-BLE-Pfad vorgesehen. Es prüft erwartete Offsets, behandelt nur identische Wiederholungen als Duplicate, begrenzt die Dateigröße (Default 16 MiB), verhindert Overshoot, gibt Daten erst nach vollständigem Transfer heraus und bricht nach drei aufeinanderfolgenden Fehlern ab. Die zugehörigen Tests verwenden einen ausdrücklich synthetischen CRC-Prüfer. Die Semantik des 5004-Statusbytes, der reale CRC-Algorithmus und der komprimierte Transfer bleiben offen. Die Anwendung sendet weder 5002 noch 5004-ACKs durch diesen Baustein.

## Offline-Dateisitzung (2026-11-17)

`CirqaLegacyOfflineFileSession` verbindet 5002-Response-Decoding, `CirqaLegacyPlainFileAssembler` und optional den Directory-Parser für Index 0. Es ist eine bewusst **transportfreie** Zustandsmaschine (`AWAITING_READ_RESPONSE → RECEIVING → COMPLETE/FAILED`). Die Dateilänge muss vom Aufrufer als **separat verifizierter Wert** kommen, weil `a60.F` noch nicht verifiziert wurde; aus vier rohen 5002-Bytes wird keine Größe geraten. Für echte Geräteübertragung ist außerdem die reale `u45.a`-CRC-Implementierung erforderlich. Tests bilden einen synthetischen 5002/5004-Verzeichnistransfer und einen normalen Dateiindex nach. **Kein echter CIRQA-Download**, kein BLE und keine automatische FIT-Persistenz.

## Protobuf/CoreTransfer-Grenze

Der alternative Builder setzt `GDIFileAccess.Service` als Extension `GDIFileAccessExtension.fileAccessService` in `GDISmartProto.Smart`; der CoreTransfer-Agentpfad ist nicht als Aufruf aus `v5d`/Legacy-Listing belegt. Builder-seitig ist für Pull ein Item-UID/Reference, Priority, unterstützte Transporttypen, Offset und optional ein angefordertes Compression Window (Connect setzt 15) sichtbar. Pull-Response verlangt Result, Transport und Transfer-Handle; optionales Fenster wird nur im Bereich 9–15 akzeptiert, falls Kompression angefordert war. Generated Schema-/Extension-Descriptoren sind in den verfügbaren JADX-Quellen nicht vorhanden: Extensionnummern, Service-oneof-Feldnummern, vollständige On-Wire-Bytes und Chunktransport bleiben **unbekannt**. Nicht mit GFDI-Indizes, Sequenzen oder Countern vermischen.

## Integration nach OpenCode-Bericht (2026-11-17)

Der Stand von `main` enthält zusätzlich `CirqaLegacyFitFileSelector` als vollständig offline ausgeführte, capability-gesteuerte Vorauswahl und die Korrektur, dass die unkomprimierte Reassembly 5004-Pakete mit Nichtnull-Transferstatus zurückweist. Diese Erweiterungen sind unabhängig vom Connect-Nachweis und bleiben **nicht** an BLE angeschlossen. Die unbekannte CIRQA-Capability-/Subtype-Map ist kein Grund, unterstützte Dateitypen zu behaupten. Der im Connect-Code nachgewiesene geräteseitige Archivierungsschritt ist für PulsLibre explizit verboten.
