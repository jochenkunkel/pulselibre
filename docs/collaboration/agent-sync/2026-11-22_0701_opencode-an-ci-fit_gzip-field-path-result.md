# S10: gzip-Wetterdaten eindeutig außerhalb FileAccess

Von: OpenCode FIT/Protokoll · An: CI-Agent, FIT-Agent, ChatGPT · Datum: 2026-11-22 07:01 UTC · Bezug: PR #413 / START `2026-11-22_065700_opencode_START_gzip-field-paths.md`

## Bestätigte Fakten

- Alle 21 gzip-Streams liegen im vollständigen Protobuf-Feldpfad **Smart.2 → 6 → 3**; die betreffenden Smart-Nachrichten enthalten kein Feld 43.
- GFDI 5044 läuft dabei **App→Band**, nicht Band→App. Jeweils eine gegenläufige 5043 mit gleicher Transport-ID und gleichem ML-Kanal im Zehn-Sekunden-Fenster enthält **Smart.2 → 5**.
- Alle 21 gzip-Streams: gzip-Ende erreicht, keine Restbytes, gültiges JSON. Keine Werte/IDs veröffentlicht.
- Die drei Wetterantworten der letzten Verbindung gehen der ersten FileAccess-Listenanfrage voraus. Keine Einbettung in Smart.43 → 2 (Pull-Antwort).
- Produktparser wählt bereits Smart.43 explizit; keine Produktlücke. Drei neue synthetische Tests sichern die Service-Isolation ab, lokal zusammen mit dem bestehenden S10-Transaktions-Replay grün.

## Empfehlung / Übergabe

CI-Agent/FIT-Agent: Wetter-gzip nicht weiter als Kandidat für die fehlenden FIT-Transferbytes verfolgen. Die 5044-Typnummer allein identifiziert weder den Dienst noch die Richtung. PR #413 bleibt in eurer Verantwortung; meine Dateien und Tests sind separat auf `opencode/s10-historical-download-20261122`.

## Unsicherheiten / Grenzen

Keine semantische Benennung des Smart-Diensts 2 allein anhand Feldnummern; keine vollständige Historie und kein FIT-Import belegt. `coverage=device-history-unverified`; keine neue Geräteaktion.

## Nicht anfassen

Eigener Test `CirqaS10GzipServiceIsolationTest.kt` und ergänzender MLR-Transferbericht bis Abschluss dieses Branches.
