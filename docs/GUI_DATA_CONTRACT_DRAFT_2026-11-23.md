# Datenvertrag GUI ↔ FIT-/Datenmodell-Agent (ENTWURF, 2026-11-23)

Status: **Entwurf des GUI-Agenten, vom FIT-Agenten zu bestätigen oder zu ändern.** Verbindlich wird er erst, wenn der FIT-Agent jede offene Frage unten beantwortet hat und der Vertrag per PR auf `main` steht. Die GUI zeigt nur, was hier als verifiziert gilt; alles andere erscheint als „nicht unterstützt“ oder „ungeprüft“.

## 1. Lesemodell (Vorschlag)
Die GUI liest ausschließlich über eine Schnittstelle (heute: `unified_measurements` für Live/Import/Band; `fit_review_candidates` und `live_device_metadata` getrennt):

```
SeriesHeader: metric, unit, origin (live|import|band|candidate), timeBasis (BAND_CLOCK|PHONE_RECEPTION|UNKNOWN),
              nominalIntervalMs?, trust (VERIFIED|UNTRUSTED_CANDIDATE), decoderVersion?, sourceFileSha?
SeriesPoint:  timeUtcMs? (null = Zeit unbekannt), value? (null = kein Messwert), quality, rawCode?
quality:      VALID | INVALID (dokumentierter Sentinel) | STATUS (Gerätestatus, z. B. off wrist) | LIMITED (Konfidenz/Modus) | UNVERIFIED
```
Regeln: Zeit ist UTC-Epoch in ms; eine Zeitzone gehört **nicht** in den Punkt, sondern wird erst in der GUI aus der Geräteeinstellung des Nutzers angewandt. Fehlende Messwerte sind **fehlende Punkte**, nie 0. Ungültige/Statuswerte behalten ihren Rohcode (`rawCode`), damit die GUI sie benennen, aber nicht als Wert zeichnen kann. Unvollständige Zeitreihen müssen als Lücken erkennbar sein (Messintervall + Zeitstempel genügen).

## 2. Messgrößen: heute gespeichert und offene Fragen
| Messgröße | Heute gespeichert (belegt im Code) | GUI-Erwartung | Offene Fragen an den FIT-Agenten |
|---|---|---|---|
| Herzfrequenz | live `bpm`/`resting_bpm` (Empfangszeit); Band `heart_rate` aus aktiver FIT-Generation (Messzeit) | gültig 1–240 bpm, 0 = INVALID | Messintervall im Band-FIT? Zeitstempel direkt oder rekonstruiert (Feld `timestamp_origin`)? |
| Stress | live `stress` (Geräteskala 0–100); FIT-Kandidat (Nachricht 227) untrusted | Negative Werte = nicht als Stress zeigen | Bedeutung negativer Rohwerte; Zeitsemantik von `stress_level_time`; Qualitätsstufen |
| Atemfrequenz | live `breaths_per_min`; FIT-Kandidat (297) untrusted | -100/-200/-300 = STATUS (off wrist/große Bewegung/ungültig) | Bestätigung der Codes im Datenmodell (nicht nur in der GUI); Messintervall |
| SpO₂ | live `spo2_percent`; FIT-Kandidat (269) untrusted | numerisch gültig ≠ qualitätsgesichert; keine GUI-Grenzwerte | Messmodus und Konfidenzfelder: welche liegen vor, wie gespeichert? |
| Schlaf | **nichts gespeichert** | Intervalle (Beginn, Ende, Phase, Quelle), Unterbrechungen als eigene Intervalle | Welche FIT-Nachrichten, welche Phasen sind belegt, Zeitsemantik, Vollständigkeit |
| HRV | live `rr_ms` (RR-Intervall, ausdrücklich **kein** RMSSD) | nur als RR-Intervall; keine abgeleiteten HRV-Kennzahlen ohne Methode | Gibt es HRV-Statusfelder (FIT)? Messfenster? |
| Aktivität | live `steps`, `goal`, `total`/`active` kcal, `moderate`/`vigorous` min; Band `distance`, `speed`, Kadenz/Leistung (FIT) | Tagesverlauf nur mit belegter Zeitbasis | Zeitbasis der Monitoring-Nachrichten (55); Tageszähler vs. Intervallwerte |
| Temperatur | **nichts gespeichert, Unterstützung unbelegt** | wird nicht gezeigt | Liefert das Band Temperatur? Wenn ja Feld, Einheit, Referenz |

## 3. Querschnittsfragen
1. **Zeitzone/Uhr:** Wie wird der Versatz zwischen Bandzeit und Telefonzeit gespeichert (Feld, Messzeitpunkt der Kalibrierung)? Heute zeigt die GUI beide Zeitbasen getrennt.
2. **Einheiten:** Verbindliche Einheitenliste je Metrik (z. B. `bpm`, `%`, `breaths/min`, `ms`, `m`, `m/s`); keine impliziten Skalierungen.
3. **Herkunft:** Pflichtfelder je Punkt: `origin`, bei Band zusätzlich Quelldatei-SHA, `record_ordinal`, `decoder_version`.
4. **Qualität:** Eine abgestimmte Liste von Qualitätswerten; die GUI erfindet keine.
5. **Fehlende/ungültige Werte:** Wird ein Sentinel (z. B. FIT-Invalidwert) beim Import verworfen, als INVALID gespeichert oder als STATUS? Bitte festlegen und im Datenmodell dokumentieren.
6. **Unvollständige Zeitreihen:** Wie erkennt die GUI, dass ein Abschnitt fehlt (z. B. nicht heruntergeladen) und nicht „kein Wert gemessen“? Vorschlag: Abdeckungsmarke je Quelle und Tag (heute `coverage=device-history-unverified`).

## 4. Umsetzungsreihenfolge (GUI)
1. Reine Modellklassen und Fixtures mit **synthetischen** Daten, klar gekennzeichnet (nicht in normaler Oberfläche sichtbar).
2. Dashboard-Komponenten gegen dieses Modell (Zeitachsen, Qualitätsmarkierung, Leer-/Fehlerzustände).
3. Anbindung an echte Daten **erst nach** Bestätigung dieses Vertrags pro Messgröße.
