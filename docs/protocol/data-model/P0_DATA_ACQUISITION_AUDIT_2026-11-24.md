# P0-Audit: vollständige lokale Erfassung aller zugänglichen CIRQA-Daten (24.11.2026)

Autor: CI-Agent (OpenCode). Grundlage: Owner-Entscheidung [`DATA_ACQUISITION_PRIORITY.md`](../../DATA_ACQUISITION_PRIORITY.md), PR #595 (`38d22ce9`). Reine Offline-Prüfung; kein Gerät, keine Rohdatenveröffentlichung.

## 1. Regelwerks-Inventur

| Dokument/Stelle | Aussage | Bewertung | Korrektur |
|---|---|---|---|
| `AGENTS.md:103` | „keine erfundenen Gesundheitswerte", kein Commit von Roh-BLE-Payloads/Adressen/privaten Gesundheitsdaten | **Zulässig** – Integrität und Datenschutz, kein Erfassungsverbot | keine |
| `docs/DATA_ACQUISITION_PRIORITY.md` (neu) | Erfassung vor Interpretation; Qualität als Metadatum | **Verbindlich** | keine |
| `docs/UNIFIED_LOCAL_STORAGE_2026-11-16.md:25` | „Es werden nur beobachtete, validierte Werte gespeichert." | **Widersprüchlich** – liest sich als Persistenzverbot für unvalidierte Werte | Ergänzen: alle beobachteten Werte werden gespeichert; unvalidierte als `UNTRUSTED_CANDIDATE` mit Rohwert/Provenienz; nur validierte Werte fließen in Kennzahlen |
| `FitHistoricalObservations.kt:10` (KDoc) | „Unknown fields are skipped, never guessed or imported." | **Missverständlich** – Code inventarisiert unbekannte Felder und archiviert Rohbytes; KDoc klingt nach Verwerfung | FIT: KDoc präzisieren (Inventar + Roharchiv statt „skipped"); keine Produktlogikänderung ohne Review |
| `docs/protocol/data-model/IST_SOLL_DATENMODELL_2026-11-22.md:108` | „Unbekannte Felder werden nur inventarisiert, nie als Werte importiert" | **Teilweise zulässig** – „nicht als vertrauenswürdige Werte" ist Interpretation; Rohwert darf nicht verloren gehen | Präzisieren: Rohwert im content-addressed Originalarchiv, SQLite-Inventar mit Referenz; Promotion bleibt gesperrt |
| `docs/SCIENCE_DASHBOARD_DESIGN_2026-11-22.md:20` | FIT-Kandidaten fließen nicht in Kennzahlen ein | **Zulässig** – Interpretationsregel | Ergänzen: Persistenz ist gerade Pflicht, nicht untersagt |
| `docs/GROUND_RULES.md:20` / `NUTRITION_RULES.md:18` / `REQUIREMENTS_AND_PRINCIPLES.md:38` | Keine farbigen Alarme, keine Kalorien aus HR ohne validierte Quelle | **Zulässig** – UI/Aussage, nicht Erfassung | Ergänzender Hinweis: erfassungsseitig Rohwerte trotzdem speichern |
| `docs/ARCHITECTURE_AND_CODE_REVIEW.md:86` | „Nur validierte Frames an künftige Parser geben" | **Zulässig** – meint CRC-/Längenvalidierung, keine medizinische Validierung | Klarstellen: Integritätsvalidierung ≠ medizinische Validierung |
| `docs/HISTORICAL_SYNC.md:22`, `docs/TESTPLAN.md:37` | Historischer Download „nicht automatisch/nicht implementiert/freigegeben" | **Veraltet** – Produktpfad `startSyncRequest` existiert und ist gemergt | Historische Aussage datiert lassen, aber aktuellen Stand nachtragen; Sicherheits-Gates bleiben |
| `docs/protocol/sync/CIRQA_HISTORICAL_DOWNLOAD_RELEASE_GATE_2026-11-17.md` | Freigabe erst nach Geräte-CRC-Evidenz | **Zulässig** – Gerätesicherheit/Testfreigabe, nicht medizinische Validierung | keine |

**Ergebnis Regelwerk:** nur `UNIFIED_LOCAL_STORAGE_2026-11-16.md:25` ist ein echter inhaltlicher Widerspruch; fünf weitere Stellen brauchen Klarstellung, damit sie nicht als Erfassungsverbot gelesen werden. Keine Regel darf Datenschutz-, Geräte- oder Hardwarefreigaben abschwächen.

## 2. Datenfluss-Matrix

| Datentyp | Vom Gerät angeboten | Heruntergeladen | Original verlustfrei gespeichert | SQLite inventarisiert/gespeichert | Backup v9 | Restore | Lücke / Grund |
|---|---|---|---|---|---|---|---|
| Echtzeitmesswerte (HR/Schritte/Service 16) | ja (beobachtet) | n/a | **nein** – nur dekodierte `payload_json`, keine rohen Notification-Bytes | ja (`events`) | ja (`cirqa_events.jsonl`) | ja | Roh-BLE-Nutzdaten fehlen im Archiv |
| GATT-Gerätemetadaten (Name, FW, Akku, Status) | ja | n/a | **nein** – nur whitelistete abgeleitete Felder | ja (`live_device_metadata`, `raw_device_metadata`) | ja | ja | Roh-GATT-Bytes nicht archiviert; Batteriequelle #532 offen |
| Item-List / Pagination | ja | ja (Read-only) | **nein** – nur Zähler/Diagnose, keine Rohantwort | teilweise (Zähler im Bericht) | nein | nein | Keine persistente rohe Item-List; Unvollständigkeit bleibt `device-history-unverified` |
| FIT-Dateien | ja | ja | **ja** – `DownloadedFileArchive` (Bytegleich, Hash, Referenz) | ja (`downloaded_files`, `downloaded_file_references`, `fit_sources`) | ja (`downloaded_file_archive.jsonl`) | ja | Auswahl/Budgets und fehlender Resume können Dateien auslassen; 39. Transfer ungeklärt |
| Nicht-FIT-/unbekannte Dateien | ja | ja | **ja** – dasselbe Archiv | ja (Referenzen, `raw_device_metadata` bei XML) | ja | ja | keine Bekanntmachung/Semantik nötig |
| Kanonische FIT-Messwerte (`PROFILE_VALID` + Zeit) | ja | ja | ja (Original im Archiv) | ja (`observations` + Provenienz) | ja | ja | keine |
| Review-Kandidaten (Stress/SpO₂/Atmung) | ja | ja | ja | ja (`fit_review_candidates`) | ja | ja | Keine generationsscope Versionierung; neue Felder (Schlaf, Monitoring) noch nicht committet; FIT-Blocker |
| Unbekannte FIT-/Developer-Felder | ja | ja | ja (Original im Archiv) | **nur Metadaten** (`fit_unmapped_fields`), kein Rohfeldwert | ja | ja | Rohwert nur im Archiv; Verknüpfung via `source_sha256` vorhanden, aber nicht als Kandidat mit Wert |
| Invalid-Sentinel / unsupported encoding | ja | ja | ja (Original im Archiv) | **nein** – nur Zähler/`ExcludedField`-Struktur, kein Rohwert | nein | nein | **P0-Lücke:** vertrauliche/ungültige Werte müssen als Kandidat mit Rohcode+Provenienz persistiert werden |
| `observation_status_events` / `import_runs` / `import_errors` | n/a | n/a | n/a | **nein** – Tabellen existieren, keine INSERT-Pfade | nein | nein | **P0-Lücke:** Status-/Importaudit nicht persistiert |
| Export | n/a | n/a | n/a | `writeRawExport` für gespeicherte Raw-Exporte | n/a | n/a | Prüfen, ob Kandidaten/Unbekannte/Archiv-Referenzen enthalten sind; aktuell nur Raw-Export-Records |

## 3. P0-Reparaturaufträge

1. **Rohwerte mit ungültigem Sentinel/Encoding persistieren** – Owner FIT. Neue Review-Kandidaten mit `raw_code`/`raw_value_text`, `ExclusionReason` als Metadatum, Quellhash, Ordinal, Feldnummer; keine Promotion. Automatisierter Test: synthetische FIT-Fixture mit Sentinel und unsupported encoding → Kandidat in SQLite + Backup-Roundtrip. Issue: [#597](https://github.com/jochenkunkel/pulselibre/issues/597).
2. **Generationsscope Kandidaten + Backup v10** – Owner FIT + DB/Backup. Freigabe mit Auflagen liegt in `agent-sync/2026-11-24_1000_*`; Decoder-Version bei Semantikänderung erhöhen, Digest inkl. Kandidaten, Replay in gleicher Transaktion, v1–v9-Restore. Tests: Migration, Rollback, Restore, Replay, Tampering. Issue: [#598](https://github.com/jochenkunkel/pulselibre/issues/598).
3. **Ressourcenfähiger, unterbrechbarer inkrementeller Abruf** – Owner FIT/CI. 120-s-Limit, 256 Dateien, 64 MiB, 64 Seiten sind Sicherheitsbudgets, dürfen aber nicht zu dauerhaftem Datenverlust führen: sichere Wiederaufnahme mit nachgewiesener Geräte-/Dateiidentität statt naive Skip (Regel 6 der Owner-Entscheidung). Keine BLE-Protokoll- oder Limitänderung ohne Freigabe. Tests: Resume nach Timeout, Duplicate-Skip nur bei verifizierter Identität. Issue: **#148** (bestehend, P0-Upgrade beantragt).
4. **Roh-Notification-/GATT-Bytes lokal archivieren** – Owner FIT/CI. Nur private 0700/0600-Ablage, keine Veröffentlichung; Provenienz/Hash/Referenz in SQLite. Tests: Archiv-Roundtrip, Redaktionsprüfung. Issue: [#600](https://github.com/jochenkunkel/pulselibre/issues/600).
5. **Status-/Importtabellen schreiben** – Owner DB/FIT. `observation_status_events`, `import_runs`, `import_errors` aus dem Importpfad befüllen; keine Gesundheitswerte in Logs. Tests: Commit/Import-Fehler → Statuszeile + Backup/Restore. Issue: [#601](https://github.com/jochenkunkel/pulselibre/issues/601).
6. **Dokumentationskorrekturen** – Owner CI-Agent. Die oben genannten Stellen korrigieren/klarstellen, ohne Schutzregeln abzuschwächen. PR folgt.

## 4. Abnahmekriterium

`alle Daten vorhanden` darf erst behauptet werden, wenn (a) alle nachweislich vom Gerät angebotenen Dateien erfasst und bytegleich archiviert sind, (b) der Replay-/Feldabdeckungsnachweis für alle decoding-relevanten Felder vorliegt und (c) Backup/Restore alle Schichten konsistent wiederherstellt. Bis dahin gilt weiterhin `coverage=device-history-unverified`; die Garmin-Vergleichsanalyse belegt 38 von 39 Pulls bytegleich, nicht die vollständige Gerätehistorie.

Keine eigenen Hardwareaktionen, keine BLE-/Timeout-/Budgetänderungen, keine Veröffentlichung privater Rohdaten in diesem Audit.
