# Arbeitsprotokoll Claude Code (Dashboard, GUI, Analysen)

Fortlaufend, neueste Einträge unten. Jeder Eintrag: Datum, Änderung, Nachweis (Test/CI-Lauf), offene Punkte. Gerätetests werden nur als solche benannt, wenn sie wirklich stattfanden.

## 2026-11-18 – Übernahme von Codex
- Übernahmeprüfung gegen `main` b6e229a. Entwicklung in eigenen Branches `claude/*` und Worktree `../pulselibre-claude`; das Haupt-Checkout (uncommittete Dokumentänderungen anderer Agenten) blieb unberührt.
- **#61, #64 geschlossen:** Code und Emulator-CI (Lauf 44260486032) geprüft, kein S25-Test.
- **#63 (PR #70):** `CirqaLinkStatus` – Online nur bei GATT verbunden oder frischer Messung (< 120 s, Recorder läuft). Recorder schreibt `last_reading_ms`. Unit-Tests `CirqaLinkStatusTest`. Ursache der GATT-Abbrüche weiter offen (OpenCode/Hardware). Issue offen.
- **#40 (PR #70, #72, folgender PR):** `DayDataQuality` (Abdeckung, Lücken), `MetricAvailability` (Zustände), Audit `docs/science/DATA_AVAILABILITY_AUDIT.md`.
- **#62 geschlossen (PR #72):** `DashboardDayReaderTest` (Emulator-CI 44267531142 grün), inkl. DB-Downgrade-Test ohne Datenverlust.
- **CI:** Lauftitel enthalten jetzt Branch, Testumfang und Commit; Jobs heißen „Build, Unit-Tests, Lint, signierte APK“ und „Emulator-Instrumentationstests“.
- Lauf 44269457396 (voll, grün): Datenverfügbarkeit (#40) und neue CI-Namen bestätigt.
- Signierte Test-APK: CI-Artefakt `pulselibre-signed-update-apk` des vollen Laufs. Zertifikatskontinuität und Update-Installation auf dem S25 **nicht** geprüft (vom Nutzer abgelehnt).

## Offen / nächste Schritte
- #40: Repository-Schicht für Nacht-/Mehrtageswerte; Zustandsanzeige in den Kacheln.
- #41 Ruhepuls-/Aktivitätstrends (7/28/90 Tage), danach #42–#47.
- #63: S25-Langzeittest mit CIRQA nötig (Freigabe des Nutzers).
