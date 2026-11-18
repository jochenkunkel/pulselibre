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
