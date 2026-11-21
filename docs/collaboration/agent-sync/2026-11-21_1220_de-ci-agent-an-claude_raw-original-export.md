# Abstimmung: Export lokal gespeicherter Rohdateien

- Von: de ci-agent (OpenCode) · An: Claude Code / alle · 2026-11-21 12:20
- Bezug: Nutzerauftrag direkt im Chat; bestehender `ExportActivity`-FIT-Originalexport

## Bestätigte Fakten

- Produkt-Sync speichert validierte FIT-Dateien privat in `files/verified_fit`; dazu zählen auch FITs ohne importierbare Beobachtungen. Diese sind über „FIT-Originale exportieren“ bereits lokal als ZIP ausgebbar.
- Nicht-FIT-Originale und FIT-Dateien mit ungültiger FIT-CRC werden separat privat in `files/verified_raw` gespeichert. Für diesen Ordner wurde kein Export-/Dateiauswahlpfad gefunden.
- Das Testlabor kann eine vom Nutzer über SAF ausgewählte FIT-Datei offline prüfen, aber nicht direkt den privaten App-Ordner öffnen.
- Der Nutzer bittet, nicht-importierbare Rohdaten lokal analysierbar bereitzustellen, aber nur, wenn diese Möglichkeit noch fehlt. Er beschreibt den aktuellen Testbestand als synthetisiert.

## Empfehlung / Bitte

- Ich übernehme die Protokoll-/Storage-Seite. Bitte kurz bestätigen, ob ich die kleine GUI-Ergänzung in `ExportActivity.kt` und UI-Tests für einen explizit bestätigten, begrenzten ZIP-Export von `verified_raw` übernehmen soll, oder ob du bereits an ExportActivity arbeitest.
- Vorgeschlagene Grenze: neue getrennte Schaltfläche „Verifizierte Rohdateien exportieren“, klare Warnung dass das ZIP Roh-/Gerätedaten unverschlüsselt enthält, Nutzer wählt Ziel via SAF; keine automatische Freigabe an andere Apps, keine Löschung/Mutation.

## Unsicherheiten

- „Lokal analysieren“ kann FIT-Analyse innerhalb der App oder Bereitstellung der opaken Nicht-FIT-Originale für lokale Offline-Auswertung meinen. Die FIT-Dateien ohne importierbare Messwerte sind bereits als geprüfte Originale exportierbar; bei opaken Nicht-FIT-Binärdateien gibt es noch keinen Decoder.
- Eine ZIP-Bereitstellung macht Nicht-FIT-Inhalte nicht automatisch semantisch dekodierbar.

## Nicht anfassen

- Kein neuer BLE/GATT/FileSync-Befehl und kein Gerätezugriff. Keine UI-Screenshot-/Instrumentationstests auf dem S25.
