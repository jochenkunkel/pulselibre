# CIRQA Sync V24 – Geräteergebnis (18.11.2026)

## Umfang und Freigabe

- App: `de.pulselibre`, Build 1090, signierte Update-APK aus Full-CI-Run
  [44373134350](https://github.com/jochenkunkel/pulselibre/actions/runs/44373134350).
- Zertifikat-SHA-256 stimmte mit der installierten Build-1087-App überein;
  Update erfolgte ohne Deinstallation, bestehende App-Daten blieben erhalten.
- V24 verwendete ein frisches persistentes Einmal-Gate. Das Gate ist verbraucht.
- Begrenzungen: höchstens 256 Dateien, 64 MiB und 120 Sekunden; keine Retries.
- Keine Archiv-, Flag-, Lösch-, Reset- oder `CLOSE_ALL`-Operation.

## Ergebnis

Die Connect-kompatible read-only `sync_request` wurde angenommen. Die
Item-List-Antwort mit Enum `status=0` wurde korrekt als `SUCCESS` verarbeitet
(V23 hatte diesen Wert fälschlich zurückgewiesen).

- Erste Item-List: 33 Einträge, ausgewählte Gesamtgröße 146,293 Bytes.
- Transfers: **33 vollständig**, 0 fehlgeschlagen; MLR-Daten 146,293 Bytes.
- Privat gespeichert: 31 FIT-Dateien (126,089 Bytes) und 2 vollständige
  Nicht-FIT-Dateien (20,204 Bytes). Eine FIT-Datei war bereits gespeichert;
  30 FIT-Dateien wurden neu angelegt.
- Jede FIT-Datei wurde im App-Pfad nur nach exakter Größe sowie gültiger
  FIT-Struktur und Header-/Datei-CRC akzeptiert. Nach dem Lauf wurden alle
  31 FIT-Dateien nochmals unabhängig geprüft: `.FIT`-Magic, FIT-Header- und
  Dateilänge, CRC-16/ARC und SHA-256 passend zum content-addressierten Namen;
  **31/31 gültig**.
- Beide Nicht-FIT-Dateien wurden nur nach exaktem Größenabgleich im privaten
  Rohdatei-Speicher veröffentlicht; keine Messwertinterpretation.
- SQLite nach Force-Stop read-only geprüft: `integrity_check=ok`, 294
  Messwertzeilen (`distance=129`, `heart_rate=129`, `speed=36`), keine
  doppelten Primärschlüssel. Die App berichtet für alle 33 Transfers
  erfolgreiche Speicherung/Importverarbeitung; nicht unterstützte FIT-Inhalte
  erzeugten erwartungsgemäß keine Messwertzeilen.

## Abbruchgrenze und Interpretation

Nach Abarbeitung der ersten Seite setzte die Item-List-Fortsetzung weitere
Requests ab. Die Antworten enthielten jeweils `items=[]`, aber weiterhin ein
`next_transaction_id`. Nach 64 Seiten stoppte der Lauf am implementierten
Seitenlimit. Damit ist die sichere Mehrdatei-Übertragung demonstriert, **nicht**
aber die Korrektheit der Paginierung oder die Vollständigkeit des Archivs.
Der exakte Fortsetzungswert wird absichtlich nicht in den Bericht geschrieben.

Der Lauf erreichte das Zeitlimit nicht und speicherte keine Teil-Dateien.
Weitere Hardwareläufe benötigen ein neues Build-Gate und eine ausdrückliche
Bestätigung. Vorher sollte die wiederholte leere Folgeseite offline gegen die
Connect-5.31-Transaktionslogik geprüft und der Fortschritts-/Terminalzustand
der Pagination präzisiert werden.
