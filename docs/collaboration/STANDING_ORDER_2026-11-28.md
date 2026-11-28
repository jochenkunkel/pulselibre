# Dauerauftrag der Projektleitung vom 28.11.2026

Erweitert [GENERAL_AUTHORIZATION_2026-11-24.md](GENERAL_AUTHORIZATION_2026-11-24.md). Ausführender Agent: Claude Code
(GUI-Agent, einziger Agent seit 26.11.).

## Freigegeben ohne Einzelrückfrage

- **Entwicklung:** alle offenen Tickets analysieren und priorisieren, Code und Doku ändern, PRs erstellen und nach
  grüner CI mergen, Tickets schließen und anlegen, Test-Builds erzeugen.
- **Smartphones S10 und S25:** ADB, Installieren und Updaten, PulsLibre starten und beenden, Logcat, HCI-Snoop,
  Screenshots, UI-Tests, Datenbank und Dateien zur Fehlersuche, Backups erstellen und wiederherstellen,
  Testeinstellungen.
- **CIRQA-Band, nur nichtdestruktiv:** Scan, Kopplung, GATT, Live-Messungen, Historie (vollständig und inkrementell,
  auch wiederholt), Garmin-Protokollabläufe, Akku und Metadaten, Aktivitäten und Schlaf, Mitschnitte und Replays.

## Schutzregeln

- Vor riskanten Änderungen ein geprüftes Backup erstellen (Manifest-SHA).
- Keine unnötigen Resets oder Löschungen.
- Keine privaten Gesundheitsdaten in öffentliche Repos.
- Keine Löschung von Daten auf dem Band und keine Firmware-Änderung.
- Destruktive Protokollbefehle (z. B. set_flags, delete) erst in ihrer Wirkung klären.

## Rückfrage nur bei

- Werksreset;
- möglichem unwiederbringlichem Verlust von Gesundheitsdaten;
- Firmware-Änderung am Band;
- erheblichem unkontrollierbarem Schaden;
- grundlegender Produktentscheidung.

#496 bleibt zurückgestellt. GitHub-CI und Branch-Protection bleiben die verbindliche Integrationsprüfung.
