# Garmin-Connect-Benchmark (#261): Lückenmatrix, Mockups, Subtasks

Stand: 2026-11-20 · Ticket: [#261](https://github.com/jochenkunkel/pulselibre/issues/261) · Umfang: **Analyse und Entwurf, kein Android-Code**.
Datenschutz: Die Garmin-Screenshots des Tickets bleiben lokal und sind nicht im Repo. Die Mockups unter [`design/mockups/benchmark-261/`](../../design/mockups/benchmark-261/) sind eigenständig und enthalten **ausschließlich synthetische Werte**. Es wird keine Garmin-Oberfläche kopiert.

## 1. Evidenzbasis (aus dem Code, nicht aus Vermutung)

- **Rohdaten:** FIT-Decoder [`FitHistoricalObservations.kt`](../../app/src/main/java/de/pulselibre/FitHistoricalObservations.kt) bildet nur fünf Metriken ab: `HEART_RATE`, `CADENCE`, `DISTANCE`, `SPEED`, `POWER` (Record 20; Monitoring 55 Feld 27 = Herzfrequenz). Kumulierte Schritte (55/9) sind dekodiert, laut [HISTORICAL_DATA_SCOPE](../HISTORICAL_DATA_SCOPE.md) aber nicht als Import belegt.
- **Nicht als Gerätefeld nachgewiesen:** Schlafbeginn/-phasen, Sleep Score, HRV, Ruhepuls, Stress, Body Battery, Kalorien, Intensitätsminuten (siehe ebenda, Abschnitt „Feldinventur“; Issues #141, #150, #204).
- **Speicher:** `observations(device_id, metric, measured_at_ms, value, unit, sample_id)` mit nativem Zeitstempel in ms ([`CirqaDatabase.kt`](../../app/src/main/java/de/pulselibre/CirqaDatabase.kt)); Live-Werte (u. a. Schritte, Schrittziel, Schlafminuten) über `DashboardMeasurement`.
- **Grafikauflösung ≠ Sensorauflösung:** Die Abtastintervalle der Quellen sind noch nicht je Datei inventarisiert (Inventur in #141/#150). Tagesschritte werden heute in 15-Minuten-Intervalle aus dem kumulierten Zähler zerlegt ([`StepIntervals.kt`](../../app/src/main/java/de/pulselibre/StepIntervals.kt)); Beobachtungen mit mehr als 10 min Abstand erzeugen eine Lücke bzw. einen deutlich gekennzeichneten Näherungsbalken, nie eine erfundene Uhrzeit.

## 2. Lückenmatrix

Legende: **vorhanden** · **teilweise** · **fehlt** · **Quelle unbekannt** (Darstellung nur nach belegter Quelle).

| Funktion (Garmin-Referenz) | Status in PulsLibre | Beleg | Konsequenz |
|---|---|---|---|
| Geräteseite mit Verbindung/Akku | vorhanden | Gerät-Reiter, Akku-Kachel (#159/#242) | Alarme/Trainings/Aktivitätsoptionen: **nur Discovery**, keine Schreibkommandos |
| Intra-Day-Kurve Herzfrequenz | teilweise | Verlauf 24 h/7 T/30 T mit Punkten, Verteilung, Farbsicher (#219/#248) | Zoom/Pan mit Rohpunkten, Peaks erhalten, Lücken sichtbar fehlen |
| Gemeinsame 24-h-Zeitachse mit Ereignissen | fehlt | keine Ansicht, die HF, Schritte, Schlaf, Aktivität auf einer Achse zeigt | Subtask A, Mockup `24h-unified-timeline.svg` |
| Schritte, Tagessumme | vorhanden | Übersicht, `DashboardMeasurement` | – |
| Schritte, Verlauf über den Tag | teilweise | 15-min-Balken (`StepIntervals`), Aktivitäts-Atlas (#41) | Treppenkurve kumuliert + Ziellinie ergänzen (Subtask C) |
| Schrittziel | teilweise | Ziel kommt als Gerätewert (`GOAL`), kein lokales Ziel | Lokal konfigurierbares Ziel + Erreichungsmarker (Subtask C) |
| Schlafdauer | teilweise | `sleepMinutes` im Dashboard-Snapshot | Zeitspanne nur mit belegter Quelle |
| Schlafphasen, Sleep Score | **Quelle unbekannt** | nicht als Felder nachgewiesen (#141/#150) | „nicht verfügbar“ mit Grund; keine Schätzung |
| Body Battery | **Quelle unbekannt**, proprietär | – | Nicht nachbilden (Ticketvorgabe) |
| Stress, Intensitätsminuten, Kalorien | **Quelle unbekannt** | – | Ausgeblendet/erklärter Leerzustand |
| Ruhepuls | teilweise | Ruhepuls-Hero und Trend in Analysen (#41/#227) | Methode dort dokumentiert; kein Tagesminimum-Ersatz |
| 7-Tage-Zusammenfassung | teilweise | Analysen-Trend-Block (7/28/90 T) | Wochenkarte mit Ø nur über Tage mit Daten und Abdeckung (Subtask D) |
| Typisierte Aktivitäten (Spaziergänge, Training) | **Quelle unbekannt** | FIT-Sessions/Laps nicht inventarisiert | Erst nach Inventur (#141) |
| Anpassbare Startseite | fehlt | Übersicht hat feste Kachelreihenfolge | Ein-/Ausblenden, Reihenfolge, Favoriten lokal (Subtask E) |
| Tooltip/Crosshair mit Zeit, Wert, Einheit, Quelle | teilweise | Detaildialog der Verteilung (#219); keine Crosshair-Geste | Subtask B |
| Textfassung der Kurve (TalkBack) | teilweise | Beschreibungen in Analysen/Verteilung | Je Chart (Subtask B) |
| Null vs. fehlend | vorhanden | `StepIntervals`, Verteilung (leere Fenster transparent) | Als Regel für neue Charts festschreiben |

## 3. Mockups (synthetisch)

- [`24h-unified-timeline.svg`](../../design/mockups/benchmark-261/24h-unified-timeline.svg): gemeinsame Zeitachse, Lücke, Crosshair mit Quelle, Ziellinie, Schlaf-/Aktivitätsband mit Text + Muster, Textfassung.
- [`sleep-detail.svg`](../../design/mockups/benchmark-261/sleep-detail.svg): Dauer und Zeitspanne getrennt von Messwerten der Nacht; Score/Phasen/Body Battery als „nicht verfügbar“ mit Grund.
- [`steps-goal-week.svg`](../../design/mockups/benchmark-261/steps-goal-week.svg): Schrittziel, unsichere Zeitpunkte gestrichelt, Wochenkarte mit Abdeckung je Zeile.

## 4. Priorisierte Subtasks (Umsetzung erst danach)

| Prio | Subtask | Abhängigkeiten / Abgrenzung |
|---|---|---|
| 1 | **A: 24-h-Detailansicht** (HF + Schritte + Schlafspanne + Lücken, DST-sicher, Zeitzone je Tag) | nutzt Verlaufs- und Dashboard-Reader; nicht #219/#227 duplizieren |
| 2 | **B: Zoom/Pan, Crosshair, Downsampling** (min/max/first/last je Pixelspalte, Lücken bleiben) und Textzusammenfassung | reine Darstellung, keine BLE-Wakeups |
| 3 | **C: Lokales Schrittziel + kumulierte Tageskurve** | nur aus Zählern mit ≤ 10 min Abstand (`StepIntervals`) |
| 4 | **D: Wochenkarte (7 T)** mit Ø nur über Tage mit Daten + Abdeckung | Ruhepuls aus Analysen-Pipeline (#41), kein Tagesminimum |
| 5 | **E: Startseite anpassen** (Ein-/Ausblenden, Reihenfolge, Favoriten, lokal gespeichert) | Reiter Analysen bleibt für Detailansichten |
| – | **F: Quellinventur** Kalorien/Intensität/Stress/Schlaf/Aktivitäts-Sessions | Voraussetzung für weitere Metriken; gehört zu #141/#150 |
| – | **G: Gerätefunktionen Discovery** (Alarme/Trainings) | nur Dokumentation; keine Schreibkommandos ohne Freigabe |

## 5. Pflichtkriterien für jeden Subtask

- Unit-Tests: Ausreißer, fehlende Daten, DST-Wechsel, nachträglicher Import, unterschiedliche Abtastraten, sehr große Datensätze, leere DB.
- Keine permanente BLE-Verbindung durch Chart-Interaktion (#171/#235 nicht verschlechtern).
- Dark/Light, 320 dp, 200 % Schrift, TalkBack; Information nie nur über Farbe.
- Emulator-/CI-Abnahme und S25-Abnahme getrennt dokumentieren; Screenshots mit echten Werten bleiben lokal.
