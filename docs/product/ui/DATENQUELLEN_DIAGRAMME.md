# Datenquellen aller Diagramme (P0-Audit 25.11.2026)

Stand: `main` nach PR #648. Ermittelt aus dem Quelltext, nicht aus Dateinamen oder älteren Notizen. Verantwortlich:
GUI-Agent. Datenbeschaffung, Decoder und Persistenz: FIT-Agent.

## Datenhaltung, die die Oberfläche liest

Alle Diagramme lesen **eine** SQLite-Datei: `cirqa_measurements.db` (`CirqaDatabase`, Schema 11). Die alte Datei
`pulselibre_readings.db` (`LocalReadingStore`) wird von keiner Ansicht direkt gelesen. Sie wird nur beim Kopieren in
die kanonische Datei angefasst (`ensureHistoricalStoreCopied`).

| Tabelle | Inhalt | Zeitbasis |
|---|---|---|
| `events` | Live-Messwerte über BLE (`kind='reading'`, JSON je CIRQA-Service) | Empfangszeit am Telefon (`received_at_utc`, `dashboard_time`) |
| `observations` + `fit_observation_provenance` | Ältere Importe ohne FIT-Provenienz (Herkunft `import`) sowie FIT-Beobachtungen alter Schema-Generationen | wie gespeichert |
| `fit_generation_observations` + `fit_active_generations` | Kanonische FIT-Beobachtungen der aktiven Decoder-Generation je Quelldatei (Herkunft `band`) | Messzeit des Bandes (`measured_at_ms`) |
| `unified_measurements` | **Abgeleitete** Vereinigung der drei obigen Quellen mit `origin` = `live`/`import`/`band`; seit #648 mit `fit_message` (FIT-Nachrichtenart) | je Herkunft unverändert |
| `live_device_metadata`, `fit_device_metadata` | Akku und Gerätedaten | Beobachtungszeit |

Die Downloadpfade (Legacy-History, FileAccess/MLR) sind **Transportwege**. Beide landen nach dem FIT-Decoding in
`fit_generation_observations`. Die Oberfläche unterscheidet deshalb nicht nach Transportweg, sondern nach Herkunft
(`live`/`band`/`import`) und, bei Band-Daten, nach FIT-Nachrichtenart.

### Normale Historie, Aktivität, Live

| Art | Erkennbar an | Gespeicherte Messgrößen (Stand Decoder) |
|---|---|---|
| Normale Tageshistorie | FIT `monitoring`, global 55 → `BandRecordKind.MONITORING` | nur `heart_rate` (Feld 27) |
| Aktivitätsaufzeichnung | FIT `record`, global 20 → `BandRecordKind.ACTIVITY` | `heart_rate`, `cadence`, `distance`, `speed`, `power` |
| Live | `events`, Herkunft `live` | Puls, Ruhepuls, Schritte, Kalorien, Intensität, RR, Stress, SpO₂, Body Battery, Atmung |

Folge: Schritte, Ruhepuls, SpO₂, Stress, Atmung und Body Battery gibt es **nur live**. Für sie kann keine Ansicht
Band-Historie zeigen, solange der Decoder sie nicht liefert (Abhängigkeit FIT, siehe unten).

## Matrix

Legende Verfügbarkeit: **ok** = liest die fachlich richtige Quelle; **behoben** = in #648 korrigiert;
**nur live** = richtig, weil keine Band-Daten dieser Größe existieren.

| Grafik / UI-Komponente | Quelldatei | Messwert | Reader | Tabelle | Herkunft | Zeitstempel / Zone | Qualität | Verfügbarkeit | Fehler / Maßnahme |
|---|---|---|---|---|---|---|---|---|---|
| Übersicht: aktuelle Kacheln (Puls, SpO₂, Stress …) | `DashboardOverviewView`, `HeartRateGaugeView`, `MetricReferenceScaleView` | jeweils letzter Wert | `CirqaDashboardReader` | `events` | live | Empfangszeit, Systemzone | Gerätewert, ungültige ausgeblendet | ok (bewusst „aktuell“) | – |
| Übersicht: Kachel-Insight mit Stundenspur | `DashboardOverviewView` → `SparkView` | Tagesverlauf + Vorwoche | `TileInsightReader` (`DashboardDayReader`, `FitHistoryReader`) | `events`, `unified_measurements` | live + band (Puls) | getrennt: Telefonzeit / Bandzeit | Band-Tag nur mit ≥ Mindestanzahl Werte | ok (#477) | – |
| Übersicht: Schritte heute | `StepBarsView` | Tageszähler | `DashboardDayReader` | `events` | live | Empfangszeit | – | nur live | FIT: Schritte nicht dekodiert |
| Übersicht: Nacht-SpO₂ | `DashboardOverviewView` | SpO₂ der Nacht | `NightOxygenReader` | `events` | live | Empfangszeit | ungeprüft (UNVERIFIED) | nur live | FIT: SpO₂ nicht dekodiert |
| **Kachel-Tagesdialog** | `DashboardDayDialog` → `DayChart` | Tagesverlauf einer Größe | `DashboardDayReader` + **`FitHistoryReader`** | `events` + `unified_measurements` | live + **band (Puls)** | getrennt, Bandzeit eigenes Diagramm | Duplikate/ungültige gezählt | **behoben** | vorher nur `events`: Band-Herzfrequenz unsichtbar |
| Tageszeitleiste | `DayTimelineDialog` → `DayTimelineView` | Puls, Schritte, Band-Puls | `DashboardDayReader`, `FitHistoryReader` | `events`, `unified_measurements` | live + band | eigene Spur je Zeitbasis | – | ok | – |
| Verlauf: Puls / SpO₂ (Telefonzeit) | `HistoryOverviewView` → `HistoryScatterChartView`, `HistoryDensityChartView` | 24 h / 7 / 30 Tage | `CirqaHistoryReader` (`HistoryDataCache`) | `events` | live | Empfangszeit | Bereichsfilter 1–240 / 1–100 | ok | – |
| **Verlauf: Herzfrequenz · Band-Verlauf (FIT)** | `HistoryOverviewView` | Band-Herzfrequenz | `FitHistoryReader` | `unified_measurements` (band) | band | Messzeit des Bandes | 1–240 bpm, max. 150 000 Punkte | ok (#477), **Anteile behoben** | vorher keine Unterscheidung Tageshistorie/Aktivität |
| Verlauf: „Alle Daten“ (Tageszeilen) | `HistoryOverviewView` | min/Ø/max je Tag, Herkunft, Größe | `UnifiedMeasurements.dailySummary` | `unified_measurements` | live + import + band | Tagesgrenze mit UTC-Offset am Fensterende | ungültige Sentinels getrennt gezählt | ok | R1 umgesetzt: Duplikate über Dateien zählen einmal |
| Analysen: Ruhepuls-Trend, Schritte (90 Tage), Aktivitätsatlas, Streudiagramm | `TrendCardView` → `RestingPulseChartView`, `DailyBarsView`, `ActivityAtlasView`, `ScatterView` | ein Wert je Tag | `DailyValueReader` | `events` | live | Empfangszeit, ganze lokale Tage | Ruhepuls = Median Gerätewert | nur live | FIT: Ruhepuls/Schritte nicht dekodiert |
| Analysen: Wissenschaft (Herzfrequenz, Distanz, Tempo, SpO₂, Atmung, Stress) | `ScienceDashboardView` → `ScienceViews` | Zeitreihen gebündelt | `ScienceSeriesLoader`, `ScienceFitSeriesAdapter` | `unified_measurements`, FIT-Tabellen | live + band, getrennt je Karte | Zeitbasis je Karte benannt | SpO₂ UNVERIFIED, Statuscodes getrennt | ok, **Band getrennt** (#656) | Band-Herzfrequenz als zwei Karten: Tageshistorie (monitoring) und Aktivität (record); Strecke/Tempo als Aktivität benannt; seit #696 eigene Band-Karten für SpO₂ (ungeprüft), Atemfrequenz und Stress |
| Header: Armbandstatus/Akku/Einstellungen (#649) | `WearableStatusButton` | Tragezustand, Armband-Akku | `WearableStatusReader` | `events` (Service 21), `live_device_metadata`, `fit_device_metadata` | live + FIT-Metadaten | Empfangs-/Beobachtungszeit | Tragezustand nur aus Atemstatus −100 / gültigem Atemwert (≤ 2 min), Akku ≤ 12 h | ok | – |
| Gerät: Empfang je Quelle | `DeviceOverviewView` | letzter Empfang, Zähler | `DeviceStatsReader` | `events` | live | Empfangszeit | – | ok (bewusst Empfang) | – |
| Gerät: Akku | `DeviceOverviewView` | Akkustand | `DeviceBatteryReader`, `latestLiveDeviceMetadata` | `fit_device_metadata`, `live_device_metadata` | FIT-Metadaten + live | Beobachtungszeit | Quelle benannt | ok | – |
| Daten: Erfassungsstand | `ScienceDashboardView` (Erfassungskarte) | Zähler FIT/Dateien | `CaptureInventory` | `downloaded_files`, FIT-Tabellen | Bestand | – | Teilweise lesbar gekennzeichnet | ok | – |

Untersucht: 14 Ansichtsgruppen mit 19 Diagramm-/Anzeigekomponenten.

## Prüfung #477 (P0-A)

Die Ursachen von #477 (Verfügbarkeit las die Alttabelle `fit_observation_provenance`, Lesefehler als „keine Daten“)
sind behoben: `FitHistoryReader` liest ausschließlich `unified_measurements` (Herkunft `band`), die aus den aktiven
Generationen gebaut wird. **Nicht** abgedeckt hatte #477 den Kachel-Tagesdialog. Er las weiter nur `events`, das ist in
#648 behoben. Alle übrigen Reader, die nur `events` lesen, betreffen Größen ohne Band-Daten (siehe Matrix).

## Restpunkte

- **R1 – Duplikate über Dateien (umgesetzt 25.11.2026 nach FIT-Empfehlung):** `sample_id` hängt an der Quelldatei.
  Enthalten **zwei verschiedene** Dateien dieselbe Messung – gleiches Gerät, gleiche Messgröße, gültiger Messzeitpunkt
  (Zeitbasis nicht `UNKNOWN`), gleiche Einheit, exakt gleicher Wert –, zählen Diagramme und Tagesübersichten sie nur
  einmal: Die Kopie der Datei mit dem kleinsten SHA-256 bleibt sichtbar, die anderen bekommen in
  `unified_measurements.display_duplicate` eine 1. Wiederholungen innerhalb einer Datei, fehlende oder unbekannte
  Zeitbasis und Werte ohne FIT-Herkunft werden nie zusammengefasst. Alle Originalzeilen bleiben gespeichert und gehen
  vollständig in den CSV-Export. Der Verlauf nennt die Zahl der so einmal gezählten Werte. Gleiche Regel im Speicher
  für die Wissenschaftsansicht: `CrossFileDuplicates`. Das ist Anzeigeaggregation, kein Beweis, dass beide Dateien
  dieselbe physische Probe enthalten. Tests: `UnifiedMeasurementsTest.sameMeasurementInTwoFilesCountsOnceButStaysExported`,
  `CrossFileDuplicatesTest`.
- **R2 – Größen ohne Band-Historie (teilweise gelöst durch #696, 26.11.2026):** Der FIT-Decoder (Version 4, Profil
  21.222.0) übernimmt jetzt kanonisch: `stress` (227/0, nur 1–100), `respiration` (297/0, nur > 0, Atemzüge/min),
  `spo2` (269/0, 1–100 %, keine medizinische Schwelle), `spo2_confidence` und `spo2_measurement_mode` (Rohcodes),
  `sleep_level` (275/0, Code 0–4), `skin_temperature_deviation` und `skin_temperature_7_day_deviation` (398/1–2, °C-Abweichung),
  `monitoring_cycles` (55/3, Profilskala 2, **keine** Schritte) und `monitoring_activity_type` (55/5, Code). Status- und
  Sentinelwerte bleiben Prüfkandidaten bzw. Rohfelder, sie sind keine Messwerte. Sichtbar sind die Werte in „Alle Daten“ (mit
  Namen; Codes ohne Mittelwert), im CSV-Export und als eigene Band-Karten in Analysen → Wissenschaft (SpO₂, Atmung,
  Stress). **Noch offen (GUI-Folgeschritt):** Übersichtskacheln, Tagesdialog und Schlaf zeigen weiter nur Live-Daten; Schritte aus FIT bleiben gesperrt (233/397, Monitoring 24/26/27
  ebenfalls).
- **R3 – Normale Historie unvollständig (FIT):** Dass Aktivitäten ankommen, beweist keine vollständige Tageshistorie.
  Die Oberfläche weist jetzt aus, wie viele Band-Werte aus `monitoring` stammen. Ist das 0, sagt sie es ausdrücklich.

## Tests

| Fall aus dem Auftrag | Test |
|---|---|
| FIT vorhanden, live fehlt / live vorhanden, FIT fehlt / beide | `FitHistoryViewTest`, `UnifiedMeasurementsTest.liveBandAndImportSitInOneTableWithSeparateOrigins` |
| Keine Werte | `FitHistoryViewTest.cardIsHiddenWithoutFitDataAndHonestWhenWindowIsEmpty` |
| Aktivität vorhanden, normale Historie fehlt | `BandHistoryDisplayTest.activityOnlyIsNeverPresentedAsAllDayHistory`, `FitHistoryViewTest.fitCardSaysWhetherValuesAreAllDayHistoryOrActivity` |
| Normale Historie vorhanden / Trennung | `UnifiedMeasurementsTest.normalHistoryAndActivityStayDistinguishable` |
| Außerhalb des Zeitfensters | `FitHistoryReader`-Tests (`newest`), `BandHistoryDisplayTest.bandDayKeepsExtremaAndLatestPerMinuteAndDropsOtherDays` |
| Mehrere Importgenerationen | `UnifiedMeasurementsTest.changedBandGenerationReplacesItsRows` |
| Abgeleitete Tabelle alten Schemas | `UnifiedMeasurementsTest.derivedTableWithoutMessageKindIsRebuilt` |
| Zeitzonenwechsel / Tagesgrenze | `BandHistoryDisplayTest.dstDayIsHandledInLocalTime` |
| Datenmenge 1 Hz | `BandHistoryDisplayTest.oneHertzActivityDayStaysBounded` |
| Lesefehler | `FitHistoryViewTest` (A6-29), Tagesdialog: `dd_band_failed` |

Nicht durch eigene Tests abgedeckt: Messzeit gegen
Importzeit über die Band-Spalte `received_at_ms` (die Oberfläche verwendet nur `measured_at_ms`).
