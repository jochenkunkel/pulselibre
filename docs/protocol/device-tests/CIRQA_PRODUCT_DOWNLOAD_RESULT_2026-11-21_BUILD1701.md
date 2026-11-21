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

## Einordnung

PR #387s zusätzliche numerische Pull-Error-Felder konnten hier nicht greifen, weil keine GATT-Verbindung bzw. Pull-Antwort zustande kam. `GATT 133` identifiziert in diesem Bericht nur den gemeldeten Transportstatus; eine CIRQA-spezifische Ursache ist nicht belegt. Kein weiterer Geräteversuch ist durch die erteilte Einmalfreigabe umfasst.
