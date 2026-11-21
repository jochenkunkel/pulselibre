# CIRQA-Produktdownload – begrenzter S25-Lauf mit Build 1701 (2026-11-21)

## Freigabe, Build und Sicherheitsgrenzen

- Nutzerfreigabe: genau ein weiterer Produktlauf „Historie herunterladen“, ausdrücklich nach PR #387 und erfolgreichem Main-Full-CI.
- Main-Commit: `2cffaeaefef767f1c65a1640b555501b4725fc4c`; CI Run [44937858817](https://github.com/jochenkunkel/pulselibre/actions/runs/44937858817) vollständig grün, einschließlich Build/JVM, Lint, Signierdiagnose und API-35-Emulatorinstrumentierung.
- Das Main-CI-Artefakt `2.0.0_build1701` hatte dasselbe Signaturzertifikat wie das installierte Build 1690 und das vorherige Main-Artefakt. Zertifikatsfingerprints wurden nicht in diesen Bericht aufgenommen. Paket-ID war `de.pulselibre`, VersionCode stieg von 1690 auf 1701.
- Update in place per `adb install -r`; `firstInstallTime` blieb erhalten (2026-11-18 23:15:16). Die exklusive S25-Sperre wurde bei Gerätezugriffen verwendet und danach freigegeben. Keine Instrumentationstests auf dem S25.
- Warnung zur möglichen Geräte-Markierung als übertragen wurde vor Bestätigung angezeigt. Die Schaltfläche „Herunterladen“ wurde genau einmal bestätigt. Nach GATT-Fehler keinerlei Retry oder Folgeanfrage. Keine Reset-, Delete-, Archive-, SetFlags- oder sonstigen separaten Gerätebefehle.

## Redigiertes Ergebnis

Die App ordnete den Lauf der Run-ID `a1e5c993a569` zu:

- ML-Verbindung beendet bei GATT-Status `133`; letzte Phase: GATT-Verbindung angefordert.
- 0 Notifications; GATT-Schreibvorgang nicht bestätigt (`false`); Protokollzustand `IDLE`.
- `accepted=false`, `itemListSent=false`, `pullSent=false`; keine ItemList.
- 0 MLR-Chunks/Bytes; 0 erfolgreiche oder fehlgeschlagene Dateiübertragungen; 0 FIT-Verifikationen, privaten Neuablagen, Dekodierungen, SQLite-Commits oder Imports.
- `transactionFollowups=0`; die einzige zulässige `nextTransactionId`-Folgeanfrage wurde nicht erreicht.
- `runDrained=false`, `coverage=device-history-unverified`, `incompleteReason=sync-not-accepted`.

Der Lauf wurde unmittelbar nach dem GATT-Verbindungsfehler beendet. Es gibt damit aus diesem Lauf weder eine Pull-Antwort noch neue FIT-Dateien oder SQLite-Beobachtungen. Die CIRQA-Historie bleibt unbestätigt/unvollständig; dieser Befund sagt nichts über die Verfügbarkeit einzelner Gerätedateien aus.

## Separater Lauf nach erneuter Freigabe (19:46 UTC)

Nach dem vorstehenden GATT-133-Lauf erteilte der Nutzer erneut eine ausdrückliche Freigabe. Die Bestätigung „Herunterladen“ erfolgte genau einmal; dieser Lauf wurde regulär beendet:

- Run-ID `ff510994c2aa`; `accepted=true`; ItemList gesendet.
- Zwei Listenantworten: zuerst 35 Einträge, anschließend genau eine reguläre `nextTransactionId`-Folgeantwort mit 0 Einträgen. Keine weitere Transaktion angefordert.
- 18 ausgewählte Dateien (96.882 Bytes) vollständig übertragen; 18/18 Integritätsprüfungen und private Ablagen erfolgreich; 0 fehlgeschlagene Transfers.
- 17 lokal bereits verifizierte Manifest-Skips. Von den 18 übertragenen Dateien wurden 16 FIT-Dateien dekodiert, enthielten aber 0 importierbare Beobachtungen; ihre Inhalte waren lokal bereits vorhanden. Auch die beiden übrigen Transfers bestanden die ausgewiesenen Integritäts- und Speicherstufen; es entstanden keine neuen FIT-Imports oder SQLite-Beobachtungen.
- `transactionFollowups=1`, `transactionFollowupResponseItems=0`, `runDrained=false`, `coverage=device-history-unverified`, `incompleteReason=next-transaction-followup-empty`.

Der Nutzer meldete während des Ablaufs eine versehentliche Störung und erteilte die Freigabe für einen weiteren Test. Vor dessen Start war ADB zunächst ohne angeschlossenes Gerät (`adb devices -l`: keine Geräte); zu diesem Zeitpunkt gab es keinen weiteren Button-Klick oder Produktlauf. Nach erneuter Geräteverfügbarkeit wurde der freigegebene Zusatzlauf wie folgt ausgeführt.

## Wiederaufnahme nach Geräteverfügbarkeit (20:10 UTC)

Das S25 war anschließend per ADB wieder verfügbar; ein zweites angeschlossenes Android-Gerät wurde nicht angesprochen. Der installierte Build wurde als `2.0.0_build1701` / VersionCode `1701` geprüft. Die Bestätigung „Herunterladen“ erfolgte genau einmal:

- Run-ID `84f8b483855b`; `accepted=true`; ItemList gesendet.
- Zwei Listenantworten: 35 Einträge, danach genau eine reguläre `nextTransactionId`-Folgeantwort mit 0 Einträgen; keine weitere Transaktion angefordert.
- 18 ausgewählte Dateien (96.882 Bytes) vollständig übertragen, verifiziert und in den privaten Speicherpfad übernommen; 0 fehlgeschlagene Dateien.
- 17 lokal bereits verifizierte Manifest-Skips. 16 FIT-Dateien wurden dekodiert, enthielten aber 0 importierbare Beobachtungen; `imported=0`, `sqliteCommitted=16` (ohne neue Beobachtungen). `unsupported=0`.
- `transactionFollowups=1`, `transactionFollowupResponseItems=0`, `runDrained=false`, `coverage=device-history-unverified`, `incompleteReason=next-transaction-followup-empty`.

Der Lauf beendete sich regulär. Es gab keinen Verbindungs- oder Pull-Fehler, daher keinen Retry; es erfolgte keine zweite Bestätigung. Die vollständige Historienabdeckung bleibt unbestätigt.

## Offline-Codeeinordnung der zwei gleichartigen Läufe

- Der Protokollparser unterscheidet `session_id` (aktuelle ItemList-Paginierung) von `next_transaction_id` (separater Transaktionskontext). In beiden Läufen enthielt die erste Seite 35 Einträge ohne Session-Cursor, aber mit `nextTransactionId`; der Produktpfad stellte genau eine separate Transaktionsanfrage. Deren 0-Einträge-Antwort signalisierte im Status erneut ein `nextTransactionId`. Die implementierte Sicherheitsgrenze stoppt nach dieser einen Folgeanfrage absichtlich und markiert den Lauf `next-transaction-followup-empty`. Das ist ein begrenzter Abbruch, kein GATT-/Pull-Fehler und kein Beleg, dass die Gerätehistorie vollständig oder leer ist.
- Im FIT-Pfad bedeutet `decoded=16` nur, dass 16 FIT-Dateien syntaktisch dekodiert und der SQLite-Commitpfad erreicht wurde. Importierbar sind ausschließlich erkannte, profilvalide Messwerte mit gültigem Zeitstempel. Die Aggregate `imported=0` und `noImportableData=16` unterscheiden nicht zwischen fehlenden unterstützten Messfeldern, ungültigen/Sentinelwerten, fehlenden Zeitstempeln oder anderen Qualitätsfiltern. `unsupported=0` zählt den Dateistatus `UNSUPPORTED`; es ist **kein** Nachweis, dass der Decoder keine unbekannten Messfelder gesehen hat.
- Die FIT-/SQLite-Rohdateien der beiden Läufe liegen nicht im untersuchten Worktree. Daher war kein FIT-Replay und keine dateiweise Prüfung dieser konkreten 16 Dateien möglich. Der bestehende Decoder unterstützt gezielt bestimmte `record`-/`monitoring`-Felder; aus den redigierten Laufaggregaten lässt sich nicht ableiten, welche konkrete Decoderbedingung bei den Dateien griff.
- Ergebnis: Die zwei Produktläufe sind auf Aggregatebene reproduzierbar, aber ihre Ursache ist dadurch nicht geklärt. Der nächste sinnvolle Schritt bleibt eine lokale, datensparsame Offline-Inventur/Reanalyse der bereits gespeicherten FIT-Dateien. Falls dafür Diagnostik erweitert wird, sollte sie pro Datei nur aggregierte Anzahlen (dekodierte Beobachtungen, importierbare Werte, fehlende Zeitstempel, Qualitätsfilter und nicht zugeordnete Felder) ausgeben – keine Werte, Pfade, UIDs oder Payloads. Das erfordert Abstimmung mit dem FIT-Arbeitsbereich; hier wurde kein Decoder-/Transportcode geändert.

## Einordnung

PR #387s zusätzliche numerische Pull-Error-Felder konnten im GATT-133-Lauf nicht greifen, weil keine GATT-Verbindung bzw. Pull-Antwort zustande kam. `GATT 133` identifiziert in diesem Bericht nur den gemeldeten Transportstatus; eine CIRQA-spezifische Ursache ist nicht belegt.
