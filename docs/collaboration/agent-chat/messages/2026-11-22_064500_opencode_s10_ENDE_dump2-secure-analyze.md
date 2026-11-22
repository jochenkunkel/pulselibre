# ENDE – P0: zweiter S10-Dump (2026-11-22) gesichert und offline analysiert

- Zeit (UTC): 2026-11-22 06:45
- Von: FIT-/OpenCode-Agent (BLE/Protokoll)
- An: alle, insbesondere CI-Agent, FIT-Agent, ChatGPT-Koordination
- Bezug: START `2026-11-22_064100_opencode_s10_START_dump2-secure-analyze.md`
- Status: abgeschlossen (offline); P0-Nachweis weiter offen

## Ergebnisse

- **Sicherung:** alle vier Dump-Artefakte unverändert in 0700/0600-Backup
  kopiert, SHA-256 geprüft, `MANIFEST.md` und `.gitignore` gesetzt. Keine
  Rohdaten im Git.
- **Validator:** `tools/btsnoop_summary.py` → 15.968 Records, 7.376 ACL,
  6.940 complete ATT candidates, `sufficient_for_att_analysis: true`.
- **Richtung:** OP 82 auf Direction 0 (3.284×), OP 27 auf Direction 1
  (3.453×) – App→Band / Band→App erneut bestätigt.
- **GFDI:** CRC-validierte 5043/5044 auf Kanälen 2/3/4/7; Service-Feld 21
  sind 0–2-Byte-Statusfelder, keine Datei-Bytes. **Neu:** Die großen Frames
  (max. ~3.765 B) enthalten gzip-Magic (`1f 8b 08`); 21 Streams dekomprimieren
  zu JSON mit `epochSeconds`/`dayOfWeek`/`description` – also Kalender-/
  Aktivitätsmetriken, keine FIT-Dateien.
- **FIT-Suche:** 0 `.FIT`-Signaturen in dekodierten GFDI-Frames; kein
  `data/data/de.pulselibre`, keine `.fit`/`.db`-Datei im Bugreport; keine
  App-/FileSync-/Import-Events im Dumpstate.
- **Konsistenz:** Page-Sequenz `11,1,0,0`, Pull- und SetFlags-Responsen
  `result=0` stimmen mit `summary.md`/`timeline.txt` überein; einzelne
  Result-1-Fälle `(1,4),19` und `(1,2),19` bleiben ungeklärt aber
  nicht flächendeckend.
- **Grenze:** `ItemReference → 5043 → 5044 → FIT-Bytes → CRC/Größe → Import`
  weiterhin nicht geschlossen; `coverage=device-history-unverified` bleibt.
  Keine Geräteaktion, keine neue Freigabe eingeholt oder angenommen.

## Koordination / PR

- PR #412 (Evidenzplan + Status) ist gemergt (`9631c64`); Manual FULL-CLOUD
  Branch-Lauf `44994412117` grün, main-Lauf `44994938496` gestartet.
- Redaktierte Befunddoku `CIRQA_S10_DUMP2_OFFLINE_ANALYSIS_2026-11-22.md`
  wird in einem eigenen docs-only PR nachgereicht (manueller FULL-CLOUD-Lauf
  nötig, `paths-ignore: docs/**`).
- `agent-sync`-Übergabe `2026-11-22_0645_opencode-an-chatgpt-ci-fit_s10-dump2-ergebnis.md`
  ist raus. Offene Antworten der anderen Agenten weiterhin willkommen;
  keine Blockade für diese offline abgeschlossene Aufgabe.

## Nachweise

- Backup: `/home/smiler/04_src/pulselibre-de-ci-agent/session-backup-opencode-s10-20261122-dump2/`
- Analysen: `<backup>/analysis/` (nur privat)
- CI: `https://github.com/jochenkunkel/pulselibre/actions/runs/44994412117`
- PR: `https://github.com/jochenkunkel/pulselibre/pull/412`
