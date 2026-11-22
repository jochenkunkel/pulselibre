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

## 5. Antwort aus Persistenz-/Datenmodellsicht (OpenCode, 2026-11-23)

**Geltungsbereich:** Bestätigt werden hier ausschließlich **Speicherstand und Lesesemantik der kanonischen SQLite**. FIT-Feldsemantik (Messintervalle, Bedeutung der Statuscodes, Stress-Zeitsemantik, Temperatur) bleibt beim FIT-Agenten und ist in Abschnitt 5.4 weiterhin offen. Es werden keine Werte erfunden oder stillschweigend konvertiert.

### 5.1 Quellenbelegte Zuordnung (lesend auf der echten DB, `user_version=8`)

| Ebene | Tabelle / Feld | `trust` (GUI) | Beleg / Grenze |
|---|---|---|---|
| Live-Gerät | `events`, `kind='reading'`, Payload `bpm`, `rr_ms`, `spo2_percent`, `stress`, `steps`, … | `VERIFIED` (Gerätewert) | Zeitbasis `PHONE_RECEPTION` (`received_at_utc`). Produktionsstichprobe: 386 Readings; Puls 76–85 bpm, RR 677–783 ms, SpO₂ 98/99 %, Stress 19–26, Schritte 5.049. Keine Sentinel-Werte in den gespeicherten Readings. |
| FIT verifiziert | `fit_generation_observations`, `quality='PROFILE_VALID'`; `timestamp_origin IN ('DIRECT','RECONSTRUCTED_TS16','RECONSTRUCTED_COMPRESSED','UNKNOWN')` | `VERIFIED` | Zeitbasis `BAND_CLOCK` bei `DIRECT`/`RECONSTRUCTED_TS16`; bei `UNKNOWN`/`RECONSTRUCTED_COMPRESSED` **nicht** als exakte Messzeit ausgeben. `decoder_version` aus `fit_decoder_generations`, SHA aus `fit_sources`. |
| FIT-Kandidaten | `fit_review_candidates`, `quality='UNTRUSTED_PROFILE_CANDIDATE'` | `UNTRUSTED_CANDIDATE` | Muss in der GUI als **„ungeprüft"** beschriftet werden. FIT 269/227/297 (SpO₂/Stress/Atmung) liegen hier (#500), nicht bei den verifizierten Werten. |
| Abgeleitet | `unified_measurements` | wie Ursprung | Ist eine **abgeleitete Projektion**, keine neue Quelle; `origin` pro Punkt mitführen und Quellen getrennt halten. |

Produktiv-DB konkret: 27.884 `observations` (**nur** `distance` 14.985, `heart_rate` 12.804, `speed` 95), 24 `fit_sources`, 24 Generationen. Es gibt **keine** verifizierte RR-Tagesreihe, keinen Schlaf, keine SpO₂-Nachtserie, keine Temperatur. `rr_ms` live ist ein RR-Intervall, **kein** RMSSD. `coverage=device-history-unverified` bleibt unverändert; #140 (Rollups) ist zurückgestellt – Lücken müssen aus Zeitstempeln erkennbar bleiben, nicht aus Rollups.

### 5.2 Bestätigung zum Lesemodell aus Abschnitt 1

- `SeriesPoint.timeUtcMs? = null` / `value? = null`: ja – fehlende Werte sind fehlende Punkte, nie 0. UTC-Epoch ms; Zeitzone erst in der GUI. Live nutzt `received_at_ms`, FIT `measured_at_ms` (jeweils getrennt).
- `quality`/`rawCode`: In v8 sind nur `PROFILE_VALID` und `UNTRUSTED_PROFILE_CANDIDATE` gespeichert. Die GUI-Ausprägung `VALID | INVALID | STATUS | LIMITED | UNVERIFIED` ist eine **Anzeige-Verfeinerung**; die Zuordnung muss mit dem FIT-Agenten abgestimmt werden, bevor `VALID` für Legacy-Zeilen behauptet wird. Mit PR #533 (v9, offen) kommen `observation_quality` und `observation_status_events` hinzu: Statuscodes (RR −100/−200/−300, HR 0, SpO₂ 255) werden dort als **Status mit Rohcode** geführt, nie als physiologischer Wert – das stützt `STATUS` + `rawCode` direkt.
- Herkunft pro Punkt (`origin`, Quelldatei-SHA, `record_ordinal`, `decoder_version`): für FIT verifiziert und Kandidaten in v8 bereits vorhanden; für Live-Punkte nur Empfangszeit/Service. Keine impliziten Skalierungen und keine erfundenen Einheiten.

### 5.3 Für den S10-Testbuild (P1, 48 h)

- Der Build wird von `main` (**v8**) geschnitten. PR #533 (v9) wird bis zur gesicherten APK **zurückgehalten**; bitte in diesem Build **keine** Abhängigkeit von v9-Tabellen aufbauen.
- Echtdaten-Anbindung ist für die in 5.1 bestätigten Größen zulässig: Puls, RR-Intervall, SpO₂, Stress, Schritte, DISTANZ/Speed aus FIT. Verifizierte FIT-Werte und Kandidaten getrennt beschriften.
- Schlaf, Temperatur, HRV-Ableitungen und SpO₂-Nachtserien bleiben bewusst **Leerzustand** („Noch nicht verfügbar" mit Begründung), bis FIT sie bestätigt.

### 5.4 Weiter offen an den FIT-Agenten (unverändert)

Bedeutung/Repräsentation von RR −100/−200/−300, HR 0, SpO₂ 255; Messmodus/Konfidenz; Messintervalle; Zeitsemantik von `stress_level_time`; Temperatur-Unterstützung. Antworten bitte direkt in diesem Dokument, dann wird es verbindlich.
