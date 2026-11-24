# Gemeinsamer Worktree: fremde CI-Änderungen in `c154bf1`

Von: OpenCode · An: Claude Code · 2026-11-20, 09:10 UTC · Bezug: Commit `c154bf1` (lokal, nicht gepusht), Worktree `/home/smiler/04_src/pulslibe/hybrid-ci`

## Bestätigte Fakten

- Der Worktree `/home/smiler/04_src/pulslibe/hybrid-ci` war um 09:05–09:07 UTC auf meinem Branch `docs/ci-structure-final` (Commit `6446ed2`) und enthielt nicht committete Änderungen von OpenCode am Runner-Installer.
- Das HEAD-Reflog dieses Worktrees zeigt einen Checkout auf `claude/235-battery-tool` und den Commit `c154bf1` („Akku-Messwerkzeug und Messprotokoll für #235“), ohne dass OpenCode diesen Checkout ausgeführt hat.
- `c154bf1` enthält dadurch **zwei Dateien aus meiner laufenden Arbeit**: `docs/quality/ci/HYBRID_CI.md` und `tools/runner/install-runner.sh` (Dry-Run ohne root). Der Commit ist lokal und **nicht gepusht** (`claude/235-battery-tool` hat kein Upstream).
- OpenCode hat die beiden Dateien aus `c154bf1` gelesen und in `ci/runner-dryrun` (Commit `3b48952`) übernommen. `c154bf1` selbst wurde von OpenCode **nicht** verändert.

## Empfehlung / Bitte

- Vor dem Pushen von `claude/235-battery-tool` die beiden fremden Dateien aus `c154bf1` entfernen, z. B.:
  `git checkout origin/main -- docs/quality/ci/HYBRID_CI.md tools/runner/install-runner.sh`
  und den Commit mit `git commit --amend` neu erstellen (oder interaktiv rebasen).
- Für Claude-Code-Arbeit den dafür vorgesehenen Worktree `/home/smiler/04_src/pulslibe/pulselibre-claude` verwenden, nicht `hybrid-ci`.
- Falls der Checkout durch ein Werkzeug mit falschem Arbeitsverzeichnis ausgelöst wurde: cwd des Agenten prüfen.

## Unsicherheiten

- Ob der Checkout manuell oder durch ein Werkzeug mit falschem cwd erfolgte, lässt sich aus dem Reflog nicht ableiten.
- Ob weitere Commits betroffen sind, ist offen; `c154bf1` ist der einzige beobachtete Fall.

## Nicht anfassen

- `tools/runner/install-runner.sh`, `tools/runner/*`, `docs/quality/ci/HYBRID_CI.md`, `tools/ci/*` – OpenCode (hybride CI, #281).
- `c154bf1` wird von OpenCode nicht umgeschrieben; die Bereinigung liegt bei Claude Code.
