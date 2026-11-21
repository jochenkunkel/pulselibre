# CIRQA-Produktdownload – begrenzter S25-Lauf mit Build 1690 (2026-11-21)

## Freigabe, Build und Grenzen

- Nutzerfreigabe: genau ein Produktlauf „Historie herunterladen“, nach PR #385 und erfolgreichem Main-Full-CI.
- Main-Commit: `9a7f119efb3cb2ab453903d314a391fe4488d2b9`; CI Run [44926464757](https://github.com/jochenkunkel/pulselibre/actions/runs/44926464757), einschließlich API-35-Emulatorinstrumentierung grün.
- Main-CI-Release-APK `2.0.0_build1690` hatte dieselbe Signatur-SHA-256 wie die vor Installierung gelesene App-Version 1680. Das Update wurde in-place mit `adb install -r` durchgeführt; keine Daten wurden gelöscht.
- Exklusive S25-Sperre wurde während der Gerätezugriffe verwendet und jeweils freigegeben. Keine Instrumentationstests am S25.
- Die Warnung vor möglicher Geräte-Markierung als übertragen wurde gelesen. Keine Reset-, Delete-, Archive-, SetFlags- oder sonstigen separaten Gerätebefehle.
- Die Bestätigung „Herunterladen“ erfolgte genau einmal. Kein zweiter Versuch und keine separate `nextTransactionId`-Anfrage nach Ende des Laufs.

## Redigiertes Ergebnis

Die App zeigte eine neue Lauf-ID `9331dd01916f`, sodass der Status diesem Lauf zugeordnet werden kann:

- `accepted=true`; eine Item-List mit 35 Einträgen wurde verarbeitet.
- 17 Einträge wurden als lokal bereits verifiziert übersprungen; 18 Einträge blieben für den Pull ausgewählt.
- Die erste Pull-Antwort meldete `result=3`, `transport=0`, `transferHandle=0`, keinen FilePath und kein Compression Window.
- Der Produktpfad stoppte daraufhin fail-closed: `runDrained=false`, `coverage=device-history-unverified`, `incompleteReason=file-processing-failed`.
- 0 MLR-Chunks/Bytes; 0 erfolgreiche Dateiübertragungen, FIT-Verifikationen, private Neuablagen oder SQLite-Importe. Der Status weist einen fehlgeschlagenen Pull aus.
- `transactionFollowups=0`: Die einmalige `nextTransactionId`-Fortsetzung wurde in diesem Lauf nicht erreicht, weil der erste Datei-Pull abgewiesen wurde. Es wurde danach keine weitere Anfrage verkettet.

In der Garmin-Connect-5.31-GDI-Protobufdefinition ist Wire-Enum-Wert `3` als `NO_AVAILABLE_RESOURCES` benannt. Das ist eine Interpretation anhand einer externen Forschungsquelle und noch keine unabhängige Bestätigung der CIRQA-spezifischen Ursache. Der App-Bericht enthält derzeit keine ausgelesenen numerischen Error-Code-/Error-Source-Felder; Ursache und Ressourcenkontext bleiben daher offen.

## Einordnung / nächste sichere Arbeit

Der Lauf belegt einen neuen Item-List- und Pull-Vorgang, aber **keinen** erfolgreichen Datei-Download. Kein FIT-Original wurde in diesem Lauf neu übertragen; der kanonische Bestand wurde dadurch nach ausgewiesenem Status nicht erweitert. Die bestehende Gerätehistorie bleibt unvollständig bzw. unbestätigt.

Offline-Folgeänderung in PR #387: `PullResult` enthält nun auch optionale numerische `errorCode`-/`errorSource`- und data-specific Error-Felder; der Sync-Bericht gibt nur diese Zahlen bzw. `none` aus, niemals FilePath oder Payload. Synthetische Parser-Tests decken vorhandene, fehlende und signed Code-Felder ab. Das gilt erst nach Merge/Installation und belegt nicht rückwirkend Error-Metadaten für diesen Build-1690-Lauf. Die konkrete Geräteursache bleibt offen. Ein weiterer Produktlauf ist nicht autorisiert und würde eine neue ausdrückliche Freigabe erfordern.
