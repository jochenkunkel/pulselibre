# UPDATE | Issue #506 – ATT-Error-Queue-Fix und Offline-Replay
- Zeit (UTC): 2026-11-24 12:53
- Von: OpenCode FIT
- An: GUI-Agent, Projektleitung
- Status: aktiv – synthetische Regression und privater Offline-Replay abgeschlossen

## Änderung und Testnachweise
- `tools/garmin_hci_replay.py`: ATT Error Response `0x01` benennt jetzt das ursprüngliche Request-Opcode, Attribut-Handle und Error-Code im redigierten Metadaten-Event. Für `0x08`/`0x10` wird die älteste ausstehende Discovery-Anfrage dieses Opcodes aus derselben Connection-Epoche und der Gegenrichtung konsumiert. Zähler zeigen erkannte, konsumierte und unmatched Fälle; Parser-Version auf 2 erhöht.
- `tools/test_att_error_response_queue.py`: vier synthetische Tests prüfen Stale-Queue-Reparatur, Opcode-Isolation, Richtungsisolation und Epoch-Isolation. Keine Garmin-UUID-Semantik wird aus dem Fixture behauptet.
- Gezielte Tests: 4 Queue-Tests und 21 Replaytests grün. Gesamter `python3 -m unittest discover -s tools -p 'test_*.py' -v`: **147 bestanden**.

## Private Capture-Re-Analyse – nur Aggregate
- Drei lokal zugriffsbeschränkte Captures wurden mit altem und korrigiertem Parser offline verglichen; keine Rohbytes oder Eventdateien wurden dem Repository hinzugefügt.
- Der neue Parser konsumierte 50, 12 und 11 ausstehende `0x08`-Discovery-Requests; keine unmatched Fälle. In den redigierten Event-Aggregaten kamen je Capture 28, 7 und 5 GATT-Characteristic-Discovery-Events hinzu (**40 insgesamt**). Keine vorherigen Discovery-Events gingen verloren; ATT-, L2CAP- und Garmin-ML-Headline-Zähler blieben gleich.
- Das belegt eine konkrete Auswirkung auf GATT-Discovery-Zuordnung. Es belegt **nicht** Item-List-Pagination, FileSync-Transaktionen, FIT-Bytes oder die vollständige Historie. Absolute Zeitbasis der privaten Mitschnitte bleibt unvalidiert.

## Nächste Schritte / Grenzen
- OpenCode setzt mit vorhandenen Offline-Referenzen Item-List-/`next_transaction_id`- und Transaktionsfilter-Analyse fort; kein unveränderter Hardwaretest.
- GUI-Dateien, SQLite-Schema und BLE-Transport wurden nicht geändert. Keine Daten oder privaten Capture-Fingerprints werden veröffentlicht.
