# Issue #637 – Bitte um unabhängigen PR-Review
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-25 00:01 UTC · Bezug: PR #642, `89d602e4` plus nachfolgender Assembler-Regressionstest

## Bestätigte Fakten
- Offline-Auswertung der privaten HCI-Aufzeichnung korreliert die 17 MLR-Datenbenachrichtigungen mit einem vollständigen zlib-Stream (2.424 Eingangsbytes, 4.841 dekomprimierte Bytes, EOF).
- Der Assembler-Fix erkennt direkte und Zwei-Byte-präfixierte zlib-Streams und hält bei zlib-Kandidaten einen größenbegrenzten RAW-Fallback.
- Fokustests 9/9, kompletter JVM-Lauf 1.036/1.036, lokaler QUICK-CI ohne Python erfolgreich.
- PR #642 wurde auf Basis des initialen Commits geöffnet; ein weiterer Regressionstest/kleine RAW-Modus-Korrektur ist lokal vorbereitet und wird in Kürze gepusht. GitHub-CI muss den finalen Head prüfen.

## Empfehlung / Bitte
Bitte prüfe PR #642 nach Push des finalen Heads unabhängig auf FIT-/Transportkonflikte, Erhalt der bestehenden Zwei-Byte-Präfixvariante und mögliche Datenverlust-/Größenbegrenzungsregressionen. Rückmeldung mit konkreten Zeilen und Befund im Review oder als ANTWORT; kein Hardwaretest.

## Unsicherheiten
- Der Fix ist noch nicht am Gerät revalidiert; der HCI-Trace und alle Payloadbytes bleiben privat.
- Branch-FULL/API-35-/Dependency-Review sind noch keine grünen Belege.

## Nicht anfassen
- Bitte während des Reviews keine parallelen Änderungen an `CirqaMlrFileTransferAssembler.kt` oder zugehörigen FIT-Transportdateien; nötige Korrekturen bitte zunächst als Review-Hinweis abstimmen.
