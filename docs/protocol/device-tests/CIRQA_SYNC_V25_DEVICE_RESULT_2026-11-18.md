# CIRQA Sync V25 – Geräteergebnis (18.11.2026)

## Umfang

- App: `de.pulselibre`, Build 1091; signierte Update-APK aus Full-CI-Run
  [44376042215](https://github.com/jochenkunkel/pulselibre/actions/runs/44376042215).
- Zertifikat-SHA-256 blieb `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`;
  Update ohne Deinstallation, bestehende App-Daten blieben erhalten.
- Neues persistentes V25-Einmal-Gate; Gate verbraucht.
- Synchronisation blieb read-only und innerhalb der Grenzen von 256 Dateien,
  64 MiB und 120 Sekunden.

## Ergebnis

- `sync_request` angenommen; Item-List-Status 0 (`SUCCESS`).
- Eine Item-List-Seite enthielt 33 Einträge; kein `session_id`, aber
  `next_transaction_id`. V25 behandelte den fehlenden `session_id` als Ende
  dieser Liste und sendete keine zusätzliche Item-List-Anfrage.
- Alle 33 Pulls wurden vollständig übertragen: 146,293 Bytes in 1,018 MLR-
  Chunks. Keine fehlgeschlagenen Transfers.
- Alle 33 Dateien waren bereits in den privaten Stores vorhanden und wurden
  idempotent bestätigt. Die privaten Ablagen blieben bei 31 FIT-Dateien
  (126,089 Bytes) und 2 Rohdateien (20,204 Bytes).
- Nach dem Lauf unabhängig geprüft: alle 31 FIT-Dateien bestanden Magic-,
  Größen-, Header-/Datei-CRC-16- und SHA-256/Dateinamen-Prüfung.
- SQLite read-only nach Force-Stop: `integrity_check=ok`, weiterhin 294 Zeilen
  (`distance=129`, `heart_rate=129`, `speed=36`), keine doppelten
  Primärschlüssel.

## Aussagegrenze

Damit ist am CIRQA bestätigt: Status 0 ist Erfolg; ohne `session_id` wird die
aktuelle Item-List nicht weiter paginiert; sequenzielle MLR-Transfers,
content-adressiertes Speichern und wiederholter idempotenter SQLite-Import
funktionieren. Das Ergebnis bestätigt **nicht**, dass 33 Einträge die gesamte
historische Datenmenge darstellen oder dass keine älteren Einträge ausgelassen
sind. Keine Archiv-, Flag-, Lösch-, Reset- oder `CLOSE_ALL`-Operation wurde
gesendet.
