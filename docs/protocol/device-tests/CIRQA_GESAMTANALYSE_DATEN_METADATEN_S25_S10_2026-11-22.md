# PulsLibre / CIRQA – konsolidierte Daten- und Metadatenanalyse (S25 V26 + S10, 22.11.2026)

**Status:** redaktierte, quellengebundene technische Analyse durch ChatGPT · **Evidenzstand:** 22.11.2026 · **Keine neuen Gerätezugriffe** · **Keine Rohgesundheitsdaten**

> **Wichtigste Korrektur gegenüber älteren Übersichten:** Der S10-Export nach dem zweiten begrenzten Lauf enthält **27.736 kanonische Beobachtungen** aus **23 FIT-Quellen**. Die älteren **294** Beobachtungen aus dem S25/V26-Versuch sind ein *anderer Geräte-/Versuchsstand* und dürfen nicht als aktueller globaler Gesamtbestand oder als Teilmenge des S10-Exports dargestellt bzw. addiert werden. Weder 27.736 noch die übertragenen Dateien belegen eine vollständige Gerätehistorie.

## 1. Quellen und Evidenzhierarchie

1. **S10 Primärzusammenfassung:** [CIRQA_S10_BERICHTE_ANALYSE_INPUT_2026-11-22.md](CIRQA_S10_BERICHTE_ANALYSE_INPUT_2026-11-22.md), auf `main` seit PR #482. Enthält redaktierte SAF-Exportvergleiche, Zeitachse, Codepfade, Metadatenzählung und offene Fragen.
2. **S10 früher Produktlauf:** [CIRQA_S10_PRODUCT_DOWNLOAD_BUILD1757_2026-11-22.md](CIRQA_S10_PRODUCT_DOWNLOAD_BUILD1757_2026-11-22.md); Status aus UI, **kein** unabhängiger DB-Zeilennachweis für diesen Build.
3. **S10 Protokolllücken:** [CIRQA_S10_P0_GAP_MATRIX_2026-11-22.md](CIRQA_S10_P0_GAP_MATRIX_2026-11-22.md).
4. **S25 Primärbeleg:** [CIRQA_SYNC_V26_DEVICE_RESULT_2026-11-19.md](CIRQA_SYNC_V26_DEVICE_RESULT_2026-11-19.md), mit V25 als historischem Vorlauf.
5. **Datenmodell/Abdeckung:** [CIRQA_SQLITE_STORAGE.md](../data-model/CIRQA_SQLITE_STORAGE.md), [HISTORICAL_DATA_SCOPE.md](../../HISTORICAL_DATA_SCOPE.md).
6. Für den S10-Export gilt die spätere, durch SAF-Archivvergleich belegte Aussage **„Import erfolgt“**, nicht die frühere Zwischenmeldung „kein DB-Import“ aus dem Laufbericht.

**Evidenzstufen:** (A) tatsächliche Geräteübertragung → (B) lokal integritätsgeprüfte Originaldatei → (C) dekodiertes Feld → (D) in SQLite persistierte Beobachtung → (E) in der UI nachgewiesen sichtbar. Eine Stufe darf nicht stillschweigend als nächste ausgegeben werden.

## 2. Was wurde wirklich heruntergeladen?

| Versuch / Gerät | Übertragung | Dateitypen | Gesicherter Interpretationsstand | Vollständigkeit |
| --- | --- | --- | --- | --- |
| **S25 V25, 18.11.** | 33/33 Dateien; 146.293 B | 31 FIT, 2 Nicht-FIT | 294 SQLite-Beobachtungen: Herzfrequenz 129, Distanz 129, Geschwindigkeit 36 | nicht belegt |
| **S25 V26, 19.11.** | **42/42 Dateien; 218.951 B**, 1.520 MLR-Chunks | **40 FIT, 1 XML, 1 unbekanntes Protobuf-Schema** | 40 FIT-Quellen, weiterhin **294** normalisierte Beobachtungen | nicht belegt |
| **S10 Build 1757, 22.11.** | 24/24 gemeldete Dateien; 485.654 B, 3.338 MLR-Chunks | 23 FIT dekodiert; ein weiterer Transfer, Typ aus Bericht nicht hinreichend sicher | UI meldet 17 Dateien mit Importstatus, 6 ohne importierbare Daten; **keine unabhängig geprüfte Beobachtungszahl** | `runDrained=false`; nicht belegt |
| **S10 Build 1806, Lauf 2, 22.11.** | Ein begrenzter Nutzerlauf; **Datei-/Bytegesamtzahl im konsolidierten Bericht nicht unabhängig ausgewiesen** | **23 FIT-Quellen in der kanonischen DB**; 1 XML-Metadatenquelle | **27.736 kanonische Beobachtungen**, 27.736 Provenienzeinträge, 23 aktive Decoder-Generationen | nicht belegt |

**Nicht addieren:** Die S10-Läufe 1757/1806 sind unterschiedliche Builds/Versuchszeitpunkte; die S25-Läufe sind ebenfalls Wiederholungen mit Überschneidungen. 23 FIT-Quellen in einer DB sind kein eigenständiger Beleg für genau 23 neue, in Lauf 2 übertragene Dateien.

## 3. Welche Fitnesswerte sind sicher belegt?

### 3.1 S25 V26 – nach Metrik ausgewiesener Import

| Metrik | Anzahl | Einheit | Evidenz |
| --- | ---: | --- | --- |
| `heart_rate` | **129** | bpm | SQLite, V26 |
| `distance` | **129** | m | SQLite, V26 |
| `speed` | **36** | m/s | SQLite, V26 |
| **Summe** | **294** | – | 40 FIT-Quellen, ohne Beleg vollständiger Feldabdeckung |

### 3.2 S10 Build 1806 – erheblich größerer Import

- **27.736** kanonische Beobachtungen und **27.736** Feld-Provenienzeinträge sind durch den dritten, prüfsummenvalidierten SAF-Export belegt; Baseline und Export nach Lauf 1 hatten jeweils 0.
- **12.730** aktive `heart_rate`/`bpm`-Punkte wurden offline gezählt. Im 24-h-Fenster: **0**, im 7- und 30-Tage-Fenster: jeweils **12.730**. Das ist eine Teilmenge der 27.736 Beobachtungen, keine zusätzliche Zahl.
- **Die vollständige Aufschlüsselung der übrigen 15.006 Beobachtungen nach `metric`/`unit` liegt in der redaktierten S10-Zusammenfassung nicht vor.** Es wäre methodisch falsch, sie ohne gruppierte SQL-Auswertung als Schritte, Distanz, Schlaf, SpO₂ oder andere Messgröße zu deklarieren.
- **23** `canonical_fit_sources`, **23** Decoder-Generationen und **23** aktive Generationen belegen eine verwaltete Importgeneration je Quelle im Export, nicht die lückenlose Gerätehistorie.

### 3.3 Decoderunterstützung ist nicht Feldnachweis

| FIT-Nachricht / Feld | Potenzielle Metrik | Interpretation | Status aus redaktierten Geräteberichten |
| --- | --- | --- | --- |
| Record (20), Feld 3 | Herzfrequenz | bpm | S25 importiert; S10 Herzfrequenz separat belegt |
| Record (20), Feld 5 | Distanz | Rohwert / 100 → m | S25 importiert |
| Record (20), Feld 6 / 73 | Geschwindigkeit / enhanced speed | Rohwert / 1000 → m/s | S25 `speed` importiert; Quelle 6 vs. 73 nicht separat ausgewiesen |
| Record (20), Feld 4 | Kadenz | rpm | Decoder kennt Feld; Import nicht gesondert belegt |
| Record (20), Feld 7 | Leistung | W | Decoder kennt Feld; Import nicht gesondert belegt |
| Monitoring (55), Feld 9 | kumulative Schritte | Rohwert × 2, **nicht** als Schrittinkrement summieren | Decoder kennt Feld; Import nicht gesondert belegt |
| Monitoring (55), Feld 27 | Herzfrequenz | bpm | Decoder kennt Feld; separate Herkunft nicht ausgewiesen |
| Record/Monitoring, Feld 253 | Zeitstempel | FIT-Epoche → Unix-Zeit | Decoder unterstützt Zeitbezug |

**Nicht nachgewiesen als konkret historisch importierte Messgrößen:** Schlafphasen, Schlafdauer/-score, HRV, SpO₂, Stress, Body Battery, Ruhepuls als eigenständiges historisches Feld, Kalorien und Aktivitätsminuten. Garmin-Connect-Dateitypen `MONITORING_B`, `SLEEP_DATA`, `BIOMETRIC`, `TRUE_UP` sind in separaten Gerätebeobachtungen gesehen worden; das belegt nicht ihre semantische Zuordnung zu den hier importierten Dateien.

## 4. Welche Metadaten bekommen wir?

### 4.1 FIT-Gerätemetadaten aus dem S10-Export (konkret gezählt)

| Feld / Tabelle | Beobachtung | Interpretation |
| --- | --- | --- |
| `canonical_fit_device_metadata` | **115 Zeilen** | FIT-`device_info`-Metadaten, nicht 115 verschiedene Geräte |
| `software_version` | **71 belegt**, 44 leer | Versionsangabe historischer Geräteinformationen; nicht zwingend aktuell installierte Firmware |
| `observed_at_ms` | **115 belegt** | zeitliche Einordnung der Metadatensätze |
| `battery_level_percent` | **0/115 belegt** | **Kein Akkuprozentwert aus diesen FIT-Metadaten** |
| `battery_status_code` | **0/115 belegt** | **Kein Batteriestatuscode aus diesen FIT-Metadaten** |
| `raw_device_metadata` | **1 Zeile** | verifiziertes Device-XML, in der DB nur freigegebene Felder |

Die XML-Metadatenstruktur kann Softwareversion, Massenspeicher-Flag und Anzahl der Transfer-Richtungen enthalten. **Die konkreten XML-Feldwerte für die S10-Zeile sind in der redaktierten Zusammenfassung nicht einzeln veröffentlicht.** Geräteidentifikatoren, Seriennummern, XML-Pfade und opake Protobuf-Payloads werden bewusst nicht in die kanonischen Metadatentabellen übernommen.

### 4.2 Datei-/Importmetadaten (technisch gespeichert)

- Integritäts- und Quelleninformationen: content-addressierter SHA-256-Bezug, verifizierte FIT-/Raw-Originale, FIT-CRC und Längenprüfung, Quellenzuordnung, Importstatus und Decoder-Generation.
- Provenienz: zugeordnete FIT-Quellen und Feldherkunft der normalisierten Beobachtungen.
- Sync-/Datenbankstatus: `canonical_sync_cursors=1`, `canonical_fit_decoder_generations=23`, `canonical_fit_active_generations=23`; DB-Integrität/Archivprüfsummen bestanden.
- `canonical_fit_unmapped_fields=10.283`: **Inventareinträge unbekannter bzw. nicht gemappter FIT-Felder**, **nicht** 10.283 unterschiedliche Feldtypen, nicht automatisch 10.283 importierbare Messwerte.
- Der S10-Export enthält `cirqa_events=0`, `legacy_readings=0`, `fit_sources=0`, `fit_observation_provenance=0` (Legacy-Tabellen); die kanonischen Tabellen sind hingegen gefüllt.

### 4.3 Batterie – präzise Aussage

Die einmalig diagnostizierten **37 % / NORMAL vom 18.11.** sind **kein** aus den 115 FIT-Gerätemetadaten abgeleiteter Wert und kein Nachweis eines implementierten Live-Batteriepfads. Build 1806 hat keine nachgewiesene Live-GATT-Batterieabfrage; der vorhandene 5024-Offline-Parser ist nicht in den Android-BLE-Produktpfad integriert. **Spannung ohne nachgewiesene Kalibrierkurve/Entladungsmodell darf nicht in einen verlässlichen Prozentwert umgerechnet werden.** Offline sind weitere bereits vorhandene, private Diagnosen prüfbar; für eine neue Live-Abfrage ist eine **gesonderte ausdrückliche Gerätefreigabe** erforderlich (Issues #142, #150, #159).

## 5. Was steckt möglicherweise zusätzlich in den Dateien?

**Belegt:** Es gibt eine erhebliche, persistierte Inventur nicht gemappter FIT-Felder (10.283 Einträge auf dem S10) sowie im S25 V26 eine vollständig erhaltene Datei mit unbekanntem Protobuf-Schema. Die Originalbytes bleiben privat; unbekannte Inhalte werden nicht als Gesundheitswerte interpretiert.

**Noch offen:** globale FIT-Message-IDs, Feldnummern, Developer-Field-Definitionen, Typen, Skalierungen, Arrays, Zeitabdeckung, Häufigkeiten und Quellenzuordnung der 10.283 Einträge. Ohne diese anonymisierte gruppierte Inventur ist keine seriöse Liste „zusätzlicher verfügbarer Fitnesswerte“ möglich.

**Nächste sichere Offline-Abfrage:** Aggregation `message_type × field_number × developer_field × base_type × source_count × occurrence_count × decoder_status`, ohne Rohwerte, Datei-Hashes oder Gerätekennungen zu veröffentlichen. Danach FIT-Profilabgleich (Version festhalten), nur verifizierte Semantik produktiv dekodieren, Originalbytes unverändert behalten.

## 6. Warum zeigte die App trotz Import keine Historie?

- Der S10-Export enthält **27.736** kanonische Beobachtungen, aber **0** `cirqa_events`. Mehrere GUI-Leser verwenden nur die Event-Tabelle und sehen daher keine historischen FIT-Werte.
- Der frühere `FitHistoryReader.available()/latestMeasuredAt()` prüfte die leere Legacy-Tabelle `fit_observation_provenance` statt der kanonischen Tabelle mit 27.736 Provenienzeinträgen (A6-28). Das kann selbst das 7-Tage-Fenster unsichtbar machen; 0 Punkte im 24-h-Fenster sind zusätzlich ein reales Zeitfensterphänomen.
- **PR #479** hat die FitHistoryReader-Prüfung korrigiert und ist auf `main`; **Build 1806 enthält den Fix nicht**, eine Sichtbarkeitsabnahme auf dem S10 ist noch offen. Andere Event-only-Leser bleiben separat zu prüfen.
- **Folgerung:** „UI leer“ ist hier **kein Beweis für fehlende oder verlorene FIT-Daten**; die GUI muss kanonische Historie über die vorgesehene Repository-Schnittstelle lesen und leere Fenster von Lesefehlern unterscheiden.

## 7. Die Differenz von 157 CSV-Datensätzen – reproduzierbarer Prüfplan

**Bekannt:** CSV hat **27.894 Zeilen inklusive Header**, also **27.893 Datenzeilen**; der dritte SAF-Export enthält **27.736** `canonical_observations`. Differenz: **157**.

**Nicht bewiesen:** Dass 157 Beobachtungen doppelt sind, dass Daten fehlen oder dass es sich um ein Zeitfensterproblem handelt. Das CSV-Exportfenster **01.10.–23.11.** umfasst einen anderen Zeitraum als die betrachtete 30-Tage-Historie; das erklärt die Differenz **nicht ohne Row-Level-Abgleich**.

**Offline-Untersuchung, ohne BLE:**
1. Exakte CSV-Zeilentypen und Exportquellen aus `ExportActivity.kt:528-556`, `CirqaDatabase.kt:905-923`, `RawDataExport.kt:31-49` anhand des **gleichen Commits/Builds** bestimmen. Enthält CSV zusätzlich Events, Legacy-Zeilen, Status-/Metadatensätze oder andere Projektionen?
2. Nur **redaktierte** Aggregationen pro `record_type`, `metric`, `source`, `date_bucket`, `import_generation` und Exportfenster vergleichen; keine privaten Einzelwerte veröffentlichen.
3. CSV- und DB-Mengen auf **identisches Zeitfenster, gleiche Filter und gleiche Deduplizierungsregel** normalisieren. `count(*)`, `count(distinct stable_key)`, Null-/Randfälle und inklusive/exklusive Datumsgrenzen getrennt ausweisen.
4. Für die Differenz eine deterministische Kategorie und eine Regressionstest-Fixture ohne echte Gesundheitsdaten liefern; bis dahin **„157 ungeklärt“**.

## 8. Vollständigkeit der Gerätehistorie: erforderlicher Beweis

`coverage=device-history-unverified` bleibt zwingend. `runDrained` (Laufqueue leer) ist **nicht** `device-history-complete`.

Ein belastbarer Vollständigkeitsnachweis erfordert mindestens:
- Nachvollziehbare **vollständige** Item-List-/Paging-/Transaktionssemantik einschließlich leerer Folgeseiten, `session_id`, `next_transaction_id`, Tokenwiederholungen und begrenzter Abbruchbedingungen. Die S10-P0-Matrix dokumentiert noch Abweichungen zu Garmin Connect.
- Eine redaktierte **Soll-/Ist-Abstimmung** der vom Gerät angebotenen Datei-Referenzen zu erfolgreich verifizierten privaten Originalen, über alle beobachtbaren Seiten/Transportwege; keine ausgelassenen/überschriebenen/retinierten Inhalte als „vollständig“ ausgeben.
- Abgleich von Transfer-, CRC-/Hash-, Decode-, Import- und Fehlerzählern **pro Datei**, inklusive `unsupported`/teilweise/keine importierbaren Werte; dokumentierte Reconnect-/Resume-Grenzen.
- Eine unabhängige, erlaubte Vergleichsevidenz zur historischen Zeitabdeckung (z. B. Garmin Connect bzw. gesicherter Dateibestand), ohne zu behaupten, dass identische App-Darstellung allein Dateivollständigkeit beweist.
- HCI-Lücke benennen: Protobuf-Pulls sind nachgewiesen, aber FIT-Bytes und klassische/RFCOMM-Transportseite nicht vollständig im vorhandenen Mitschnitt rekonstruierbar. Eine neue aktive Geräteerhebung braucht **vorherige ausdrückliche Freigabe**.

## 9. Priorisierter Arbeitsplan und Zuständigkeiten

| Priorität | Agent | Nächster Schritt | Ohne Gerätezugriff? |
| --- | --- | --- | --- |
| **P0** | **OpenCode / FIT** | S10-`metric × unit × source × quality`-Inventur der **27.736** Beobachtungen aus privaten Exporten, nur aggregierte redaktierte Ergebnisse veröffentlichen; 10.283 unmapped-Einträge nach Feld koordinatenweise inventarisieren | **Ja** |
| **P0** | **OpenCode + CI** | **157** CSV/DB-Differenz am gleichen Build/Filter nachstellen; synthetische Regression | **Ja** |
| **P0** | **Claude / GUI** | PR #479 und weitere Historien-/Dashboard-Leser auf kanonische Historie prüfen; S10-UI-Abnahme nur mit gesonderter Freigabe; kein „keine Daten“ bei Lesefehler | Code: **Ja**; Gerät: **Freigabe** |
| **P1** | **OpenCode / FIT** | Vollständige Feld-/Developer-Field-Matrix mit FIT-Profil und Importstatus; Parser nur evidenzbasiert erweitern | **Ja**, sofern private Originale zugänglich |
| **P1** | **OpenCode / BLE** | Dokumentierten Paging-/Transportabgleich weiter offline prüfen, Vollständigkeitskriterien/Stop-Gründe schärfen | Offline: **Ja**; neue Mitschrift: **Freigabe** |
| **P1** | **GUI + OpenCode** | Metadatenansicht: historische Softwareversion mit Quellenzeitpunkt; Batterie explizit „nicht verfügbar“, keine Prozent-Schätzung | **Ja** |
| **P1** | **CI** | Backup-v7-/Import-/Provenienz-/UI-Regression auf synthetischen Daten, klare `coverage`-Semantik | **Ja** |

**Sicherheitsgrenzen:** keine neuen BLE-/GATT-/FileSync-Kommandos, kein Reset, Entkoppeln, `CLOSE_ALL`, Löschen, Firmwareeingriff oder physische Instrumentation ohne neue ausdrückliche Nutzerfreigabe. Keine Roh-BLE-Daten, privaten FIT-/SAF-Dateien, Gesundheitswerte, Seriennummern, vollständigen Geräte-IDs oder Secrets in GitHub. Garmin Connect nicht dauerhaft deaktivieren.

## 10. Management-Fazit

1. **Download und Import sind real:** S25 V26 42 Dateien; S10 Build 1806 **27.736** kanonische Messwertbeobachtungen aus 23 FIT-Quellen.
2. **Welche konkreten Fitnessmetriken?** S25: Herzfrequenz, Distanz, Geschwindigkeit. S10: **mindestens 12.730 Herzfrequenzpunkte**; übrige **15.006 Beobachtungen noch nicht nach Metrik publiziert**. Keine Spekulation über Schlaf, SpO₂ oder HRV.
3. **Welche Metadaten?** S10: **115** FIT-Gerätemetadatensätze, davon **71** mit Softwareversion, **115** mit Zeitbezug, **0** mit Batteriewert; **1** XML-Metadatensatz.
4. **Welche unanalysierten Daten?** **10.283** nicht gemappte FIT-Feldinventareinträge (S10) und mindestens eine unbekannte Protobuf-Datei (S25 V26). Inhalt und Anzahl unterschiedlicher Felder noch nicht geklärt.
5. **Warum leere Anzeige?** Bekannte Trennung zwischen kanonischer Historie und `events`/Legacy-Lesern; PR #479 teilweise korrigierend, nicht auf S10 Build 1806 nachgewiesen.
6. **Offene Qualitätspunkte:** 157 CSV/DB-Differenz, fehlende vollständige Historienabdeckung, fehlender Live-Batteriepfad und noch ausstehende Feld-für-Feld-Inventur.

**Dieses Dokument ersetzt keine Geräteabnahme und keine privaten Inventurdaten.** Es ist eine redaktierte, überprüfbare Zusammenführung der genannten Primärberichte.
