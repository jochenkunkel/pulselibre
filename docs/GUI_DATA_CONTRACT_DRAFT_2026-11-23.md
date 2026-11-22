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

## 5. FIT-Agent-Antwort (2026-11-23; Review-Ergänzung, noch kein Gesamtvertrag)

Diese Ergänzung bestätigt den **heutigen FIT-Codepfad**, nicht medizinische Validität, vollständige Historienabdeckung oder die Aktivierung neuer Metriken. Der Dokumentstatus bleibt Entwurf, bis GUI-/Datenmodell-/Abnahme-Owner die Schnittstelle bestätigen.

### FIT-Zeit und Provenienz

- FIT-`timestamp` Feld 253 wird als FIT-Epoch-Sekunden gelesen und in UTC-Epoch-Millisekunden umgerechnet. Es gibt in der FIT-Pipeline derzeit **keine dokumentierte Band-/Telefon-Uhrkorrektur**. FIT-Messzeit und Telefon-Empfangszeit (`received_at_ms`) bleiben getrennt.
- Kanonische FIT-Beobachtungen können `timestamp_origin` als `DIRECT`, `RECONSTRUCTED_COMPRESSED` oder `RECONSTRUCTED_TS16` ausgeben; wenn keine rekonstruierbare Messzeit vorliegt, wird die Beobachtung nicht importiert. Für Review-Kandidaten darf die Messzeit `null` sein; bei fehlender Zeit ist `UNKNOWN` kein Ersatzzeitpunkt.
- `HistoricalObservationRepository` liefert für aktive kanonische FIT-Generationen Quelle-SHA-256, FIT-Record-Ordinal, Global Message/Feldnummer, Qualitätslabel, Decoder-Version und Zeitstempelherkunft. Legacy-/Live-Punkte haben keine FIT-Provenienz. Review-Kandidaten liegen separat und dürfen nicht als `origin=band`-validierte Zeitreihe behandelt werden.
- Es gibt **kein allgemein gültiges nominales FIT-Messintervall** für Herzfrequenz oder andere Metriken. GUI darf Abstände aus tatsächlichen Zeitstempeln berechnen, aber nicht daraus Vollständigkeit oder eine verlässliche Intervallfrequenz ableiten.

### FIT-Metrikabdeckung und Einheiten

| FIT-Daten | Heutiger Status | Einheit / Darstellungsgrenze |
|---|---|---|
| Record-Herzfrequenz (20/3), Monitoring-Herzfrequenz (55/27) | Kanonische FIT-Generation; aktiver Verlauf ist HF-only | `bpm`; Verlauf lässt nur 1–240 bpm zu |
| Record-Distanz (20/5), Geschwindigkeit (20/6, 20/73), Kadenz (20/4), Leistung (20/7) | Kanonische FIT-Generation; generisch abrufbar, keine entsprechende FIT-Verlaufskarte belegt | `m`, `m/s`, `rpm`, `W`; Kadenz ist keine Schrittzahl |
| SpO₂ (269/0), Stress (227/0), Atemfrequenz (297/0) | Untrusted Review-Kandidaten, keine kanonischen FIT-Observations/Trends. Speichern Kandidaten-Profilwert und Qualitätslabel getrennt | `%`, `profile_raw`, `breaths/min`; Kandidatenqualität ist `UNTRUSTED_PROFILE_CANDIDATE` |
| Monitoring-Schritte/Kalorien, Schlaf, HRV, Hauttemperatur | Keine entsprechende kanonische FIT-Metrik im aktuellen Decoder; unbekannte Felder werden inventarisiert, nicht semantisch interpretiert | Keine FIT-Anzeige ableiten |

Es gibt in der FIT-Pipeline keine bestätigten Werte für SpO₂-Messmodus/Konfidenz. Stress-Feld 227/1 (`stress_level_time`) wird **nicht** als FIT-Epoche interpretiert. Atemfrequenz-Review-Kandidaten können negative Rohwerte enthalten; die GUI-Frage, ob -100/-200/-300 Gerätestatus sind, ist im vorliegenden Vertrag **nicht durch den gespeicherten FIT-Qualitätscode bestätigt**. Diese Codes daher nicht als bestätigte FIT-Statussemantik ausgeben, bis sie profil-/datenmodellseitig verifiziert und separat codiert sind.

### Qualität, Sentinels und Abdeckung

- Kanonischer Commit verlangt Zeitstempel und `PROFILE_VALID`; Herzfrequenz außerhalb 1–240 wird nicht kanonisch importiert. `MISSING_TIMESTAMP` und `OUTSIDE_DISPLAY_RANGE` sind Decoder-Qualitätszustände, keine dargestellten kanonischen Punkte.
- FIT-Profil-Sentinels werden aus Messwerten verworfen; sie werden nicht als 0 oder als kanonische `INVALID`-Messung gespeichert. Der Feldinventurpfad zählt gültig/ungültig, und das CRC-validierte FIT-Original bleibt für erneute Auswertung erhalten.
- Kandidaten behalten `UNTRUSTED_PROFILE_CANDIDATE`; sie sind keine bestätigten `VALID`-Punkte. FIT 227/1 bleibt semantisch separat. Unbekannte oder inkompatible Felder werden ohne Rohwert in der Feldinventur erfasst; ihr vollständiger Inhalt verbleibt nur im FIT-Original.
- Der Import belegt nur die untersuchten Quelldateien. Es gibt derzeit keine verlässliche Abdeckungsmarke je Gerät/Tag, die „nicht heruntergeladen“ von „kein Wert gemessen“ unterscheidet. `coverage=device-history-unverified` bleibt daher maßgeblich; fehlende FIT-Punkte dürfen nicht als Nullwerte interpretiert werden.

### Noch offen / nicht freigegeben

- Kein versionsübergreifender Decoder-Reimport mit expliziter Aktivierung einer neuen Generation ist nachgewiesen. Neue kanonische FIT-Mappings erfordern zuerst die #144-Datenmodellentscheidung und Wiederholungs-/Aktivierungsregressionen.
- Die 31 S25-Originale sind in diesem Checkout nicht vorhanden. Fünf separat verfügbare S10-Kandidaten liefen lokal durch den Decoder; das belegt weder S25- noch vollständige SQLite-Integration.
- FIT Global Message 233 bleibt semantisch unbekannt; kein Diagramm, Messwert oder Periodizitätsstatus daraus ableiten.
