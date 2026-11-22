# S10 – zweiter Dump (2026-11-22): offline gesicherte Analyse

## Scope und Datenschutz

Dieses Dokument enthält ausschließlich **redaktierte Metadaten und abgeleitete
Befunde**. Keine Roh-BLE-Payloads, Geräteadressen, Tokens, UIDs, FIT-Bytes oder
Gesundheitsdaten. Die Rohdaten liegen nur im lokalen, nicht git-getrackten
0700/0600-Backup (`session-backup-opencode-s10-20261122-dump2`).

## Eingangsdaten (hashes geprüft, unverändert gesichert)

| Artefakt | Bytes | SHA-256 (gekürzt) |
|---|---:|---|
| `s10-after-sync-20261122.zip` | 42.985.879 | `5ff7c6e2…d29cd17` |
| `btsnoop_hci.log` | 1.006.684 | `8f564f9b…7b13a2c4` |
| `timeline.txt` | 130.737 | `24c57ab3…f324b38d8` |
| `summary.md` | 1.615 | `6e61c6ca…51837833f` |

## Unabhängige Validierung

`tools/btsnoop_summary.py` (datenschutzkonformer, metadaten-only Validator):

```
{"acl_records": 7376, "complete_att_candidates": 6940, "records": 15968,
 "sufficient_for_att_analysis": true,
 "types": {"acl": 7376, "command": 2229, "event": 6363}, ...}
```

Richtungsbeleg über ATT-Opcodes (wie schon im ersten Dump): Write-Request
`0x52`/OP 82 auf Direction 0 (3.284×), Notification `0x1b`/OP 27 auf
Direction 1 (3.453×). Die Richtung ist damit erneut bestätigt.

## GFDI-/ItemList-Befunde

- CRC-validierte GFDI-Frames: 5043 (Requests) und 5044 (Responses) auf den
  Direction-0-/Direction-1-Kanälen 2, 3, 4 und 7. Keine verwertbaren Frames auf
  den Kanälen 0, 1, 5 und 6.
- Service-Feld-Verteilung (gezählt in Protobuf-Feld 43):
  - Direction 0: u. a. Feld 1/15 (Item-List-Bestätigung), Feld 21 (kleine
    Status-/Zählerfelder: 0–2 Bytes, nur varint-Feld 1), Feld 9 (List-Request),
    Feld 3 (Item-Referenzen, z. B. 38× auf Kanal 4).
  - Direction 1: korrespondierende Felder 2, 5, 16, 17 (Result/Pull-Antworten),
    Feld 4 (Item-Daten), Feld 10 (Page-Antwort).
- Frames mit Service-Feld 21 sind **nicht** die gesuchte Byte-Ebene: die
  Payloads sind 0–2 Bytes groß und enthalten nur ein varint-Feld. Die großen
  5044-Frames (max. ~3.765 Bytes pro Frame) sind Protobuf-Struktur, keine
  FIT-Datei-Chunks.
- Page-Sequenz der letzten Connection (Kanal 1/7): `11, 1, 0, 0` Items mit
  `status=0` und `next_transaction=true` – deckt sich mit `summary.md`.
- Pull-Responses: 18/16/20/6 pro Kanal, alle `resultCode=0`, `transport=0`,
  `compressionWindow=11`. Set-Flags-Responsen likewise `result=0`.
- `result counts per stream` enthält u. a. `(1,4),19,1: 1` und
  `(1,2),19,1: 1`. Beide Service-19-Nachrichten enthalten **nur ein einziges
  varint-Feld `1 = 1`**; die Deutung als „Fehler result=1" ist eine
  Analysten-Annahme des Zählskripts, keine belegte Semantik. Service 2/4/16
  antworten dagegen flächendeckend `result=0`.

## Komprimierte Nutzdaten in den großen 5044-Frames

Die großen 5044-Frames enthalten `1f 8b 08` (gzip-Magic). Ein gezielter Scan
aller CRC-validierten GFDI-Frames fand **21 dekomprimierbare gzip-Streams**
(Offset 32 in den Frames, 705–8.353 Bytes dekomprimiert). Alle dekomprimierten
Nutzdaten sind **JSON-Strukturen mit Wettervorhersage-Daten**, **keine**
FIT-Dateien. Die gezählten Keys (Metadata-only-Auswertung):

```
value, units, description, wind, speed, directionString, direction,
precipProb, epochSeconds, gustSpeed, dewPoint, relativeHumidity,
feelsLikeTemperature, visibility, pressure, ceilingHeight, temp, uvIndex,
airQuality, cloudCover, dayOfWeek, summary, high, low, epochSunrise,
epochSunset, humidity, locationName, windChill, pressureChange, ...
```

Es gibt **keine** Datei-/URI-/Download-/FIT-bezogenen Keys (nur `id` als
Wetter CONDITIONS-ID). Damit ist die bislang ungeklärte große Byte-Ebene
identifiziert: es handelt sich um komprimierte **Wettervorhersage-Daten im
JSON-Format**, nicht um FIT-Bytes.

## FIT-Byte-Ebene auf den MLR-Kanälen (verifiziert)

Nach Hinweis des FIT-Agenten wurden die bislang als „keine GFDI-Frames"
eingeordneten MLR-Kanäle separat geprüft: die ATT-Werte tragen **zlib-komprimierte
Transferbytes**. Nach Entfernen von zwei Headerbytes pro ATT-Wert dekomprimieren
die Kandidaten vollständig. Unabhängige Verifikation (nur Metadaten):

- Direkter Scan (`data[2:]`, zlib pro ATT-Wert): 3 FIT-Dateien.
- Erweiterter Scan (Offsets 1–6 und reassemblierte MLR-Streams):
  **8 dekomprimierte FIT-Dateien, 8/8 mit `.FIT` bei Offset 8**,
  Header-Size 14, Proto 16, Profile 21216/21220.
- **Alle 8** mit gültigem Header-CRC, gültiger Gesamtgröße
  (Header + deklarierte Data-Size + 2 CRC-Bytes) und gültigem Data-CRC.
- Dekomprimierte Größen 72–568 Bytes (deklarierte Data-Sizes 56–552 Bytes).
- Zeitpunkte der direkten ATT-Funde: 1554.312 s / 1561.603 s / 2875.468 s,
  alle Direction 0.

**Damit ist die Byte-Ebene der Kette belegt:**
`ItemReference → 5043 → 5044 → zlib → FIT-Bytes → Header-/Data-CRC → Größe`.
Offen bleiben der **DB-Import-Nachweis** (keine App-Datenbank im Bugreport) und
die **Vollständigkeit** über alle historischen Dateien.

## Suche nach FIT-Bytes und Dateiintegrität

- **Kein** `.FIT`-Magic in den CRC-validierten GFDI-Frames und in den
  dekomprimierten gzip-JSON-Streams (0 Treffer) – die FIT-Dateien liegen
  ausschließlich in den zlib-Streams der MLR-Kanäle (siehe oben).
- Der Bugreport enthält **keine** App-Datenbank, keine `.fit`/`.gpx`/`.tcx`-Datei
  und kein `data/data/de.pulselibre/…` (3.847 Dateien, vor allem System-Logs und
  Tombstones).
- `dumpstate-…txt` enthält nur Bluetooth-System-Logs (u. a.
  `CIRQA Smart Band`, `btsnoop`-Property-Zugriffe), keine App-/FileSync- oder
  FIT-Import-Ereignisse.
- Die Bluetooth-Aktivität im Mitschnitt stammt nach Dumpstate-Zeitstempeln vom
  21.11. abends; der Snoop-Schalter war vor dem Dump bereits auf `1`. Die
  chronologische Zuordnung zur letzten CIRQA-Connection bleibt plausible, ist
  aber **kein** unabhängiger App-Nachweis.

## Konsequenz für P0

Die Kette `ItemReference → 5043 → 5044 → zlib → FIT-Bytes → CRC/Größe → Import` ist
**teilweise** geschlossen: FIT-Bytes, Header-/Data-CRC und per-Datei-Größen
sind aus dem Mitschnitt belegt (siehe oben). Weiterhin fehlen:

1. ~~FIT-Bytes als eigene, dekodierbare Byte-Ebene~~ – nachgewiesen via zlib.
2. ~~Per-Datei-Größen und CRC aus dem Mitschnitt~~ – nachgewiesen (8/8 valid).
3. DB-Import-Nachweis (keine App-Datenbank im Bugreport).
4. Vollständigkeitsnachweis über alle historischen Dateien.

`coverage=device-history-unverified` bleibt daher unverändert gültig (Import und
Vollständigkeit offen). Es wurden keine neuen Geräteaktionen unternommen oder
freigegeben; eine Produktänderung steht beim FIT-Agenten.

## Erzeugte Analysen (nur privat)

Im Backup unter `analysis/`:
`full_analyzer.out`, `service21.out`, `service21b.out`, `fit_search.out`,
`field21.out`, `check_all_channels.out`, `check_service_fields.out`,
`decode_pull_results.out`, `item_shape_counter.out`, `trace_pagination.out`,
`trace_pagination_timed.out`, `enumerate_att2.out`, `debug_pb.out`,
`verify_zlib_fit.out`, `verify_zlib_extended.out`,
`big_frames.out`, `gzip_scan.out`, `json_keys.out`, `zip_listing.txt`, `dumpstate_hits.txt`,
`dumpstate_app_hits.txt`.
