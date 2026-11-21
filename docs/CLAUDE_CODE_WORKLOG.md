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

## 2026-11-18 – Validierungsgates akzeptiert, ungeprüfte Kacheln rot gerahmt (Branch `claude/validation-gates`)
- **Entscheidung des Nutzers (2026-11-18):** alle Validierungsgates werden akzeptiert; gesperrte Funktionen werden angezeigt, aber **jede Kachel/Karte mit ungeprüfter Quelle trägt einen dunkelvioletten Rand** (`UnvalidatedFeatures`, 2 dp, #CD3941) plus Barrierefreiheitstext „Nicht validiert, violetter Rand“. Legende oben im Dashboard. Das validiert nichts und ist keine medizinische Aussage.
- Betroffene Kacheln: SpO₂, Ruhepuls, Stress, Body Battery, Atemfrequenz, Kalorien, Schlaf, Nacht-Insights, Trendkarte (enthält Geräte-Ruhepuls).
- SpO₂-Farbskala 95/90 % jetzt standardmäßig aktiv (`OWNER_ACCEPTED_UNVALIDATED`); Texte in Karte und Einstellungen benennen den Zustand („vom Nutzer akzeptiert, Sensor NICHT validiert“).
- Nacht-Insights zeigen jetzt echte Werte aus `NocturnalOxygen` für die letzte Uhrzeit-Nacht 22–6 Uhr (`NightOxygenReader`); die mathematischen Gates (Abdeckung ≥ 70 %, ODI nur bei dichter Abtastung) bleiben bewusst bestehen, sonst „nicht berechenbar“ – das sind Rechengrenzen, keine Validierungsgates.
- Weiterhin nicht umsetzbar ohne Datenquelle: Schlaf/Regeneration (#43), VO₂max/Fitnessalter (#46/#47).

## 2026-11-18 – Revalidierung R01–R12 (#5, Branch `claude/audit-5`)
- Ergebnis in `docs/AUDIT_REVALIDATION_2026-11-18.md`. Nur Code-Lektüre, keine neuen Tests. Offen: Test „Diagnoseexport ohne Rohdaten“, Herkunft `device_id` im CSV; R06/R07/R10 → OpenCode; R09 dokumentiert.

## 2026-11-18 – #79 Verlauf-Moduswechsel (Branch `claude/history-perf`)
- **Messung vorher (S25, Build 989, 112 311 Ereignisse, ~16 000 Pulswerte/24 h):** Wechsel 24 h/7 d/30 d dauerte bis zum stabilen Bild ca. 1,5–12 s (grobe adb-Screenshot-Messung, Schwankungen durch Messmethode). Ursachen im Code: synchroner Lesevorgang auf dem UI-Thread, `julianday()`-Filter ohne Indexnutzung, JSON-Parsing aller Zeilen bei jedem Wechsel.
- **Änderung:** `HistoryDataCache` (einmalige indizierte Abfrage der letzten 30 Tage in primitive Arrays, danach nur neue Ereignisse nach `id`; alle Bereiche sind Ausschnitte), Laden im Hintergrund mit Generations-Token (nur die neueste Anfrage rendert), Lade-Hinweis, Invalidierung bei Backup-Wiederherstellung/Reset und bei Datenbankwechsel. `HistoryBucket` enthält jetzt zusätzlich den arithmetischen Mittelwert (Min/Max/Median/Mittel/Anzahl).
- Tests: `HistoryPointStoreTest` (4), Instrumentationstest für asynchrones Rendern. **Nicht erledigt:** Phase B/C des Issues (persistente Rollup-Tabellen), Mittelwert noch nicht in der Anzeige.

## 2026-11-18 – Einstellungen: Version/Build und kompaktes Layout (Branch `claude/settings-compact`)
- Neu: `AppVersionInfo` – Version und Build (aus versionName/versionCode, Debug-Kennzeichnung) als erste Zeile der Einstellungen (Avatar oben rechts → Einstellungen). Test `AppVersionInfoTest`.
- Einstellungen kompakter: Abschnittsüberschriften mit Symbol, Zahlenfelder nebeneinander (Größe/Gewicht, Schrittziel/Schlafziel, Geburtsjahr/Alter, Ruhe/Sport/Fenster, SpO₂ grün/orange), lange Erklärtexte hinter ⓘ-Dialogen (Aktivität, SpO₂, Profil). Alle Felder, Schlüssel und Validierungen unverändert.
- Verlauf (#79) Gerätemessung auf dem S25 (Build 996, adb-Screenshot-Polling, grob): Wechsel zwischen 24 h/7 d/30 d ≈ 1 s bis stabiles Bild (Messfloor ≈ 0,9 s), vorher 1,5–12 s. Die Messung war nicht sauber kontrolliert (ein Ausreißer 3,8 s, Fehltipp-Störungen); Aussagekraft: deutliche Verbesserung, kein exakter Wert.

## 2026-11-18 – Einstellungen: Schaltflächen und Löschschutz (Branch `claude/settings-buttons`)
- Eigene feste Schaltflächenleiste unten im Dialog („Schließen“ | „Speichern“, volle Breite, 48 dp) statt der drei AlertDialog-Buttons, die untereinander lagen und abgeschnitten waren.
- „Profil löschen“ ist von Speichern getrennt (eigener Abschnitt am Ende, dezent umrandet) und verlangt **zwei Bestätigungen** (1. Umfang erklären → „Weiter“, 2. „Wirklich endgültig löschen?“ → „Endgültig löschen“). Gelöscht werden nur die Profil-Einstellungen, keine Messdaten.

## 2026-11-18 – Fehler: Nacht-Insights blieben bei „Nachtserie wird gelesen …“ (Branch `claude/night-fix`)
- Auf dem S25 entdeckt (Build 1014): Die Übersicht wird bei jedem neuen Messwert neu aufgebaut; die Hintergrundabfrage aktualisierte jeweils die bereits ersetzte Karte und startete pro Neuaufbau einen weiteren Thread. Ergebnis: Anzeige blieb im Ladezustand.
- Behebung: Ergebnis wird 5 min zwischengespeichert und sofort angezeigt, höchstens ein Ladevorgang gleichzeitig, das Ergebnis aktualisiert die aktuell sichtbare Karte.
## 2026-11-18 – Trendkarte kompakt, SpO₂-Titel (Branch `claude/trend-compact`)
- Trendkarte statt Textwand: Zeile je Dimension mit Zustandssymbol (✅ ➖ ⚠️ ⏳), Tabelle 💓 Ruhepuls / 👣 Schritte × 7/28/90 Tage (Median + Pfeil + Tage mit Daten), eine Zeile 🔗 für die vorab festgelegte Hypothese (Tippen → 📊 Streudiagramm). Methode, Sätze und Einschränkungen im ⓘ-Dialog; Barrierefreiheitstexte enthalten die vollen Sätze. Berechnung unverändert (`TrendAnalysis`, `FitnessOverview`, `PairedCorrelation`).
- Kachel „Sauerstoffsättigung · SpO₂“ zeigt sichtbar nur „SpO₂“ (Wortumbruch mitten im Wort behoben); Tag/Accessibility-Name unverändert.

## 2026-11-18 – #5 Abschluss (Branch `claude/audit-5-tests`)
- `DiagnosticReport` extrahiert (Parameter nur Zählwerte/Flags/Aggregat-Zusammenfassungen) und `DiagnosticReportPrivacyTest` ergänzt: nach realen Beobachtungen mit MAC/Namen darf keine Adresse/kein Name/keine Payload im geteilten Text stehen.
- `device_id` im CSV: Konstante `CIRQA`; Legacy-Datenbank auf dem Testgerät leer. Details in `docs/AUDIT_REVALIDATION_2026-11-18.md`.

## 2026-11-18 – ODI-Artefaktfilter und kürzere Kachelnamen (Branch `claude/odi-and-titles`)
- **Befund auf dem S25 (Build 1021):** Nacht-Insights zeigen jetzt echte Werte (22 331 Messwerte, Abdeckung 85,4 %, T90 1,5 min), aber ODI3/ODI4 = 10,9/5,6 je Stunde – für Handgelenk-PPG sehr wahrscheinlich durch Einzelmesswert-Artefakte aufgebläht (die Abtastung ist dicht, ~1 Wert/1,3 s). Behandlung: ein Ereignis zählt nur, wenn der Abfall ≥ N Punkte **mindestens 10 s** anhält; Einzelwert-Ausreißer zählen nicht. Weiterhin nicht klinisch validiert, violetter Rand bleibt. Tests ergänzt (Einzelwert-Dips, 8 s vs. 12 s).
- Kacheln: kürzere sichtbare Namen („Intensität · moderat/hoch“, „Kalorien gesamt/aktiv“, „SpO₂“) gegen Umbrüche mitten im Wort bzw. Auslassungspunkte; Barrierefreiheitsname bleibt der volle Titel.

## 2026-11-18 – Oberflächenkontrolle auf dem S25 (Build 1025), Texte (Branch `claude/dialog-texts`)
- Geprüft auf dem Gerät: Übersicht (Kopf, Profilkarte, Legende, SpO₂-/Pulskachel, Nacht-Insights), Info-Dialoge (ⓘ Körperprofil, Herzfrequenz, SpO₂). Dann ging die USB-Verbindung verloren und das Telefon wurde anderweitig benutzt (Kamera im Vordergrund); Verlauf/Konfig/weitere Info-Dialoge konnten in diesem Durchgang **nicht** erneut geprüft werden (Konfig zuvor mit Build 1005 gesehen).
- Gefunden und behoben: (1) „Deine Messdaten“-Dialog und die HCI-Snoop-Anleitung zeigten literal `\n` statt Zeilenumbrüchen (doppelt maskiert in `MainActivity`). (2) SpO₂-Info-Dialog redundant (zwei Quellenzeilen, zwei Validierungshinweise) → zusammengeführt.
- Nicht angefasst: `GarminFileListProbe` enthält dasselbe `\\n`-Muster in Diagnosetext (BLE-Bereich, OpenCode).

## 2026-11-18 – #6 Testinventur Teil 1 (Branch `claude/test-inventory`)
- `docs/TEST_INVENTORY_2026-11-18.md`: statischer Abgleich Produktionsdateien ↔ Tests (13 von 169 ohne Testbezug) mit Zuordnung und Folgen. Keine Zeilenabdeckung.
- Neue Tests: `MainNavigationViewTest` (5 Tabs, Auswahl, Wechsel), `PersonalSettingsDialogTest` (Versionszeile, zweistufige Löschbestätigung, Abbruch behält Profil; per UiAutomation, nur Emulator), `NightOxygenWindowTest` (Nachtfenster inkl. Sommerzeit).
- Geräte-Hinweis: Auf dem S25 liefen währenddessen Testlabor-Sitzungen anderer Agenten (Build 1034, Testlabor im Vordergrund); ich habe **nicht** installiert oder getippt, um diese nicht zu stören.

## 2026-11-18 – GUI-Korrekturen aus dem Qualitätstest (Branch `claude/gui-fixes`; Issues #99–#106)
Ohne Gerätezugriff umgesetzt (Handy gesperrt); Wirkung **nicht** auf dem S25 gesehen, nur CI/Emulator.
- #99: Verlauf-Y-Achse passt sich den Daten an (`AxisScale`, 6 Unit-Tests), Caption in zwei Zeilen („Skala a–b Einheit“ in eigener Zeile → keine Waise).
- #100: Profilkarte kompakt bei < 340 dp Breite oder Schrift ≥ 1,5 (Label über Wert, kleinere Figur); Navigationslabels einzeilig mit Auto-Verkleinerung 8–11 sp; Speichern-Button nur Text.
- #101: Kacheln einer Zeile gleich hoch (MATCH_PARENT), Titel „Intensität moderat/hoch“, reservierter Titelrand 28 → 18 dp.
- #102 (teilweise): Dialoge im App-Design (Theme `Theme.PulsLibre`, abgerundeter Kartenhintergrund, Akzentfarbe, Buttons ohne Versalien). Offen: Diagramm im Tagesdialog nach oben, Y-Achsenwerte, abgeschnittener Button.
- #103 (teilweise): Nacht-Insights Titel 11 sp / Wert 12 sp zentriert. Offen: ODI als eigene Kennzahl, Leerraum.
- #104 (teilweise): Info-Symbol Profilkarte 48 dp, Legendenchips 11 sp, Navigation 11 sp. Offen: Eingabefelder 45 → 48 dp.
- #105 (teilweise): Caption. Offen: gleichmäßige X-Ticks, Datumsnavigation.
- #106 offen.

## 2026-11-18 – GUI-Korrekturen Teil 2 (Branch `claude/gui-fixes-2`)
Ohne Gerätezugriff; nur CI/Emulator-Nachweis.
- #102: Tagesdialog neu geordnet (Datum, Diagramm, gewählter Wert, „‹ Vorheriger | Nächster ›“ nebeneinander, Statistik zuletzt und gedämpft); Wertachse mit Min/Mitte/Max links vom Diagramm (`DayChart.left`), Touch-Test an die neue Geometrie angepasst (`xAt`).
- #103: Nacht-Insights mit vier Spalten (22–6 Uhr · Abdeckung · T90 · ODI3/ODI4), ⏳ nur noch im Ladezustand.
- #104: Eingabefelder der Config mindestens 48 dp.
- #105: Verlauf-X-Beschriftung einheitlich zentriert (rechter Rand 18 dp), Caption bereits zweizeilig. **Datumsnavigation nicht umgesetzt** (eigenes Feature-Issue).
- #106: Empfangspunkt „alt“ nicht mehr rot, sondern gedämpftes Grau-Violett.

## 2026-11-18 – Gerätekontrolle der GUI-Korrekturen (Build 1046) und Teil 3 (Branch `claude/gui-fixes-3`)
Am S25 geprüft (Build 1046): Übersicht – gleiche Kachelhöhen, Titel „Intensität moderat/hoch“, „Kalorien gesamt“ einzeilig, Nacht-Insights vierspaltig, Empfangspunkte grün (Live-Daten), Kopfring grün. Verlauf – SpO₂-Skala 80–100 % lesbar, X-Beschriftung gleichmäßig, Caption zweizeilig. Tagesdialog – App-Design, Diagramm oben, Wertachse (66/103/140), Vorheriger/Nächster nebeneinander. 300 dp + Schrift 1,3: Profilkarte gestapelt und lesbar, Navigation einzeilig, Config ohne abgeschnittenen Spinner.
Verbleibend gefunden und hier behoben: Config-Eingabefelder in einer Zeile waren bei umbrochenen Labels versetzt (Labels jetzt zweizeilig reserviert bei schmaler Breite/großer Schrift); Navigationslabels uneinheitliche Größe (max. 10 sp statt 11 sp).
Noch sichtbar offen (P3): Nacht-Insights erste Spalte „22–6 Uhr / 22331 / Werte“ dreizeilig; Kacheln mit viel Leerraum bei gleicher Zeilenhöhe; Datum-/Navigationsbuttons im Tagesdialog noch Standard-Grau.

## 2026-11-18 – #117 Geburtsdatum, #118 Gewicht-Schnelleingabe und BMI (Branch `claude/profile-bmi`)
- `AgeCalculator`, `Bmi` (reine Logik, `ProfileMathTest`), Einstellungen mit Datumsauswahl (kein Zukunftsdatum, Altwerte bleiben erhalten), Profilkarte mit abgeleitetem Alter, BMI-Zeile (Wert + Kategorietext + Farbe, Tippen = Erklärung), Waagen-Button (48 dp) → Gewichtsdialog (Prefill, Plausibilität 15–400 kg, Abbrechen ändert nichts). Dokumentation: `docs/science/BMI_CLASSIFICATION.md`.
- Tests: `AgeCalculatorTest`, `BmiTest`, Instrumentation `BodyProfileCardTest`. Nicht am Gerät betrachtet (CI/Emulator).

## 2026-11-18 – Code-Review-Befunde #112–#115 (Branch `claude/service-fixes`)
- #112: `POST_NOTIFICATIONS` im Manifest; beim Start der Aufzeichnung (Android 13+) wird die Berechtigung angefragt; bei Ablehnung Hinweis „Beenden über Stoppen in der App“. Aufzeichnung startet unabhängig vom Ergebnis. **Am Gerät nicht geprüft.**
- #113: `RecorderStatus` – „läuft“ = Flag **und** frischer Herzschlag (Dienst schreibt alle 20 s `heartbeat_ms`, veraltet nach 65 s); UI (Gerätetab, Kopfstatus) und Export-Recorderstopp nutzen es. Test `RecorderStatusTest`.
- #114: gemeinsame `CirqaDatabase`-Instanz für den 1-s-Poll der Übersicht, Legacy-Bereinigung in `onOpen` unverändert (ein `stat()`; läuft dank gemeinsamer Instanz nicht mehr jede Sekunde). Weitere Aufrufstellen (einmalige Lesevorgänge) unverändert.
- #115: material-Abhängigkeit und viewBinding entfernt, ungenutzte Ressource gelöscht, Schreibweise „PulsLibre“ in Label/Benachrichtigung/Dialogen/Diagnose, `dataExtractionRules`/`fullBackupContent` (alles ausgeschlossen), Pfade in `BodyProfileAvatarView` nicht mehr pro Frame allokiert. Offen: `ObsoleteSdkInt` in `CirqaMlHandshakeProbe` (BLE, OpenCode), `!!`/stille catch-Blöcke.

## 2026-11-18 – #110 Datumsnavigation im Verlauf und P3-Restpunkte (Branch `claude/history-nav`)
- #110: Verlauf hat Fensternavigation ‹ Titel › für 24 h (bis 29 Fenster zurück) und 7 d (bis 3); begrenzt auf den lokal gecachten 30-Tage-Horizont (`HistoryRange.maxOffset`), 30 d ohne Navigation. Das Fenster liegt immer in Vielfachen der Fensterlänge vor „jetzt“ (kein freier Datumswähler). Cache-Synchronisation bleibt an der echten Zeit, nur das Fensterende wandert. Tests: `HistoryRangeWindowTest`, Instrumentation `windowNavigationStepsBack…`.
- Nacht-Insights: erste Spalte kürzer („22–6 Uhr“ / „22.331“), Beschreibung bleibt vollständig. Tagesdialog: Datum-/Navigationsbuttons im App-Stil.

## 2026-11-18 – #122 Schrittbalken im Hintergrund der Schrittekachel (Branch `claude/history-nav`, mit #110)
- `StepIntervals` (Differenzen des kumulativen Zählers, 15-Minuten-Intervalle, Lücken statt Nullen, Reset-/Duplikat-/Sommerzeit-Behandlung), `StepBarsView` (dezente Säulen hinter dem Text, geclippt), Kachel lädt im Hintergrund (60 s Cache). Doku `docs/science/STEP_INTERVAL_BARS.md`. Tests `StepIntervalsTest` (8). **Am Gerät nicht betrachtet.**

## 2026-11-18 – #124 Premium-Redesign (Branch `claude/premium-redesign`)
- Umgebung: eigener Emulator (`~/Android/Sdk`, AVD `s25`, API 35, 1080×2340) – adb-Aufrufe nur mit `-s emulator-5554`, echtes Handy unberührt. Synthetische Daten per Skript eingespielt. Details und Grenzen: `docs/test-evidence/REDESIGN_2026-11-18.md`.

## 2026-11-18 – Rückmeldungen zum Redesign (Branch `claude/avatar-v2`)
1. Avatar neu gezeichnet (Gesicht mit Brauen/Augenlichtern/Nase/Wangen, Haare, Kapuzenpullover, Dämmerungs-Berge); 2. Waagen-Button unter der Karte entfernt – Tippen auf die Gewichtszeile öffnet die Schnelleingabe mit − / + in 0,1-kg-Schritten (`WeightStepper`, Halten wiederholt); 3. Hinweis zum violetten Rand von der Übersicht in die Konfiguration verschoben; 4. Entwicklerangabe „Jochen Kunkel“ in der Konfiguration; 5. Reiter „Geräte“ → „Gerät“, Überschrift „Gerät · CIRQA“.
- Emulator-Hinweis: Der lokale Software-GPU-Emulator ist sehr langsam (wiederholt „System UI isn't responding“); er läuft nur als Hintergrundaufgabe stabil. Visuelle Prüfung dort eingeschränkt.

## 2026-11-18 – Kachel-Rahmen, Kennzahlen, Schritte, Gerät (Branch `claude/tiles-v2`)
- Empfangspunkte entfernt: dünner Rahmen in der Farbe des Empfangsalters (grün/gelb/orange, sonst neutral); unvalidierte Kacheln zusätzlich ein innerer violetter Rahmen. Info nur noch als stilles ⓘ.
- `TileInsights` (reine Logik, 8 Tests) + `SparkView`: Kacheln zeigen „Heute Ø · min–max“, 24-h-Sparkline und **Wochenschnitt zur gleichen Uhrzeit** (Ø der letzten 7 Tage ±30 min, Mindestens 3 Tage; bei Zählern wie Kalorien/Intensität der Zählerstand zu dieser Uhrzeit) mit Abweichung ▲▼. SpO₂ zusätzlich Anteilsband (≥95 / 90–94 / <90 %).
- Schritte: auf dem Handy (Build 1083) liegen nur ~170 Schrittereignisse/Tag in kurzen Sitzungen; die meisten Schritte fallen in Pausen > 10 min → es gab fast keine gemessenen Balken. Jetzt zusätzlich blasse **Umrissbalken** für Pausen bis 12 h („ungefähr, Zeitpunkt unbekannt“), nie als gemessen gezählt; gemessene Balken kräftiger (≈ 33 %).
- Reiter Gerät: `DeviceOverviewView` – Statuskarte mit Ring, Fakten-Chips (Empfang 1 Std., Speicher, Sitzungen heute, seit wann), letzter Empfang je Messgröße mit Farbpille, Bandinformationen (Akku, Firmware, Uhrzeit, Modell) als „noch nicht gelesen“ (#126). Nur echte lokale Daten.

## 2026-11-18 – #79 Abschluss Phase A (Branch `claude/history-mean`)
- Mittelwert im Verlauf-Diagramm (Ring neben Median-Punkt), `BucketStats` mit Tests, Messung und Query-Plan in `docs/HISTORY_PERFORMANCE_2026-11-18.md`; Rollup-Tabellen bewusst zurückgestellt (kein nachgewiesener Nutzen, Schemaabstimmung nötig).

## 2026-11-18 – #110 Datumswahl im Verlauf (Branch `claude/history-datepick`)
- 📅-Knopf neben ‹ › öffnet die Datumsauswahl (nur die letzten 30 Tage = Cache-Horizont); 24 h springt auf den Tag, 7 d auf die Woche (`HistoryRange.offsetForDate`, geklemmt). Fenster bleiben Vielfache der Fensterlänge vor „jetzt“, keine Kalender-Wochen. Wischgeste nicht umgesetzt.

## 2026-11-18 – #145 Profilkarte rechts, Basecap (Branch `claude/profile-cap`)
- Werteblock mit festen Spalten (Symbol 22 dp | Label 58 dp | Wert) und 30 dp Abstand zum Avatar (≈ 16 dp weiter rechts), 3 dp Zeilenabstand, BMI-Kategorie klein darunter bündig zur Wertspalte; kompakt/groß: Label über Wert.
- Avatar mit Basecap (gewölbte Krone, Nähte, Knopf, nach vorn zeigender Schirm mit Schatten, Haare seitlich), Halsschatten entfernt (wirkte wie Bart). Beschreibung „Person mit Basecap und violettem Hoodie“.
- Im Emulator (API 35, `-gpu guest` läuft stabil, swiftshader nicht) mit synthetischen Daten angesehen: 1080×2340/Schrift 1,0 und 960×2080/Schrift 1,3. Beim zweiten Fall wurde „· Offline“ im Claim abgeschnitten → bei großer Schrift/schmal jetzt eigene Zeile.

## 2026-11-18 – Abschlussrunde #124/#122/#41 (Branch `claude/final-pass`)
- Am Emulator (1080×2340, `-gpu guest`, synthetische Daten) geprüft: Header/Profil, Kachel-Rahmen, Kennzahlen, Gerät-Reiter, Schrittekachel.
- **Fehler gefunden:** `StepBarsView` zeichnete schwarz auf dunklem Grund (Konstruktorparameter `color` wurde im `Paint.apply` vom Paint-Attribut überdeckt) → Balken waren praktisch unsichtbar. Das erklärt „ich sehe keine vertikalen Balken“. Behoben (`barColor`).
- Kachel-Kennzahlen: Einheit nur einmal, „Heute 61 bpm (1 Wert)“ statt „61–61“, Zähler ohne doppelte Zeile, Sparkline nur ab 2 Stunden mit Daten, kürzere Wochenschnitt-Zeile, Herzfrequenz-Kachel ebenfalls mit Kennzahlen.
- #41 Drilldown: Tippen auf die Trendzeilen Ruhepuls/Schritte öffnet `DailyBarsView` (90 Tageswerte, Lücken leer, gestrichelte Mediane 7/28 T).

## 2026-11-18 – #6 Teil 2 (Branch `claude/audit-6b`)
- `docs/AUDIT_TEIL2_2026-11-18.md`: JaCoCo-Unit-Abdeckung (Zeilen 31,1 %, untere Schranke ohne Emulator-Tests), Manifest/Gradle/Workflow-Prüfung, MASVS-Kurzabgleich. Wichtigster Befund: die verteilte „Update-APK“ ist ein **debuggbarer Debug-Build** (run-as konnte die Datenbank lesen).
- `enableUnitTestCoverage = true` im Debug-Build (JaCoCo-Bericht lokal mit `:app:createDebugUnitTestCoverageReport`).

## 2026-11-18 – Stabile Dialog-Tests (Branch `claude/test-probe`)
- Die UiAutomation-basierten Dialogtests (`PersonalSettingsDialogTest`, `BodyProfileCardTest`) fielen auf frisch gebooteten CI-Emulatoren sporadisch aus (aktives Fenster nicht der Dialog), lokal liefen sie. Umstellung auf `DialogProbe` (zuletzt gezeigter Dialog) + `DialogTestSupport`: Dialoginhalt und Buttons werden direkt geprüft, ohne UiAutomation. Zusätzlich prüft der Gewichtstest jetzt, dass „+“ den Editor auf 80,1 setzt, aber nichts speichert.
- Lokal: 49/49 Instrumentationstests im Emulator grün (5,5 min).

## 2026-11-18 · #157 Designsystem-Icons
- 32 SVGs → VectorDrawables (`tools/svg_icons_to_vector.py`), `PlIcon`-Helper, Theme-Farben hell/dunkel.
- Integriert: Navigation, Gerät (CIRQA-Band statt Uhr, Metadaten, Empfang), Trends, Info-Buttons, Starten/Stoppen.
- Geprüft: Build + Lint lokal grün, Emulator (synthetisch, dunkel) Gerät-Tab. Offen: Hellmodus-/Großschrift-Screenshots, TalkBack, S25.

## 2026-11-18 · Absturz beim Öffnen der Konfig (Fund am S25)
- Ursache (logcat S25): `ClassCastException Integer→String` in `PersonalSettings.newField` (`getString`), weil SpO₂-Schwellen als Int im gleichen Prefs-Store liegen. Tritt auf, sobald die SpO₂-Skala einmal gespeichert wurde.
- Fix: Wert über `prefs.all[key]?.toString()` lesen; Regressionstest `opensWithIntegerStoredSpo2ThresholdsWithoutCrashing`.

## 2026-11-18 · Profilkarte nach Speichern über Header-Button
- Fund am S25: Größe/Gewicht in der Konfig gespeichert (Prefs enthalten 196 / 99.2), Übersicht zeigte „–“.
- Ursache: Der Config-Button im Header übergab beim Speichern keinen Refresh der Profilkarte (nur der Tap auf die Karte tat es).
- Fix: `refreshBodyCardRef` wird auch aus dem Header-Callback aufgerufen; Regressionstest `savingViaHeaderSettingsButtonRefreshesProfileCard`.

## 2026-11-18 · Config-Menü: neue Icons und Reihenfolge
- Überschriften nutzen jetzt die PulsLibre-Icons (Profil=Identität, Körper & Ziele=Fitness, Sonstiges=CIRQA-Band, Darstellung=Übersicht, Aktivität=Schritte, SpO₂=Lunge, Löschen=Speicher); Emojis in Version, Entwickler, Info-Hinweis, Geburtsdatum, SpO₂-Feldern entfernt.
- Reihenfolge: Profil (Name, Geschlecht, Geburtsdatum) → Körper & Ziele (Größe, Gewicht, Ziele, Ruhepuls) → Sonstiges → Darstellung → Aktivität → SpO₂-Grenzwerte → Version/Entwickler/Hinweis → Profil löschen.
- Nicht geprüft: Hellmodus/große Schrift der neuen Anordnung am Gerät.

## 2026-11-19 · #164 Header-Bergpanorama Eiger–Mönch–Jungfrau
- `HeaderBackdropDrawable` neu: Punktdaten aus der eigenen SVG-Studie (`HeaderMountainData`), gleichmäßige Skalierung ohne Verzerrung, Trio horizontal zentriert, Kanten deterministisch aufgeraut, Felsrippen, Schattenrinnen, dezente Lavendel-Schneekappen, zwei Vorberg-Ebenen, Abendlicht-Sonne hinter der Jungfrau-Seite. Farbwelt (Pflaume/Violett, Rosé-Saum) beibehalten.
- Emulator geprüft: 320/360/393/430 dp, dunkel/hell, Schrift 200 %, Querformat. Nicht geprüft: S25 (ohne Freigabe), Scroll-Performance gemessen, Foto-Vergleich (Foto nicht verfügbar).
## 2026-11-18 · Config: Aufzeichnung beim App-Start (Autostart)
- Neuer Schalter „Beim App-Start automatisch aufzeichnen“ (Standard **aus**), eigener Prefs-Store `recording_settings` (wird von „Profil löschen“ nicht berührt).
- `AutoStartRecording.shouldStart(...)`: nur bei frischem Start (nicht bei Rotation), Berechtigungen schon erteilt, Bluetooth schon an, Dienst nicht bereits aktiv. App schaltet Bluetooth nie selbst ein und fragt hier keine Berechtigungen ab.
- Der Aufzeichnungsdienst selbst (OpenCode) wurde nicht verändert; es wird nur der bestehende START-Intent ausgelöst.
- Tests: Unit `AutoStartRecordingTest` (6 Fälle), Instrumentation `autoStartSwitchDefaultsToOffAndIsPersistedOnSave`. Nicht getestet: realer Start am S25 mit Band.

## 2026-11-19 · Header-Untertitel brach/kürzte bei 320 dp und Schrift 200 %
- Ursache: Untertitel-Zeile mit `wrap_content`-Breite (+ 60 dp Einzug) → bei Umbruch wurde nur die erste Zeile gemessen („Deine Dat…“).
- Fix: volle Breite (`MATCH_PARENT`), bei Umbruch ohne Einzug. Emulator: 320 dp, 320 dp + Schrift 200 %, 360 dp + 200 %, 360 dp normal – alle vollständig lesbar (Bild `untertitel_nachher_320dp_font2.png`).

## 2026-11-19 – Review-Punkte behoben
- Profil löschen erhält SpO₂-Schwellen/Gate (#169), Store-Konstante, toter Avatar-Code entfernt, Hinweis bei fehlgeschlagenem Autostart.
- Tests: lokal Unit/Lint/androidTest-Kompilierung grün; `PersonalSettingsDialogTest` auf Emulator grün. GitHub-CI weiterhin wegen Billing nicht startbar.
- Emoji→PlIcon für CIRQA-Überschrift, Verlauf-Datumsknopf, Daten-Tab-Datenschutzzeile (Teil von #168). Unit/Lint/androidTest-Compile lokal grün, Emulator-Sichtprüfung. Profilkarten-Icons brauchen neue Entwürfe.

## 2026-11-19 – Profilkarte mit Designer-Icons (#168)
- Neue Icons profile/height/weight/age/bmi (vom Designer) in `PlIcon` aufgenommen; Profilkarte nutzt sie statt 👤 ↕ ⚖ ◷ ▣. Unit/Lint/androidTest-Compile lokal grün, Emulator-Sichtprüfung ok. Offen in #168: Info-Dialogzeilen, Statusglyphen (○ ◌ ◉ ↧).

## 2026-11-19 – Restglyphen auf Icons (#168 abgeschlossen)
- Nacht-Indikatoren (SpO₂, 22–6 Uhr, Abdeckung, T90, ODI) und Lade-Sanduhr nutzen `PlIcon`; Aufzeichnungsstatus (aus/startet/beendet/aktiv) zeigt RECORD_STOP/HOURGLASS/RECORD_START. Info-Dialogzeilen verwenden bereits gezeichnete `DashboardInfoIconView`-Icons (Review-Fund dazu war ein Fehlalarm: die Glyphen sind nur interne Schlüssel).
- Unit/Lint/androidTest-Compile lokal grün; Emulator: Gerätereiter geprüft. Nacht-Indikatoren auf dem Emulator nicht sichtbar (keine SpO₂-Nachtdaten) – nur per Code/CI geprüft.

## 2026-11-19 – Rote main-CI nach #182 repariert
- Commit eddeff3 (#182, History-Legende → Beschriftung je Diagramm) ließ zwei Emulator-Tests scheitern (`DashboardTest.historyTabIsPresentAndOpensLocalTimeSeries`, `HistoryOverviewViewTest.compactLegend…`), CI-Lauf 44481751539. Tests an die neue Beschriftung (`history_chart_legend`, „Zeitachse: Telefonzeit …“) angepasst; `HistoryOverviewViewTest` lokal auf Emulator grün.

## 2026-11-19 – main-CI rot durch AGP 9.5.1 (Dependabot #188) – Revert
- Nach Merge von Dependabot-PR #188 (AGP 8.7.3 → 9.5.1) scheitert der Build: „Minimum supported Gradle version is 9.6.0. Current version is 8.13“ (Lauf 44485578810). AGP 9 braucht Gradle 9.6 und weitere Migrationen (u. a. eingebautes Kotlin). Revert auf AGP 8.7.3, damit main baubar bleibt; der AGP-9-Sprung gehört in ein eigenes Migrationsticket (#155).

## 2026-11-19 – main baubar gemacht (Icon-Commits anderer Sessions)
- Nach dem AGP-Revert (PR #190, versehentlich trotz rotem Lauf 44485944752 gemergt – Skriptfehler: Merge nicht an Grün-Prüfung gekoppelt) zeigte der Lauf zwei weitere Fehler aus Commit 47042de „centralize metric detail icon semantics“: `iconFor` war public mit internem Rückgabetyp `PlIcon` (Kotlin-Fehler), und `DashboardInfoIconView`/`DashboardMetricIconView` erbten von `ImageView` (Lint-Fehler AppCompatCustomView). Behoben (`internal`, `AppCompatImageView`). Lokal Unit/Lint/androidTest-Compile grün.
- Drei Emulator-Tests an geänderte UI angepasst (Statusbutton ist seit #179 ein FrameLayout, Schritt-Chip nutzt PlIcon): `DashboardTest`, `BodyProfileCardTest`, `DashboardOverviewViewTest`. Nachweis über GitHub-CI (lokaler Emulator wegen Host-RAM nicht nutzbar; Tests laufen nie auf dem S25, weil sie Profildaten verändern).

## 2026-11-19 – StepBarsView-Lint (Kotlin-2.4-Vorbereitung)
- `[Range]`-Lintfehler in `StepBarsView.kt` (alpha in `Paint.apply` innerhalb einer View-Unterklasse → View.alpha) durch `also { it.alpha = … }` behoben; lint grün auf `main`-Basis und auf der Kotlin-2.4.21-Basis (`add8728`). Abstimmung mit OpenCode über `docs/collaboration/agent-sync/`.

## 2026-11-19 – Toolchain-Migration (#155): Gradle 9.6.0, AGP 9.5.1, Kotlin 2.4.21, compileSdk 37, AndroidX
- **Änderungen:** Gradle 8.13 → 9.6.0 (CI: `setup-gradle gradle-version`), AGP 8.7.3 → 9.5.1 (eingebautes Kotlin: Plugin `org.jetbrains.kotlin.android` entfällt; Kotlin-Version 2.4.21 über `buildscript`-Classpath erzwungen, sonst nutzt AGP seine gebündelte 2.2.10), `compileSdk` 35 → 37 (`targetSdk` bleibt 35), `core-ktx` 1.15.0 → 1.19.1, `appcompat` 1.7.0 → 1.8.0. Basis: OpenCodes `add8728` (compilerOptions-DSL, `arrayOf<Any?>` für SQL-Argumente); zusätzlich `arrayOf<Any?>` in `CirqaStorageMigrationTest` (Kotlin-2.4-Fehler „intersection as reified“).
- **Lokal grün (JDK 17, Gradle 9.6.0, SDK 37):** `testDebugUnitTest`, `assembleDebug`, `createDebugUnitTestCoverageReport`, `lintDebug`, `assembleDebugAndroidTest`. Emulator-Lauf lokal nicht möglich (Host-RAM) → Nachweis über GitHub-CI.
- **Lokale Werkzeuge** liegen jetzt unter `/home/smiler/pulselibre-tools` (Gradle 9.6.0, SDK inkl. `platforms;android-37.0`, Gradle-Home) statt im RAM-gestützten `/tmp`.

## 2026-11-19 – #179 fertig, #208, #209, #172 (Teilumsetzung)
- GUI-Abnahme am S25 (siehe Ticketkommentare) ergab: Statusleiste in Hell unleserlich (#208), widersprüchliche Statustexte im Gerätereiter (#209), Testlabor weiter sichtbarer Reiter (#172).
- Umgesetzt: Theme-abhängige Systemleisten-Icons; Untertitel ohne „· Offline“; Header-Kollaps auf allen Reitern mit Überstand-Schwelle (kein Flackern); technische/veraltete Texte des Gerätereiters in den versteckten Debugbereich; Reiter „Testlabor“ aus der Navigation entfernt, Einstieg per 3-s-Long-Press auf „Gerät · CIRQA“ + TalkBack-Aktion; inline-Debug-Toggle entfällt. Tests: `HeaderAndDeviceCleanupTest` (neu), Navigationstests auf vier sichtbare Reiter angepasst. Lokal Unit/Lint/androidTest-Compile grün; Emulator-Läufe über CI.
- Nicht umgesetzt (bleibt in #172): Bottom Sheet, fünf Diagnosebereiche, Opt-in-Trace, redigierter Export, Entfernen des toten Debug-Codes.

## 2026-11-19 – GUI-Abnahme S25 und Abschluss #179/#208/#209
- Build 1212 (CI-signiert, Lauf 44520571327) am S25: großer/kompakter Header auf Übersicht und Gerät, Hell-Modus-Statusleiste lesbar, Gerätereiter ohne widersprüchliche Texte, Testlabor per 3-s-Long-Press (bei 1,5 s nichts, bei 3,4 s öffnet). Autostart nach frischem Start: Aufzeichnung läuft nach 25 s; nach Paketersatz startete sie beim ersten Start nicht (veraltetes `running`-Flag, #212).
- Geschlossen: #179, #208, #209, #182; auf Anweisung #63, #126. Offen mit Nacharbeit: #159, #172, #204, #212.

## 2026-11-19 – #212 Autostart nach Update/Kill
- Ursache: `RecorderStatus.isRunning` (Flag + Heartbeat < 65 s) blieb nach dem Force-Stop durch das App-Update „wahr“ und blockierte den Autostart. Fix: Der Autostart verlangt zusätzlich, dass der Dienst tatsächlich lebt (`AutoStartRecording.isServiceAlive` über `ActivityManager.getRunningServices`, nur eigene Dienste). Unit-Tests für beide Fälle; Lint/androidTest-Compile lokal grün; Hardwarenachweis nach Merge am S25 (Update installieren, frisch starten, Aufzeichnung muss ohne Zweitstart laufen).
- Bewusst nicht umgesetzt: automatischer Neustart per `MY_PACKAGE_REPLACED` (Foreground-Service-Start aus dem Hintergrund unterliegt Android-Einschränkungen; Nutzerentscheidung nötig).

## 2026-11-19 – #204 Phase B (GUI): Hinweis und Liste unbekannter FIT-Felder
- Geräteseite zeigt bei gespeicherten Einträgen in `fit_unmapped_fields` (Phase A, OpenCode) einen dezenten Hinweis „Unbekannte Datenfelder entdeckt (N Typen)“ (Icon DATA); Tippen öffnet eine nur lesende Liste mit FIT-Koordinaten, Vorkommen (Meldungen/Dateien), Klassifikation und Status. Aggregation in `UnknownFieldsSummary` (Unit-Tests), Anzeige in `UnknownFieldsView` (Instrumentationstest). Lesen über `CirqaDatabase.storedFitUnmappedFields()` auf einem Worker-Thread beim Öffnen des Gerätereiters.
- Bewusst nicht umgesetzt: Hex-Vorschau/Export (Phase B Nr. 8, braucht explizite Nutzeraktion + Backend), Statusänderung in der Oberfläche, Phase C. Keine Rohwerte, keine medizinischen Bezeichnungen.

## 2026-11-19 – #172 Stufe 1: Diagnose-Sheet statt direktem Testlabor
- Der 3-s-Long-Press auf „Gerät · CIRQA“ öffnet jetzt ein **nur lesendes** Diagnose-Sheet mit den fünf Bereichen A App, B Bluetooth (Status/Berechtigungen, kein Scan/Connect), C Kommunikationsaufzeichnung (ehrlicher Platzhalter), D Synchronisation (Dateien, Tabellenzähler), E Diagnosepaket (Vorschau, dann bewusst teilen; ohne MAC/Seriennummern/Messwerte). Eintrag „Erweitertes Testlabor“ führt zur bisherigen Activity (nichts gelöscht).
- Dateien: `DiagnosticsSheet`, `DiagnosticsCollector` (kein BLE, Tabellen-Whitelist, `PRAGMA quick_check`), `DiagnosticsFormat` (Unit-getestet inkl. Redaktionsmuster); Instrumentationstest `DiagnosticsSheetTest`.
- Offen in #172: Trace-Recorder (C, Opt-in, begrenzt, Redaktion – Schnittstelle von OpenCode angefragt), Entfernen des unerreichbaren Inline-Debugcodes und der bisherigen Experimente-Oberfläche, Hex-Vorschau.

## 2026-11-19 – #45 Sensitivitätsanalyse (Ausreißer/Einzeltage)
- `PairedCorrelation.sensitivity`: Spearman-ρ bei Weglassen jedes einzelnen Tages (Bereich) und ohne die 5 % (mind. 1) rangmäßig extremsten Tage; „stabil“ nur bei gleichem Vorzeichen überall und Abweichung < 0,1 von ρ der Gesamtstichprobe; unter 30 Paaren oder konstanter Reihe: keine Aussage. Im Streudiagramm-Drilldown (Schritte → Ruhepuls am Folgetag) steht eine Zeile „Robustheit: …“. Tests: Mindestzahl, stabil bei starkem Zusammenhang, instabil bei deterministisch konstruiertem schwachem Zusammenhang, konstante Reihe. Hinweis: „stabil“ ist keine Signifikaussage (Verdikt/Intervall bleiben getrennt).
## 2026-11-19 – #159 Akku-Anzeige (datengetrieben, ehrlich)
- Gerätereiter „Bandinformationen“: Die Zeile „Akku“ zeigt, **sofern gespeichert**, Prozent, Füllbalken (Füllung exakt = Prozent, weiche Farbstufen rot/amber/grün in Hell und Dunkel, nie einziger Hinweis) und Frische („Stand: vor …“ bzw. „Veraltet · letzter bekannter Stand …“ nach > 6 h, „Zeitpunkt unbekannt“ ohne Zeitstempel). Quelle: neuester `fit_device_metadata`-Eintrag mit `battery_level_percent` (nur lesend, Worker-Thread). Ohne Daten bleibt „Nicht verfügbar“. Keine harte 37 %, keine Restlaufzeit, Ladezustand nur bei belegtem FIT-Code 6 („lädt“), sonst keine Aussage.
- Tests: `DeviceBatteryFormatTest` (Frische, Veraltung, Ladecode, Klemmen, Farben), `DeviceBatteryViewTest` (ohne/mit/veraltet). Offen in #159: echte Daten (FIT enthält laut OpenCode keine brauchbaren Batteriewerte; Live-Statusabfrage `GDIDeviceStatus` ist BLE/OpenCode), Firmware/Uhrzeit/Modell, S25-Abnahme.

## 2026-11-19 – #41 Stufe 1: Aktivitäts-Atlas (Pulse Flow × Fitness Atlas)
- Trend-Karte der Verlaufsansicht erhält den **Aktivitäts-Atlas**: kalenderausgerichtetes Raster (Mo–So) der letzten 7/28/90 Tage (Default 28) aus den verifizierten Tages-Schrittständen. Neutrale Skalierung (Terzile der abgeschlossenen Tage; unter 3 Tagen eine einzige Klasse), graue Zelle = „keine verifizierten Tagesdaten“ (nie „0 Schritte“), heutiger Tag umrandet und aus der Skala herausgenommen (Zähler läuft noch), Zahl in der Zelle (gerundete Tausender) als zweites Merkmal neben Farbe, Tippen öffnet Tagesdetail, Zusammenfassung „x von y Tagen“. Persönliches Ziel wird bewusst **nicht** für Historie verwendet (nur für „heute“ bekannt).
- Dateien: `ActivityAtlas` (reine Logik, 9 Unit-Tests zu Spezifikationspunkten 7–9 u. a.), `ActivityAtlasView` (Canvas), Anbindung in `TrendCardView`. Instrumentationstest `ActivityAtlasViewTest`.
- Offen in #41 (nächste Stufen): Ruhepuls-Hero mit Delta „−x bpm gegenüber vorherigen N Tagen“ und Lücken ohne Linie, „Regelmäßigkeit“ (nur mit verifiziertem Ziel), getrennte Abdeckungsleiste, Asset-Kit `design/icons/fitness-v1` in VectorDrawables, Emulator-Screenshots mit synthetischen Daten, S25-Abnahme.

## 2026-11-19 – #41 Stufe 2: Ruhepuls-Hero und gemeinsames Zeitfenster
- Trend-Karte: ein gemeinsames Segment **7 T | 28 T | 90 T** (Default 28) steuert Ruhepuls-Hero und Aktivitäts-Atlas. Hero „Ruhepuls · Entwicklung“: Median des **Geräte-Ruhepulses** (nie Tagesminimum/Live-Puls), neutrales Delta „−4 bpm gegenüber vorherigen 28 Tagen“ (nicht überlappende Vorperiode, nur bei genügend Tagen: 7 T ≥ 4, 28 T ≥ 14, 90 T ≥ 45 – übernommen aus `TrendAnalysis`), Tagespunkte, Linie/Fläche nur zwischen aufeinanderfolgenden gültigen Tagen (keine Brücke über Lücken), gestrichelte Vorperioden-Baseline; zu wenige Tage → nur Tageswerte + Hinweis; keine Daten → „Noch keine verifizierten Ruhepulswerte“ ohne Kurve.
- Dateien: `RestingPulseHero` (Logik, 7 Unit-Tests zu Spezifikationspunkten 2, 4, 7 u. a.), `RestingPulseChartView` (Canvas), Umbau `TrendCardView`; Instrumentationstests ergänzt. Offen in #41: Regelmäßigkeit (nur mit verifiziertem Ziel), getrennte Abdeckungsleiste (Ruhepuls/Schritte), Asset-Kit → VectorDrawables, Emulator-Screenshots (synthetisch), S25-Abnahme.

## 2026-11-19 – #41 Stufe 3: getrennte Datenabdeckung
- Unter dem Atlas: Block „Datenabdeckung“ mit **getrennten** Zeilen „Ruhepuls: x von y Tagen“ und „Schritte: x von y Tagen“ (nur vollständige Tage vor heute, Fenster wie die Trends), dünnem Balken (Füllung = Anteil) und Hinweis „für eine Trendaussage fehlen Tage (mindestens N nötig)“ bei Unterschreitung (4/14/45). Folgt dem gemeinsamen Zeitfenster. Logik `DataCoverage` (Unit-Tests), Anzeige in `TrendCardView`, Instrumentationstest ergänzt. Offen in #41: Regelmäßigkeit (nur mit verifiziertem Ziel), Asset-Kit → VectorDrawables, Emulator-Screenshots (synthetisch), S25-Abnahme.

## 2026-11-19 – Nacharbeit aus der S25-Abnahme (Build 1292)
- #212 am S25 bestätigt und geschlossen. #204: 930 Typen als Einzelliste unübersichtlich → Gruppierung nach FIT-Nachrichtennummer, Gruppen einklappbar (Felder werden erst beim Aufklappen erzeugt), Test angepasst. #41: Aktivitäts-Atlas zeigte „heute“ als „–“, weil der Tagesleser nur vollständige Tage liefert → der Atlas liest zusätzlich den laufenden Tagesstand (`stepsWithToday`), die Zelle ist als „heute, noch nicht abgeschlossen“ umrandet; Trends/Abdeckung bleiben bei vollständigen Tagen.

## 2026-11-19 – #227 Hauptreiter „Analysen“ (Stufe 1)
- Neuer sichtbarer Reiter **Analysen** (Reihenfolge: Übersicht, Verlauf, Analysen, Gerät, Daten; Tab-Index 5 = `MainNavigationView.ANALYSEN`, Deep Link über `EXTRA_TAB`, Testlabor bleibt versteckt). Inhalt: fünf Gruppen (Herz & Kreislauf, Schlaf & Erholung, Bewegung & Entwicklung, Zusammenhänge & Muster, Datenqualität & Methoden) als Section-Header + Karten; Module ohne belegte Daten sind „Noch nicht verfügbar“ mit Grund und nicht anklickbar; verfügbare/teilweise: Trend-Block (Ruhepuls-Hero, Aktivitäts-Atlas, Korrelation, Abdeckung – **ein** `TrendCardView`, aus dem Verlauf hierher verschoben), Link „SpO₂ → Verlauf“, Dialog „Methoden und Grenzen“.
- Dateien: `AnalysisCatalog` (Gruppen/Module/Verfügbarkeit, Unit-getestet inkl. „keine Diagnose-/Score-Begriffe“), `AnalysenView`, Anpassungen `MainNavigationView`, `MainActivity`, `HistoryOverviewView`. Instrumentationstests `AnalysenTabTest` (Gruppen, Zustände, Methoden-Dialog, Link, Deep Link, Trend-Block nicht mehr im Verlauf).
- Offen in #227: Teaser auf der Übersicht, Such-/Filterfunktion, Zeitfenster pro Modul statt global, Emulator-Screenshots (Hell/Dunkel/große Schrift/320 dp), S25-Abnahme; weitere Module (#43/#44/#46/#47) bleiben „nicht verfügbar“, bis Quellen belegt sind.

## 2026-11-20 – #219 Verlauf „Verteilung“ (UI auf OpenCodes Datenmodell)
- Je Verlaufskarte (Herzfrequenz, SpO₂) Umschalter „Punkte | Verteilung“ (Default Punkte, bestehende Ansicht unverändert). „Verteilung“: Dichteband aus `aggregateHistoryDensity` (Bins ≈ Achsenspanne/20), grün selten → gelb → rot häufig mit wachsender Deckkraft, leere Zeitfenster transparent (keine Brücke/Interpolation), gemeinsame p95-Skala (aus dem Modell), Legende „relative Häufigkeit … nicht die Anzahl“, Tippen → Detail je Fenster (Stichprobe, Median, Mittelwert, Min/Max, häufigstes Intervall, außerhalb der Achse).
- Tests: `HistoryDensityDetailTest` (bimodal: Spitze unabhängig vom Mittelwert, Fenstergrenzen, Lücke, Außerhalb-Achse, Legende), `HistoryDensityViewTest`. Offen in #219: Emulator-Screenshots (synthetisch), Hell/Dunkel/große Schrift/TalkBack-Prüfung, Farbenfehlsichtigkeits-Alternative, S25-Abnahme, 90-Tage-Auflösung (Verlauf zeigt aktuell 24 h/7 T/30 T).

## 2026-11-20 – #172: unerreichbaren Inline-Debugcode aus `MainActivity` entfernt
- Seit PR #211 waren die 20 Debug-Buttons der Gerätesicht (BLE-Suche, GATT-/Handshake-/Bootstrap-/5050-/SYNC-Tests, Bewegungssensor-Probes, Sammlung start/stopp, Dateistatistik, Protokoll-Export u. a.) nicht mehr erreichbar. Entfernt: diese `debugView(actionButton(...))`-Blöcke samt jetzt ungenutzter Hilfen (`shareDiagnostics`, `showBondedDevices`, `exportCirqaLog`, `lastHistoryDiagnostic`, `cirqaGattProbe`, `cirqaMlProbe`, `bondedRows`). `MainActivity.kt` von ~1350 auf ~1090 Zeilen. **Nicht gelöscht:** die Probe-/Protokollklassen selbst (`CirqaGattProbe`, `CirqaMlHandshakeProbe`, …), ihre Unit-Tests, Protokolldokumentation und das „Erweiterte Testlabor“ (`CirqaTestLaboratoryActivity`); Produktfunktionen (Starten/Stoppen der Aufzeichnung, Autostart) sind unberührt. Der Code bleibt in der Git-Historie (PR #211 und früher) abrufbar.
- Unit/Lint/Instrumentation-Compile grün; Emulator-Lauf über die CI.

## 2026-11-20 – #177 Textfassung des Benutzerhandbuchs
- `docs/user/BENUTZERHANDBUCH.md`: vollständige Beschreibung aller Reiter und Einstellungen des aktuellen Stands (Übersicht, Verlauf inkl. Verteilung, Analysen inkl. Trend-Block/Atlas/Abdeckung, Gerät inkl. Akku-Zeile/unbekannte Felder/Testlabor-Einstieg, Daten, Einstellungen, Grenzen) – **ohne Screenshots**. Die bebilderte Fassung bleibt #177 (Testgerät, geschwärzte/synthetische Bilder, nichts Privates im Repo).

## 2026-11-20 – #227 Teaser auf der Übersicht
- Kompakte Karte „Ruhepuls-Trend ansehen“ unter den CIRQA-Kacheln der Übersicht, Tippen öffnet den Reiter Analysen (keine doppelte Berechnung). Instrumentationstest `overviewTeaserLinksToTheAnalysenTab`.

## 2026-11-20 – #219 Fund am S25: Modus „Verteilung“ sprang zurück
- Auf dem S25 (Build 1316) schaltete der Umschalter „Verteilung“ nicht dauerhaft um: Der Verlauf wird bei jedem neuen Messwert neu aufgebaut (Live-Polling), dabei ging der Modus verloren (im Emulatortest nicht aufgefallen, weil dort kein Live-Strom läuft). Fix: gewählter Modus je Karte bleibt im `HistoryOverviewView` erhalten; Test `densityModeSurvivesTheRerenderThatEveryNewReadingTriggers`.

## 2026-11-20 – Hell + Schrift 1,3× am S25: zwei Fehler behoben
- Kompakter Header (nach dem Scrollen) zeigte in **Hell** „PulsLibre“ und den Untertitel weiß auf dem hellen Seitenhintergrund (#179-Folgefehler, unleserlich) → Textfarben je nach Zustand/Theme (`palette.ink`/`muted` im kompakten Hell-Header). Test `compactHeaderTitleStaysReadableOnThePageBackground`.
- Gerät: „3 Wiederverbindungen“ brach bei Schrift 1,3× mitten im Wort („Wiede/rverbindungen“) → Starts und Wiederverbindungen stehen jetzt in zwei Zeilen.
- Sonst in Hell/1,3× gesehen und ok: Statusleiste, Gerät (Bandinformationen, Unbekannte-Felder-Hinweis), Verlauf mit Legende, Navigationsleiste.

## 2026-11-19 – #248: farbsichere Verteilung (Emulator-Abnahme in Hell/Dunkel)
- Dritter Schalter „Farbsicher“ neben „Punkte | Verteilung“ im Verlauf: blau (selten) → türkis → gelb (häufig), keine Rot/Grün-Unterscheidung, eigene Legende; Modus bleibt beim Neurendern erhalten.
- Geprüft im lokalen Emulator (S25-Auflösung 1080x2340, 480 dpi, synthetische Daten für 60 Tage), Hell und Dunkel; Hell-Ansicht der Verteilung ohne Auffälligkeiten.
- Emulator-Daten wurden lokal per nicht eingechecktem Instrumentationstest erzeugt; der Test läuft nur per `adb -s emulator-5554`, nie auf dem S25.
## 2026-11-19 – S25-Abnahme Analysen/Verlauf-Dichte und Gerät-Kacheln
- Nutzerfreigabe nach Screenshots (Build 1326, Dunkel, 1,0×): Reiter Analysen (#227 Stufe 1), Verlauf „Punkte | Verteilung" (#219 UI), Gerät-Kacheln (#242). Screenshots bleiben lokal (echte Gesundheitswerte).
- Fund: Speicherkachel brach „91,4 / MB" mitten in der Einheit um → Zeilenumbruch vor der Größe.
- Offen bleiben #219 (Hell/Farbenblind-Alternative) und #227 (Suche/Filter, TalkBack/320 dp).

## 2026-11-19 – #177: bebildertes Benutzerhandbuch (Emulator, Demo-Daten)
- 9 Abbildungen (Übersicht ×2, Verlauf Punkte/Verteilung, Analysen ×2, Gerät, Daten, Einstellungen) aus dem S25-Emulator (1080×2340, Dunkel) mit synthetischen Daten, auf 540 px verkleinert (PNG8, je 29–72 KB) in `docs/user/images/`, im Handbuch mit nummerierten Bildunterschriften; als Emulator/Demo-Daten gekennzeichnet, nicht als reale Gerätetests.
- Nicht abgedeckt (ehrlich): echtes Gerät/Band (nur Emulator), Hell-Modus, Dialoge (Rückfragen beim Backup/Löschen), Testlabor, BLE-Zustände mit verbundenem Band.
## 2026-11-19 – #248: 320-dp-Prüfung im Emulator
- Emulator mit `wm density 540` (= 320 dp Breite) geprüft: App-Titel „PulsLibre“ brach mitten im Wort um → einzeilig mit Autosize (18–30 sp) und Abstand zum Status-Button; lange Modultitel in Analysen mit Silbentrennung.
- Rest: „Herzfrequenzvariabilität“ bricht bei 320 dp weiterhin in der Karte um (ein 24-Buchstaben-Wort ohne Trennstelle), bewusst akzeptiert.
- Fund TalkBack-Semantik (uiautomator-Dump, kein echter Screenreader): Beschreibung des Status-Buttons wiederholte „CIRQA-Verbindungsstatus:“ und endete auf „öffnen.. Details anzeigen“ → bereinigt. Gruppen und Modulkarten tragen vollständige Beschreibungen inkl. Status und „Tippen zum Öffnen“.
- Entscheidung Analysen-Suche/Filter: laut #227 „erst bei Bedarf“; bei 17 Modulen in 5 Gruppen nicht nötig, bewusst nicht gebaut. Zeitfenster gilt nur im Ruhepuls-/Atlas-Block (7/28/90 T), kein globaler Zwang.

## 2026-11-19 – #204: Tap auf „Unbekannte Datenfelder“ im Emulator geprüft
- Auf dem S25 (Build 1320) öffnete sich der Dialog bei zwei Tests nicht. Im Emulator mit 1000 synthetischen Feldtypen (100 FIT-Nachrichten × 10 Felder) öffnet der echte Touch-Tap den Dialog sofort, Gruppen lassen sich auf-/zuklappen. Fehler damit **nicht reproduzierbar**; wahrscheinlichste Ursache: der Tap lag nach dem Scrollen neben der Karte (ihre Position hängt von der Länge der Seite ab). Kein Codechange; Beobachtung bei der nächsten Handyprüfung wiederholen (Position per `uiautomator dump` bestimmen).

## 2026-11-20 – #41: Asset-Kit fitness-v1 als VectorDrawables
- Fünf Icons aus `design/icons/fitness-v1/` von Hand in VectorDrawables umgesetzt (`ic_pl_resting_pulse/activity_calendar/coverage/data_gap/baseline`, Farben über `pl_icon_*` → Hell/Dunkel), als `PlIcon`-Einträge; eingesetzt im Ruhepuls-Hero, Aktivitäts-Atlas und in der Datenabdeckung. `data_gap` und `baseline` stehen bereit (noch ohne Einsatzort, kein Platzhalter-UI). Gestrichelte Linie aus `baseline.svg` als Einzelstriche.
- Emulator-Sichtprüfung (Dunkel, synthetische Daten) ohne Auffälligkeiten.
## 2026-11-19 – #204: Status je unbekanntem Feld in der UI ändern
- Pro Feld „Status ändern“ (ungeprüft · in Prüfung · unterstützt · ignoriert) schreibt die Prüfmarke für alle Quelldateien-Zeilen des Feldtyps (`updateFitUnmappedFieldStatus`), aktualisiert die Zeile live und die Zusammenfassung; ändert keine Messdaten.
- Im Emulator (1000 synthetische Feldtypen, echte Touch-Taps) geprüft; dabei gefunden: `setMessage` + `setSingleChoiceItems` versteckt unter AppCompat die Liste → eigene Ansicht mit Hinweistext und vier Zeilen. DB-Stand nach Test: 1× IGNORED, 1× SUPPORTED, Rest UNREVIEWED.
- Test `entryOffersAStatusChoiceThatStatesNoMeasurementDataIsChanged`.
- Korrektur: Der lokale Emulator-Seed-Test `SyntheticSeedTest` war versehentlich mit #250 in `main` gelandet (`git add -A`). Er schreibt synthetische Daten in die App-DB und gehört nie ins Repo/auf ein Handy → entfernt. Künftig nur gezielte `git add`-Pfade.

## 2026-11-20 – Header-Flimmern beim Scrollen behoben
- Ursache: Der Kopf wechselte beim Scrollen zwischen zwei Größen (170 dp ↔ kompakt) und war Geschwister des ScrollViews; jede Größenänderung veränderte Sichtfenster und Scrollbereich und löste neue Scroll-Ereignisse aus (Sprung, Flimmern, besonders um die 40-dp-Schwelle).
- Neu: Der Hintergrund (Berge + Untertitel) ist das erste Element des gescrollten Inhalts und scrollt einfach weg; nur die schmale Titelleiste (Logo, Titel, Status-/Einstellungs-Button) bleibt als Overlay fixiert, ihr Hintergrund blendet kontinuierlich ein, der Untertitel blendet aus. Keine Ansicht ändert beim Scrollen ihre Größe.
- Tests: `scrollingNeverResizesTheHeaderOrTheScrollRange` (Größen bleiben bei y = 0/60/150/400/900/0 gleich), vorhandene Header-/Dashboard-/Analysen-Tests laufen im Emulator grün; Sichtprüfung Dunkel und Hell im Emulator.

## 2026-11-20 – #261 Garmin-Benchmark: Lückenmatrix, Mockups, Subtasks
- Neu: `docs/research/GARMIN_CONNECT_BENCHMARK_261_2026-11-20.md` (Evidenz aus Code/Doku, Lückenmatrix je Tabellenzeile, Subtasks A–G, Pflichtkriterien) und drei synthetische SVG-Mockups in `design/mockups/benchmark-261/` (24-h-Zeitachse, Schlafdetail, Schrittziel/Woche). Keine Garmin-Screenshots, keine echten Gesundheitswerte im Repo, **kein Android-Code geändert**.
- Kernbefund: Schlafphasen/Sleep Score/Body Battery/Stress/Kalorien/Intensität sind **nicht als CIRQA-Felder belegt** und bleiben „nicht verfügbar“; echte Lücken sind gemeinsame 24-h-Achse, lokales Schrittziel, Wochenkarte, anpassbare Startseite. Nicht geprüft: Vergleich gegen frische S25-Screenshots (nicht ohne Handy möglich).

## 2026-11-20 – #266 Lokales Schrittziel auf der Übersicht
- Das in den Einstellungen gespeicherte `step_goal` wurde bisher nicht genutzt; die Schrittkarte zeigte nur das Gerätziel. Jetzt hat ein gültiges eigenes Ziel (100–100 000) Vorrang, Quelle wird gezeigt („Ziel (eigenes)“/„Ziel (Gerät)“), ab dem Ziel „Tagesziel erreicht“. Ungültige Eingaben werden ignoriert, nicht korrigiert.
- Reine Funktionen `applyLocalStepGoal`, `stepGoalStatus` (Unit-Tests in `DashboardOverviewTest`), Instrumentationstest `ownStepGoalFromSettingsReplacesDeviceGoalAndMarksReachedGoal` (stellt die Einstellung danach wieder her). Handbuch ergänzt.
- Bewusst nicht Teil dieses Schritts: kumulierte Tageskurve mit Ziellinie (bleibt in #266 offen, braucht die 24-h-Achse aus #263).

## 2026-11-20 – #263 „Tag im Überblick“ (gemeinsame 24-h-Zeitachse)
- Neue Karte unter den Übersichtskacheln öffnet einen Dialog mit Datumsauswahl: Herzfrequenz und Schrittzähler des gewählten Tages untereinander auf **einer** lokalen Tagesachse (`DashboardDay`, DST-sicher: 23-/25-h-Tage). Lücken (> 5 min Herzfrequenz, > 10 min Schritte, inkl. Tagesanfang und „jetzt“) sind schraffierte Felder mit „keine Daten“, nie überbrückt oder interpoliert; Schritte als Treppe, Zählerrückgang unterbricht die Linie; Schrittziel als gestrichelte Linie. Tippen/Ziehen zeigt Fadenkreuz, Uhrzeit und je Spur Wert oder „keine Daten“. Textfassung für TalkBack und unter dem Diagramm.
- Dateien: `DayTimeline` (Modell, Unit-Tests `DayTimelineTest`), `DayTimelineView`, `DayTimelineDialog`, Teaser in `MainActivity`; Instrumentationstest `DayTimelineViewTest`.
- Nicht enthalten (offen in #263): Schlaf-/Aktivitätsband (Quelle nicht belegt), Zoom/Pan (#264). Nicht geprüft: S25, Hell/200 % Schrift/320 dp, TalkBack real.

## 2026-11-20 – #264 Zoom/Pan in „Tag im Überblick“
- `DayViewport` (Unit-getestet): sichtbarer Ausschnitt der Tagesachse, Zoom um einen Fokus, Pan, Grenzen (min. 15 min, nie außerhalb des Tages, ungültige Faktoren ignoriert), Tick-Abstand je Ausschnitt. Ansicht: Zwei-Finger-Zoom/-Pan, dazu Tasten ‹ − + › Tag (TalkBack-bedienbar), Hinweiszeile „Ausschnitt x h von 24 h“; Achsenbeschriftung aus echten Zeitpunkten (DST-sicher). Gezeichnet werden immer die Rohpunkte, keine Mittelwerte; Lücken bleiben Lücken.
- Downsampling: nicht nötig und nicht gebaut – der Reader liefert je Minute höchstens Minimum, Maximum, letzten Wert (≤ ~4 300 Punkte/Spur), alle werden gezeichnet, Spitzen bleiben erhalten. Bei deutlich größeren Datenmengen wäre min/max je Pixelspalte nachzuziehen.
- Nicht geprüft: echte Pinch-Geste auf dem S25/Emulator (Instrumentationstest prüft Modell und Tasten, nicht Touch-Gesten), TalkBack.

## 2026-11-20 – #265 Wochenkarte „Letzte 7 Tage“
- Im Trend-Block der Analysen: die sieben vollständigen Tage vor heute, je Zeile eigene Abdeckung („x von 7 Tagen“): Ø Schritte/Tag, Schritte gesamt (nur Tage mit Daten), Ø Geräte-Ruhepuls (nur gemeldete positive Werte, nie aus dem Tagesminimum geschätzt); mit eigenem Schrittziel zusätzlich „Ziel erreicht an x von y Tagen mit Daten“. Fehlende Tage zählen nie als 0; ohne Daten steht „keine Daten“. Schlafdauer, Kalorien, Intensitätsminuten und Stress stehen als „nicht verfügbar (keine belegte Quelle)“.
- `WeekSummary` (Unit-Tests `WeekSummaryTest`, 6), Einbau `TrendCardView.addWeekSummary`.
- Nicht geprüft: Emulator-/S25-Optik, TalkBack real.

## 2026-11-20 – S25-Abnahme Build 1380 (#263/#264/#265/#266) und Korrekturen
- Getestet am S25 (Build 1380 per `adb install -r`, Daten blieben erhalten; keine Instrumentationstests; Screenshots nur lokal, enthalten echte Werte): Übersicht, „Tag im Überblick“ (19.11.: 2 715 HF-Punkte, 265 Schrittpunkte; Fadenkreuz „15:25 · HF 80 bpm · Schritte 4303“; Zoom-Tasten auf 3 h, ‹ ›), Verlauf Punkte/Verteilung/Farbsicher, Analysen mit Wochenkarte (Ø 7.228 Schritte, 4 von 7 Tagen), Gerät, Daten, Einstellungen, Hell + Schrift 1,3×. Eigenes Schrittziel: „Ziel (eigenes): 8.100 Schritte“ erschien; die Einstellung wurde danach wieder geleert, Dunkel/Schrift 1,0 wiederhergestellt. Kein Absturz/ANR im Logcat.
- Funde und Korrekturen: (1) Spurtitel „Herzfrequenz“ oben abgeschnitten → mehr Platz über der ersten Spur. (2) Lücken-Schraffur war ein unruhiger Fächer → saubere 45°-Schraffur je Lücke, heller. (3) Werteachse (Min/Max) war unsichtbar (vom Plot-Clip abgeschnitten) → außerhalb des Clips. (4) Zeitachse „12:0012:30“ überlappte → Beschriftungen überlappen nie. (5) „8315  zuletzt“ (Doppelleerzeichen bei leerer Einheit). (6) Wochenkarte sagte „keine belegte Quelle“, obwohl Übersicht Kalorien/Stress/Intensität live zeigt → Wortlaut „Ohne Tageswerte (noch keine geprüften Tagesreihen; Gerätewerte erscheinen nur live)“.
- Nicht geprüft: echte Pinch-Geste (adb kann keine zwei Finger), TalkBack, 320 dp, Hell-Ansicht von Verlauf/Analysen im Detail, Akku (#235: batterystats nur seit kurzem Ladevorgang, keine belastbare Aussage).
- Beobachtung (nicht von mir): Gerät zeigt „GATT verbunden (Aufzeichnung aus)“, letzte Messwerte „vor 8 h 39 min“.

## 2026-11-20 – Ticketabschluss #263–#266 und Restpunkte
- #263, #264, #265, #266 geschlossen auf ausdrückliche Anweisung des Nutzers; die offenen Teile stehen gesammelt im Folgeticket (Restpunkte aus #263 bis #266): Schlaf-/Aktivitätsband, kumulierte Schrittkurve mit Ziellinie, Zoom im Verlauf, echte Pinch-Geste/TalkBack/320 dp, Kosmetik Zeitbeschriftung am rechten Rand, Tagesreihen für Kalorien/Intensität/Stress/Schlaf, Akku-Messung (#235).
- Weiter offen und unberührt: #267 (Startseite anpassen).

## 2026-11-20 – #155 SCA: Dependency Graph belegt, Dependabot-Alerts behoben
- SBOM per API: 225 Pakete inkl. Gradle-Abhängigkeiten (Dependency Submission funktioniert). 8 offene Dependabot-Alerts, alle im AGP-Build-Klassenpfad, nicht im APK (`releaseRuntimeClasspath` geprüft). Sieben Bibliotheken per `buildscript`-Constraint auf gepatchte Versionen angehoben (`build.gradle.kts`); Details und offene Repo-Einstellungen in `docs/SCA_DEPENDENCY_REVIEW_2026-11-20.md`.

## 2026-11-20 – #267 Startseite anpassen
- Unten auf der Übersicht: „Startseite anpassen“ öffnet einen Dialog (Sichtbar-Häkchen, ★ Favorit, ▲ Hoch, ▼ Runter, Speichern/Zurücksetzen/Abbrechen; alle Bedienelemente mit Textbeschriftung, nicht nur Symbol). Favoriten erscheinen zuerst unter „Im Fokus“. Ausgeblendete Kacheln werden weiter aufgezeichnet. Sind alle ausgeblendet, steht ein Hinweis mit Weg zurück. Die Standardansicht bleibt unverändert, solange nichts angepasst wurde (eigener Codepfad nur für ein gespeichertes Layout).
- Modell `DashboardLayout` (Unit-getestet: Standard erkannt, Ausblenden/Favorit/Verschieben mit Grenzen, neue Kacheln hinten, verschwundene entfallen, Zeilenbildung, beschädigte Einstellung fällt auf Standard zurück), Dialog `DashboardCustomizeDialog`, Instrumentationstest `customLayoutHidesTilesShowsFavouritesFirstAndResetsToDefault` (stellt die Einstellung danach wieder her). Speicherort: SharedPreferences `dashboard_display`/`layout_v1`, nur lokal.
- Nicht geprüft: S25-Optik/Bedienung, TalkBack, große Schrift im Dialog.
## 2026-11-20 – #41 Regelmäßigkeit, #265 Ziellinie mit eigenem Ziel, Zeitbeschriftung (Restpunkte #276)
- **Regelmäßigkeit (#41):** Im Aktivitäts-Atlas steht bei gesetztem eigenem Schrittziel „Eigenes Ziel (x) erreicht an a von b Tagen mit Daten (n-Tage-Fenster)“. Gezählt werden nur vollständige Tage vor heute mit Schrittwert; fehlende Tage zählen weder als erreicht noch als verfehlt; kein Score. Das Gerätziel wird dafür bewusst nicht verwendet (unvalidiert). `GoalRegularity` (Unit-getestet).
- **Ziellinie (#265):** In „Tag im Überblick“ nutzt die gestrichelte Ziellinie jetzt das eigene Ziel (Vorrang vor dem Gerätziel, `effectiveStepGoal`). Damit ist die kumulierte Tageskurve mit Ziellinie als Treppe in dieser Ansicht vorhanden; eine zusätzliche Kurve in der Übersichtskarte ist nicht gebaut (die Karte hat die 15-min-Balken).
- **Zeitbeschriftung:** Bei Kollision fällt die spätere Beschriftung weg, nur die letzte (Achsenende) ersetzt ihren Nachbarn.
- Bewusst nicht gebaut: Zoom in den Verlauf-Charts (die Fenster 24 h/7 T/30 T sind Bin-Darstellungen mit eigener Navigation).

## 2026-11-20 – #155 Nachtrag: Lint-Werkzeug-Konfiguration
- Nach PR #280 waren noch 6 von 8 Alerts offen: `androidLintTool` löst eigene alte Bibliotheken auf. Fix in `app/build.gradle.kts` (`resolutionStrategy.eachDependency` für Guava, Bouncy Castle ×3, commons-lang3, httpclient), `:app:dependencies` zeigt keine ungepatchte Version mehr. Details: `docs/SCA_DEPENDENCY_REVIEW_2026-11-20.md`.

## 2026-11-20 – #6 Codeaudit: Komponenteninventur und Sicherheits-Stichprobe (Teilprüfung)
- Neu: `docs/AUDIT_ISSUE_6_KOMPONENTEN_2026-11-20.md`: skriptgestützte Inventur (233 Dateien, 24.997 Zeilen) mit Testreferenzen je Komponente, Liste der 32 Dateien ohne Testreferenz mit Risikoeinordnung, belegte Sicherheits-Stichprobe (kein INTERNET, `allowBackup=false` + Extraktionsregeln, nur Launcher exportiert, PendingIntent immutable, kein WebView/Clipboard/externer Speicher, SAF-Export, Bind-Argumente) und sechs Befunde A6-1…A6-6 (u. a. `RECEIVER_EXPORTED` in `CirqaRfcommRunner`, fehlende Tests für `CirqaContinuousRecordingService` und `FitObservationCommitter`).
- **Kein Android-Code geändert.** Befunde in BLE/Datenbank gehören OpenCode; Hinweise stehen im Dokument. #6 bleibt offen (dateiweise Tiefenprüfung, MASVS-Zuordnung, Coverage-Bericht, S25-Evidenz stehen aus).
## 2026-11-20 – #177 Nutzerdokumentation ergänzt (ohne neue Bilder)
- Neu: `docs/user/README.md` (Startseite/Navigation, Kurzstart), `docs/user/FEHLERBEHEBUNG.md` (häufige Fragen: „Keine Messdaten“, „GATT verbunden (Aufzeichnung aus)“, Berechtigungen, Lücken, fehlende Trends, fehlende Schlaf-/HRV-Werte, Datenlöschung bei Deinstallation, Akku). Handbuch um Tag im Überblick, Wochenkarte, Regelmäßigkeit und Gerätewerte ergänzt; Link in der Repo-`README.md`. Alle Aussagen aus dem tatsächlichen Verhalten (S25-/Emulator-Prüfung, Code), keine nicht vorhandenen Funktionen.
- Weiter offen in #177: Abbildungen für die neuen Ansichten und ein Durchlauf auf dem Handy mit geschwärzten/synthetischen Bildern (echte Gesundheitsdaten gehören nicht ins Repo). Das Ticket bleibt dafür offen.

## 2026-11-20 – #235 Akku-Messwerkzeug (Handy-Seite)
- Neu: `tools/battery_baseline.py` (nur lesend per adb: Pegel, Ladezähler, Ladezustand, Bildschirm, App-Zeilen Bluetooth/Wakelock/Foreground; `report` berechnet %/h und mAh/h und warnt bei Fenstern unter 60 min, Laden, Build-Wechsel), Unit-Tests `tools/test_battery_baseline.py` (6), Protokoll `docs/BATTERY_MEASUREMENT_PROTOCOL.md`. Snapshot-Test am S25 lief (Handy lud gerade, Werkzeug meldete `plugged=True`).
- **Keine Messwerte, keine Android-Änderung.** Die Baseline selbst braucht mehrere Stunden am Handy; #235 bleibt offen.

## 2026-11-20 – #6 Härtung A6-1, Korrektur A6-3
- A6-1 behoben: `CirqaRfcommRunner` registriert den `ACTION_UUID`-Receiver mit `RECEIVER_NOT_EXPORTED` (geschützter System-Broadcast wird weiter zugestellt). Echte RFCOMM-/SDP-Antwort am Band nicht erneut geprüft.
- A6-3 im Audit korrigiert: der Committer ist indirekt über `commitFitFile` in `LocalReadingStoreTest` abgedeckt (Idempotenz, Konflikt, Rollback); fehlend sind nur gezielte Einzel-Konflikttests.
## 2026-11-20 – #6 A6-5 Log-/Diagnoseinhalt geprüft
- Ergebnis im Auditdokument: Diagnosepaket = feste Feldliste (bereits durch `DiagnosticReportPrivacyTest` gestützt); `CirqaLocalLog`-Statustexte enthalten nur Phase/Status/Zähler/Antwortformen (keine Nutzdaten, MAC, Namen); Messwerte bleiben lokal; `CirqaLocalLog.copyTo` hat keinen Aufrufer.
- Neu: `NoPlatformLoggingTest` (Quelltext-Wächter: kein `Log.*`/`println`/`printStackTrace` im Produktionscode; lokal grün, Gradle offline mit `ANDROID_HOME=/home/smiler/pulselibre-tools/sdk`). Kein Produktionscode geändert.

## 2026-11-20 – #6 JVM-Tests für Einmal-Gates, RFCOMM-Framer, Service-Discovery
- Neu (alle lokal grün, 17 Tests): `OneShotGatesTest` (Fail-closed-Verhalten der vier Einmal-Gates inkl. „nicht freigegeben / nicht bestätigt / schon verbraucht / Persistieren fehlgeschlagen“, Begrenzung und Manipulationsschutz des Versuchszählers), `CirqaRfcommStreamFramerTest` (COBS: Einzelframe, geteilte/zusammengeklebte Chunks, ungültige und übergroße Frames, reset), `GarminServiceDiscoveryTest`. Kein Produktionscode geändert.
- Damit sind 8 der 32 Dateien ohne Testreferenz im Inventar nun referenziert (`CirqaFileAccessAttemptGate`, `CirqaDirectoryAfterChainGate`, `CirqaDirectoryReadV2AttemptGate`, `CirqaHistoricalSyncV1MultiPhaseAttemptGate`, `CirqaItemListRepeatabilityAttemptGate`, `CirqaRfcommStreamFramer`, `GarminServiceDiscovery`; Inventurzahl wird bei der nächsten Skriptauswertung aktualisiert).
## 2026-11-20 – #6 Persistenz-Teilprüfung `DatabaseBackup`
- Auditdokument Abschnitt 7: Zip-Slip nicht möglich (feste Eintragsnamen), Größen-/Zeilenlimits beim Entpacken, Versions-/Eintragslistenprüfung, SHA-256 je Datei, Validierung vor Überschreiben. Befunde: B7-1 Backup unverschlüsselt (Nutzerhandbuch jetzt mit Datenschutz-Hinweis), B7-2 kein gezielter Test mit manipuliertem ZIP (offen). Migrationen in `CirqaDatabase` noch nicht geprüft. Kein Code geändert.

## 2026-11-20 – #3/#6 A6-2 Recorder-Lebenszyklus gelesen, Backoff testbar gemacht
- `RecorderBackoff.delayMs` aus `CirqaContinuousRecordingService` extrahiert (identisches Verhalten, Unit-Test `RecorderBackoffTest`). Beobachtung O-1 (Backoff zählt Versuche seit Dienststart, Reset fehlt) im Audit Abschnitt 8 für #235 festgehalten; bewusst nicht geändert, bis die Akku-Baseline vorliegt.
## 2026-11-20 – #6 Persistenz: `CirqaDatabase`-Migrationen gelesen
- Auditdokument Abschnitt 9: linearer, transaktionaler, additiver Upgrade-Pfad v1→v6; Emulator-Tests für alle Ausgangsversionen v1–v5; kein Downgrade (B9-1, jetzt in `docs/user/FEHLERBEHEBUNG.md` erklärt). Kein Code geändert.

## 2026-11-20 – #6 Barrierefreiheit: Verteilungsdiagramm mit Screenreader-Text
- Prüfung: Von 23 eigenen View-Klassen setzt nur `HistoryDensityChartView` weder `contentDescription` noch `importantForAccessibility`. Jetzt trägt es eine Zusammenfassung (`HistoryDensityAccessibility.summary`: Anzahl Werte/Zeitfenster, Achsenbereich, Bedienhinweis; keine erfundenen Werte), Unit-Test `HistoryDensityAccessibilityTest`. Echtes TalkBack am Gerät nicht geprüft.

## 2026-11-20 – #6 Ressourcen-Teilprüfung (Abschnitt 10 im Audit)
- Nach einem Sitzungsabsturz am letzten gemergten Stand (PR #306) wieder aufgesetzt (neuer Branch `claude/audit6-resources` von `origin/main`; nichts Uncommittetes ging verloren).
- Befund A10-1: keine `strings.xml`, 120 feste Textliterale in 13 Dateien, Oberfläche nur Deutsch. Kein Code geändert; Empfehlung und offene Produktfrage (Mehrsprachigkeit) im Audit. #6 bleibt offen (offen: `UnusedResources`-Lint, Coverage, MASVS, S25-Evidenz).

## 2026-11-20 – S25-Prüfung Build 1487 (nur lesend) und Korrektur der Achsenansage
- `adb install -r` des signierten Artefakts aus `main` (Lauf 1487) auf das S25 (`ANDROID_SERIAL` gesetzt, weil zusätzlich ein Emulator am adb hing); Start und Verlauf-Ansicht ohne Absturz, Crash-Puffer leer. Keine Instrumentationstests auf dem Handy; Screenshots nur lokal.
- Befund am Gerät: Die Beschreibung des Punktediagramms (`HistoryScatterChartView`) sagte im 24-h-Fenster „Horizontale Achse 15:24 bis 15:24“. Neu `HistoryAxisSpeech.span` (Datum + Uhrzeit, Test `HistoryAxisSpeechTest`); die gezeichneten Achsenbeschriftungen bleiben unverändert.

## 2026-11-20 – Nacht-Insights: Rundung der Begründung an die Kachel angeglichen
- Am S25 (Build 1487) zeigte die Kachel „Abdeckung 0,5 %“, die Begründung darunter „Abdeckung 1 % unter 70 %“. `NocturnalOxygen` rundet jetzt wie die Kachel auf eine Nachkommastelle; Test `withheldReasonUsesTheSameOneDecimalAsTheTile`. Reine Textänderung, keine Rechenlogik.
- Beobachtet: In „Schritte heute“ liegen die blassen 15-min-Balken hinter dem Wort „Schritte“; das ist so gewollt (Hintergrundebene laut Design #122, `StepBarsView`, Alpha ≈ 33 %), kein Fehler.

## 2026-11-20 – S25 an OpenCode übergeben (FIT-Workflow-Test)
- S25-Prüfung Build 1487 abgeschlossen (nur Bedienung/Lesen): Start, Verlauf, Analysen, Gerät, Startseite anpassen (SpO₂ aus-/einblenden, Zurücksetzen) ohne Absturz und ohne BLE-Aktion. Korrekturen aus der Prüfung: PR #309 (Achsenansage), PR #312 (Rundung Nacht-Insights). Offen aus der Prüfung: Favorit/Hoch/Runter im Anpassen-Dialog nicht ausprobiert.
- Ab 15:45 keine adb-Verbindung mehr; Übergabenachricht an OpenCode im `agent-sync`.

## 2026-11-20 – #6 Lint-Auswertung (Abschnitt 11 im Audit), ohne S25
- `lintDebug` lokal: 0 Fehler, keine `UnusedResources`-Meldung, alle 59 Drawables referenziert. Die drei `ClickableViewAccessibility`-Treffer in `MainActivity` sind bewertet (unkritisch, kein Code geändert); `ApplySharedPref` im Testlabor und `OldTargetApi` als Hinweise für OpenCode bzw. ein eigenes Migrationsticket vermerkt. #6 bleibt offen (Coverage, MASVS, S25-Evidenz).
- Coverage nur JVM lokal erhoben: 30,9 % Zeilen (Audit Abschnitt 12); kombinierter Wert mit Emulator-Tests fehlt, bewusst nicht lokal gestartet (S25 am selben adb).

## 2026-11-20 – S25-Abnahme Build 1502 (Bedienung/Lesen, ohne BLE-Aktion)
- Installiert: signiertes Update aus `main` (Lauf 1502) per `adb install -r`, `ANDROID_SERIAL` auf das S25; keine Instrumentationstests, kein Download/Scan, Screenshots nur lokal.
- **#309 bestätigt:** Verlauf-Beschreibung sagt „Horizontale Achse 19.11. 16:28 bis 20.11. 16:28“. **#312 bestätigt:** „Abdeckung 0,5 %“ in Kachel und Begründung.
- **#267 am Gerät:** Anpassen-Dialog öffnet; Kachel ausblenden (SpO₂) und wieder einblenden, „Hoch“ (SpO₂ vor Herzfrequenz), „Favorit“ (Schlaf unter „Im Fokus“), „Zurücksetzen“ stellt den Standard her. Nicht geprüft: „Runter“ einzeln, Erhalt nach App-Neustart.
- **Hell-Ansicht** (Übersicht, Verlauf, Analysen) und **320 dp** (`wm density 540`, Übersicht/Verlauf/Gerät): lesbar, keine abgeschnittenen Texte, Umbrüche sauber. Kosmetik: App-Symbol im Kopfbereich hebt sich in Hell kaum vom Banner ab.
- Zurückgesetzt: `wm density reset` (480), Dunkelmodus an, Schrift 1,0 unverändert, Startseite auf Standard. Nicht geprüft: echte Pinch-Geste, TalkBack real.

## 2026-11-20 – #6 MASVS-Zuordnung (Abschnitt 13 im Audit)
- Befunde A6-1…A10-1, B7, B9 den MASVS-Kategorien zugeordnet, inkl. „noch nicht geprüft“ je Kategorie und Reihenfolge der nächsten Schritte. Ausdrücklich keine MASVS-Abnahme. Kein Code geändert.
- #6 S25-Evidenz (nur dumpsys, lesend) als Audit-Abschnitt 14: Berechtigungen ohne INTERNET/Standort, nicht debuggbar, kein ALLOW_BACKUP.

## 2026-11-20 – #204 Phase B Punkt 8: Beispielbytes ansehen (ohne Export)
- Neu: `FitFieldSampleReader` (liest aus einem CRC-geprüften FIT-Original höchstens 3 Beispielwerte à höchstens 32 Byte eines Feldes; Standard- und Developer-Felder, komprimierte Zeitstempel; ungültige Datei → null/fail closed; keine Deutung, keine Einheit, keine Speicherung) mit 7 JVM-Tests. Oberfläche: in der Feldliste neuer Link „Beispielbytes ansehen“ → Warnung („kann sensible Gesundheits- und Geräteinformationen enthalten …“) → erst nach Bestätigung Anzeige als Hex je Datei (Ordinal „Datei 1“, nie Dateiname/Hash), höchstens 3 Dateien. Emulator-Test `bytePreviewNeedsAWarnedConfirmationAndFailsClosedForMissingOriginals`. Kein Decoder geändert (`FitHistoricalObservations`/Inventur unberührt).
- **Nicht gebaut:** optionaler SAF-Export der Rohbytes (Ticketpunkt „optional“); wegen Gesundheitsdaten-Risiko erst auf ausdrücklichen Wunsch.

## 2026-11-20 – #6 kombinierte Coverage: Versuch scheiterte an Klassen-Prüfsummen
- Lokales Zusammenführen JVM+Emulator ergab 0 % (Messartefakt, nicht-identische Klassen); Audit Abschnitt 15, Vorschlag an den CI-Agenten per agent-sync (Emulator-Job soll Unit-Tests mitlaufen lassen und dort mergen). Kein Workflow geändert.

## 2026-11-20 – S25-Sammelabnahme vorbereitet (Plan, Skript, Offline-Tests)
- Neu: `tools/s25_acceptance.py` (ein Zug, ca. 12–15 min: Preflight inkl. Flags/Berechtigungen, Navigation, Übersicht inkl. Abdeckungs-Konsistenz #312, Verlauf/Achsenansage #309, Analysen/Atlas #41, Startseite anpassen #267, Gerät/Testlabor-Einstieg #172/Unbekannte Felder #204, Anzeigevarianten Hell/Schrift 1,3/2,0/320 dp #276, 90-s-Hintergrundbeobachtung #171/#25/#3), Plan `docs/S25_ACCEPTANCE_PLAN_2026-11-20.md`, Offline-Tests `tools/test_s25_acceptance.py` (14, inkl. Gesamtlauf mit gespieltem Handy).
- Sicherheit technisch erzwungen: Erlaubnisliste für adb-Befehle (kein Install/Clear/Instrumentation/Bluetooth/Texteingabe), nie Tippen auf Geräte-/Datenaktionen, Ausgabeordner außerhalb des Repos, Wiederherstellung von Nachtmodus/Schrift/Dichte im `finally`. Das Handy wurde dafür **nicht** angefasst (OpenCode nutzt es für den FIT-Test); Rückmeldung per agent-sync erbeten.

## 2026-11-20 – S25-Sammelabnahme durchgeführt (Build 1520, 17:35–17:52)
- **Ergebnis (ohne Gesundheitswerte):** 52 PASS, 1 FAIL (Werkzeugfehler, behoben), 3 INFO, 5 MANUELL. Navigation, Verlauf (Achsenansage mit Datum #309, Verteilungs-Zusammenfassung), Analysen, Startseite anpassen (#267 aus/zurück), Testlabor per 3-s-Druck (#172, Sheet A–E „Nur lesende Diagnose“), Feldliste auf dem echten Bestand (930 Typen), Beispielbytes: Warnung erscheint, Abbrechen zeigt keine Bytes (#204), Hell/Schrift 1,3/Schrift 2,0/320 dp ohne Absturz und ohne Überlappung (Heuristik; Stichprobe der Screenshots gesehen), Berechtigungen/Flags (#6).
- **FAIL war ein Werkzeugfehler:** „Schritte heute“ nicht gefunden, weil `input swipe` einen Fling auslöste und die sequentielle Suche die Kachel übersprang; die Kachel war vorhanden. Behoben: langsamer kurzer Zug (900 ms, 28 %) und `scan_page` (einmaliger Durchlauf, Vereinigung aller Knoten). Wiederholung der Phasen preflight/navigation/overview: alle PASS, **#312 am Gerät belegt (Kachel 0,5 % = Begründung 0,5 %)**.
- **INFO:** Akku 100 %, lädt; CIRQA-Batteriestand wird auf der Geräteseite nicht angezeigt (#159 hängt an OpenCode); nach 90 s im Hintergrund kein Vordergrunddienst aktiv (Zustand „Aufzeichnung aus“; Beleg für #171/#25/#3, keine Aussage über Dauerbetrieb).
- **Offen/manuell:** TalkBack durch die neuen Dialoge, Beispielbytes bewusst anzeigen (nur „Datei n“), Plausibilität Fitness-Ansicht, Gestaltungswunsch Geräteseite. Bericht und Screenshots liegen lokal (`~/s25-acceptance/…`), enthalten echte Werte, nicht im Repo.

## 2026-11-20 – Kosmetik: App-Symbol im Kopfbereich in der Hell-Ansicht
- Befund aus der S25-Prüfung: Logo-Quadrat (#5127C9) und Banner (#5A33C7 im Hellmodus) fast gleich. `PulseLogoView` zeichnet in Hell eine 1,5-dp-Kontur (weiß, 80 %); Dunkel unverändert. Parameter `outline` injizierbar; Emulator-Test `PulseLogoViewTest` prüft per Pixel, dass die Kante deutlich heller ist als die Füllung. Wartet auf stabile CI (kein Push während der LOCAL-Läufe).

## 2026-11-20 – S25-Sammelabnahme, zweiter Komplettlauf mit korrigiertem Werkzeug (Build 1520, 18:11–18:22)
- **53 PASS, 0 FAIL, 3 INFO** (Akku 100 % lädt; CIRQA-Batteriestand nicht angezeigt (#159); kein Vordergrunddienst nach 90 s Hintergrund). Alle Phasen inkl. Überlappungs-Heuristik in Hell/Schrift 1,3/Schrift 2,0/320 dp, #312 (Kachel = Begründung), #309, #267, #172, #204-Warnung. Einstellungen wiederhergestellt (Nachtmodus an, Schrift 1,0, Dichte 480). Bericht lokal (echte Werte, nicht im Repo).

## 2026-11-20 (abends) – Offline-Nachweis nach Umstellung auf LOCAL

- Stand: PR-/Fork-Läufe bleiben CLOUD (Billing-Blocker, wird nicht umgangen); PR #330 (direktes LOCAL-Routing) ist offen. Keine Pushes/Merges von mir, solange kein grüner voller Lauf auf dem Branch-Kopf lesbar ist.
- Branch `claude/header-icon-kontrast` auf aktuellen `origin/main` rebased (konfliktfrei). Lokal offline grün: `:app:testDebugUnitTest` und `:app:compileDebugAndroidTestKotlin` (inkl. `PulseLogoViewTest`). Emulator-Tests laufen weiterhin nur in der CI.
- Nächster Schritt: sobald PR #330 integriert und Billing geklärt ist, LOCAL QUICK/FULL per `workflow_dispatch` auf dem Branch auslösen (mit CI-Agent abstimmen, Concurrency #321), dann #327 und den Logo-PR mergen.

## 2026-11-20 (abends) – Audit #6: ExportActivity abgeschlossen

- A6-7 umgesetzt: Fehlermeldungen bei Backup/CSV-Export nennen die mögliche unvollständige Zieldatei (reiner Text, `compileDebugKotlin` offline grün, keine Tests an den Texten).
- A6-8 (still verworfene beschädigte Zeilen, Speicherlast von `readingLines()`) im Audit Abschnitt 16 dokumentiert, nicht umgesetzt.
- Nur lokal committet; kein Push bis LOCAL-Nachweis (siehe oben).

## 2026-11-20 (spät) – PR #332: Emulator-Test `PulseLogoViewTest` korrigiert

- Cloud-FULL (Lauf 44808320489): 112 Emulator-Tests, 1 Fehler – mein eigener neuer Test: `plain=107 outlined=225`. Die Produktionskontur wirkt (225 gegenüber 107), die Schwelle `plain < 90` war falsch gerechnet (Kanalmittel von #5127C9 = 107). Schwelle auf `< 130` angepasst, Unterschied weiter `> +70`. Der Fehler ist damit ein Testfehler, kein Produktfehler.

## 2026-11-20 (nachts) – #334: FIT-Historie im Verlauf

- Ticket #334 angelegt (Folgepunkt zu #149). Die genannte agent-sync-Datei (22:08) und der Geräte-Befund liegen auf `origin` nicht vor; Umsetzung nach Auftragsbeschreibung und Code, Rückfrage im agent-sync (22:30).
- Neue Karte „Herzfrequenz · Band-Verlauf (FIT)“ im Reiter Verlauf: Zeitachse Messzeit des Bandes, getrennt von der bestehenden Karte (Telefonzeit). `FitHistoryReader` liest nur lesend über `HistoricalObservationRepository`, nur Beobachtungen mit FIT-Herkunft, `heart_rate`, 1–240 bpm. Karte nur sichtbar, wenn FIT-Daten existieren; sonst ehrlicher Leertext.
- Tests: JVM `FitHistoryMappingTest` (Zeit, Herkunft, Wertebereich, Zeitbasen), Emulator `FitHistoryViewTest` (Reader-Fenster halboffen, Karten getrennt, Leerzustand). Offline: Unit-Tests grün, Emulator-Tests kompilieren; Emulator-Lauf in der CI.
- Bekanntes Risiko: Der Verlauf lädt bei jedem Refresh das FIT-Fenster neu (Hintergrundthread, bis 150.000 Punkte pro Fenster). Falls das auf dem S25 spürbar ist, Cache ergänzen.

## 2026-11-20 (nachts) – #334 Teil 2: Übersicht zeigt Band-Verlauf (FIT)

- Nutzerhinweis: Die Graphen der Übersicht zeigen die historischen Daten ebenfalls nicht (gleiche Ursache: nur Echtzeit-`events`).
- Herzfrequenz-Kachel: zusätzliche Zeile „Band-Verlauf heute (Messzeit): Ø · min–max (n Werte)“ und eigene Sparkline (24 Stunden, Messzeit des Bandes). Live-Werte und Band-Werte werden nicht gemischt. Hat die Kachel heute nur Band-Daten, erscheint nur dieser Block (kein Wochenvergleich, der wäre live-only).
- Tests: `FitDaySummaryTest` (nur heute bis jetzt, Stundenmittel nach Messzeit, leer = null). Offline Unit-Tests grün, Emulator-Tests kompilieren.
- Offen: Tageszeitleiste (`DayTimeline`) und Tagesdialoge lesen weiter nur Live-Events; weitere FIT-Metriken gibt es im Import nicht für diese Kacheln.

## 2026-11-21 – S25-Sichtprüfung #334 (CI-signiertes APK, Build 1561)

- Installiert: `app-release.apk` aus dem main-Lauf 44821236628 (enthält #335/#336), `adb install -r`, Daten erhalten. Nur lesend bedient (Tabs, Zeitraumwahl, Scrollen); keine BLE-Aktion, keine Instrumentation.
- **Verlauf, 30 Tage:** zweite Karte „Herzfrequenz · Band-Verlauf (FIT)“ vorhanden, Werte in mehreren belegten Tagesfenstern, Legende „Zeitachse: Messzeit des Bandes (aus FIT-Dateien)“; die Live-Karte bleibt „Telefonzeit“ (eigene Werte, Telefonzeit). Beide Reihen getrennt.
- **Übersicht:** Herzfrequenz-Kachel zeigt die Live-Zeile („Heute Ø … bpm“); die Zeile „Band-Verlauf heute (Messzeit)“ erscheint nicht, weil die FIT-Daten nicht von heute sind (erwartetes Verhalten: nur heutige Band-Werte).
- **Performance:** Wechsel 7 → 30 Tage bis zur sichtbaren Karte unter etwa 2 s (grobe Messung inklusive UI-Dump); kein Absturz im Crash-Log. Kein Anlass für Rollups (#140).
- Offen: die Übersicht zeigt historische Band-Werte nur für den heutigen Tag; Tage davor wären ein eigener Schritt (Wochenkarte, #329) und braucht belegte weitere Metriken.

## 2026-11-21 – Übersicht: neuester Band-Tag, wenn heute keiner vorhanden ist

- Befund am S25: Die Band-Zeile der Herzfrequenz-Kachel erschien nicht, weil die FIT-Daten nicht von heute sind. Das erfüllt den Nutzerwunsch („historische Daten in der Übersicht“) nicht.
- Jetzt: ohne heutige Band-Werte zeigt die Kachel den neuesten Tag mit Band-Werten, mit Datum in der Zeile („Band-Verlauf 17.11. (Messzeit): Ø … (n Werte)“) und eigener Sparkline. `FitHistoryReader.latestMeasuredAt` (nur lesend). Live-Werte bleiben getrennt.
- Tests: `FitDaySummaryTest` (Datum des Vortags), `FitHistoryViewTest` (latestMeasuredAt halboffen). Unit-Tests grün, Emulator-Tests kompilieren.

## 2026-11-21 – S25-Abnahme #334: Messtag der FIT-Herzfrequenz (Build 1569, nur lesend)

- Aufbau: CI-signiertes APK aus dem main-Lauf 44827310290 (Stand nach #339), `adb install -r` (Daten erhalten), nur Tabs/Zeitraum/Zurückblättern/Scrollen; **keine BLE-Synchronisation, keine Instrumentation**.
- Aus Datenschutzgründen stehen hier bewusst **keine Messwerte, Zählwerte oder Messdaten-Tage** (Regel: keine Gesundheitsdaten im Repo); die Zahlen liegen nur im lokalen Gerätebericht.
- **Übersicht:** Die Herzfrequenz-Kachel zeigt neben der Live-Zeile eine getrennte Zeile „Band-Verlauf <Datum> (Messzeit)“ mit dem Datum des neuesten Messtags, weil heute keine Band-Werte vorliegen.
- **Verlauf:** 30 Tage und ein 24-h-Fenster zurück zeigen die getrennte Band-Karte („Zeitachse: Messzeit des Bandes“); die Live-Karte im selben Fenster hat eigene Werte (Telefonzeit). Beide Reihen getrennt.
- **Konsistenz:** Der Messtag der Kachel liegt im Verlaufsfenster, in dem die Band-Karte ihre Werte hat; Ortsdarstellung und UTC-Epoche entsprechen der Zusage von OpenCode. Grenze: keine unabhängige Referenz für den absoluten Tag (kein Garmin-Connect-Vergleich); geprüft ist die interne Konsistenz von Kachel, Verlauf und Fensterachse.
- **Performance:** Zeitraumwechsel und Zurückblättern ohne spürbare Verzögerung, kein Absturz im Crash-Log. Rollups (#140) nicht nötig.
- **Offen:** Metrikverteilung der 17 importierten Dateien (OpenCode); Tageszeitleiste/Tagesdialoge lesen weiter nur Live-Daten; weitere Metriken erst nach Import.

## 2026-11-21 – Lücken-Auswertung und zweiter Historien-Download (Nutzerfreigabe)

- Aus einem App-Backup (lokal, nicht im Repo) mit `tools/db_gap_report.py` ausgewertet: Die Herzfrequenz (Live + FIT) hat über mehrere Tage sehr viele Lücken über 5 Minuten; Live-Lücken folgen den Aufzeichnungszeiten, die FIT-Reihe deckt nur wenige Tage ab. Keine Messwerte oder Zeiten hier.
- Auf Nutzerfreigabe einmal „Historie herunterladen“ ausgeführt (Dialog gelesen, bestätigt; keine Gate-Änderung, kein Reset, keine Instrumentation). Ergebnis: dieselben 25 Dateien/488.067 Bytes wie beim ersten Lauf, `ALREADY_PRESENT`, nichts Neues importiert, `coverage=device-history-unverified`, `nextTransaction=true`. Details und Fragen an OpenCode im agent-sync (00:30).
- Folgerung: Weitere Daten kommen so nicht; das Listenende/`nextTransaction` und die Werte ohne Zeitstempel sind Sache von OpenCode.
## 2026-11-21 – Export der FIT-Originale (#328, optionaler Export) auf Nutzerwunsch

- Anlass: Nutzer will die unbekannten Feldtypen lokal analysieren (Daten sind synthetisch). Das App-Backup enthält nur das Feldinventar (Koordinaten, Typ, Größe, Zähler), keine Rohwerte; die Originale liegen im privaten App-Ordner (Release-App nicht debugbar, kein `run-as`).
- Neu: Daten-Seite „FIT-Originale exportieren“: Warnhinweis, dann ZIP in einen vom Nutzer gewählten Ort (Dateiauswahl). `FitOriginalsArchive` nimmt nur reguläre Dateien `<64 hex>.fit`, keine Links, Obergrenzen 2.000 Dateien/64 MB; verändert, löscht und sendet nichts. Tests: `FitOriginalsArchiveTest` (JVM, grün).
- Die ZIP-Datei bleibt lokal (nicht im Repo); Auswertung der Feldinventur lokal, Ergebnisse hier nur ohne Werte.

## 2026-11-21 – FIT-Feldanalyse der 45 Originale

- Signiertes APK Build 1580 (CI-grün, #344) auf dem S25 installiert, „FIT-Originale exportieren“ ausgelöst (Warnung gelesen), ZIP per `adb pull` lokal (nicht im Repo). 45 Dateien, rund 0,6 MB.
- Eigener Parser `tools/fit_field_survey.py`; Ergebnis in `docs/protocol/FIT_FELDANALYSE_2026-11-21.md` (ohne Rohwerte). Hauptbefund: Die 1.622 Herzfrequenzwerte ohne Zeitstempel sind Monitoring-Werte mit `timestamp_16` und per FIT-Regel zeitlich zuordenbar (Lücken sinken um etwa einen Tag). Aufgabe an OpenCode im agent-sync (00:45).

## 2026-11-21 – timestamp_16 im FIT-Decoder (Nutzerauftrag)

- Auf Nutzerwunsch Erkenntnis umgesetzt: Monitoring-Herzfrequenz mit `timestamp_16` bekommt den Zeitstempel per FIT-Regel (3 neue Unit-Tests, alle Unit-Tests grün). Auf den 45 echten Originalen (lokal, temporärer Test, nicht eingecheckt): 29.358 importierbar statt 27.736, 0 ohne Zeitstempel.
- Bewusst unverändert: `DECODER_VERSION` und Qualitätswert (Schema-CHECK, Quellenabgleich). Hinweis an OpenCode im agent-sync (01:20).
- Zuständigkeit: Eingriff im Bereich von OpenCode nur wegen des ausdrücklichen Nutzerauftrags und mit Meldung.

## 2026-11-21 – S25-Update mit timestamp_16 (Build 1594) und Lückenvergleich
- Update-Installation (`adb install -r`, CI-signiertes APK von der grünen main-CI) ohne Datenverlust; Sicherung vor und nach dem Update verglichen (Rohdaten nur lokal, hier keine Werte/Zahlen/Daten).
- **A (neu importiert aus neuen Quellen):** zwei zusätzliche Quelldateien, direkt nach dem App-Start validiert; Ursache offen, Frage an OpenCode (agent-sync). Es wurde von mir keine BLE-Synchronisation gestartet.
- **B (bekannte Quellen, rekonstruierter Zeitstempel):** zusätzliche Herzfrequenz-Beobachtungen aus Monitoring Feld 27 – entspricht der Vorabprüfung zu #347.
- **C (Darstellung):** Lückensumme und längste Lücke im Band-Verlauf sinken deutlich; keine neuen Metriken.
- Befund GUI: Kachel zeigte „heute“ mit nur einem Wert kurz nach Mitternacht → jetzt fällt sie bei sehr dünnem heutigem Band-Tag auf den neuesten reicheren Tag zurück. Info-Text der Band-Karte nennt rückgerechnete Zeiten.
- Rekonstruierte Zeiten sind in der DB nicht gekennzeichnet (CHECK nur PROFILE_VALID); Vorschlag für Herkunftsfeld an OpenCode.

## 2026-11-21 – #334: Band-Herzfrequenz in der Tageszeitleiste der Übersicht
- „Tag im Überblick“ bekommt eine dritte Spur „Herzfrequenz · Band (FIT)“ (Messzeit des Bandes, eigene Spur, nicht mit Telefonzeit-Werten vermischt); Lücken bleiben leer. Der Erklärtext unter dem Diagramm nennt beide Zeitbasen. Lesefehler der FIT-Daten blenden nur die Spur aus.
