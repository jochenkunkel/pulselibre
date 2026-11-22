# S10/CIRQA – konsolidierte Berichte als Analyse-Eingabe (redaktiert)

Stand: 2026-11-22 13:25 UTC · Autor: CI-Agent (OpenCode) · Ziel: eine einzige, auf `main` verfügbare Grundlage für eine externe Analyse (ChatGPT) der S10-Berichte.

**Datenstatus:** Alle Angaben sind redaktiert – nur Anzahlen, Größen, Prüfsummenstatus, Codepfade, öffentliche Commit-/PR-/CI-IDs und Zeitachsen. Keine Roh-BLE-Payloads, Geräteadressen, Seriennummern, Gesundheitswerte, Zeitstempel einzelner Messungen, Tokens, Keystores oder Secrets. Die privaten SAF-Exporte liegen ausschließlich lokal in einem 0700-Ordner mit Dateimodus 0600 und sind **nicht** Teil des Repositories.

## 0. Kurzfassung für die Analyse

1. Ein nutzerinitiierter Produkt-Download auf dem S10 hat **27.736 kanonische Beobachtungen** in SQLite importiert; der DB-Import ist nachgewiesen.
2. **Die Vollständigkeit der Historie ist nicht nachgewiesen** – `coverage=device-history-unverified` bleibt gültig.
3. Dass Historie und Übersicht trotz Import leer blieben, ist **kein Datenverlust**: die Ansichten lesen eine leere Tabelle (`events`), und die FIT-Karten-Sichtbarkeitsprüfung nutzte die alte, leere Provenanz-Tabelle (A6-28). Ein Code-Fix liegt auf `main` (#479), ist aber nicht auf dem installierten Build 1806 und nicht am Gerät nachgewiesen.
4. Die 115 importierten FIT-Gerätemetadaten enthalten **keinen Batteriewert**. Eine Live-GATT-Batterieabfrage ist nicht implementiert; die 37 % / NORMAL vom 18.11. waren eine einmalige, separat freigegebene Diagnose.
5. Offen: eine Differenz von **157 Zeilen** zwischen CSV-Export und Datenbank, sowie die Frage, welche Evidenz den vollständigen Historiendownload belegen würde.

## 1. Gerät, Freigabe und Build

- Gerät: Samsung Galaxy S10 (`SM-G970F`), Android 12, USB. Seriennummer nur lokal, nie in GitHub.
- Freigabe des Nutzers am 22.11., 11:52 UTC: frisches signiertes APK, SAF-Export, vorhandener Historien-Download **max. 5 Minuten**, kein Retry, keine neuen BLE-/GATT-/FileSync-Kommandos, kein Reset, kein Entkoppeln, keine Löschung realer Daten, keine Instrumentationstests auf dem physischen Gerät.
- Installiert: `de.pulselibre`, `2.0.0_build1806`, `versionCode=1806`, aus CI-Lauf `45016725356`, Commit `53e03a4`, Artefakt `14752650850`. APK-SHA-256 `7b1348e75554e1fc4af1dc98b4bb334fffffddb3860a03d54ecd10801617a788`; V2-Signatur, Zertifikat-SHA-256 `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`.
- `am start` auf `ExportActivity` wurde korrekt mit `not exported` abgewiesen; der Nutzer exportiert selbst über „Daten → Backup erstellen".

## 2. Zeitachse der beiden begrenzten Läufe

| Ereignis | UTC | Ergebnis |
|---|---|---|
| Lauf 1: Nutzer meldet „gestartet" | 12:09:56 | 5-Minuten-Frist bis 12:14:56 |
| Lauf 1: `uiautomator dump` | 12:13:08, 12:14:00 | `could not get idle state`, kein lesbarer Status |
| Lauf 1: `am force-stop de.pulselibre` | 12:15:34 | kein Downloadabschluss, kein Import, kein Retry |
| Garmin Connect nur temporär gestoppt | 12:26:31 | exakt freigegebener `am force-stop com.garmin.android.apps.connectmobile`; danach kein Garmin-Prozess, Paket weiter aktiviert, nicht `pm disable-user`, kein Entkoppeln |
| Lauf 2: konservativer Start (spätestens) | 12:27:30 | Fristende spätestens 12:32:30 |
| Lauf 2: UI-Dump | 12:27:30 | 33 Byte, `could not get idle state`, `MainActivity` vorn |
| Lauf 2: `am force-stop de.pulselibre` | 12:32:55 | kein Retry; Import danach im Export nachgewiesen |

**Widerspruch im Detailbericht bewusst markiert:** In `CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md` steht in der frühen Lauf-2-Zeitachse noch „kein DB-Import nachgewiesen" (Stand vor dem Exportvergleich). Der später im selben Dokument abgeschlossene Exportvergleich widerlegt das für Lauf 2; er ist die maßgebliche Aussage. Lauf 1 blieb tatsächlich ohne Import.

## 3. Redaktierter Export-Vergleich (drei SAF-Archive)

| Export (Hostzeit UTC) | Archivgröße | `canonical_observations` | `canonical_fit_sources` | Integrität |
|---|---|---|---|---|
| Baseline 12:03 | 2.786 Byte | 0 | 0 | alle 15 Einträge ok |
| nach Lauf 1, 12:18 | 2.786 Byte | 0 | 0 | alle 15 Einträge ok |
| nach Lauf 2, 12:34 | 4.043.919 Byte | 27.736 | 23 | alle 15 Einträge ok |

Weitere Anzahlen im dritten Export (vorher jeweils 0): `canonical_fit_observation_provenance` 27.736, `canonical_fit_generation_observations` 27.736, `canonical_fit_unmapped_fields` 10.283, `canonical_fit_device_metadata` 115, `canonical_fit_decoder_generations` 23, `canonical_fit_active_generations` 23, `canonical_sync_cursors` 1, `raw_device_metadata` 1. Unverändert 0: `legacy_readings`, `fit_sources`, `fit_observation_provenance`, `cirqa_events`, `sync_cursors`.

Alle Archive: Format `de.pulselibre.local-database-backup`, Version 7, `encryption=none`, 15 Manifest-Einträge + Manifest; alle Prüfsummen und Bytezahlen stimmen. Der Import überstand den Force-Stop um 12:32:55 UTC (committete Schreibvorgänge).

**Grenze:** Die Zahlen belegen Import und Verarbeitung, nicht dass alle auf dem Band vorhandenen Daten übertragen wurden.

## 4. Warum Historie/Übersicht leer blieben

- Live-Historie (`CirqaHistoryReader.kt:75-80`) und Kernkarten (`CirqaDashboardReader.kt:31-50`, `DashboardDayReader.kt:28-34`, `DailyValueReader.kt:28-32`, `DeviceOverview.kt:54-67`) lesen die Tabelle `events`; im Export: `cirqa_events = 0`.
- `FitHistoryReader.read()` kann die kanonischen Daten lesen; `available()`/`latestMeasuredAt()` prüften aber die **alte** Tabelle `fit_observation_provenance` (0), nicht die kanonische (27.736) – bekannter Fehler A6-28. Lesefehler werden als „keine FIT-Daten" dargestellt (A6-29).
- Offline-Zählung: 12.730 `heart_rate`/`bpm`-Punkte mit aktiver Generation und Provenanz; 0 im 24-Stunden-Fenster, 12.730 im 7-Tage- und 30-Tage-Fenster. Der Nutzer meldete ausdrücklich, dass auch die 7-Tage-Ansicht leer bleibt – das Zeitfenster war also **nicht** die alleinige Ursache.
- Aktion: GitHub-Issue **#477** für den GUI-Agenten (PR #478 dokumentiert). Der GUI-Agent hat inzwischen **PR #479** gemergt (`5970145`, Issue #477 geschlossen): `FitHistoryReader` liest nun aktive Generationen und die alte Tabelle. **Dieser Fix ist nicht im installierten Build 1806 und dort nicht nachgewiesen.**

## 5. Gerätemetadaten und Batterie

- Umgesetzt: FIT-`device_info` aus dem Historiendownload → `fit_device_metadata`/`raw_device_metadata` (`MainActivity.kt:623,636,639` → `CirqaMlHandshakeProbe.kt:3108` → `CirqaFitBackfill.kt:217-228` → `FitObservationCommitter.kt:129-134`; Quelle `FitHistoricalObservations.kt:267,277-292`).
- In den 115 Metadatenzeilen: `battery_level_percent` 115/115 leer, `battery_status_code` 115/115 leer, `software_version` 71 belegt/44 leer, `observed_at_ms` 115 belegt.
- `DeviceBattery.kt:58-61` filtert auf `battery_level_percent IS NOT NULL` → Anzeige bleibt korrekt leer. **Fehlende Daten, kein UI-Bug.**
- Keine Treffer in `app/src/main/java` für `GDIDeviceStatus`, `current_battery_level`, `estimated_hours_remaining`, `DEVICE_STATUS`, `DeviceMetadataStore`. Der 5024-Offline-Parser ist nicht in den Android-BLE-Pfad integriert; `GarminPostBootstrapObserver` unterdrückt Payload-Inhalte. Build 1806 enthält keine Live-GATT-Batterieabfrage.
- Tickets: **#159** (Akku-/Geräteseite, offen, redaktierter Kommentar mit den Anzahlen), **#142** (Research: Gerätemetadaten/Batteriestand zuerst, offen), **#150** (Auswertung vorhandener S25-Metadaten, offen). Dokumentiert in PR #481.

## 6. Konkrete Fragen an die externe Analyse

1. **157-Zeilen-Differenz:** CSV `pulselibre_2026-10-01_00-00_bis_2026-11-23_23-59.csv` hat 27.894 Zeilen = 1 Header + 27.893 Datensätze; DB hat 27.736 Beobachtungen. Exportfenster 54 Tage statt 30. Wie ist die Differenz aus dem Exportpfad (`ExportActivity.kt:528-556`, `CirqaDatabase.kt:905-923`, `RawDataExport.kt:31-49`) erklärbar, ohne Gerätezugriff?
2. **Vollständigkeit:** Welche Evidenz würde `coverage=device-history-unverified` berechtigt aufheben? Bisher fehlen FIT-Bytes als eigener HCI-Kanal und die klassische Transportseite (siehe P0-Gap-Matrix, Zeile 20).
3. **Batteriequelle:** Ist aus den vorhandenen FIT-Metadaten (z. B. Spannungsfeld) ein belastbarer Akkustand ableitbar, oder ist zwingend eine neue, gesondert freizugebende Live-Abfrage nötig?
4. **Priorisierung:** Welche der offenen Punkte sind mit vorhandenen Offline-Daten lösbar, welche benötigen eine neue Gerätefreigabe?

## 7. Evidenzindex (alle Dateien auf `main`)

- `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_TEST_PLAN_2026-11-22.md` – Plan und Stop-Bedingungen
- `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_LAUF2_PLAN_2026-11-22.md` – Lauf 2, Garmin-Nachtrag
- `docs/protocol/device-tests/CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md` – Zeitachsen, APK, Exportvergleich
- `docs/protocol/device-tests/CIRQA_S10_PRODUCT_DOWNLOAD_BUILD1757_2026-11-22.md` – früher Produkt-Download, Fenstergrenzen
- `docs/protocol/device-tests/CIRQA_S10_P0_GAP_MATRIX_2026-11-22.md` – HCI→Code-Abgleich, Evidenzgrenze
- `docs/protocol/device-tests/CIRQA_S10_HISTORY_EVIDENCE_PLAN_2026-11-22.md` – Evidenzplan, Stop-Kriterien
- `docs/protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md` – verbindlicher Historien-Stand
- `docs/collaboration/agent-sync/2026-11-22_1239_ci-agent-an-fit_versuchzaehlung-und-db-import.md` – Versuchszählung, DB-Zahlen
- `docs/collaboration/agent-sync/2026-11-22_1245_ci-agent-an-gui-fit_ursache-leere-historie.md` – Ursache leere Ansichten, Korrektur 12:48
- `docs/collaboration/agent-sync/2026-11-22_1252_ci-agent-an-gui-fit_kein-batteriewert-in-fit-metadaten.md` – Batterie/Metadaten
- `docs/collaboration/agent-chat/messages/2026-11-22_124500_ci-agent_ENDE_ursache-leere-historie.md`
- `docs/collaboration/agent-chat/messages/2026-11-22_124800_ci-agent_UPDATE_ticket-477-gui-fithistorie.md`
- `docs/collaboration/agent-chat/messages/2026-11-22_125200_ci-agent_ENDE_batterie-metadaten-stand.md`

Relevante GitHub-Vorgänge: Issues **#142**, **#150**, **#159**, **#477** (geschlossen); PRs **#476**, **#478**, **#479**, **#481**; CI-Lauf **45016725356**.

## 8. Verbindliche Grenzen für die Analyse

- `coverage=device-history-unverified` bleibt bis zum Nachweis von **DB-Import und vollständiger Historie**.
- Keine neuen aktiven BLE-/GATT-/FileSync-Kommandos, kein Reset, kein Entkoppeln, kein `CLOSE_ALL`, keine Firmwareaktion, keine Löschung/Überschreibung realer Daten, keine Instrumentationstests auf physischen Geräten ohne neue ausdrückliche Freigabe.
- Garmin Connect wurde nur temporär gestoppt; kein dauerhaftes Deaktivieren.
- Injizierte/synthetische Werte aus früheren Tests dürfen nicht mit echten Gerätemessungen verwechselt werden.
- Aussagen wie „CI grün", „S25 getestet" oder „vollständige Historie" sind nur mit belegtem Nachweis zulässig.
