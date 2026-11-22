# PulsLibre – iterativer Modularitätsaudit (ChatGPT)

- **Issue:** https://github.com/jochenkunkel/pulselibre/issues/496
- **Fachlicher Owner:** ChatGPT (keine GitHub-Account-Zuweisung)
- **Beginn:** 2026-11-22
- **Arbeitsmodus:** Iterative, belegte Analyse auf Basis von GitHub `main`; Dokumentation nach jeder Runde.
- **Umfang:** Analyse und Planung, **kein** großflächiges Refactoring und keine aktiven Gerätetests.

## Zielbild

GUI, Anwendungslogik, herstellerunabhängige Domäne und Datenmodelle, Persistenz/Export, wissenschaftliche Analyse, Gerätesynchronisation, Bluetooth-Transport und herstellerspezifische Adapter werden durch explizite Schnittstellen getrennt. Garmin CIRQA ist ein Adapter unter mehreren künftigen Trackern. Domain und wissenschaftliche Berechnungen sollen ohne Android- und Herstellerabhängigkeiten testbar sein.

## Runde 1 – vorläufige Bestandsaufnahme (2026-11-22)

**Untersuchter GitHub-main-Commit:** `95b6895d425d0da2679aedadd7b8fff89b0b38d4` (Verzeichnisindex zum Prüfzeitpunkt). Die nachstehenden Befunde sind gezielte Codeprüfungen und **kein vollständiger Repository-Audit**. Builds und Tests wurden nicht ausgeführt.

| Priorität | Vorläufiger Befund | Beleg | Empfohlene Prüfung |
|---|---|---|---|
| P0 | Nur ein Gradle-App-Modul eingebunden | `settings.gradle.kts` mit `include(":app")` | Paket-/Importgraph und mögliche Modulgrenzen prüfen |
| P0 | Activity verknüpft Android-Bluetooth, Polling, konkrete CIRQA-Synchronisation und Datenzugriff | `app/src/main/java/de/pulselibre/MainActivity.kt` | UI-Anwendungsdienste und ViewModel-Verträge ableiten |
| P0 | Sehr umfangreiche GATT-Session-Orchestrierung mit Android- und SQLite-Abhängigkeiten | `app/src/main/java/de/pulselibre/CirqaMlHandshakeProbe.kt` | Zustandsmaschine, Transport, Protokoll, FIT-Verarbeitung und Speichergrenzen kartieren |
| P1 | Zentrale SQLite-/Migrationsklasse | `app/src/main/java/de/pulselibre/CirqaDatabase.kt` | Repository-Interfaces und Schema-Migrationssicherung prüfen |
| P1 | Dashboard-View greift selbst auf SharedPreferences zu | `app/src/main/java/de/pulselibre/DashboardOverviewView.kt` | UI-State-/Settings-Schnittstelle definieren |
| P1 | Historien-Repository erzeugt konkrete SQLite-Datenbank über Android Context | `app/src/main/java/de/pulselibre/HistoricalObservationRepository.kt` | Domänen-Query-Interface vom SQLite-Adapter trennen |
| P1 | FIT-Committer ist an Android SQLiteDatabase gebunden | `app/src/main/java/de/pulselibre/FitObservationCommitter.kt` | Transaktions-/Persistenzgrenze festlegen |
| Positiv | Rein rechnerische Statistik bereits separierbar | `app/src/main/java/de/pulselibre/PairedCorrelation.kt` | Als Referenz für reines Analysemodul prüfen |
| Positiv | RFCOMM-Transport bereits in separater Klasse und explizit gesperrt | `app/src/main/java/de/pulselibre/CirqaRfcommTransport.kt` | Gate und Transport-Schnittstelle erhalten |

## Vorgeschlagene Modulgrenzen (noch nicht beschlossen)

- `:core:domain`: Geräte-/Messwert-/Provenienzmodelle, Repository- und Dienstverträge; reines Kotlin.
- `:core:analysis`: Statistik und wissenschaftliche Auswertungen; reines Kotlin, keine Android- oder BLE-Imports.
- `:device:api`: Gerätefähigkeiten, Discovery, Session-/Sync-Verträge.
- `:transport:bluetooth`: Android BLE/GATT, Classic/RFCOMM, Verbindungs- und Berechtigungsadapter.
- `:device:garmin-cirqa`: Garmin-spezifisches Protokoll, FIT-Transfer, Decoder-/Synchronisationsadapter.
- `:data:storage`: SQLite, Migrationen, Repository-Implementierungen, Provenienzspeicher.
- `:feature:dashboard`, `:feature:history`, `:feature:devices` und `:app`: Präsentation, Navigation, Composition Root.

**Abhängigkeitsregel:** Kern-Domäne darf keine Android-, Bluetooth-, SQLite- oder Garmin-Implementierung importieren. Adapter implementieren Schnittstellen des Kerns; die App verdrahtet Implementierungen. Ein zweiter Fake-Tracker soll ohne Änderungen an Dashboard und Analysemodul integrierbar sein.

## Iterationsprotokoll

| Runde | Status | Ergebnis | Nächster Schritt |
|---|---|---|---|
| 1 – Inventar und Baseline | In Arbeit | Gradle-Einmodulstruktur und erste kritische Klassen identifiziert | Vollständige Dateiliste, Tests, Importgraph, CI-Stand und Datenfluss erfassen |
| 2 – Kopplungsanalyse | Offen | – | Zyklen und konkrete Schnittstellenbrüche mit Quellverweisen belegen |
| 3 – Soll-Architektur | Offen | – | API-Verträge und ADRs ausarbeiten |
| 4 – Migrationsplan | Offen | – | Kleine unabhängige Tickets, Regressionen und Rollback definieren |
| 5+ – Re-Audits | Offen | – | Nach main-Änderungen erneut prüfen |

## Regeln für Folgearbeiten

1. Jeden Durchlauf mit Datum, Git-Commit-SHA, tatsächlich geprüften Dateien, Ergebnissen, Unsicherheiten und offenen Aufgaben ergänzen.
2. Nach jedem Durchlauf Issue #496 aktualisieren; bestehende Agent-Chat-/Sync- und Session-Regeln in `AGENTS.md` beachten.
3. Keine unerlaubten BLE-/GATT-/FileSync-Aktivitäten, keine physischen Gerätetests ohne Freigabe.
4. Bereits funktionierende FIT-Imports, SQLite-Daten, Provenienz, Exporte und Sicherheitsschranken durch Regressionstests schützen.
5. Refactoring nur über separate, klein zugeschnittene Umsetzungstickets.
