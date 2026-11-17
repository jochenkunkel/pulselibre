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

`wtz.c(i)` erzeugt einen 13-Byte-Requestbody in der beobachteten Schreibfolge `T(fileIndex) || U(0) || 01 || T(0) || U(0) || compressionFlag`. Die Primitive `lbb.T/U` sind noch nicht verifiziert; **keine Feldbreiten/Offsets raten oder einen aktiven Request daraus konstruieren**. Verzeichnisindex 0 wird bevorzugt komprimiert gelesen; bei `CANCELLED_BY_DEVICE` wird einmal unkomprimiert neu versucht.

Response Status an Byte 0: `0=SUCCESS` (mindestens 5 Bytes, `a60.F(1,body)` liefert erwartete Größe/Parameter), `1=INDEX_MISSING`, `2=NOT_READABLE`, `3=NOT_READY`, `4=INVALID_REQUEST`, `5=CRC_MISMATCH`, `6=EXCEEDS_FILE_SIZE`. Bei Status 3 genau **ein** Retry nach 1000 ms; erneutes 3 beendet den Versuch.

## GFDI 5004 File Data

Unkomprimiert: `u8 status | u16LE chunkCRC | u32LE offset | payload` (mindestens 7 Bytes). CRC `u45.a` ausschließlich über Payloadbytes, Algorithmus noch zu verifizieren. Offset muss dem erwarteten Gesamtbytezähler entsprechen; exaktes Duplicate des zuletzt akzeptierten Offsets wird nicht erneut angehängt. Antwortbody `u8 result | u32LE currentOffset`: `0=OK/duplicate`, `2=malformed`, `3=CRC mismatch`, `4=offset mismatch` (Connect fügt ein zusätzliches Byte an; Bedeutung unbekannt). Drei aufeinanderfolgende ungültige Chunks brechen ab.

Komprimiert: `u8 sequence | u8 flags | u32LE uncompressedOffset | u16LE expectedCRC | deflateBytes` (mindestens 8 Bytes). Sequenz 8-bit mit Wrap; Flag `0x02` beendet den aktuellen Deflate-Stream; CRC über **dekomprimierte** Bytes. ACK/NACK: `[sequence,00]` OK, `01` malformed, `02` CRC, `03` Deflate, `04` Sequenz-/Offsetabweichung. Ohne verifizierte CRC-Routine kein produktiver Reader.

## Directory-Datei (Index 0)

16 Bytes Header werden übersprungen, Bedeutung unbekannt. Danach 16-Byte-Einträge (little endian):

| Offset | Länge | Bedeutung |
|---:|---:|---|
| 0 | 2 | u16 LE fileIndex |
| 2 | 1 | u8 type |
| 3 | 3 | rohe Subtypebytes; Byte 0 als Subtype |
| 6 | 1 | ignoriert/reserviert |
| 7 | 1 | Attribute: READ=0x80, WRITE=0x40, ERASE=0x20, ARCHIVE=0x10, APPEND=0x08, CRYPTO=0x04 |
| 8 | 4 | u32 LE fieldA; möglicher File-Size-Wert, nicht abschließend verifiziert |
| 12 | 4 | u32 LE fieldB; Semantik unbekannt |

Connect ignoriert trailing 1–15 Bytes; PulsLibre lehnt unvollständige Einträge defensiv ab. `a7d` filtert nach type=0x80 und Upload-/Supported-Subtypes und liest ausgewählte Indizes über `DeviceManager.extractFile → FileManager.readFile → 5002/5004`. Die CIRQA-Subtype-Menge ist unbekannt.

## Umsetzung und Grenzen

Offline implementiert: `CirqaLegacyDirectoryParser`, `CirqaLegacyFileTypesParser` mit JVM-Tests; passiver GFDI-Zähler `CirqaLegacyDirectoryObserver`. Kein neuer BLE-Sendepfad. Offene Punkte: CRC `u45.a`, `lbb.T/U`, `a60.F`, reale CIRQA-Capabilities, v5d-Registrierung, Dateiauswahl, persistente Speicherung und Geräteantworten. Der abgelehnte `0x8A39`-Kandidat bleibt deaktiviert. Kein Upload-Flag setzen, kein Löschen, kein CLOSE_ALL.

## Implementierungsfortschritt (2026-11-17)

`CirqaLegacyTransferDecoder.kt` decodiert **offline** 5002-Response-Status (0–6), validiert die Mindestlänge der Success-Response und bewahrt die vier Bytes ab Offset 1 ausdrücklich als rohe Bytes auf, bis `a60.F` verifiziert ist. Für 5004 extrahiert er aus unkomprimierten Chunks Status, CRC16, u32LE Offset und Payload; für komprimierte Chunks Sequenz, Flags, u32LE Offset, CRC16 und Deflate-Rohdaten. JVM-Tests enthalten synthetische Grenzwerte. Der Decoder **validiert keine CRC**, reassembliert keine Daten und sendet keine ACKs. Nicht für eine produktive Dateiübertragung verwenden, bis `u45.a`, `a60.F`, `lbb.T/U` und die Transferzustandsmaschine belegt und getestet sind.

## Offline-Reassembly (2026-11-17)

`CirqaLegacyPlainFileAssembler` implementiert ausschließlich die **unkomprimierte** 5004-Reassembly im JVM-Code. Eine injizierte CRC-Prüffunktion ist zwingend; ohne nachgewiesene `u45.a`-Implementierung ist keine Verbindung zum Live-BLE-Pfad vorgesehen. Es prüft erwartete Offsets, behandelt nur identische Wiederholungen als Duplicate, begrenzt die Dateigröße (Default 16 MiB), verhindert Overshoot, gibt Daten erst nach vollständigem Transfer heraus und bricht nach drei aufeinanderfolgenden Fehlern ab. Die zugehörigen Tests verwenden einen ausdrücklich synthetischen CRC-Prüfer. Die Semantik des 5004-Statusbytes, der reale CRC-Algorithmus und der komprimierte Transfer bleiben offen. Die Anwendung sendet weder 5002 noch 5004-ACKs durch diesen Baustein.
