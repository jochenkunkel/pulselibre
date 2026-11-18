# Dokumentationsprüfung – zweiter Durchgang (18.11.2026)

## Umfang und Prüfmethode

Bestandsaufnahme des GitHub-`main`-Baums: **505 Repository-Einträge**, darunter **105 Markdown-Dateien** (Zählung anhand der aktuellen GitHub-Tree-API; dieser Bericht ist beim Anlegen noch nicht enthalten). Die **13 zentralen Einstiegs- und Fachindexdateien** wurden vollständig auf lokale Markdown-Linkziele geprüft. Existierende Verzeichnisse gelten als gültige Linkziele. Die Prüfung umfasst nicht automatisch alle Anker, externen URLs, Grafiken, nicht-Markdown-Referenzen oder die Inhalte aller 105 Dateien.

Zusätzlich wurden die tatsächliche [Android-CI-Konfiguration](../.github/workflows/android.yml), [README](../README.md), [AGENTS.md](../AGENTS.md) und der ausführliche [Garmin-Connect-5.31-Forschungsbericht](protocol/research/GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md) abgeglichen.

## Ergebnisse

| Befund | Änderung / Nachweis | Restgrenze |
| --- | --- | --- |
| Haupt-README nutzte noch frühe BLE-Statusbeschreibung | Auf validierte 5030→5007→5031→5002-Kette, BLE-Platzhalter und RFCOMM-V1 ohne Gegenverbindung aktualisiert | Kein vollständiger FIT-Download bestätigt |
| Projektname uneinheitlich | Sichtbare README-Überschrift/Logo-Alternativtext auf **PulsLibre** vereinheitlicht | Historische Eigenbezeichnungen nicht pauschal umgeschrieben |
| `AGENTS.md` verwies auf frühe 5050-Tests | Kanonischen aktuellen Forschungsstand verlinkt | Historische Testberichte unverändert |
| `AGENTS.md` behauptete automatische CI auf Pushes/PRs/Tags | Gegen YAML geprüft und auf **nur `workflow_dispatch`** korrigiert | CI-Lauf wurde nicht gestartet |
| Mitarbeit für Außenstehende schwer auffindbar | [Beitragsleitfaden](CONTRIBUTING_GUIDE.md) erstellt und in README/Dokumentationsindex verlinkt | Kein Code of Conduct oder Security-Reporting-Prozess beschlossen |
| Binary-Zusammenfassung ließ bereits dokumentierte APK-Metadaten aus | Auf versionCode 11761, SHA-256 und DEX/JADX-Nachweise im ausführlichen Bericht abgeglichen | APK-Herkunft und reproduzierbarer Nachvollzug bleiben offen |
| Lokale Links der 13 geprüften Einstiegs-/Indexdateien | **Keine fehlenden lokalen Datei-/Verzeichnisziele gefunden** | Keine Aussage zu den übrigen Markdown-Dateien oder Fragmentankern |
| Kollaborationsindex suggerierte stündlich laufenden Agenten | Klarstellung: nur tatsächlich eingerichtete Automation oder aktiv gestarteter Agent | Keine Automation hier eingerichtet |

## Nächste Qualitätsrunde

1. Alle Markdown-Dateien automatisiert auf relative Pfade **und Anker** prüfen; externes Link-Checking separat und ressourcenschonend.
2. Veraltete CI- und Freigabeaussagen in älteren, als historisch markierten Dokumenten inventarisieren; Primärnachweise nicht stillschweigend verändern.
3. Einmalige sprachliche Überarbeitung der öffentlich sichtbaren Produkt-, UI-, Wissenschafts- und Architektur-Einstiege; Protokoll-Bytewerte, Hashes und Testvektoren dabei unangetastet lassen.
4. Für externe Mitwirkende die gewünschte Security-Meldestelle, Verhaltensregeln und Lizenz-/Drittanbieter-Provenienz abstimmen; keine erfundenen Kontaktadressen eintragen.
5. Den tatsächlich manuellen CI-Prozess sichtbar halten; Änderungen an CI-Triggern sind eine eigene technische Entscheidung, keine reine Dokumentationskorrektur.

**Verifikation:** Redaktionelle GitHub-Dateiprüfung und lokale Pfadauflösung anhand des GitHub-Baums; **keine** Gradle-Builds, Hardwaretests oder GitHub-Actions-Läufe in diesem Durchgang. Die Prüfung ist keine vollständige Rechts-, Sicherheits- oder Codeaudit-Freigabe.
