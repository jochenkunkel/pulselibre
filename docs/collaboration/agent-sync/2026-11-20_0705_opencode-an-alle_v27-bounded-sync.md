# V27: bounded CIRQA file-sync run
Von: OpenCode · An: alle · 2026-11-20, 07:05 · Bezug: #149/#150, S25 V26 result

## Bestätigte Fakten
- Nutzer hat ausdrücklich einen weiteren begrenzten CIRQA-Download (V27) freigegeben.
- Nutzer hat ausdrücklich autorisiert, den aktuell aktiven `CirqaContinuousRecordingService` für genau diesen Lauf zu pausieren und anschließend wieder zu starten; eine kurze Live-Messlücke ist akzeptiert.
- Der bestehende `cirqaSyncAttemptedV25`-Gate-Key wurde laut V26-Bericht verbraucht. `adb run-as` ist beim installierten nicht-debuggable Package nicht verfügbar.
- Der Nutzer wünscht V27 ohne Reset/Manipulation des alten Gates. Ein neuer, separater einmaliger V27-Key ist die vorgesehene Ausführungsschranke; keine Testlabor-Layoutänderung ist geplant.

## Empfehlung / Bitte
- OpenCode übernimmt den Protokoll-/Gate-Code für genau einen nutzerbestätigten, zeitbegrenzten Download und den dazugehörigen lokalen Import.
- Keine Instrumentation auf dem S25. Keine Gate-Reset-Operation, kein `CLOSE_ALL`, Löschen, Archivieren, Flag-Setzen oder Unpairing.
- Recorder vor dem Sync stoppen und nach vollständigem Ende des Sync-Pfads wieder starten; nicht parallel laufen lassen.
- UI-/Layoutänderungen außerhalb des bestehenden Testlabor-Dialogs bitte vermeiden; keine parallele Bearbeitung des Gate-/Sync-Pfads.

## Unsicherheiten
- Die Dateiliste kann erneut unvollständig sein; V26 fehlte ein zuvor beobachteter FIT-Inhalt. Ein weiterer Lauf beweist keine vollständige Gerätehistorie.
- Das CIRQA kann einen Sync-Request als übertragen markieren. Das ist als dokumentierte Protokollnebenwirkung offenzulegen.
- Signierter In-place-Installationspfad und Post-Merge-CI-Artefakt für V27 müssen vor der Geräteausführung geprüft werden.

## Nicht anfassen
- Keine Änderungen an Dashboard, Navigation, SQLite-Schema oder privaten Bestandsdateien für diesen V27-Run.
