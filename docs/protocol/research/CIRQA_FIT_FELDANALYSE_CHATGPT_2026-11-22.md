# CIRQA FIT-Feldanalyse – ChatGPT, erste Offline-Auswertung (22.11.2026)

## Evidenz und Grenzen
Quellen auf main: `CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md`, `CIRQA_FIT_FILE_INVENTORY_2026-11-18.json`, `CIRQA_S10_BERICHTE_ANALYSE_INPUT_2026-11-22.md`, `FitHistoricalObservations.kt`, `CirqaUnknownFitInventory.kt`, `tools/fit_field_survey.py`. Keine neue Hardwareaktion, kein Zugriff auf private Original-FIT-Bytes. Die Inventarmatrix bezieht sich auf den S25-Dateibestand vom 18.11.; S10-Zahlen sind getrennte Beobachtungen.

## Schon im S25-FIT-Inventar erkannte Nachrichtentypen
| FIT Global Message | Profilbezeichnung | Konkrete Evidenz aus Inventarmatrix | Aktuelle Einordnung |
|---|---|---|---|
| 269 | `spo2_data` | in zwei Dateien mit 101 und 475 Records | **vorhandene FIT-Nachrichten**, SpO₂-Werte noch nicht als importierte SQLite-Metrik belegt |
| 227 | `stress_level` | 105 und 471 Records in zwei Dateien | vorhanden, keine validierte importierte Stress-Zeitreihe belegt |
| 297 | `respiration_rate` | 105 und 471 Records | vorhanden, keine validierte importierte Atemfrequenz belegt |
| 398 | `skin_temp_overnight` | 1 Record in einer Datei | vorhanden, keine importierte Temperaturmetrik belegt |
| 412 | `nap_event` | 8 Records in einer Datei | vorhanden, Ereignissemantik/Zeiten und Import offen |
| 211 | `monitoring_hr_data` | 2 und 1 Records | vorhanden, Timestamp-/HR-Feldzuordnung gesondert prüfen |
| 55 | `monitoring` | 141 und 506 Records | vorhanden, nicht gleichbedeutend mit Schritten in SQLite |
| 470 / 471 | `sleep_disruption_severity_period` / `sleep_disruption_overnight_severity` | Feldmatrix enthält 6 bzw. 1 Records | vorhanden laut Matrix, kein Schlafscore-/Phasenimport belegt |
| 397 | Profilname in Inventur nicht aufgelöst | 1459 Records in einer Datei | hohe Priorität zur Feldanalyse, keine Semantik behaupten |
| 233 | Profilname nicht aufgelöst | zahlreiche Records, auch in Monitoring-Dateien | mögliche herstellerspezifische Daten; erst Typ/Feld/Korrelation prüfen |

**Wichtig:** Die genannten Record-Zahlen sind Dateiinventurzahlen, nicht Messwertimporte, und dürfen nicht zur S10-Zahl von 27.736 addiert werden. Aus den S10-Exporten sind 10.283 `canonical_fit_unmapped_fields`-Einträge belegt, aber noch keine gruppierte Typ-/Feldverteilung publiziert.

## Sofortiger Offline-Arbeitsplan
1. OpenCode: auf Basis der vorhandenen Originale **Feldmatrix** je global message, field number, developer field, FIT base type, Größe, Skalierung, gültig/ungültig, Anzahl und Dateiabdeckung erstellen. Rohwertverteilungen und einzelne Zeitstempel nur in freigegebenen **synthetischen, geprüften** Fixtures publizieren.
2. S10: gruppierte, maschinenlesbare `metric × unit × source`-Zählung für alle 27.736 kanonischen Beobachtungen bereitstellen; 12.730 HR-Punkte separat ausweisen. Die restlichen 15.006 nicht spekulativ benennen.
3. Profile 21.222.0 und ggf. aktuelle FIT-SDK-Profilversion gegen Message 269, 227, 297, 398, 412, 470/471 abgleichen: Feldnummer, Einheit, Scale, Offset, Invalid-Sentinels, Zeitbasis, Arrays, Enums. Bei proprietären Messages 233/397 keine Namen raten.
4. Synthetische FIT-Fixtures mit eindeutiger Herkunftskennzeichnung und Referenz-Erwartungswerten in Git veröffentlichen; zunächst Parser-/Decoder-Regression, danach Import- und GUI-Test. Originaldateien nur nach Datensatzprüfung öffentlich freigeben.
5. Unbekanntes S25-Protobuf-Schema getrennt behandeln; FIT-Message-IDs nicht mit Protobuf-Feldnummern vermischen.

## Akzeptanz
- pro Feld Evidenzkette **beobachtet → typisiert → skaliert → zeitlich validiert → SQLite importiert → UI getestet**;
- maschinenlesbare Aggregatdatei und mindestens ein synthetischer Regressionstest je neuem Metriktyp;
- keine stillschweigende Umklassifizierung von Rohwerten zu echten Gesundheitsmessungen;
- Gerätehistorie bleibt `coverage=device-history-unverified`.
