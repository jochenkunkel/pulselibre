# START – S10e/CIRQA: Build 1939 installieren und einen Historien-Sync testen
- Zeit (UTC): 2026-11-23 08:15
- Von: OpenCode FIT / Integrations-Owner
- An: CI-Agent, GUI-Agent, Nutzer, alle
- Bezug: Nutzerfreigabe `2026-11-23_0800_chatgpt-an-alle_s10-cirqa-testfreigabe.md`; vorherige Koordinationskorrektur `2026-11-23_080800_opencode_UPDATE_ci-lease-collision-and-fit-gui-answer.md`
- Status: vor Installation; gesamter Hardwarezugriff ausschließlich innerhalb eines atomar erworbenen S10-Lease

## Build-/Code-Nachweis
- Neuester erfolgreicher Main-Build: Run `45132518466`, Build/Run 1939, FULL `success`, exact head `b784e56cb774ff87d0b382ed5ec6c7355d5b8006`.
- Der aktuelle `origin/main=bf19a887` enthält danach nur Dokumentationsänderungen in den Android-App-Dateien; Historien-Sync-Produktcode aus dem Build ist somit unverändert. Der separate FIT-Coverage-PR #547 ist weiterhin offen und **nicht** in diesem Build enthalten.
- APK-Artifact `14797629394`, `de.pulselibre`, versionCode `1939`, versionName `2.0.0_build1939`, APK SHA-256 `1ef5613657ca2d4c2aa2921bf73301d138d63a09c971aeddbc5cc39e6d019ff5`; APK-Signatur v2 gültig, Zertifikat SHA-256 `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`, passend zur verifizierten 1934-Update-APK.

## Testumfang / Grenzen
- Status vor Wiederaufnahme zunächst FREI; unmittelbar vor Lease-Erwerb erneut prüfen. Danach genau ein Wrapper `tools/s25_lease.py run --device s10 ...`, der Modell-/Installationsvorbedingungen, `adb install -r` und Test samt Nachprüfung umfasst.
- Ziel ist ausschließlich S10e. Vor Installation aktuelle Paketversion, Recorderzustand und Garmin Connect prüfen; keinen aktiven Recorder/Connect-Sync unterbrechen. `install -r` nur mit bestätigter Signaturkontinuität; nicht deinstallieren, Daten nicht löschen.
- Danach genau ein manuell bestätigter Historien-/FIT-Sync über die App, maximal 120 s (App-interne Grenzen: 256 Dateien, 8 MiB je Datei, 64 MiB insgesamt). Dialoghinweis beachten: CIRQA kann Dateien als übertragen markieren. Kein Retry, kein separates GATT-Probing, keine Instrumentation, kein Archiv-/SetFlags-/Lösch-/Reset-/Unpairing-/`CLOSE_ALL`.
- Stoppen bei unklarem Ziel/Build, aktivem Recorder oder Garmin Connect, Installationsfehler, unerwarteter Statusänderung, Protokoll-/Datenbankfehler oder Ablauf des Limits. Danach redigierte Phasen-/Zähl-/Integritätsevidenz, keine Messwerte/UIDs/Rohdaten.
