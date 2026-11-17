# CIRQA / Garmin Connect 5.31 – Legacy-GFDI-Dateitransfer

Stand 2026-11-17. Grundlage: vom Projektinhaber bereitgestellter vollständiger Bericht `GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md` (410 Zeilen) und `GARMIN_CONNECT_531_HANDSHAKE_TEST_NOTES.md` (151 Zeilen). Diese Beschreibung ist **Connect-Code-Evidenz**, keine nachgewiesene CIRQA-Kompatibilität.

## FIT-Offline-Auswertung (kein Geräteimport)

`FitHistoricalObservations` dekodiert nach bestandenem FIT-Header-/Datei-CRC-Check nur ausgewählte Standardfelder. Die Feldnummern, Basistypen, Einheiten und Skalierungen wurden gegen Garmins `fit-python-sdk` `Profile.py`, Profilversion **21.221.0**, geprüft: https://github.com/garmin/fit-python-sdk/blob/main/garmin_fit_sdk/profile.py

Unterstützt sind Record (Global Message 20) Timestamp 253, Herzfrequenz 3, Kadenz 4, Distanz 5, Speed 6/73 und Leistung 7; außerdem Monitoring (Global Message 55) Timestamp 253, `cycles_16` 9 und Herzfrequenz 27. `cycles_16` ist laut Profil als `2 * cycles (steps)` dokumentiert und wird entsprechend verdoppelt. Kadenz bleibt rpm und wird ausdrücklich **nicht** als Schrittzahl interpretiert. Andere Felder, insbesondere unbekannte Developer Fields, werden übersprungen.

Der Parser verarbeitet FIT-Definitionen, Endianness, Developer-Field-Größen und normale/komprimierte FIT-Zeitstempel; ungültige Feldsentinels werden ausgelassen. Herzfrequenz außerhalb 1–240 bpm wird als `OUTSIDE_DISPLAY_RANGE` markiert (Qualitätsflag, keine klinische Grenze). Ohne Zeitstempel bleibt ein Wert mit `MISSING_TIMESTAMP` erhalten, darf aber nicht in eine zeitbasierte kanonische Datenbank importiert werden. SHA-256 des Originalfiles plus Nachrichten-/Feldordinal bilden den Reimport-Schlüssel; ein getrenntes Fingerprint markiert identische Zeit-/Wertbeobachtungen lediglich als mögliche Duplikate. Nichts wird automatisch zusammengeführt, verworfen oder in die Datenbank geschrieben.

Diese Offline-Komponente verarbeitet valide FIT-Bytes unabhängig von Herkunft. Sie beweist weder CIRQA-Kompatibilität noch Download-/Importfreigabe; der historische BLE-Transfer bleibt bis zu CIRQA-spezifischen Antworten und Sicherheitsfreigabe gesperrt.

## Zwei getrennte Wege

**Legacy FULL_SYNC (Connect):** 5050-Hostresponse-Completion → 5030 `HANDSHAKE_COMPLETE` (8) → unter Bedingungen `p860.f()` → optionaler `l860`-Callback (Implementierung `v5d`; als `ConnectionCallback` bei `DeviceSyncService.onCreate()` registriert, aber Bindung an `p860.f` unbelegt) → `hqi` FULL_SYNC → Zeit-Sync `x6d` → Upload-Komponente `a7d` → `FileManager` → 5007 → ggf. 5031 → 5002 Index 0 → 5004 Directory-Daten → einzelne Dateiindizes per 5002/5004.

**Separater CoreTransfer:** `CoreTransferManager → AgentQueueHelper → FileTransferManager → GDIFileAccess.ItemListRequest` in `GDISmartProto.Smart`. Kein nachgewiesener Übergang vom `l860`-Legacy-Pfad. Nicht mit Legacy-GFDI vermischen.

Der 5030-Completion-Aufruf von `p860.f()` hängt vom **an yb60 übergebenen HostConfiguration-Set Bit 3** ab, nicht nachweislich von CIRQA-Gerätebit 3. `p860.f()` benötigt initialisierte Sync-Optionen und fehlendes Gerätebit 4. Ein eingehendes gültiges 5037 ist ein alternativer Sync-Einstieg. `v5d` verlangt Sync-Bibliothek, Credentials, Gerät/Verbindung, abgeschlossenen Handshake, Internet und keinen konkurrierenden Sync.

## GFDI 5007 Directory Filter

Requestbody: exakt ein Byte Filterenum: `NO_FILTER=00`, `DEFAULT=01`, `PENDING_UPLOADS_ONLY=03`. Response mindestens ein Byte; `00` bedeutet unterstützt, sonst nicht unterstützt. Ein Nichtnullstatus wird von Connect protokolliert, führt nicht allein zum Fehler. Leere Response ist malformed.

## GFDI 5031 Supported File Types

Leerer Requestbody. Response: `u8 count`, dann je Eintrag `u8 type | u8 subtype | u8 nameLength | UTF8(nameLength)`. Bei fehlendem Support-Type-Cache wird 5031 abgefragt. Der Connect-Fallback bei `UNKNOWN_OR_NOT_SUPPORTED` enthält `(128,4,FIT_TYPE_4)` und `(128,36,FIT_TYPE_36)`; das beweist keine CIRQA-Unterstützung. Der PulsLibre-Offlineparser lehnt abgeschnittene oder ungültig UTF-8-kodierte Daten kontrolliert ab.

## GFDI 5002 Download File

`wtz.c(i)` erzeugt den Request mit `ByteArrayOutputStream(13)`; `13` ist nur dessen initiale Kapazität. Die jetzt untersuchten Writer `classes3.dex:defpackage.lbb.T/U` legen die Breiten fest:

* `T(ByteArrayOutputStream,int)`: genau zwei Bytes, low byte dann high byte (u16 LE).
* `U(ByteArrayOutputStream,long)`: genau vier Bytes, Bits 0–7, 8–15, 16–23, 24–31 (u32 LE).

Damit ist der 5002-Request vollständig:

| Offset | Länge | Serialisierung | Belegte Semantik |
|---:|---:|---|---|
| 0 | 2 | `T(fileIndex)` u16 LE | Dateiindex (`0` ist Directory) |
| 2 | 4 | `U(0)` u32 LE | Wert konstant 0; Feldbedeutung unbekannt |
| 6 | 1 | literal `01` | konstante Option; Bedeutung unbekannt |
| 7 | 2 | `T(0)` u16 LE | Wert konstant 0; Feldbedeutung unbekannt |
| 9 | 4 | `U(0)` u32 LE | Wert konstant 0; Feldbedeutung unbekannt |
| 13 | 1 | compression flag | `01` komprimierter Versuch, `00` unkomprimiert |

Bodylänge: **14 bytes** (also eine Byte mehr als die anfängliche BAOS-Kapazität; `ByteArrayOutputStream` wächst automatisch). Beispiel mit `fileIndex=0x1234`, Kompression an: `34 12 00 00 00 00 01 00 00 00 00 00 00 01`. Verzeichnisindex 0 wird bevorzugt komprimiert gelesen; bei `CANCELLED_BY_DEVICE` wird einmal unkomprimiert neu versucht. Die drei konstanten/optionalen Felder sind wire-genau, ihre Protokollsemantik bleibt unbekannt.

Response: `u8 status | u32LE value`. `classes14.dex:defpackage.a60.F(offset,bytes)` liest vier unsigned Bytes als `long` (masked `0xffL`, Shifts 0/8/16/24). Bei Success muss der Body mindestens 5 Bytes haben; `wtz.c` liest `F(1,body)` und castet anschließend zu Java `int`, weshalb Werte oberhalb `Int.MAX_VALUE` im Connect-Aufrufer signed überlaufen können. Synthetisches Beispiel: `00 78 56 34 12` → unsigned `0x12345678` = 305419896. Statusmapping: `0=SUCCESS`, `1=INDEX_MISSING`, `2=NOT_READABLE`, `3=NOT_READY`, `4=INVALID_REQUEST`, `5=CRC_MISMATCH`, `6=EXCEEDS_FILE_SIZE`. Bei Status 3 genau **ein** Retry nach 1000 ms; erneutes 3 beendet den Versuch.

## GFDI 5004 File Data

Unkomprimiert: `u8 byte0 | u16LE chunkCRC | u32LE offset | payload` (mindestens 7 Bytes). `yw40.a` liest CRC via `a60.E(1,body)`, Offset via `a60.F(3,body)` und akkumuliert ausschließlich Bytes ab Offset 7 mittels `u45.a`. Die CRC-Routine ist eine nibbleweise reflektierte CRC-16/ARC-Routine (Details und wichtiger Vergleichsbesonderheit siehe unten). `byte0` wird vom gezeigten unkomprimierten Parser nicht geprüft; seine Semantik ist hier unbekannt. Offset muss dem erwarteten Gesamtbytezähler entsprechen; exaktes Duplicate des zuletzt akzeptierten Offsets wird nicht erneut angehängt. Antwortbody `u8 result | u32LE currentOffset`: `0=OK/duplicate`, `2=malformed`, `3=CRC mismatch`, `4=offset mismatch`; `yw40.e` erzeugt genau fünf Bytes. Drei aufeinanderfolgende ungültige Chunks brechen ab.

### CRC-16: Routine, Parameter und Aufrufpfad

`classes10.dex:defpackage.u45.a(byte,int)` verwendet die 16-Einträge-Tabelle
`0000 CC01 D801 1400 F001 3C00 2800 E401 A001 6C00 7800 B401 5000 9C01 8801 4400`.
Der Helper verarbeitet low/high nibble mit reflektierter Lookup-Tabelle; äquivalent ist CRC-16/ARC (auch CRC-16/IBM): width 16, reflected polynomial `0xA001` (normal-form `0x8005`), `init=0x0000`, `refin=true`, `refout=true`, `xorout=0x0000`. Der Helper selbst hat keinen final-xor-Schritt. Aufrufpfad `yw40.a` startet mit Instanzfeld `e` (JVM-default 0), aktualisiert es für Payloadbytes 7..end. Die Vergleichsbytes sind little endian aus `a60.E(1,body)`.

Dieselbe Routine wird auch für den äußeren GFDI-Frame-CRC in `vns.a` verwendet: initial 0, über alle Nachrichtbytes außer den letzten zwei CRC-Bytes, danach schreibt `a60.O` die CRC als u16 LE. Beim 5004-Dateichunk dagegen nimmt `yw40.a` nur Bytes ab Offset 7 als Updateinput und liest das erwartete Feld bei Offset 1.

**DEX-Kontrollfluss verifiziert:** Das außerhalb des Repositorys vorhandene `classes.dex` wurde mit Android SDK `dexdump -c` checksum-validiert. In `yw40.a([B)Ljow;` laden Code-Unit-Offsets `0x0032–0x0033` `this.e` nach `v7`; `0x0037–0x003d` aktualisieren `v7` pro Payloadbyte mit `u45.a` und speichern den Wert; `0x0035` springt bei leerem Payload direkt zu `0x0042`, wo `if-eq v7,v4,0x005a` den aktualisierten Endakkumulator vergleicht. CRC-Aufruf: DEX-Adresse `0x4f834a`; Rückgabewert nach `v7`: `0x4f8350`; Vergleich: `0x4f835c`. Die frühere JADX-Variable war irreführend rekonstruiert. Das ist Connect-Evidenz, kein CIRQA-Kompatibilitätsnachweis.

#### Reproduzierbare synthetische Referenzvektoren für `u45.a`

| Datenbytes | Init | CRC numerisch | CRC als LE bytes |
|---|---:|---:|---|
| leer | `0000` | `0000` | `00 00` |
| ein Byte `41` (`A`) | `0000` | `30C0` | `C0 30` |
| `31 32 33 34 35 36 37 38 39` (`123456789`) | `0000` | `BB3D` | `3D BB` |
| `00` | `0000` | `0000` | `00 00` |
| `01 02 03` | `0000` | `A110` | `10 A1` |
| `46 49 54` (`FIT`) | `0000` | `BAD7` | `D7 BA` |

Ein synthetischer Standard-Chunk mit Payload `123456789`, Offset 0, CRC-16/ARC: `00 3D BB 00 00 00 00 31 32 33 34 35 36 37 38 39`. Der laufende Akkumulator wird über Chunks fortgeführt; ein leerer Payload vergleicht den bestehenden Wert ohne Update.

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

Offline implementiert: `CirqaLegacyDirectoryParser`, `CirqaLegacyFileTypesParser` mit JVM-Tests; passiver GFDI-Zähler `CirqaLegacyDirectoryObserver`. Kein neuer BLE-Sendepfad. CRC-Primitive, aktualisierter DEX-Vergleichsfluss und 5002-Serialisierung sind aus Connect-Code rekonstruiert; offen bleiben reale CIRQA-Capabilities, eine mögliche `v5d`-zu-`p860.f` Sync-Listener-Bindung, Dateiauswahl und CIRQA-Antworten. Die separate Registrierung von `v5d` als ConnectionCallback ist belegt. Der abgelehnte `0x8A39`-Kandidat bleibt deaktiviert. Kein Upload-Flag setzen, kein Löschen, kein CLOSE_ALL.

**Mutationswarnung:** Connects `a7d` ruft nach erfolgreichem Server-Upload `archiveFile(fileIndex)` auf. `DeviceManager.archiveFile` delegiert an asynchrones `FileManagerCompat.archive`; das ist geräteseitige Mutation und für PulsLibre verboten. Auch ERASE/WRITE/Flagänderungen dürfen im ersten Abrufpfad nicht auftreten. Die genaue Archive-Wire-Nachricht und deren Geräteseiteneffekt wurden nicht vollständig dekodiert.

## Implementierungsfortschritt (2026-11-17)

`CirqaLegacyTransferDecoder.kt` decodiert **offline** 5002-Response-Status (0–6), validiert die Mindestlänge der Success-Response und bewahrt die vier Bytes ab Offset 1 roh auf. Für 5004 extrahiert er die Felder und sendet keine ACKs. Die statischen Connect-Helfer sind inzwischen aufgelöst; der Kotlin-Decoder selbst implementiert aber weiterhin weder CRC-Prüfung noch den vollständigen Live-Reader. DEX-Bytecode bestätigt den Vergleich des aktualisierten Endakkumulators; dies ist kein CIRQA-Geräteakzeptanzvertrag.

## Offline-Reassembly (2026-11-17)

`CirqaLegacyPlainFileAssembler` implementiert ausschließlich die **unkomprimierte** 5004-Reassembly im JVM-Code. Eine injizierte CRC-Prüffunktion ist zwingend; die Connect-CRC-16/ARC-Updatefunktion ist nun statisch bekannt, aber noch nicht als produktive PulsLibre-Prüfung integriert. Der Assembler prüft erwartete Offsets, behandelt nur identische Wiederholungen als Duplicate, begrenzt die Dateigröße (Default 16 MiB), verhindert Overshoot, gibt Daten erst nach vollständigem Transfer heraus und bricht nach drei aufeinanderfolgenden Fehlern ab. Bisherige Assembler-Tests verwenden einen ausdrücklich synthetischen CRC-Prüfer. CIRQA-Kompatibilität und der komprimierte Transfer bleiben offen. Die Anwendung sendet weder 5002 noch 5004-ACKs durch diesen Baustein.

## Offline-Dateisitzung (2026-11-17)

`CirqaLegacyOfflineFileSession` verbindet 5002-Response-Decoding, `CirqaLegacyPlainFileAssembler` und optional den Directory-Parser für Index 0. Es ist eine bewusst **transportfreie** Zustandsmaschine (`AWAITING_READ_RESPONSE → RECEIVING → COMPLETE/FAILED`). Die Session sollte den Größenwert erst nach unsigned-u32LE-Decoding und Größenlimitprüfung akzeptieren; `a60.F` ist als vier-Byte-LE-Reader statisch belegt. Tests bilden einen synthetischen 5002/5004-Verzeichnistransfer und einen normalen Dateiindex nach. **Kein echter CIRQA-Download**, kein BLE und keine automatische FIT-Persistenz.

## Protobuf/CoreTransfer-Grenze

Der alternative Builder setzt `GDIFileAccess.Service` als Extension `GDIFileAccessExtension.fileAccessService` in `GDISmartProto.Smart`; der CoreTransfer-Agentpfad ist nicht als Aufruf aus `v5d`/Legacy-Listing belegt. Builder-seitig ist für Pull ein Item-UID/Reference, Priority, unterstützte Transporttypen, Offset und optional ein angefordertes Compression Window (Connect setzt 15) sichtbar. Pull-Response verlangt Result, Transport und Transfer-Handle; optionales Fenster wird nur im Bereich 9–15 akzeptiert, falls Kompression angefordert war. Generated Schema-/Extension-Descriptoren sind in den verfügbaren JADX-Quellen nicht vorhanden: Extensionnummern, Service-oneof-Feldnummern, vollständige On-Wire-Bytes und Chunktransport bleiben **unbekannt**. Nicht mit GFDI-Indizes, Sequenzen oder Countern vermischen.

## Integration nach OpenCode-Bericht (2026-11-17)

Der Stand von `main` enthält zusätzlich `CirqaLegacyFitFileSelector` als vollständig offline ausgeführte, capability-gesteuerte Vorauswahl und die Korrektur, dass die unkomprimierte Reassembly 5004-Pakete mit Nichtnull-Transferstatus zurückweist. Diese Erweiterungen sind unabhängig vom Connect-Nachweis und bleiben **nicht** an BLE angeschlossen. Die unbekannte CIRQA-Capability-/Subtype-Map ist kein Grund, unterstützte Dateitypen zu behaupten. Der im Connect-Code nachgewiesene geräteseitige Archivierungsschritt ist für PulsLibre explizit verboten.


## Nachtrag: OpenCode-Prüfung `research/gc531-crc-5002` (`eb847b5`)

### CRC-16: Routine, Parameter und Aufrufpfad

`classes10.dex:defpackage.u45.a(byte,int)` verwendet die 16-Einträge-Tabelle
`0000 CC01 D801 1400 F001 3C00 2800 E401 A001 6C00 7800 B401 5000 9C01 8801 4400`.
Der Helper verarbeitet low/high nibble mit reflektierter Lookup-Tabelle; äquivalent ist CRC-16/ARC (auch CRC-16/IBM): width 16, reflected polynomial `0xA001` (normal-form `0x8005`), `init=0x0000`, `refin=true`, `refout=true`, `xorout=0x0000`. Der Helper selbst hat keinen final-xor-Schritt. Aufrufpfad `yw40.a` startet mit Instanzfeld `e` (JVM-default 0), aktualisiert es für Payloadbytes 7..end. Die Vergleichsbytes sind little endian aus `a60.E(1,body)`.

Dieselbe Routine wird auch für den äußeren GFDI-Frame-CRC in `vns.a` verwendet: initial 0, über alle Nachrichtbytes außer den letzten zwei CRC-Bytes, danach schreibt `a60.O` die CRC als u16 LE. Beim 5004-Dateichunk dagegen nimmt `yw40.a` nur Bytes ab Offset 7 als Updateinput und liest das erwartete Feld bei Offset 1.

**DEX-Kontrollfluss verifiziert:** Das außerhalb des Repositorys vorhandene `classes.dex` wurde mit Android SDK `dexdump -c` checksum-validiert. In `yw40.a([B)Ljow;` laden Code-Unit-Offsets `0x0032–0x0033` `this.e` nach `v7`; `0x0037–0x003d` aktualisieren `v7` pro Payloadbyte mit `u45.a` und speichern den Wert; `0x0035` springt bei leerem Payload direkt zu `0x0042`, wo `if-eq v7,v4,0x005a` den aktualisierten Endakkumulator vergleicht. CRC-Aufruf: DEX-Adresse `0x4f834a`; Rückgabewert nach `v7`: `0x4f8350`; Vergleich: `0x4f835c`. Die frühere JADX-Variable war irreführend rekonstruiert. Das ist Connect-Evidenz, kein CIRQA-Kompatibilitätsnachweis.

#### Reproduzierbare synthetische Referenzvektoren für `u45.a`

| Datenbytes | Init | CRC numerisch | CRC als LE bytes |
|---|---:|---:|---|
| leer | `0000` | `0000` | `00 00` |
| ein Byte `41` (`A`) | `0000` | `30C0` | `C0 30` |
| `31 32 33 34 35 36 37 38 39` (`123456789`) | `0000` | `BB3D` | `3D BB` |
| `00` | `0000` | `0000` | `00 00` |
| `01 02 03` | `0000` | `A110` | `10 A1` |
| `46 49 54` (`FIT`) | `0000` | `BAD7` | `D7 BA` |

Ein synthetischer Standard-Chunk mit Payload `123456789`, Offset 0, CRC-16/ARC: `00 3D BB 00 00 00 00 31 32 33 34 35 36 37 38 39`. Der laufende Akkumulator wird über Chunks fortgeführt; ein leerer Payload vergleicht den bestehenden Wert ohne Update.

Komprimiert: `u8 sequence | u8 flags | u32LE uncompressedOffset | u16LE expectedCRC | deflateBytes` (mindestens 8 Bytes). Sequenz 8-bit mit Wrap; Flag `0x02` beendet den aktuellen Deflate-Stream; CRC über **dekomprimierte** Bytes. ACK/NACK: `[sequence,00]` OK, `01` malformed, `02` CRC, `03` Deflate, `04` Sequenz-/Offsetabweichung. Ohne verifizierte CRC-Routine kein produktiver Reader.


**PulsLibre-Umsetzung:** `CirqaLegacy5002Codec` (14-Byte-Offline-Request, unsigned-u32LE-Response, begrenzte Größe), `CirqaLegacyCrc16` (reiner CRC-16/ARC-Helper) und `CirqaLegacyOfflineFileSession.beginFrom5002Response`. Keine BLE-Integration; DEX bestätigt den aktualisierten Endakkumulator, CIRQA-Kompatibilität bleibt unbelegt.
