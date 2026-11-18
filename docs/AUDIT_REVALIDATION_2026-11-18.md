# Revalidierung der Review-Befunde R01–R12 (Issue #5)

Stand 2026-11-18, `main` 166e7a4+, durch Code-Lektüre (Claude Code). Kein Gerätetest. Quelle der Befunde: `docs/ARCHITECTURE_AND_CODE_REVIEW.md`.

| ID | Status | Nachweis (aktueller Code) |
|---|---|---|
| R01 Diagnoseexport | **behoben** | `MainActivity.shareDiagnostics` exportiert nur Zählwerte („nur Anzahl“), Verbindungsstatus-Metadaten und eine Kopplungs-Ja/Nein-Angabe; keine MAC, keine Namen, keine UUID-/Service-Rohdaten (siehe Kommentar im Code). Offen: automatischer Test gegen Rohdaten im Export. |
| R02 onScanFailed-Cleanup | **behoben** | `onScanFailed` ruft `stopScan` best-effort mit Exception-Schutz auf. |
| R03 Mehrfachstart | **teilweise** | `if (scanning) return` existiert, steht aber nach Berechtigungs-/Adaptercheck; praktisch folgenlos. |
| R04 Evidenz monoton | **behoben** (laut Issue #5, `CirqaScanEvidence`) | nicht erneut per Test nachvollzogen. |
| R05 Formulierung Adressgleichheit | **behoben** (laut Issue #5) | Anzeige „Gleiche BLE-Adresse im Scan erkannt“. |
| R06 `inspect` ohne CRC | **offen, BLE-Bereich** | `GarminFrameHeader.inspect` bleibt; `GarminFrameValidator` ruft es auf und prüft CRC. → OpenCode. |
| R07 Sync-Checkpoint | **offen, BLE/Protokoll** | Cursor aus Messzeitstempel; relevant erst mit Historien-Download. → OpenCode. |
| R08 Export-Zustand | **behoben** | `ExportActivity.onSaveInstanceState` sichert `pendingWindow`, Executor-Shutdown in `onDestroy`. |
| R09 `neverForLocation` | **offen, dokumentiert** | Manifest unverändert (`BLUETOOTH_SCAN` mit `neverForLocation`); Nichterkennung ist kein Beweis, dass das Band nicht wirbt. Änderung nur nach Datenschutzabwägung. |
| R10 ungenutzte BLE-Helfer | **offen, BLE-Bereich** | Inventur steht aus. → OpenCode. |
| R11 Versionsnummern | **behoben** | `versionCode` aus Gradle-Property (Standard 2), `versionName` „2.0.0_build…“. Release-Pipeline siehe `docs/APK_SIGNING_AND_UPDATES.md`. |
| R12 README | **behoben** | bereits im Review-Update vermerkt. |

Weitere Datenschutzprüfung: Manifest enthält keine `INTERNET`-Berechtigung (Offline-first bestätigt). CSV-Export enthält `device_id` als Spalte (vom Nutzer ausgelöst, lokaler Speicherort) – die Herkunft von `device_id` im Standardexport sollte vor einer Weitergabe von Exporten geprüft werden (offen).
