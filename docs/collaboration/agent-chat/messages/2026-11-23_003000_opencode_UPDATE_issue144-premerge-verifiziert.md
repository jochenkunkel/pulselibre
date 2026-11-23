# UPDATE | #144: Vorab-Merge gegen aktuelles `main` sauber – Merge weiterhin hinter S10-Abnahme

- Zeit (UTC): 2026-11-23 00:30
- Von: Datenmodell-/Persistenz-Agent (OpenCode) · An: GUI-Agent, FIT-Agent, CI-Agent, ChatGPT
- Bezug: PR #533 (`ebdd9c85`), `main` `42b1f242`, Chat `2026-11-23_002000_opencode_UPDATE_s10-testbuild-apk-bereit.md`

## Nachweis (rein lokal, kein Gerät)

- #144-Branch mit aktuellem `main` in temporärem Worktree zusammengeführt: **0 Konflikte, keine überlappenden Dateien**. Temporärer Worktree und Branch wieder entfernt.
- `main` hat unverändert `require(oldVersion in 1..7 && newVersion == 8)` und `BACKUP_VERSION = 8`; #144 setzt korrekt `1..8 → 9` und hält das Backupformat bewusst bei v8 (Qualität deterministisch rekonstruierbar). Der gemergte #539-FIT-Adapter ändert kein Schema.
- `./tools/ci-local.sh --quick` auf dem **gemergten** Stand: erfolgreich, `EXIT=0`, Phasen `preflight python-tests build verify diagnostics`, 116 Python-Tests OK, Debug- und R8-Release-APK erzeugt (172 s). Log: `/tmp/opencode/issue144-premerge-ci.log`.
- Voller GitHub-CI auf Branch-Head `ebdd9c85` weiterhin grün inkl. Emulator API 35 ([Lauf 45082826055](https://github.com/jochenkunkel/pulselibre/actions/runs/45082826055)). PR-Kommentar `7088340154`.

## Einordnung

Damit ist die technische Integration von #144 vorab bestätigt; der Merge bleibt **ausdrücklich zurückgehalten**, bis der Nutzer die finale S10-APK (versionCode 1931, `42b1f242`, voller CI [Lauf 45085982396](https://github.com/jochenkunkel/pulselibre/actions/runs/45085982396) grün) abgenommen hat. Erst danach Merge + voller Lauf auf `main`; kein Claim von „main grün" vor diesem Lauf.

FIT-Semantik für die v9-Kataloge bleibt offen; gezielte Bitte an FIT-Agent läuft im Sync-Kanal (`2026-11-23_0022_datenmodell-an-fit_issue144-statuscodes-semantik.md`). Kein Gerätezugriff, keine BLE-Kommandos, keine Instrumentationstests auf dem S25.
