# PulsLibre – wissenschaftlicher Dashboard-Kachelbauplan

**Version:** 1.0 · **Stand:** 2026-11-17 · **Status:** Forschungs- und Implementierungsplan, **keine** Freigabe medizinischer Algorithmen oder Garmin-CIRQA-Historienfunktionen.

**Zweck:** Verbindliche fachliche Spezifikation für einzelne Dashboard-Kacheln: Herkunft, Formel, Kontext, Bereich, Farbsemantik, Fehlwerte, Drilldown, Tests und Quellen. Die Umsetzung erfolgt durch den Dashboard-/Wissenschaftsagenten in Abstimmung mit `AGENTS.md`; der BLE-/GFDI-/Historientransport bleibt unberührt.

## 0. Grundprinzip: wissenschaftlich statt dekorativ

Eine „gut / mittel / schlecht“-Ampel ist **nur** dort zulässig, wo eine belastbare, auf die Zielgruppe und Messmethode anwendbare Schwelle existiert. Für die meisten Wearable-Metriken ist das **nicht** der Fall. Daher vier ausdrücklich verschiedene Darstellungsarten:

- **Zielerreichung** (z. B. 0–49 / 50–99 / ≥100 % des *selbst gewählten* Schrittziels): Produktfortschritt, **keine** Gesundheitsbewertung. Diese Prozentbänder sind **PulseLibre-UI-Heuristik**, keine Literaturgrenzen.
- **Population/Referenz** (z. B. Ruhepuls 60–100 bpm bei Erwachsenen): neutrale Kontextmarkierung, keine individuelle Diagnose.
- **Persönlicher Verlauf** (z. B. Veränderung gegenüber der eigenen 28-Tage-Baseline): „niedriger / üblich / höher“, **nicht** automatisch gut/mittel/schlecht. Individuelle Quantilgrenzen sind Statistik/Produktheuristik und keine klinischen Cutoffs.
- **Datenqualität** (vollständig / eingeschränkt / unzureichend): darf eine echte Qualitätsampel tragen, weil die Grenzen als transparentes *internes Prüfverfahren* definiert und getestet werden.

Alle Kacheln zeigen auf Abruf: **Wert + Einheit + Zeitbezug + Quelle/Service + Mess-/Empfangszeit + Datenqualität + wissenschaftliche Aussagegrenze**. Kein Wert wird aus fehlenden Daten geschätzt, ohne als Schätzung ausgewiesen zu sein. Ein unbestätigter Service ergibt „Noch keine validierten Daten“.

## 1. Messwertinventar des aktuellen Repositories

**Technischer Ausgangspunkt:** `app/src/main/java/de/pulselibre/GarminRealtime.kt`, `docs/protocol/CIRQA_PROTOCOL_SPEC.md`, `docs/protocol/data-model/CIRQA_DASHBOARD_GROUND_RULES.md`, `docs/DASHBOARD_IMPLEMENTATION.md`.

| Rohsignal | Garmin-ML-Service | Decoder-Feld/Einheit | CIRQA-Evidenz | Wichtige Einschränkung |
| --- | --- | --- | --- | --- |
| Herzfrequenz | 6 | `bpm`, 1–240 | **Am Gerät beobachtet** | Zeitpunkt = Telefonempfang, nicht bewiesener Messzeitpunkt |
| Ruhepuls (vom Gerät) | 6 | `restingBpm`, 1–240 oder unbekannt | HR-Service bestätigt; Feld-/Berechnungssemantik separat prüfen | Kein selbst errechneter Ruhepuls |
| Schritte | 7 | `count`, kumulativer Tageszähler, u32 | **Am Gerät beobachtet** | Tageswechsel, Resets, Zonenwechsel |
| Schrittziel | 7 | `goal`, u32 | **Am Gerät beobachtet** | Geräte-/Nutzerziel, keine medizinische Schwelle |
| Kalorien gesamt/aktiv | 8 | zwei u32-Rohwerte | Drittcode-Kandidat | Einheit, Skalierung, Akkumulationszeit **unbestätigt** |
| Intensitätsminuten moderat/intensiv | 10 | zwei u16-Werte | Drittcode-Kandidat | Zählfenster/Definition unbestätigt |
| RR-Intervall | 12 | `rrMs`, u16 | Drittcode-Kandidat | Einzelnes RR ≠ HRV; Beat-to-beat-Kontinuität fehlt |
| Stressindex | 13 | Geräteindex 0–100 | Drittcode-Kandidat | Proprietäre Skala, keine validierte Stressdiagnose |
| SpO₂ | 19 | 1–100 % | Drittcode-Kandidat | Genauigkeit am Handgelenk nicht belegt |
| Body Battery | 20 | Geräteindex 0–100 | Drittcode-Kandidat | Proprietär, keine klinische Energiemessung |
| Atemfrequenz | 21 | 1–100 Atemzüge/min | Drittcode-Kandidat | Ruhe-/Bewegungskontext fehlt |
| Rohbeschleunigung | 16 (Protokollkandidat) | gepackte Samples | **Nicht implementiert, nicht CIRQA-bestätigt** | Keine Roh-Aktigraphie behaupten |
| Schlaf, FIT-Monitoring, HRV-Status, Aktivitätsdateien | GFDI/FIT-Dateitypen, *falls verfügbar* | historisch, Schema offen | **Kein validierter CIRQA-Historienimport** | Dateityp garantiert weder Datei noch Feld |

**Widerspruchsregel:** Ältere Dashboard-Notizen und reale S25-Nutzerbeobachtungen können voneinander abweichen. Nur im aktuellen Datenmodell gespeicherte, gültige, rückverfolgbare Ereignisse sind als Datenquelle zulässig; die Bestätigung einzelner Services muss durch CIRQA-Tests erfolgen.

## 2. Kachelvertrag – für jede Kachel verpflichtend

`TileSpec(id, title, category, sourceIds, evidenceLevel, rawOrDerived, unit, temporalWindow, eligibility, formulaVersion, thresholdsType, thresholdsSource, context, freshness, coverage, quality, trend, explanation, drilldown, accessibilityText, tests)`.

**Datenstatus:** `VERIFIED_DEVICE` / `THIRD_PARTY_PROTOCOL` / `EXPERIMENTAL` / `NOT_AVAILABLE`. **Analysequalität:** `SUFFICIENT` / `LIMITED` / `INSUFFICIENT`. **Anzeige:** gemessen / vom Gerät berechnet / von PulseLibre berechnet / hypothetisch. Ein proprietärer Herstellerwert wird **nie** als eigener wissenschaftlich validierter Score ausgegeben.

**Gemeinsame Vorbedingungen:** validierte Einheit; UTC-Zeit und Gerätezeit getrennt; Herkunft/Service; monoton sortierte, deduplizierte Zeitreihe; fehlende Abschnitte als Lücken; keine lineare Verbindung über Ausfälle; lokale Zeitzone bei Tages-/Wochenwerten; Zukunftszeit, Resets und Zählerüberläufe markieren; Filterparameter versionieren. Die vorhandenen Empfangsalterfarben (≤3, ≤15, ≤60, >60 min) bedeuten **nur Aktualität**, niemals physiologische Gesundheit.

### Kategorie A – Herz und Kreislauf

#### H01 · Live-Puls / Tachometer — **P0, jetzt darstellbar**
- **Quelle:** Service 6, `bpm`; **Einheit:** bpm; **Zeit:** letzter Empfang.
- **Berechnung:** keine; nur gültigen Originalwert anzeigen. Tacho 40–180 bpm ist eine **Anzeigeskala**, keine Norm. Werte außerhalb nicht abschneiden, sondern numerisch erhalten und Skalenüberlauf kennzeichnen.
- **Gut/mittel/schlecht:** **nicht zulässig** ohne Ruhe-/Belastungskontext, individuelle Voraussetzungen und validierte Messung. Optionale neutrale Referenz *Ruhe* 60–100 bpm für viele Erwachsene, **nicht** während Aktivität und **nicht** als Ampel.
- **Visualisierung:** kompakter Tacho, Sparkline nur bei ausreichend echter Zeitreihe, Empfangsalterpunkt, Kontext „unbekannt“ möglich; Detail: 24 h/7 d/30 d als Einzelpunkte mit Lücken.
- **Tests:** fehlender Puls, 1/240, alte Werte, Out-of-range-Tacho, Modus unbekannt, TalkBack.

#### H02 · Geräte-Ruhepuls & persönlicher Trend — **P1, nach Verlaufsprüfung**
- **Quelle:** Service 6 `restingBpm`; *keine* Schätzung aus beliebigen Live-Pulsen.
- **Berechnung:** je lokalem Tag letzter eindeutig datierter, gültiger Geräte-Ruhepuls; danach z. B. Median der letzten 7 verfügbaren Tage und Vergleich zur **vorherigen** 28-Tage-Baseline (Überlappung vermeiden). `Δ = median_7d − median_baseline`, bpm.
- **Gut/mittel/schlecht:** **kein** generelles „je niedriger, desto besser“. Neutraler Populationskontext 60–100 bpm (erwachsene Ruhe); individuelle Abweichung „unter/innerhalb/über persönlichem Verlauf“ nur mit dokumentierter Statistik. Vorschlag: Baseline ≥14 gültige Tage aus 28, aktuelle Woche ≥4/7 Tage; **interne Mindestdaten-Heuristik, nicht wissenschaftlicher Grenzwert**.
- **Visualisierung:** Hauptwert, 7-/28-Tage-Median, Mini-Verlauf, Herkunft „Garmin-Gerätewert“.
- **Tests:** unvollständige Wochen, Zeitzonenwechsel, Baseline ohne aktuelle Tage, gleiche Werte, Ausreißer.

#### H03 · Herzfrequenz während Aktivität — **P2, Kontext nötig**
- **Quelle:** Puls + validierte Bewegungs-/Aktivitätssegmente; Schrittfrequenz allein ist kein Sportbeweis.
- **Berechnung:** medianer/zeitgewichteter Puls **nur** innerhalb beobachteter Aktivitätsfenster; Zeitgewichtung ausschließlich bei begrenzten, validierten Abständen, sonst Punktstatistik. Keine „Kalorien aus Puls“-Schätzung.
- **Bereiche:** **nicht allgemein** gut/mittel/schlecht. Optional separat **geschätzte** maximale Herzfrequenz `HRmax = 208 − 0.7 × Alter` für *gesunde Erwachsene* (Tanaka et al., 2001), ausdrücklich populationsbasierte Schätzung mit großer individueller Streuung, keine sichere Belastungsgrenze. Trainingszonen erst nach wissenschaftlicher Auswahl/Validierung gemäß Issue #11.
- **Visualisierung:** Puls über Schrittfrequenz, Belastungsfenster markiert, Legende „geschätzt“; keine automatische Sportfreigabe.

#### H04 · RR-Rohintervall — **P1 bei verifiziertem Service**
- **Quelle:** Service 12, Einzel-RR in ms. **Keine** Umrechnung eines einzelnen RR in RMSSD, SDNN, „Stress“ oder EKG-Diagnose.
- **Bereiche:** keine Ampel, nur plausibler Decoderbereich und Empfangsqualität.
- **Visualisierung:** RR-Messwert, Qualitäts-/Kontinuitätshinweis, Erklärung „kein HRV-Score“.

#### H05 · HRV: RMSSD, SDNN, pNN50 — **P3, streng gesperrt**
- **Benötigt:** echte **zeitlich zusammenhängende** Folge validierter normal-to-normal (NN)-Intervalle, Artefakt-/Ektopie-Erkennung, dokumentierte Abtastrate, Messdauer und Messkontext; PPG-RR nicht ungeprüft als EKG-NN behandeln.
- **Formeln:** `RMSSD = sqrt(Σ(NN[i+1] − NN[i])² / (N−1))`; `SDNN = sample_sd(NN)`; `pNN50 = 100 × count(|ΔNN|>50 ms)/(N−1)`.
- **Bereiche:** **keine universellen gut/mittel/schlecht-Grenzen**; Messdauer, Alter, Atmung, Haltung, Tageszeit und Methodik beeinflussen Werte. 1996er Referenztabelle ist kein individueller Freigabeschwellwert.
- **Visualisierung:** Messkontext, 5-Minuten-Standardfenster *nur wenn tatsächlich vorhanden*, Qualitätsbericht und persönlicher Verlauf; **nicht implementieren**, solange nur isolierte RR-Snapshots vorliegen.
- **Tests:** RR-Artefakte, fehlende Beats, Ektopie, unterschiedliche Messdauer, N<2, Überläufe.

### Kategorie B – Bewegung und Aktivität

#### A01 · Schritte heute — **P0, jetzt darstellbar**
- **Quelle:** Service 7 kumulativer Zähler; **Formel:** letzter gültiger Tagesstand, **nicht** Summe aller Snapshots. Bei Rücksprung/Reset Segment kennzeichnen.
- **Gut/mittel/schlecht:** optional *Zielstatus* `<50 % / 50–99 % / ≥100 %` (UI-Heuristik, **keine** Gesundheitsbewertung). Der **wissenschaftliche Kontext**: Ding et al. 2025 fanden in Beobachtungsdaten bei ~7.000 Schritten/Tag gegenüber 2.000 eine Assoziation mit niedrigeren Risiken; **keine** individuelle Garantie oder Kausalbehauptung, kein universelles Optimum.
- **Visualisierung:** Tageszahl, Geräte-/persönliches Ziel, 7-Tage-Kalender und Tageslücken. Kein „schlechter Mensch“-Rot bei geringer Aktivität.
- **Tests:** Tageswechsel, Zählerreset, verspätete Daten, mehrere Empfangspunkte, fehlendes Ziel.

#### A02 · Schrittfrequenz / Geh-Intensität — **P1, bei dichten Daten**
- **Formel:** `cadence = (steps_end − steps_start) / elapsed_minutes`, nur gleicher lokaler Tag, monotone Zähler, zwei gültige Punkte, keine große Lücke.
- **Bereiche:** ~100 Schritte/min ≈ 3 MET und ~130 ≈ 6 MET in untersuchten Erwachsenen (CADENCE-Adults, 41–60 Jahre, Laufband). **Keine universelle** Ruhe-/Sportklassifikation. Bestehende <10/10–100/>100-Schritt-Modi sind **PulseLibre-Heuristik**, nicht klinische Norm.
- **Visualisierung:** Kadenz-Zeitreihe mit Unsicherheits-/Abdeckungslabel; Modus „unbekannt“ bei unzureichender Datenlage.
- **Tests:** Zählerreset, 0 Schritte, Zeitsprünge, >3 min Lücke, Mitternacht, 30-s-Mindestfenster.

#### A03 · Aktivität über die Woche — **P2, nach Zeitfensterklärung**
- **Quelle:** Service 10 moderat/intensiv; nur wenn Akkumulationsfenster und Minuten-Einheit am CIRQA bestätigt sind.
- **Formel:** nach validierter Wochenlogik moderate Minuten plus ggf. `2 × intensive Minuten` als *moderate Äquivalenzminuten* (WHO-äquivalente Kombination; nicht mit Garmin-eigenem Algorithmus verwechseln). **Keine** Addition wiederholter kumulativer Snapshots.
- **Bereiche:** WHO 2020: Erwachsene **150–300 min moderat/Woche** oder **75–150 min intensiv/Woche**, plus Muskelkräftigung an ≥2 Tagen. Anzeige `<150` = „unter Mindestempfehlung“, `150–300` = „im empfohlenen Bereich“, `>300` = „darüber“ **nur für tatsächlich äquivalente und vollständige Wochenwerte**. Nicht als medizinisch „schlecht/mittel/gut“ bezeichnen.
- **Visualisierung:** Wochenbalken, Datenabdeckung, explizite Trennung moderat/intensiv, Kontextkarte.

#### A04 · Aktivitätskonsistenz — **P2**
- **Quelle:** mindestens 14–28 Tage vollständige Tages-Schrittstände; sonst nur vorhandene Tage zeigen.
- **Formel:** `active_days = count(days ≥ user_goal)`; `coverage = valid_days / days_in_window`; optional Median/Interquartilsabstand der Tageswerte. „Aktiver Tag“ nach *Nutzerziel*, nicht klinisch.
- **Bereiche:** `0–2 / 3–4 / 5–7` Tage als **konfigurierbare Motivationsdarstellung** (PulseLibre-Heuristik), niemals wissenschaftliche Gesundheitsklassifikation.
- **Visualisierung:** Kalender-Heatmap mit fehlenden Tagen **grau**, nicht null/rot; Wochenvergleich nur bei ausreichender Abdeckung.

#### A05 · Bewegungsunterbrechungen — **P3**
- **Benötigt:** kontinuierliche, validierte Bewegungs-/Inaktivitätsintervalle, idealerweise Rohbeschleunigung; sporadische kumulative Schrittstände reichen nicht.
- **Formel:** Dauer zusammenhängender als inaktiv klassifizierter Fenster; Schwellen müssen mit Sensorvalidierung und Fachliteratur begründet werden.
- **Bereiche:** keine allgemeingültige „Sitzzeit“ aus fehlenden Schritten; **nicht freigeben**, bis Aktigraphie validiert ist.
- **Visualisierung:** Tagesstreifen, Unbekannt-Anteile, optionale sanfte Bewegungserinnerung, keine Diagnose.

### Kategorie C – Sauerstoff und Atmung

#### O01 · SpO₂ — **P1, wenn CIRQA-Wert verifiziert**
- **Quelle:** Service 19; 1–100 % (Decodergültigkeit ist **keine** physiologische Plausibilität).
- **Bereiche:** 95–100 % als **neutrale** typische Orientierung aus NHS-Fingerpulsoximeter-Patienteninformation für viele Erwachsene in Ruhe; **nicht** auf CIRQA-Handgelenk als validierte Alarm-/Diagnosegrenze übertragen. Für 90–94 % und <90 % **keine** automatische medizinische „gelb/rot“-Klassifikation ohne validiertes Messgerät, klinischen Kontext und Sicherheitsprüfung.
- **Visualisierung:** Wert, Signalqualität soweit vorhanden, Verlauf ohne interpolierte Lücken, Warntext zu Bewegung, Durchblutung, Hautpigmentierung und Sensorfehlern.
- **Tests:** 0, 255, Grenzwerte, falsche Datenquelle, alte/isolierte Messung, fehlende Validierung.

#### O02 · Atemfrequenz in Ruhe — **P2**
- **Quelle:** Service 21 + **gesichert** ruhiger Kontext (nicht allein fehlende Schritte).
- **Bereiche:** ca. **12–20 Atemzüge/min** bei Erwachsenen in Ruhe als neutraler MedlinePlus-Kontext; während Sport, Schlaf, Krankheit oder bei Kindern **nicht** anwenden.
- **Visualisierung:** Wert und persönlicher Ruheverlauf, Messkontext sichtbar; ohne Kontext nur Rohwert ohne Normband.
- **Tests:** Ruhekontext fehlt, gemischte Aktivitäten, Ausreißer, alte Daten.

#### O03 · Nächtliche Sauerstoff-/Atemtrends — **P3, Historie nötig**
- **Benötigt:** validierte Schlafintervalle + dichte SpO₂-/Atemdaten, Sensorgüte.
- **Berechnung:** zeitliche Verteilung/Median, beobachtete Minima mit Artefaktregeln und Datenabdeckung; **keine** Schlafapnoe-Diagnose, kein ODI/AHI ohne geeignete Messvalidierung.
- **Bereiche:** kein pauschales Ampelsystem; nur kontextgebundene Trends.
- **Visualisierung:** Nachtkurve mit Lücken und Qualitätsabdeckung.

### Kategorie D – Stress, Energie, Erholung

#### R01 · Garmin-Stressindex — **P1 nach CIRQA-Verifikation**
- **Quelle:** Service 13, proprietärer 0–100-Index.
- **Bereiche:** `0–100` ist **Skala**, **keine** unabhängige wissenschaftliche Gesundheitsnorm. Hersteller-Kategorien dürfen nur als ausdrücklich **Garmin-definierte Gerätekategorien** nach überprüfter offizieller Dokumentation erscheinen; sonst keine gut/mittel/schlecht-Ampel.
- **Visualisierung:** Herstellerwert, zeitlicher Verlauf, „Geräteindex, kein klinischer Stresswert“; keine Diagnose aus Einzelwerten.

#### R02 · Body Battery — **P1 nach CIRQA-Verifikation**
- **Quelle:** Service 20, proprietärer 0–100-Index.
- **Bereiche:** 0–100 als **Geräteskala**, nicht medizinische Energie/Reserven. Keine erfundenen Gewichtungen aus HRV/Schlaf/Stress, um einen „eigenen Body Battery“-Score vorzutäuschen.
- **Visualisierung:** Tankanzeige, Verlauf, Quelle/Algorithmus nicht öffentlich verifiziert.

#### R03 · Persönliche Erholungstrends — **P3, Forschungsfunktion**
- **Benötigt:** belastbare Ruhepuls-, HRV- und Schlafzeitreihen mit gleicher Messmethodik.
- **Berechnung:** *getrennte* Trends und Veränderungen zu persönlicher Baseline; **kein** aggregierter Recovery-/Readiness-Score ohne unabhängige Validierung der Gewichte und Vorhersageleistung.
- **Bereiche:** „unter/typisch/über eigener Baseline“, keine „fit/unfit“-Diagnose. Für statistische Quantilbänder mindestens ausreichende Datenabdeckung und stabile Messbedingungen nach dokumentiertem Prüfplan.
- **Visualisierung:** drei getrennte Trendkarten, Transparenz zur Datenlage, erklärbare Faktoren.

### Kategorie E – Schlaf und Rhythmus

#### S01 · Schlafdauer — **P3, Historienimport + Validierung**
- **Benötigt:** tatsächlich vorhandene CIRQA-FIT-Schlafintervalle mit bestätigter Semantik oder validierte Schlafdetektion; Schrittstillstand **nicht** als Schlaf zählen.
- **Formel:** `sleep_duration = Σ(validated_sleep_intervals)`, über Mitternacht/Zeitzonen korrekt; Zeit im Bett separat.
- **Bereiche:** Erwachsene 18–60: **≥7 h/Nacht** als populationsbasierte Empfehlung (CDC/AASM); obere Grenze und Alter differenzieren. UI `<7 h` „unter Orientierung“, `≥7 h` „Orientierung erreicht“ – **keine** automatische Schlafqualitätsdiagnose. Für ältere Altersgruppen Empfehlungen separat prüfen.
- **Visualisierung:** Nachtbalken, Schlaf-/Bettzeit getrennt, Lücken und Schätzstatus.

#### S02 · Schlafregelmäßigkeit — **P3**
- **Benötigt:** ≥14 Nächte mit validiertem Schlafbeginn/-ende.
- **Formel:** Streuung der Schlafmitte oder Schlafbeginnzeit mit korrekter zyklischer Zeitrechnung; optional Wochenend-/Werktagsverschiebung („social jetlag“) als **deskriptive** Größe.
- **Bereiche:** keine universellen gesund/ungesund-Cutoffs ohne populations- und altersbezogene Validierung; nur persönlicher Trend.
- **Visualisierung:** Kalender-/Uhrzeitplot, Abdeckung.

#### S03 · Schlafstadien / Schlafscore — **P4, vorerst gesperrt**
- **Benötigt:** CIRQA-bestätigte FIT-Stadien *oder* gegen Polysomnographie validierte Klassifikation. Consumer-Wearables sind kein Ersatz für Schlafdiagnostik.
- **Bereiche:** keine REM-/Tiefschlaf-Prozentampel und kein selbst erfundener „Sleep Score“.
- **Visualisierung:** erst nach Validierung, mit klarer Kennzeichnung „Geräteschätzung“.

### Kategorie F – Energie, Ernährung und Kontext

#### N01 · Geräte-Kalorien — **P2 nach Einheitenprüfung**
- **Quelle:** Service 8, `total`/`active` Rohwerte; erst als **kcal** ausweisen, wenn CIRQA-Einheit, Skala, Zeitfenster und Semantik nachgewiesen sind.
- **Berechnung:** `rest = total − active` **nur** falls das Geräteprotokoll additive Komponenten tatsächlich bestätigt; sonst nicht rechnen.
- **Bereiche:** **keine** „weniger Kalorien = besser“-Ampel; Energiebedarf ist individuell und vom Ziel abhängig.
- **Visualisierung:** zwei getrennte Werte, Quellenhinweis, Tagesverlauf nur bei validierter Kumulation.

#### N02 · Energie-Bilanz — **P4, optional mit manuellem Ernährungstagebuch**
- **Benötigt:** valide Ernährungseingaben, geeignete individuelle Energiebedarfs-/Verbrauchsmethode und explizite Unsicherheiten; Wearable-Kalorien alleine reichen nicht.
- **Berechnung:** `intake − expenditure` nur bei vergleichbaren kcal-Einheiten und Zeiträumen; keine klinischen Gewichtsprognosen aus einer Tagesbilanz.
- **Bereiche:** **nicht** universell gut/mittel/schlecht; Ziel und medizinischer Kontext entscheidend.
- **Visualisierung:** Eingaben, Unsicherheitsband, fehlende Mahlzeiten sichtbar, keine moralischen Bewertungen.

### Kategorie G – die moderne Meta-Ebene

#### Q01 · Datenvertrauen / Messabdeckung — **P0, unabhängig von neuen Sensoren**
- **Quelle:** lokale Datenbank, bestätigte Quellen, Zeitstempel, bekannte Lücken und Decoder-/Qualitätsflags.
- **Formeln:** `day_coverage = observed_valid_bins / expected_bins` **nur bei definierter erwarteter Messkadenz**; `validity = valid_events / received_events`; `freshness = now − received_at`. Ohne bekannte Kadenz **keine** erfundene Prozentabdeckung, stattdessen „Kadenz unbekannt“.
- **Gut/mittel/schlecht:** echte **technische Qualitätsklassifikation** nach *offengelegten Produktgrenzen*, z. B. „vollständig / eingeschränkt / nicht auswertbar“ anhand je Algorithmus definiertem Mindestdatensatz; niemals physiologischer Gesundheitszustand.
- **Visualisierung:** kleine Vertrauensmarke an jeder Kachel, eigene Diagnoseseite „Warum fehlt dieser Wert?“.
- **Tests:** fehlende Zeitstempel, Empfang ohne Messung, Duplikate, unklare Kadenz, Teilabdeckung.

#### Q02 · Persönliche Wochenzusammenfassung — **P2**
- **Quelle:** ausschließlich bereits validierte Kacheln; keine eigenen unbekannten Rohsignale.
- **Berechnung:** regelbasierte, **erklärbare** Aussagen: „An 5 von 7 Tagen liegen gültige Schrittwerte vor“, „Ruhepuls an 4 Tagen erfasst“, „Schrittziel an 3 Tagen erreicht“. Kein generischer „Gesundheitsscore“.
- **Bereiche:** Datenlage und persönliche Zielerreichung, **keine** medizinische Gesamtbewertung.
- **Visualisierung:** maximal drei konkrete Erkenntnisse, Evidenz-Button, kein Alarmismus.

## 3. Wertebereiche: kompakte verbindliche Matrix

| Kennzahl | Wissenschaftlich/technisch begründeter Bereich | Erlaubte Beschriftung | „Gut / mittel / schlecht“? |
| --- | --- | --- | --- |
| Live-HR | 1–240 bpm = **Decoder**; 60–100 bpm = allgemeiner Erwachsenen-*Ruhe*-Kontext | „Messwert“, „Ruhe-Orientierung“ | **Nein** |
| Geräte-Ruhepuls | 60–100 bpm häufige Erwachsenen-Ruhe-Orientierung | „unter / innerhalb / über Orientierung“, mit Kontext | **Nein** |
| Schrittzahl | ~7.000/Tag = epidemiologisch relevanter Orientierungswert, kein individuelles Minimum | „Schritte“, „Zielerreichung“, „Studienkontext“ | Nur **Zielstatus**, nicht Gesundheit |
| Schrittfrequenz | ~100/130 Schritte/min in CADENCE-Adults mit Alters-/Laufbandkontext | „mögliche moderate/intensive Gehbelastung“ | **Nein** |
| Aktivitätsminuten | WHO 150–300 moderat oder 75–150 intensiv/Woche | „unter / im / über empfohlenen Umfang“, **nur bei validiertem Wochenwert** | Kein „schlecht/gut“ |
| SpO₂ | 95–100 % Orientierung für Fingerpulsoximeter in Ruhe; **keine CIRQA-Validierung** | „nicht diagnostisch“, neutraler Kontext | **Nein** |
| Atemfrequenz | 12–20/min bei Erwachsenen **in Ruhe** | „Ruhe-Orientierung“ | **Nein** |
| Einzel-RR | >0 ms = Parserkandidat, nicht HRV | „RR-Intervall“ | **Nein** |
| RMSSD/SDNN | Methodenspezifische, keine universellen klinischen Schwellen | „persönlicher Verlauf“, erst nach Validierung | **Nein** |
| Stress / Body Battery | 0–100 proprietäre Geräteskala | „Garmin-Geräteindex“ | **Nein** |
| Schlafdauer | Erwachsene 18–60: ≥7 h/Nacht Orientierung, erst nach Schlafvalidierung | „unter / erreicht Orientierung“ | Kein Qualitätsurteil |
| Kalorien | Einheit, Fenster, Validität derzeit unbekannt | „Rohwert“ oder „keine validierten Daten“ | **Nein** |
| Datenqualität | Algorithmusspezifische Coverage-/Validitätsregeln | „auswertbar / eingeschränkt / nicht auswertbar“ | **Ja, nur technisch** |

**Farbregel:** Grün/Gelb/Rot dürfen nicht implizit medizinische Sicherheit/Erkrankung bedeuten. Gesundheitswerte neutral/kontextuell einfärben; getrennte Farbsysteme für *Aktualität*, *Zielerreichung* und *Datenqualität*; jeweils mit Textlegende, nicht allein Farbe.

## 4. Moderne Interaktionen – ohne wissenschaftliche Übertreibung

1. **Dashboard-Ebenen:** „Jetzt“ (Puls/SpO₂/Schritte/Empfang), „Tag“ (Schritte, Aktivität, Datenlücken), „Trends“ (Ruhepuls, HRV später, Schlaf später), „Wissenschaft“ (Methoden/Quellen/Unsicherheit).
2. **Kachel-Drilldown:** Tippen öffnet Originalwerte, Zeitreihe, Messbedingungen, Formel, Quellen, Qualitätsprüfung und „Was bedeutet das *nicht*?“.
3. **Wissenschafts-Pass:** Badge „CIRQA-bestätigt“, „Drittanbieter-Protokoll“, „Geräteberechnung“, „experimentell“; Klick erklärt Nachweis.
4. **Erklärbare Wochenkarten:** Kein „Health Score“, sondern prüfbare Aussagen mit Zähler/Nenner, z. B. „5/7 Tage vorhanden“.
5. **Lücken ehrlich zeichnen:** fehlende Daten als Unterbrechungen, nicht als Nullwerte; Historien-Backfill darf historische Ereignisse nachträglich ergänzen, aber nur nach dedupliziertem validiertem Import.
6. **Personalisierung:** Referenzwerte und Baselines alters-/kontextbewusst, **keine** erfundenen geschlechts- oder gewichtsabhängigen Koeffizienten; medizinische Ausnahmen und Medikamente als Ausschluss-/Hinweiskriterien, nicht versteckte Diagnosen.
7. **Barrierefreiheit:** dynamische Schriftgrößen, TalkBack, nicht allein farbliche Semantik, reduzierbare Animation, Hell/Dunkel.
8. **Datenschutz:** offline-first; keine Gesundheitsdaten/Diagnose-Logs ins öffentliche GitHub; lokaler Export nur nach Nutzeraktion.

## 5. Inspiration von anderen Anbietern – **Produktideen, keine Validierungsbelege**

- **Garmin Connect:** Body Battery, Stress, Intensitätsminuten, Schlafansichten – attraktiv wegen zusammenhängender Zeitachsen; proprietäre Formeln **nicht** kopieren oder als wissenschaftliche Wahrheit behandeln.
- **Oura:** persönliche HRV-/Ruhepuls-Baselines, Schlaf-/Readiness-Trends – Inspiration für persönliche Kontextdarstellung, nicht für unvalidierten eigenen Readiness-Score.
- **WHOOP:** Erholungs-/Belastungsnarrative und klare Trenddarstellung – inspirierend, aber kein Beleg, dass CIRQA dieselben Eingangsdaten oder validierte Modelle liefert.
- **Apple Health/Fitness:** konsistente Verlaufskarten, Quellenhinweise und Zielvisualisierung – Inspiration für Zugänglichkeit und Datenprovenienz.
- **Fitbit:** Aktivitäts-/Schlafübersichten und Trends – Inspiration für Wochenvergleiche, nicht für eigene medizinische Grenzwerte.

**Entscheidender Wettbewerbsvorteil von PulsLibre:** *Transparenz statt Blackbox*: Jede Kachel zeigt Datenquelle, Abdeckung, Formel, Evidenzniveau und Grenzen. Keine Anbieter-Markenbegriffe als eigene wissenschaftliche Algorithmen ausgeben.

## 6. Wissenschaftliche Quellen und Zuordnung

Die folgenden Referenzen sind **Belege für die jeweils genannte Aussage**, keine pauschale Validierung des CIRQA oder sämtlicher Kacheln.

- **[R1]** Ding D et al. (2025), *Daily steps and health outcomes in adults: a systematic review and dose-response meta-analysis*, **Lancet Public Health** 10:e668–e681, DOI **10.1016/S2468-2667(25)00164-1**, https://pubmed.ncbi.nlm.nih.gov/40713949/ . **A01:** 7.000-Schritte-Assoziation, Beobachtungsdaten, Residualkonfundierung.
- **[R2]** WHO (2020), *Guidelines on physical activity and sedentary behaviour*, https://www.who.int/publications/i/item/9789240014886 . **A03:** 150–300 moderate bzw. 75–150 intensive Minuten/Woche, Erwachsene; nur bei kompatibler Intensitätsmessung.
- **[R3]** Tudor-Locke C et al. (2020), *Walking cadence (steps/min) and intensity in 41 to 60-year-old adults: CADENCE-adults study*, DOI **10.1186/s12966-020-01045-z**, https://doi.org/10.1186/s12966-020-01045-z . **A02:** ~100/~130 Schritte/min, populations-/protokollabhängig.
- **[R4]** Tanaka H, Monahan KD, Seals DR (2001), *Age-predicted maximal heart rate revisited*, **JACC**, DOI **10.1016/S0735-1097(00)01054-8**, https://pubmed.ncbi.nlm.nih.gov/11153730/ . **H03:** HRmax-Schätzung `208−0.7×Alter`, keine individuelle sichere Trainingsgrenze.
- **[R5]** ESC/NASPE Task Force (1996), *Heart Rate Variability: Standards of Measurement, Physiological Interpretation, and Clinical Use*, **European Heart Journal** 17:354–381, https://www.escardio.org/static-file/Escardio/Guidelines/Scientific-Statements/guidelines-Heart-Rate-Variability-FT-1996.pdf . **H04/H05:** NN-Definitionen, RMSSD, SDNN, pNN50, Messdauer.
- **[R6]** *Guidelines for rigor and reproducibility of heart rate variability within human cardiovascular research* (2026), DOI **10.1152/ajpheart.00041.2026**, https://doi.org/10.1152/ajpheart.00041.2026 . **H05/R03:** Störfaktoren, Reproduzierbarkeit, PPG-/ECG-Kontext.
- **[R7]** Khosla S et al. (2018), *Consumer Sleep Technology: An American Academy of Sleep Medicine Position Statement*, **J Clin Sleep Med** 14:877–880, DOI **10.5664/jcsm.7128**, https://pmc.ncbi.nlm.nih.gov/articles/PMC5940440/ . **S01–S03:** Consumer-Schlafdaten nicht für Diagnose/Therapie.
- **[R8]** FDA, *Pulse Oximeters*, https://www.fda.gov/medical-devices/products-and-medical-procedures/pulse-oximeters . **O01/O03:** Messunsicherheit, Einfluss von Durchblutung, Bewegung und Hautpigmentierung.
- **[R9]** Singh S et al. (2024), *Impact of Skin Pigmentation on Pulse Oximetry Blood Oxygenation and Wearable Pulse Rate Accuracy: Systematic Review and Meta-Analysis*, https://pubmed.ncbi.nlm.nih.gov/39388258/ . **O01:** Fehler und Bias bei Wearables.
- **[R10]** American Heart Association, *Target Heart Rates Chart*, https://www.heart.org/en/healthy-living/exercise-and-physical-activity/fitness-basics/target-heart-rates . **H01/H02:** Ruhepuls-Orientierung.
- **[R11]** MedlinePlus, *Rapid shallow breathing*, https://medlineplus.gov/ency/article/007198.htm . **O02:** Erwachsenen-Atemfrequenz in Ruhe.
- **[R12]** Oxford University Hospitals NHS Foundation Trust, *How to use your pulse oximeter*, https://www.ouh.nhs.uk/redirecthandler/?id=82415Poximeter&type=patientleaflet . **O01:** Finger-Pulsoximeter-Kontext, nicht CIRQA-Validierung.
- **[R13]** CDC, *About Sleep*, https://www.cdc.gov/sleep/about/index.html . **S01:** Schlafdauerempfehlungen nach Altersgruppe; vor Implementierung Seitenfassung und Altersgrenzen prüfen.

**Wissenschaftliche Umsetzungspflicht:** Jede tatsächliche Berechnung/Schwelle erhält KDoc mit Autor/Jahr/DOI, exakter Definition, Einheiten, Kontext, Artefaktregeln, Fehlwerten, Einschränkungen und Tests; passende Ergänzung in `docs/SCIENTIFIC_REFERENCES.md`. PulseLibre-eigene Grenzwerte ausdrücklich als `PULSELIBRE EXPERIMENTAL HEURISTIC — NOT CLINICALLY VALIDATED` markieren. Siehe `docs/ENGINEERING_SCIENTIFIC_STANDARDS.md`.

## 7. Implementierungs-Roadmap als Kachel-Abnahmeplan

| Phase | Kacheln | Voraussetzung | Abnahme |
| --- | --- | --- | --- |
| **0: Wahrhaftige Basis** | H01, A01, Q01 | bestehende validierte lokale Daten | korrekte Einheiten/Zeiten, Lücken, Herkunft, leere Zustände, Accessibility |
| **1: Echte Trends** | H02, A02, A04, Q02 | stabile Tageswerte, Datenbankabfragen und Deduplizierung | Tageswechsel/DST/Reset/fehlende Tage, Tests mit isolierten Fixtures |
| **2: Weitere Dienste** | O01, O02, R01, R02, N01, A03 | echte CIRQA-Servicevalidierung + Semantik/Zeiträume | keine Anzeige erfundener Werte; Methoden-/Quellennachweis |
| **3: Kontinuierliche Physiologie** | H03, H05, O03, R03, A05 | validierte Zeitreihen, Aktivitätskontext und Artefaktfilter | wissenschaftliches Review, Sensitivitätsanalyse, gegen Referenzmethode geprüft |
| **4: FIT-Historie/Schlaf** | S01, S02, S03, N02 | gesicherter, autorisierter CIRQA-History-Download, FIT-Parser, Schlafvalidierung | dokumentierte Hardwaretests, Datenvollständigkeit, kein unvalidierter Sleep Score |

**Definition of Done pro Kachel:**
1. `TileSpec` vollständig und Evidenzniveau korrekt.
2. Quelle tatsächlich vorhanden; kein Fallback auf Demo-/Pseudo-Messwerte.
3. Berechnung rein/deterministisch, versioniert und durch nachvollziehbare Fixtures getestet.
4. Fehlerfälle: Null, invalid, outlier, reset, missing, stale, duplicate, DST, Datenlücke.
5. UI: Status/Zeitraum/Einheit, Quellen-Info, klare Unsicherheit, TalkBack und Dark Mode.
6. Wissenschaft: Primärquelle, genaue Gleichung/Schwelle, Zielpopulation, Messbedingungen, Grenzen.
7. Build/Unit-/UI-Teststatus ausdrücklich dokumentiert; kein Release bei unbekannten Pflichtchecks.
8. Keine Änderung am Garmin-GFDI-/5050-/FileSync-Transport durch dieses Dashboard-Paket.

## 8. Offene wissenschaftliche Entscheidungen vor der Implementierung

- Ist CIRQA-Ruhepuls der aktuelle, Tages- oder historische Gerätewert? Erst nach Test taggen.
- Liefern RR-Notifications wirklich zusammenhängende Beats mit belastbarer Zeitbasis oder nur gelegentliche Stichproben? Bis dahin **keine HRV-Kachel**.
- Welche Services 8/10/13/19/20/21 funktionieren am echten CIRQA und mit welchen Einheiten/Skalen?
- Gibt es echten Rohbeschleunigungszugriff oder nur kumulative Schritte? Keine „Sitzzeit“ ohne Aktigraphie.
- Welche FIT-Dateitypen und Schlaf-/HRV-Felder sind nachweislich verfügbar? Kein spekulativer Historienimport.
- Wie werden Altersgruppen, individuelle gesundheitliche Einschränkungen und Medikamente bei Trainingsreferenzen berücksichtigt? Siehe Issue #11.
- Wie werden persönliche Quantile/Trendgrenzen bei Datenlücken und saisonalen Änderungen validiert? Erst **deskriptiv**, keine klinischen Alerts.

**Entscheidung:** Ein modernes Dashboard wird nicht durch möglichst viele rote/grüne Scores besser, sondern durch mehr *belegbare*, *nützliche*, *verständliche* Erkenntnisse – einschließlich einer ehrlichen Aussage, wenn die Daten nicht reichen.
