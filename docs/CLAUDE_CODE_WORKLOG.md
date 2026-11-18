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

## 2026-11-18 – #41 Trends (Branch `claude/trends`)
- `TrendAnalysis`: Median der letzten 7/28/90 vollständigen Tage gegen gleich langen Vorzeitraum; Mindesttage = max(3, ⌈N/2⌉) je Fenster (4/14/45), sonst „nicht bewertbar“; Stabilitätsschwelle = max(Absolutwert, 3 % der Baseline). Heute ausgeschlossen, Lücken nie aufgefüllt, Median robust gegen Ausreißer.
- `DailyValueReader`: Geräte-Ruhepuls (`resting_bpm`, Median je lokalem Tag; **kein** Tagesminimum der Herzfrequenz) und Schritte (höchster Tageszähler). Gruppierung nach lokalem Kalendertag.
- `TrendCardView` im Verlauf-Tab, lädt im Hintergrund, 10 min Cache; Erklärtext mit methodischen Grenzen, keine Diagnose.
- Tests: `TrendAnalysisTest` (9), `DailyAggregationTest` (2), Instrumentationstest für den Reader. Offen: Aktive Minuten (keine verifizierte Quelle), Drilldown, individuelle Baseline über Mehrmonatsdaten, Pfeil-/Farbdesign.

## 2026-11-18 – #42 Fitnessübersicht, #43 Prüfung (Branch `claude/fitness`)
- `FitnessOverview`: drei getrennte Dimensionen (Fitnessentwicklung, Regeneration, Aktivitätskonstanz), **keine Gesamtpunktzahl**. Zustände positiv/neutral/uneinheitlich/nicht bewertbar. „Positiv“ nur, wenn Schritte steigen **und** Geräte-Ruhepuls fällt; ein fallender Ruhepuls allein ist neutral. Gegenläufige Trends = uneinheitlich. Anzeige in der Trendkarte (28 Tage), Text statt Farbe.
- Regeneration ist immer „nicht bewertbar“, solange keine verifizierten Schlaf-/HRV-Daten existieren (siehe `docs/science/DATA_AVAILABILITY_AUDIT.md`). Damit ist auch #43 aktuell **nicht berechenbar**; keine Schätzung aus Schrittlosigkeit.
- Tests: `FitnessOverviewTest` (6). Nicht umgesetzt: VO₂max, Fitnessalter, Ampeln, Body-Battery-Umdeutung (laut Issue ausgeschlossen).

## 2026-11-18 – #44/#45 Zusammenhänge (Branch `claude/correlation`)
- `PairedCorrelation` (reine Logik): Paarung Tag t ↔ Tag t+Lag (nur Tage mit beiden Werten), Spearman ρ (Ränge mit Bindungsmittelung), Pearson zusätzlich, 95-%-Fisher-z-Intervall mit effektivem n nach Lag-1-Autokorrelation (Bretherton et al. 1999), Mindestens 30 gepaarte Tage, konstante Reihen = nicht berechenbar, „explorativer Zusammenhang“ nur wenn Intervall 0 ausschließt und |ρ| ≥ 0,1. Benjamini–Hochberg-Funktion für mehrere vorab festgelegte Hypothesen vorhanden (aktuell 1 Hypothese, daher keine Korrektur nötig).
- Verdrahtet: Schritte(Tag) ↔ Geräte-Ruhepuls(Folgetag), Trendkarte. Alle Hypothesen mit Schlaf/HRV bleiben nicht berechenbar (keine verifizierte Quelle, #43).
- Nicht umgesetzt: Streudiagramm, Stratifizierung nach Bewegungsumfang (#44), Sensitivitätsanalyse mit Ausreißern, Wochentagseffekte. Tests: `PairedCorrelationTest` (9) mit synthetischen unabhängigen, korrelierten, zeitverschobenen, lückenhaften Daten.

## 2026-11-18 – Drilldown Streudiagramm (#44/#45, Branch `claude/scatter`)
- `ScatterView`: Streudiagramm der echten Wertepaare (Schritte Tag ↔ Geräte-Ruhepuls Folgetag), Achsenbeschriftung, **keine** Trendlinie, Screenreader-Beschreibung mit Anzahl und Wertebereich. Öffnet per Tippen auf die Trendkarte, Hinweis „keine Ursache-Wirkung-Aussage“.
- Test: `ScatterViewTest` (Instrumentation: Punkte werden gezeichnet; identische Werte ohne Absturz). Optik auf dem S25 ungeprüft.

## 2026-11-18 – #39 SpO₂ (Branch `claude/spo2`)
- Ist-Stand: Orientierungsskala 95/90 % mit Feature-Gate (`SpO2DisplaySettings`, Standard AUS, neutrale Farbe ohne Geräte-Validierung) bereits von Codex umgesetzt und getestet; unverändert.
- Neu: `NocturnalOxygen` (reine Logik, **nicht in die UI eingebunden**): Zeit < 90 % und ODI3/ODI4 je Stunde über ein vom Aufrufer gelieferte Uhrzeit-Fenster (keine Schlafphasen). Gates: Fenster ≥ 4 h, Abdeckung ≥ 70 % (jeder Wert gilt höchstens 120 s), ODI nur bei Median-Abstand ≤ 30 s, Werte 50–100 % gültig, keine Interpolation. Ereignis = Abfall ≥ N Punkte gegen das Maximum der vorangehenden 120 s, Wiederbewaffnung nach Erholung auf ≤ 1 Punkt. Sonst „nicht berechenbar“ ohne Zahlen.
- Tests: `NocturnalOxygenTest` (8). **Blocker:** Geräte-Validierung des CIRQA-Sensors, tatsächliche Abtastrate der SpO₂-Messung und fachliche ODI-Definition (AASM-Version) sind nicht geprüft; die Auswertung bleibt bis dahin unsichtbar und ist keine Diagnose (keine Schlafapnoe-Aussage).
