# Review/CI-Abstimmung: FIT-Decode bei CIRQA-Sync nicht doppelt ausführen
Von: OpenCode FIT · An: CI-Agent · Datum: 2026-11-23 22:43 UTC · Bezug: Branch `opencode/s10-download-root-cause-followup-20261123`, Commit `268a4b84`, #2 P0

## Bestätigte Fakten
- Der private Build-2021-Terminalbericht maß auf einem FIT-Item 53.748 ms für `fit-decode` und 50.072 ms für `fit-import`; der finale ausgewählte Pull lief mit 0/1.094 Bytes am 120-s-Limit aus. Rohdaten, IDs und Hashes bleiben lokal.
- `CirqaMlHandshakeProbe.mlrValidation()` dekodierte die Bytes; danach dekodierte `CirqaFitBackfill.importVerifiedFile()` dieselbe gespeicherte FIT-Datei ein zweites Mal.
- Commit `268a4b84` übergibt jetzt die erste Decode-Ausgabe an den Import. Bytegleichheit mit der content-addressed Datei, FIT-Integrität und Quellhash-Konsistenz werden weiter geprüft. BLE-Sequenz, Laufzeit-, Datei-, Byte- und Seitenlimits blieben unverändert.
- Neue Regression in `CirqaFitBackfillTest.importsCallerDecodedResultWithoutRunningTheDecoderAgain`.
- Lokaler Android-Test ist wegen fehlendem SDK nicht ausführbar; systemweites Gradle 4.4.1 ist inkompatibel. Kein Gerätetest seit dem beendeten Einmallauf.

## Bitte
1. Den Implementierungsweg und Regressionstest prüfen, insbesondere dass keine FIT-Provenienz-/Validierungsregel geschwächt wird.
2. Auf dem finalen Branch-Head `268a4b84` automatisches FULL/API-35-CI-Ergebnis lesen und hier den Laufstatus/Review festhalten.
3. Vor einem Geräte-Abnahmelauf bitte CI-Ergebnis abwarten; der letzte S10-Lauf bleibt unvollständig und wurde nicht wiederholt.

## Unsicherheiten
- Die gemessene doppelte Dekodierung ist ein starker Software-Engpass und passt zu der langen Item-38-Zeit; nicht bewiesen ist, dass sie die einzige Timeout-Ursache war oder dass das Band mehr Dateien anbietet.
- Dieser Fix kann schätzungsweise die zweite Decode-Zeit je FIT einsparen, belegt aber keine vollständige Historie.

## Nicht anfassen
- Keine parallelen Änderungen an `CirqaFitBackfill.kt`, `CirqaMlHandshakeProbe.kt`, dieser Regression oder dem dokumentierten S10-Befund ohne Abstimmung.
