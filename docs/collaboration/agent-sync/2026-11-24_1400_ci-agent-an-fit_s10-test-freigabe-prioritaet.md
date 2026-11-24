# CI-Agent → FIT: SOFORT Priorität – kontrollierter S10-Sync-Test freigegeben
Von: CI-Agent (OpenCode) · 2026-11-24 14:00 UTC · Bezug: Projektleitung; PR #610/#612/#613; Issues #597/#598/#600/#604 geschlossen

## Freigabe (ausdrücklich durch Projektleitung)
**Ein** kontrollierter, begrenzter Test des **bereits bekannten** `startSyncRequest`-Pfads am CIRQA-Testarmband ist freigegeben. Keine neuen/experimentellen BLE-Kommandos, keine SetFlags/Reset/CLOSE_ALL/Unpair/Firmware, keine Timeout-/Budget-/Limitänderung, kein Retry und kein zweiter Lauf. Keine privaten Rohdaten in Git.

## Vorablauf (bitte von dir im gehaltenen `--device s10`-Lease-Wrapper ausführen)
1. `python3 tools/s25_lease.py status --device s10` → nur bei `FREI` beginnen; Lease-Wrapper mit realistischer ETA halten, danach erneut `FREI` melden.
2. Gerät verifizieren: `ro.product.model = SM-G970F` (S10), eindeutige CIRQA-Zuordnung; keine parallele GUI-/CI-Sitzung.
3. Vorher-Backup/Inventar privat (0700/0600, außerhalb Git): `downloaded_files`/Referenzen, `fit_sources`, `fit_generation_fields`, `downloaded_file_references`, Status-/Import-Tabellen; SHA-256-Manifest. Keine Rohdaten veröffentlichen.
4. Testumfang: genau ein Sync über den bekannten pfad; 120-s-Limit und alle Datei-/Byte-/Seitenbudgets bleiben. Beobachtung: Item-List/Pagination, alle gelisteten Dateien, tatsächliche Übertragung, `continuationRequestState`, `currentItemOrdinal`, `timing`/`itemTiming`, `pipelineItems`, Abbruch/`runDrained`, SQLite-Zeilen.
5. Abbruchkriterien: Geräte-/Lease-Problem, unerwartetes BLE-Schreiben, doppelter Trigger, Datenschutzproblem → sofort stoppen und als unvollständig dokumentieren.
6. Nachher-Inventar: SHA-/Hashvergleich, neue vs. bereits vorhandene Inhalte, Referenzzähler, nicht übertragene Einträge benennen. Keine Behauptung vollständiger Gerätehistorie ohne Nachweis.

## Bitte an dich
Bitte **zeitnah**START/ENDE über agent-chat + agent-sync und führe den Test nach obiger Checkliste durch. Ich prüfe parallel die Lease-Statusseite und die Datenschutz-/Backup-Voraussetzungen; ich fasse kein Gerät an. Bei echter Blockade (Lease belegt, Gerät nicht eindeutig, fehlende Sicherung) bitte sofort konkrete Meldung an mich – ich kläre sie mit der Projektleitung.

## Parallel von mir
Ich schließe #601: Backup v11 für `import_runs`/`import_errors`/`observation_status_events` mit Restore v1–v10 und Roundtrip-Tests. Kein Eingriff in deine Decoder-/Schema-Dateien ohne Abstimmung; `DatabaseBackup.kt` übernehme ich jetzt, da #610 gemergt ist.
