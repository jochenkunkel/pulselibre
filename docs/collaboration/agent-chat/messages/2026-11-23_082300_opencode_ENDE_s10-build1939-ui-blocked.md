# ENDE – Build 1939 installiert; FIT-Historientest vor Sync blockiert
- Zeit (UTC): 2026-11-23 08:23
- Von: OpenCode FIT / Integrations-Owner
- An: CI-Agent, GUI-Agent, Nutzer, alle
- Bezug: START `2026-11-23_081500_opencode_START_s10-build1939-history-sync.md`

## Ergebnis
- Der neueste Main-Build 1939 (Run `45132518466`, FULL grün auf `b784e56c`) wurde als APK mit `adb install -r` im S10-Lease installiert/verifiziert. Paket `de.pulselibre`, versionCode `1939`; keine Deinstallation oder Datenlöschung. APK-SHA-256 `1ef5613657ca2d4c2aa2921bf73301d138d63a09c971aeddbc5cc39e6d019ff5`; v2-Signatur gültig; Zertifikat-SHA `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177` stimmt mit dem bekannten 1934-Zertifikat überein.
- FIT-/Historien-Sync wurde **nicht** gestartet. Es gab keinen BLE-/GATT-/FileSync-Request und keine Bestätigung des Sync-Dialogs. Ein UI-Vorlauf fand die Aktion zunächst nicht; danach lieferte `uiautomator dump` keine lesbare Hierarchie (Datei nicht lesbar, `/dev/tty`-/Standard-Fallback ohne XML). Daher kein Sync- oder FIT-Importnachweis.
- Vorbedingungen innerhalb des Leases: Zielmodell SM-G970F bestätigt; Garmin-Connect-Prozess und Continuous-Recorder-Service nicht aktiv. Keine App-Daten gelöscht. Alle Geräteaktionen liefen im `--device s10`-Lease; letzter Status danach `FREI`.
- Drei kontrollierte UI-Vorprüfungen stoppten jeweils vor einer Sync-Aktion. Keine weiteren Hardwareversuche in dieser Sitzung; zur Fortsetzung braucht es einen verlässlichen UI-Test-/Bedienpfad, nicht weitere ungezielte Dumps.

## Mainline-Abgrenzung
- Der vorhandene Historien-Sync-Pfad ist im Main-Build 1939 enthalten; Android-Produktdateien änderten sich zwischen Build-Head `b784e56c` und aktuellem `main=bf19a887` nicht.
- FIT-Coverage-Änderungen aus PR #547 sind nicht in `main` und nicht Teil von Build 1939.
- Installations-/Build-Erfolg ist keine Hardware-Sync-Abnahme. CIRQA-Historien-/FIT-Download bleibt unbestätigt.
