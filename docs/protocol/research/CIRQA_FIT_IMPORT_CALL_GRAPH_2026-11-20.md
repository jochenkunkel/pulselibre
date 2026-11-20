# FIT-Import-Aufrufpfad und Fehlerstatus (#149)

Stand: 2026-11-20 · Codebasis: `origin/main` `ce3ebe8` plus die in diesem Branch
geprüften Änderungen. Offline-Codeaudit; kein BLE-Lauf, kein App-Start und keine
Änderung an privaten S25-Daten.

## Produktiver Smart/MLR-Pfad

1. `CirqaMlHandshakeProbe.sendNextSyncFile` wählt ein Item aus der begrenzten
   `syncPendingItems`-Queue, setzt erwartete Bytezahl und Transferzustand und
   sendet genau den vorhandenen FileAccess-Pull.
2. Die MLR-Empfangsbehandlung verwirft zunächst die Configure-Kontrollantwort
   und sammelt danach unveränderte Payload-Bytes für das aktuelle Item. Sobald
   die angesammelte Größe exakt `syncExpectedSize` erreicht, ruft die
   Koordination `completeSyncFile` auf; ein Überlauf bricht den Lauf ab. Die
   Abschlussbedingung ist damit der erwartete Bytestand, nicht ein separates
   FIT-/SQLite-Ergebnis vom Band.
3. `completeSyncFile` ruft `mlrValidation()` auf und zählt das Item erst dann
   als erfolgreich, wenn `CirqaSyncFileCompletion.isSuccessful` zustimmt.
   Andernfalls steigt `syncFailedFiles`; das Ergebnis wird in die begrenzte,
   redigierte Datei-Statusliste aufgenommen. Der nächste Queue-Eintrag wird
   unabhängig davon verarbeitet; der Laufabschluss meldet Fehler über
   `runDrained`/`incompleteReason`.
4. `mlrValidation()` verlangt exakte Item-/Empfangsgröße. Nicht-FIT-Bytes
   werden privat exakt gespeichert. FIT-Kandidaten durchlaufen
   `FitFileHeader.inspectVerified` (Envelope/CRC), danach
   `CirqaVerifiedFitStore.saveVerified` (SHA-256-adressierte, private
   Originaldatei). Erst anschließend folgen `FitHistoricalObservations.decode`
   und `CirqaFitBackfill.importVerifiedFile`.
5. `CirqaFitBackfill.importVerifiedFile` verifiziert Dateiname gegen SHA-256,
   dekodiert erneut, filtert auf zeitgestempelte `PROFILE_VALID`-Observations
   und ruft `FitObservationStore.commitFitFile` auf. Die Implementierung der
   kanonischen Speicherung delegiert an `FitObservationCommitter.commit`, der
   Quellen, Observationen, Provenienz, Metadaten und unbekannte Felder in
   SQLite transaktional/idempotent persistiert.
6. SQLite-Ausnahmen im Livepfad werden in `mlrValidation()` als
   `sqlite=failed:<Exceptionstyp>` redigiert; der Dateiabschluss darf sie nicht
   als importiert zählen. Ein späterer erneuter Pull derselben FIT-Datei ist
   durch den privaten content-addressed Store idempotent; der Import wird
   erneut versucht.

## Festgestellte Statuslücke und Korrektur

Vor diesem Branch akzeptierte `CirqaSyncFileCompletion` jede exakt große,
privat gespeicherte `privateRaw`-Datei als erfolgreich. `mlrValidation()`
speichert jedoch auch FIT-markierte Dateien mit ungültigem FIT-Envelope/CRC
absichtlich als Raw-Datei, um die Bytes nicht zu verwerfen. Damit konnte eine
ungültige FIT-Datei als erfolgreich abgeschlossener FIT-Import gezählt werden.

Die Klassifikation akzeptiert solche Raw-Fallbacks nun nicht mehr als
erfolgreiche Datei, wenn `fitHeader=false` oder der sichere Typ `FIT_TYPE_*`
anzeigt. Die unveränderte Raw-Aufbewahrung bleibt erhalten. Gültige Nicht-FIT-
Dateien gelten weiter nach exakter Größenprüfung und privater Speicherung als
erfolgreich. Regressionen decken gültigen Raw-Fall, ungültige FIT-Fallbacks,
SQLite-Fehler, Import- und Raw-Speicherfehler ab.

Der Backfill-Fehlerpfad propagiert Commit-Ausnahmen (er fängt nur
`IOException` beim Lesen/Importieren ungültiger Inputs als ungültige Datei ab).
Ein JVM-Test simuliert einen einmalig fehlschlagenden Commit und belegt, dass
der nächste Lauf erneut denselben stabilen Observation-Schlüssel übergibt. Ein
zusätzlicher Android-Instrumentationstest setzt in einer isolierten
kanonischen DB einen temporären SQLite-Trigger, der den Observation-Insert
abbricht; er prüft den Rollback von Quelle/Observations/Provenienz und den
erfolgreichen Folgeimport nach Entfernen des Triggers. Dieser neue
Instrumentationstest bestand im lokalen API-35-Full-Gate vom 2026-11-20.
Der Lauf beinhaltete Python-Tests, JVM-Tests, Lint,
Instrumentation-APK und Emulator-Instrumentation/Coverage. Das ist lokaler
Emulatornachweis, kein S25-Test.

## Abgrenzung der Evidenz

- Der V27-Bericht weist für den damaligen Build 1399 einen begrenzten Lauf mit
  25/25 Transfers, 24 FIT und 1 Nicht-FIT aus; danach waren lokal 45/45 FIT
  verifiziert und 27.736/27.736 importierbare Kandidatenschlüssel vorhanden.
  Das ist begrenzte Bestands-/Laufevidenz, keine vollständige Gerätehistorie.
- Die Feldmatrix vom 18.11.2026 beschreibt 31 ältere FIT-Dateien und kann nicht
  ohne erneute private Inventur auf den späteren 45-Datei-Bestand übertragen
  werden. Der lokale private Bestand ist diesem Arbeitscheckout nicht
  zugänglich; keine Rohdaten wurden aus dem S25 abgerufen.
- Die redigierte damalige FIT-Matrix enthält 9.784 FIT-Feldzeilen. Bei drei
  Profilfeldkombinationen wurden importierte Werte mit SQLite-Schlüsseln
  abgeglichen: `record.heart_rate` 129, `record.distance` 129 und
  `record.enhanced_speed` 36. 2.496 Zeilen haben keinen Profilmessagenamen;
  5.938 sind als `unmapped` klassifiziert. Das belegt damalige Abdeckung,
  nicht die spätere 45-Datei-Feldabdeckung oder fachliche Richtigkeit aller
  Profileinheiten.
- Die redigierte damalige FIT-Matrix enthält 9.784 FIT-Feldzeilen. Bei drei
  Profilfeldkombinationen wurden importierte Werte mit SQLite-Schlüsseln
  abgeglichen: `record.heart_rate` 129, `record.distance` 129 und
  `record.enhanced_speed` 36. 2.496 Zeilen haben keinen Profilmessagenamen;
  5.938 sind als `unmapped` klassifiziert. Das belegt damalige Abdeckung,
  nicht die spätere 45-Datei-Feldabdeckung oder fachliche Richtigkeit aller
  Profileinheiten.
- Diese Prüfung verifiziert die Codeverknüpfung und Failure-Klassifikation,
  nicht die semantische Richtigkeit aller FIT-Felder oder einen SQLite-
  Transaktionsfehler auf dem S25.
- Ein S25-App-Start wurde nicht durchgeführt: `CirqaDatabase.onOpen()` kann
  Legacy-Nachtaufzeichnungsdateien bereinigen. Ohne Klärung/Vorher-Nachher-
  Sicherung dieses Startseiteneffekts wäre das kein sicherer Read-only-Test.
- Keine Änderungen an Schema/DDL, Backupformat, BLE-Kommandos oder
  Transportverhalten.
