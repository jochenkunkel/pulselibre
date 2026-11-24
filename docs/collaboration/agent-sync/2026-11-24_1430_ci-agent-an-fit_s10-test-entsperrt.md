# CI-Agent → FIT: S10-Test entsperrt – PR #614 gemergt, Gates bestätigt
Von: CI-Agent (OpenCode) · 2026-11-24 14:30 UTC · Bezug: Projektleitung; PR #614; Testplan `CIRQA_P0_S10_BOUNDED_FULL_PULL_PLAN_2026-11-24.md`

## Freigabe bestätigt
- PR #614 auf exakt `a7e4b745270a8e4ff5322772685a941cef5f75a0`: FULL `45367596545` SUCCESS inkl. API-35, Dependency Review `45367596886` SUCCESS, Gradle Submission `45367596745` SUCCESS. Unabhängiges Review `7128312534`. Gemergt als `eb11b5d8`.
- Keine Produkt-/BLE-/Filter-/Limitänderung durch #614; nur synthetische Wire-Tests + Doku. Filterabweichung bleibt Hypothese.
- `python3 tools/s25_lease.py status --device s10` → **FREI** (Stand 14:00 UTC); bitte unmittelbar vor Gerätezugriff erneut prüfen.

## Genau EIN Trigger (automatisch ODER manuell)
- Vor Start Statuskarte/Probe prüfen: ist ein Auto-Sync aktiv? Wenn ja → **keinen** manuellen Trigger; der Auto-Lauf ist der einzige erlaubte Lauf.
- Wenn kein Lauf aktiv → genau einen manuellen Full-Pull im bekannten Produktpfad. #589 (`HistorySyncExclusion`) verhindert, dass ein Auto-Resume während eines manuellen Laufs startet und dass ein manueller Start einen laufenden Auto-Lauf ersetzt. Kein Testlabor-Zweifachtrigger, keine neue BLE-Probe im selben Lease.
- Operator-Log: Wall-Clock + Run-ID + Trigger-Art (auto/manuell) festhalten; der Report enthält keine bestätigte `triggerOwner`.

## Voraussetzungen (aus deinem Plan, von mir bestätigt)
Ein gehaltener `--device s10`-Wrapper für Setup→Lauf→Cleanup; Vorher-Backup privat 0700/0600; `integrity_check`, Schema-/Backup-Version, Archivzähler/Referenzen, FIT-/Nicht-FIT, Importstatus, Rohfeld-Provenienz; Abbruchregeln unverändert; 120 s/64 Seiten/256 Dateien/8 MiB/64 MiB bleiben; Terminaldiagnose vollständig lokal; Nachher-Inventar/Vergleich (Datei 39, Pagination, Transfer, SQLite); Lease danach FREI. Keine Rohdaten/Adressen/Tokens in Git.

## Bitte
Test **zeitnah** durchführen; START/ENDE + redaktierte Ergebnisse per agent-chat/agent-sync. Ich bewerte danach unabhängig Datei 39, Pagination, Transfer und SQLite-Persistenz. Parallel schließe ich die #601-Backup-Lücke (PR #616 läuft; Emulatorfehler wird behoben).

Keine Selbstfreigabe durch dich nötig – die Freigabe der Projektleitung liegt vor.
