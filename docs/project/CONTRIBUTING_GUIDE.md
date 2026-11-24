# Mitarbeit an PulsLibre – Einstieg für externe Entwickler

**Stand: 18.11.2026.** PulsLibre ist eine experimentelle, datenschutzorientierte Offline-first-Android-App für Garmin-CIRQA-Daten. Das Projekt ist **kein** fertig validierter Ersatz für Garmin Connect und keine medizinische Software. Aussagen zu Gerätetests und CI immer anhand der datierten Nachweise prüfen.

## In fünf Minuten orientieren

1. [README](../../README.md): Projektziel, derzeitiger Funktionsumfang und Build-Einstieg.
2. [AGENTS.md](../../AGENTS.md): Arbeitsregeln, Datenschutz, Gerätesicherheit und parallele Agenten.
3. [Projektstatus](PROJECT_STATUS.md): datierte Team- und Entwicklungsübergabe; historische Abschnitte bleiben archiviert.
4. [Dokumentationswegweiser](../../README.md): UI, Wissenschaft, Protokoll und Tests.
5. [Aktueller CIRQA-Forschungsstand](../protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md): bestätigte BLE-Befunde, RFCOMM-Versuche und offene Fragen.

## Woran Mitarbeit sinnvoll ist

- **Android/Kotlin und UI:** Navigation, Barrierefreiheit, belastbare Offline-Zeitreihen, leere und unvalidierte Zustände. Siehe [UI-Dokumentation](../product/ui/README.md).
- **Qualität und Tests:** reproduzierbare Unit-/Instrumentation-Tests, CI-Evidenz, sichere Upgrades und Regressionstests. Siehe [Qualitätsanforderungen](../quality/QUALITY_GATE.md) und [Testinventar](../archive/2026-11-24/TEST_INVENTORY_2026-11-18.md).
- **CIRQA-Protokoll:** GFDI-/COBS-/CRC-Offline-Fixtures, Fehlerpfade und dokumentierte Protokollvergleiche. **Keine ungefragten aktiven Gerätetests oder destruktiven Befehle.** Siehe [Protokollindex](../protocol/README.md).
- **FIT und Wissenschaft:** Profile, Einheiten, Zeitstempel, Datenherkunft und unabhängige Decoder-Validierung. Siehe [Wissenschaftsbereich](../science/README.md) und [Garmin FIT SDK](https://developer.garmin.com/fit/).

## Vor dem ersten Beitrag

- Einen kleinen, klar abgegrenzten GitHub-Issue-Bereich wählen; vor Änderungen den aktuellen `main`-Stand und parallele Arbeiten prüfen.
- Keine echten Gesundheitsdaten, Geräteadressen, Zugangsdaten, APK-/DEX-Dateien, Schlüssel oder privaten Diagnoserohdaten in Commits, Issues oder Logs veröffentlichen.
- Für Protokolländerungen zunächst synthetische Fixtures und deterministische Tests nutzen; Geräteaktionen erfordern eine separate, ausdrückliche Freigabe.
- Fremden Quellcode nicht ungeprüft übernehmen: Lizenz, Urheberschaft und Herkunft dokumentieren. Für Garmin-Connect-Binary-Forschung gelten die [Interoperabilitätsleitplanken](../protocol/research/GARMIN_CONNECT_BINARY_ANALYSIS.md).
- Änderungen nachvollziehbar beschreiben: **Was wurde geändert? Warum? Welche Tests liefen tatsächlich? Was bleibt unbestätigt?**
- Bei paralleler Arbeit separate Branches/Worktrees verwenden und den in [AGENTS.md](../../AGENTS.md) beschriebenen Review-/Merge-Prozess beachten.

## Lokale Entwicklung und Qualität

- Android-Projekt mit Kotlin; SDK- und JDK-Voraussetzungen sowie Beispielbefehle stehen in der [README](../../README.md). Der CI-Workflow ist [Android CI](../../.github/workflows/android.yml).
- Ein erfolgreicher JVM-Test oder APK-Build ist **kein** Nachweis für einen bestandenen Emulator-, Geräte- oder vollständigen Qualitätstest.
- Für UI- und wissenschaftliche Beiträge müssen fehlende beziehungsweise unvalidierte Daten sichtbar bleiben. Keine erfundenen Messwerte und keine klinischen Wirkversprechen.

## Dokumentation schreiben

- Neue Dokumente möglichst in vorhandenen Fachordnern ablegen und im passenden Index verlinken.
- Technische Aussagen als **Codebefund**, **Offline-Test**, **Gerätebeobachtung**, **externe Referenz** oder **Hypothese** kennzeichnen.
- Datierte Versuchsberichte nicht stillschweigend umschreiben; neue Erkenntnisse mit Datum ergänzen und auf den aktuellen Forschungsstand verweisen.
- Kurze Sätze, einheitliche Begriffe (`PulsLibre`, `CIRQA`, `GFDI`, `FIT`, `RFCOMM`) und funktionierende relative Markdown-Links verwenden.

**Fragen oder Vorschläge:** Ein GitHub-Issue mit reproduzierbarer Beschreibung, betroffenen Dateien und erwarteten Ergebnissen eröffnen; bei Sicherheits-/Datenschutzproblemen keine sensiblen Details öffentlich posten.
