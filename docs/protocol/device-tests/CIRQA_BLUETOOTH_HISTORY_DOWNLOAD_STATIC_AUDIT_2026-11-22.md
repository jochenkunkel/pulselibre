# Statisches Audit: Bluetooth-Pfad für CIRQA-Historien-Download (2026-11-22)

## Umfang / Grenze

Geprüft wurde `origin/main=4dc1c4e`, ohne Codeänderung und ohne Gerät/Bluetooth-Zugriff. Scope: GATT-Setup, MLR-Routing, 5043/5044-Reassembly, FileAccess-Item/Pull, Transferabschluss und FIT/SQLite-Verarbeitung. Das ist eine Codeprüfung, keine Aussage über den aktuellen Funkzustand oder CIRQA-Firmware-Kompatibilität.

## Befunde

### A1 – zlib-Transfervariante erreicht den FIT-Import nicht (hohe Sicherheit, potenzieller Download-/Importblocker)

- `CirqaMlHandshakeProbe.receive()` hängt MLR-Datenbytes ab `CirqaMlHandshakeProbe.kt` um Zeile 1400 unverändert an `mlrData` und beendet die Datei erst, wenn die rohe Bytezahl exakt `syncExpectedSize` erreicht.
- `mlrValidation()` (ab ca. Zeile 2875) prüft diese Rohbytes unmittelbar mit `FitFileHeader.inspectVerified(data)` und dekodiert sie anschließend direkt. `CirqaZlibFileDecoder` wird im Produktpfad nicht aufgerufen. `PullResult.compressionWindow` wird zwar in der Diagnose ausgegeben, aber nicht zur Verarbeitung benutzt.
- Der unabhängige S10-HCI-Befund `CIRQA_S10_DUMP2_OFFLINE_ANALYSIS_2026-11-22.md` beschreibt für eine Band→App-Übertragung zlib-Streams auf MLR-Kanälen; bei mehreren UID-zugeordneten FITs entspricht `ItemReference.data_size` der **dekomprimierten** Dateigröße. Damit kann diese Kompressionsvariante die rohe Längengleichheit nie erfüllen und fällt in den Sitzungstimeout, statt FIT-CRC/Import zu erreichen.
- Das beweist nicht, dass jede FileAccess-Übertragung komprimiert ist. Der dokumentierte Build-1757-Lauf (PR #422) meldet 24/24 Dateien übertragen/verifiziert/gespeichert und 23 FIT-Dateien dekodiert; die unkomprimierte Variante funktioniert also im beobachteten Lauf. Dieser Befund widerlegt nicht die fehlende Kompressionsvariante.
- **Folgerung:** die bereits vorhandene, begrenzte `CirqaZlibFileDecoder`-Komponente mit dem *identifizierten* MLR-Transferformat verbinden. Vor einem Live-Einsatz müssen MLR-Kanal/Handle, die beobachteten Transportheader, Chunk-/Streamgrenzen und die Bedeutung von `compressionWindow` explizit modelliert und synthetisch getestet werden. Nicht versuchsweise einzelne Notifications aufblasen.

### A2 – MLR-Handle-Routing akzeptiert nur unmarkierte Handles (hohe Sicherheit im Code, Geräteauswirkung zu verifizieren)

- In `CirqaMlHandshakeProbe.receive()` wird das erste Byte um Zeilen 1384/1418 direkt mit `mlrHandle` bzw. `historyHandle` verglichen. `GarminMlOffline.inspectDataHandle()` ist im Produktcode unaufgerufen.
- Der dokumentierte MLR-Vertrag in `CIRQA_PROTOCOL_SPEC.md` Zeilen 46–50 beschreibt zusätzlich das markierte Datenformat (`bit 7=1`, Handle in Bits 6..4, Payload ab Byte 1). Solche korrekt gerouteten Datenframes verfehlen die Gleichheitsvergleiche und werden nicht der Transferdatei zugeordnet.
- **Folgerung:** das Routing auf einen normalisierten Handle-Parser umstellen und synthetische Tests für unmarkierte/markierte Frames, unbekannte Handles, Control-Frames und dynamische Handle-Zuordnungen ergänzen. Für die CIRQA-Auswirkung muss zuerst festgestellt werden, welche Form das konkrete Gerät sendet; die Codeprüfung allein beweist nicht, dass der erfolgreiche Build-1757-Lauf diese Form tatsächlich verwendet hat.

### A3 – `syncAccepted` ist kein semantischer Sync-Erfolgsnachweis (mittlere Sicherheit, Diagnose-/Zustandsfehler)

- Sobald irgendein vollständig reassemblierter und CRC-validierter 5044-Frame vorliegt, setzt `handleFileAccessResponse()` um Zeile 2276 `syncAccepted = true`; danach wird unabhängig vom identifizierten Response-Untertyp gegebenenfalls eine Item-Liste angefordert.
- Die Implementierung prüft dort nicht explizit, dass der Smart-Body ein erfolgreiches `sync_response` ist. `parsePullResult()` und `parsePage()` prüfen andere Antworttypen, sind aber keine Sync-Response-Erfolgsprüfung.
- **Folgerung:** Antworttyp und Erfolgsstatus getrennt validieren, bevor `syncAccepted` gesetzt oder der nächste Sync-Schritt gesendet wird. Tests für fremden 5044-Untertyp, negative Sync-Antwort, malformed/empty body und echte erfolgreiche Sync-Response ergänzen.

### A4 – GATT-Schreibmodus bevorzugt „ohne Antwort“ (Risikohinweis, kein nachgewiesener CIRQA-Ausfall)

- `send()` um Zeile 1298 wählt `WRITE_TYPE_NO_RESPONSE`, sobald die Characteristic diese Property hat – auch wenn zusätzlich `PROPERTY_WRITE` vorhanden ist. Der Zustandsautomat serialisiert weitere Writes über `onCharacteristicWrite()` und `mlWritePending`.
- Das nimmt für alle Kontroll-/ACK-Schreibvorgänge eine schnellere, aber nicht per ATT bestätigte Variante, ohne dass ein gerätespezifischer Nachweis oder ein entsprechender Regressionstest im Repo vorliegt.
- **Folgerung:** `WRITE` mit Antwort bevorzugen, wenn verfügbar; `NO_RESPONSE` nur verwenden, wenn es die einzige unterstützte Option bzw. ein nachgewiesener Gerätevertrag ist. Callback-/Timeoutverhalten und ACK-Reihenfolge für beide Modi separat testen. Dies ist eine Zuverlässigkeitsverbesserung, kein anhand der vorhandenen S10-Evidenz bestätigter Fehler.

## Nachweise und nächste sichere Schritte

- PR #425 ergänzt einen synthetischen Offline-Nachweis der Kette `zlib → FIT-CRC → Store → Backfill`; seine Beschreibung bestätigt ausdrücklich, dass dies den Produktpfad nicht verdrahtet.
- PR #422 berichtet einen begrenzten erfolgreichen Produktlauf (24 Dateien übertragen/verifiziert/gespeichert, 23 FIT decodiert/SQLite-Commit, drei leere Transaktions-Folgeantworten, `runDrained=false`, `coverage=device-history-unverified`). Das belegt einen konkreten partiellen Lauf, nicht den hier fehlenden Kompressionspfad oder vollständige Historie.
- Keine Tests wurden in diesem statischen Audit ausgeführt; keine Hardware-/BLE-Aktion fand statt.
- Vor Codeänderungen an `CirqaMlHandshakeProbe.kt` ist die laufende #149-Arbeit des FIT-Agenten abzugleichen. Priorität für eine koordinierte Korrektur: A1 (komprimierter Transfer), danach A2/A3; anschließend JVM-Tests, Full-CI/Emulator. Keine neuen proprietären Geräteschreibvorgänge oder weiteren Gerätesyncs ohne passende ausdrückliche Freigabe.
