# Produktpfad-Korrekturen für CIRQA-Historien-Download (2026-11-22)

## Umfang

Die ursprüngliche Prüfung erfolgte auf `origin/main=3ea315f`. Die dort
identifizierten Decoder-, Handle- und Response-Parser-Korrekturen wurden
inzwischen in PR #433 nach `main` gemergt (`6dee48e`). Dieser Folge-PR trägt
ausschließlich die noch fehlende inkrementelle MLR-Reassemblierung samt
synthetischen JVM-Regressionstests auf aktuellem `main`. Es fand kein
BLE-/Gerätezugriff statt.

## Änderungen

1. **MLR-Datei-Reassembly (dieser Folge-PR):** `CirqaMlrFileTransferAssembler` hält den bisherigen
   unkomprimierten Pfad (exakte `data_size`-Bytezahl) bei. Erkennt es nach dem
   dokumentierten Zwei-Byte-Transportpräfix einen gültigen zlib-Header, entfernt
   es dieses Präfix je ATT-Wert, inflatet inkrementell und begrenzt komprimierte
   sowie dekomprimierte Bytes. FIT-Header-/CRC-Prüfung, private Speicherung und
   Import erfolgen erst nach Zlib-Ende und exakter dekomprimierter Größe über
   die vorhandene Validierungskette. Ein fehlgeschlagener/abgeschnittener Stream
   wird verworfen.
2. **MLR-Handle-Routing (bereits in #433):** Der Empfangspfad verwendet
   `GarminMlOffline.inspectDataHandle()` für unmarkierte und markierte
   Datenframes; die Nutzdaten beginnen weiterhin nach dem Routing-Byte.
3. **Sync-Antwort (bereits in #433):** `syncAccepted` wird nicht mehr bei beliebigen vollständigen
   5044-Protobufs gesetzt. Der neue Parser verlangt ein kanonisches
   `Smart.core_service.sync_response` (CoreService-Feld 2); erst dann wird die
   FileAccess-Item-Liste angefordert. Der bekannte CIRQA-Erfolgsbefund enthält
   dieses Response-Element, aber keine dokumentierte numerische Status-/Code-
   Definition. Die Änderung erfindet daher keinen Status-Enum. Ein künftiger
   bestätigter Fehlerstatus muss zusätzlich explizit ausgewertet werden.

## Tests / Grenzen

- Synthetische JVM-Tests decken den unveränderten Raw-Pfad, chunkweise zlib-
  Reassembly mit Zwei-Byte-Präfix, Größenabweichungen, Überlauf und nachlaufende
  Bytes ab. Handle-Routing und strukturelle `sync_response`-Erkennung wurden in
  #433 separat getestet.
- Die Tests beweisen die lokale Verarbeitungskette, nicht den Live-Funkpfad.
  Vollständiger Historien-Download, Coverage und Garmin-Connect-Interoperabilität
  bleiben getrennte Geräteevidenz.
