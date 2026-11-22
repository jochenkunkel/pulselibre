# S10 – Ausführung des begrenzten DB-Import-Tests (redaktiert)

Stand: 2026-11-22 11:57 UTC · Plan: `CIRQA_S10_BOUNDED_DB_IMPORT_TEST_PLAN_2026-11-22.md`

## Freigabe

Der Nutzer hat am 22.11.2026 um 11:52 UTC ausdrücklich die Option „Frisches APK + 5-Min-Download"
gewählt und ergänzt, dass S10 und Armband zur Verfügung stehen. Damit sind Installation des
frischen signierten APK, SAF-Export und der vorhandene Historien-Download (max. 5 Minuten)
freigegeben. Die Stop-Bedingungen des Plans gelten unverändert.

## Read-only Vorbefund

- Gerät: S10, SM-G970F, Android 12; über USB verbunden; kein Emulator in der Sitzung.
- Installierte App vor dem Test: `2.0.0_build1805` (`versionCode=1805`).
- Keine Instrumentationstests; keine BLE-/GATT-/FileSync-Kommandos vor der Installation.
- Andere Agenten: FIT-Agent arbeitet an offline Inventur/Decoder-Evidenz; keine Anzeichen
  paralleler Geräteaktion. Bei Widerspruch wird sofort abgebrochen.

## Frisches APK

- Quelle: erfolgreicher CLOUD/FULL-CI-Lauf `45016725356` auf main-Commit `53e03a4`.
- Artefakt: `pulselibre-signed-release-update-apk`, ID `14752650850`.
- Paket: `de.pulselibre`, `versionName=2.0.0_build1806`, `versionCode=1806`.
- APK-SHA-256: `7b1348e75554e1fc4af1dc98b4bb334fffffddb3860a03d54ecd10801617a788`.
- Signatur geprüft: V2, CN=PulsLibre, SHA-256-Zertifikatdigest
  `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`.
- Grund für build1806 statt build1805: build1806 ist der frischeste signierte Build nach den
  Merges von #432/#433; er enthält die verdrahtete zlib-/MLR-Pipeline. Der spätere GUI-Refactor
  #455 berührt den DB-Importpfad nicht.
- **Keine** Secrets, Keystores oder privaten Gerätedaten in diesem Dokument.

## Geplante Schritte / Status

| Schritt | Status |
|---|---|
| 1 Read-only Geräte-/Buildprüfung | erledigt |
| 2 APK installieren (`adb install -r`, bei Signatur-/Versionsfehler sofort STOPP) | erledigt – `Success`, danach `versionCode=1806` bestätigt |
| 3 SAF-Export durch Nutzer, lokale Kopie, offline DB-Prüfung | erledigt – Baseline exportiert und offline geprüft |
| 4 Vorhandener Historien-Download über UI, max. 5 Min, kein Retry | abgebrochen bei 5 Min – siehe Zeitachse |
| 5 Erneuter Export, offline Vergleich | ausstehend |

### Zeitachse des Downloadversuchs

- 12:09:56 UTC: Nutzer meldet „gestartet" (Tap auf `Herunterladen`), Beginn der 5-Minuten-Frist.
- 12:13:08 und 12:14:00 UTC: `uiautomator dump` meldet `could not get idle state`;
  `MainActivity` bleibt im Vordergrund. Kein lesbarer Statustext, kein Abschluss erkennbar.
- 12:14:56 UTC: Frist ablief.
- 12:15:34 UTC: `am force-stop de.pulselibre` ausgeführt; danach kein App-Prozess mehr,
  `versionCode=1806` unverändert. Kein Retry, keine Schleife.
- **Ergebnis:** kein nachgewiesener Downloadabschluss, kein zwischenzeitlicher DB-Import
  nachgewiesen. Keine von mir ausgeführten BLE-/GATT-/FileSync-Kommandos, kein Reset,
  keine Entkopplung, keine Datenlöschung. Der Force-Stop beendete die laufende Probe; ein
  genauer innerer Zustand zum Abbruchzeitpunkt ist nicht beobachtet worden.

### Zwischenbefund zur Installation

- `adb install -r` war erfolgreich; keine Signatur-, Versions- oder Kapazitätsfehler, kein
  `-d`/`-f` und kein Downgrade nötig.
- Der Versuch, `ExportActivity` per `am start` zu öffnen, wurde vom System korrekt abgewiesen
  (`not exported`). Kein Umweg, kein Root, keine Instrumentation. Der Nutzer öffnet den
  Export selbst über den Reiter „Daten" und die Schaltfläche „Backup erstellen".
- Keine BLE-/GATT-/FileSync-Kommandos, kein Download, kein Reset, keine Entkopplung.

### Baseline-Befund (redaktiert)

- Backup-Archiv: Format `de.pulselibre.local-database-backup`, Version 7,
  `app_id=de.pulselibre`, `encryption=none`; 15 Dateneinträge + Manifest; **alle
  Prüfsummen und Bytezahlen stimmen**.
- **Alle 15 Tabellen haben 0 Datensätze**, u. a. `canonical_observations`,
  `canonical_fit_sources`, `canonical_fit_observation_provenance`, `legacy_readings`,
  `cirqa_events` und alle Generierungs-/Metadatentabellen.
- Damit: kein DB-Import im aktuellen App-Profil. Das ist ein negativer Befund für den
  aktuellen Produktpfad, **kein** Beweis gegen die Geräteunterstützung.
- Bekannte Einschränkung: die früheren lokalen Instrumentationstests auf dem S10 können
  Profil-/Einstellungswerte verändert haben; ein vorheriger Vergleichsstand liegt nicht vor.
- In GitHub stehen ausschließlich Anzahlen, Größen und Prüfsummenstatus – keine
  Nutzerdaten, Gerätekennungen oder Gesundheitswerte.

### Bedienkette für Schritt 4 (aus Code und Gerät bestätigt)

Reiter `Gerät` → Schaltfläche `Historie herunterladen` → Bestätigungsdialog
`CIRQA-Historie herunterladen?` → `Herunterladen`. Die vorhandene UI ruft
`startHistoricalDownload(userConfirmed = true)` auf; keine neuen BLE-Kommandos, kein
Testlabor, kein `CLOSE_ALL`, kein Reset. Die Probe verlangt genau eine gepaarte CIRQA-
Kopplung und bricht sonst sicher ab.

### Stopp bei Zeitüberschreitung

Höchstens 5 Minuten ab Betätigung von `Herunterladen`. Danach kein Retry: App aus dem
Hintergrund entfernen oder, falls das nicht greift, `am force-stop de.pulselibre`
(beendet die Probe über `onDestroy`; keine Datenlöschung). Danach Abbruch und Dokumentation.

## Datenschutz / GitHub

Export und HCI bleiben lokal im 0700/0600-Backup. In GitHub erscheinen ausschließlich
redaktierte Angaben: Anzahlen, Größen, CRC-Status, Buildnummern und öffentliche
Artefakt-/Commit-IDs. Keine Roh-BLE-Payloads, Geräteadressen, Tokens, UIDs oder
Gesundheitswerte.

## Grenzen

Auch ein positiver DB-Befund beweist nur den Import, nicht die Vollständigkeit der Historie.
`coverage=device-history-unverified` bleibt bis zu beiden Nachweisen gültig.
