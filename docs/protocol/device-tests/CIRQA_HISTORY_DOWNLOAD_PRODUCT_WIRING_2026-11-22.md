# Produktpfad-Korrekturen für CIRQA-Historien-Download (2026-11-22)

## Umfang

Auf `origin/main=3ea315f` wurden drei statisch ermittelten Integrationslücken
Produktcode-Änderungen und synthetische JVM-Regressionstests hinzugefügt. Es
fand kein BLE-/Gerätezugriff statt.

## Änderungen

1. **MLR-Datei-Reassembly:** `CirqaMlrFileTransferAssembler` hält den bisherigen
   unkomprimierten Pfad (exakte `data_size`-Bytezahl) bei. Erkennt es nach dem
   dokumentierten Zwei-Byte-Transportpräfix einen gültigen zlib-Header, entfernt
   es dieses Präfix je ATT-Wert, inflatet inkrementell und begrenzt komprimierte
   sowie dekomprimierte Bytes. FIT-Header-/CRC-Prüfung, private Speicherung und
   Import erfolgen erst nach Zlib-Ende und exakter dekomprimierter Größe über
   die vorhandene Validierungskette. Ein fehlgeschlagener/abgeschnittener Stream
   wird verworfen.
2. **MLR-Handle-Routing:** Der Empfangspfad verwendet
   `GarminMlOffline.inspectDataHandle()` für unmarkierte und markierte
   Datenframes; die Nutzdaten beginnen weiterhin nach dem Routing-Byte.
3. **Sync-Antwort:** `syncAccepted` wird nicht mehr bei beliebigen vollständigen
   5044-Protobufs gesetzt. Der neue Parser verlangt ein kanonisches
   `Smart.core_service.sync_response` (CoreService-Feld 2); erst dann wird die
   FileAccess-Item-Liste angefordert. Der bekannte CIRQA-Erfolgsbefund enthält
   dieses Response-Element, aber keine dokumentierte numerische Status-/Code-
   Definition. Die Änderung erfindet daher keinen Status-Enum. Ein künftiger
   bestätigter Fehlerstatus muss zusätzlich explizit ausgewertet werden.

## Tests / Grenzen

- Synthetische JVM-Tests decken den unveränderten Raw-Pfad, chunkweise zlib-
  Reassembly mit Zwei-Byte-Präfix, Größenabweichungen, Überlauf, nachlaufende
  Bytes, markierte Handles und die strukturelle `sync_response`-Erkennung ab.
- Die Tests beweisen die lokale Verarbeitungskette, nicht den Live-Funkpfad.
  Vollständiger Historien-Download, Coverage und Garmin-Connect-Interoperabilität
  bleiben getrennte Geräteevidenz.
