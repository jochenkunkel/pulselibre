#!/usr/bin/env bash
# Remove the PulseLibre self-hosted runner installation.
#
# By default only the local service and files are removed. The runner is NOT
# removed from GitHub unless --deregister is passed explicitly, so an existing
# registration is never dropped by accident.
#
# Usage:
#   sudo bash tools/runner/uninstall-runner.sh --system [--deregister]
#   bash tools/runner/uninstall-runner.sh --user [--deregister]
#
#   --deregister   also remove the runner registration on GitHub (needs gh auth)
#   --purge-user   also delete the dedicated system user (system mode only)
#   --dry-run      print actions without changing anything
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)/ci/lib.sh"

REPO="jochenkunkel/pulselibre"
MODE="system"
DEREGISTER=0
PURGE_USER=0
DRY_RUN=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --system) MODE="system" ;;
    --user) MODE="user" ;;
    --repo) [ -n "${2:-}" ] || ci_die "--repo braucht OWNER/NAME" 2; REPO="$2"; shift ;;
    --deregister) DEREGISTER=1 ;;
    --purge-user) PURGE_USER=1 ;;
    --dry-run) DRY_RUN=1 ;;
    -h|--help) sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) ci_die "Unbekannte Option: $1" 2 ;;
  esac
  shift
done

run() {
  if [ "$DRY_RUN" -eq 1 ]; then printf '[dry-run] %s\n' "$*"; else "$@"; fi
}

if [ "$MODE" = "system" ]; then
  [ "$(id -u)" -eq 0 ] || ci_die "System-Deinstallation benötigt root." 2
  RUNNER_ROOT="/opt/pulselibre-runner"
  RUNNER_USER="ghrunner"
else
  RUNNER_ROOT="${PULSELIBRE_RUNNER_ROOT:-$HOME/.local/share/pulselibre-runner}"
  RUNNER_USER="$(id -un)"
fi

[ -d "$RUNNER_ROOT" ] || ci_die "Runner-Verzeichnis nicht gefunden: $RUNNER_ROOT" 2

if [ "$DEREGISTER" -eq 1 ] && [ -f "$RUNNER_ROOT/.runner" ]; then
  token="$(gh api -X POST "repos/$REPO/actions/runners/remove-token" --jq .token 2>/dev/null || true)"
  [ -n "$token" ] || ci_die "Kein Remove-Token. 'gh auth login' mit Admin-Rechten ausführen." 2
  ci_info "Entferne Runner-Registrierung bei GitHub"
  if [ "$MODE" = "system" ]; then
    (cd "$RUNNER_ROOT" && sudo -u "$RUNNER_USER" -H ./config.sh remove --token "$token")
  else
    (cd "$RUNNER_ROOT" && ./config.sh remove --token "$token")
  fi
else
  ci_warn "Runner-Registrierung auf GitHub bleibt bestehen (kein --deregister)."
fi

if [ "$MODE" = "system" ]; then
  run bash -c "cd '$RUNNER_ROOT' && ./svc.sh stop" || true
  run bash -c "cd '$RUNNER_ROOT' && ./svc.sh uninstall" || true
else
  run systemctl --user disable --now pulselibre-runner.service || true
  run rm -f "${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user/pulselibre-runner.service"
  run systemctl --user daemon-reload || true
fi

run rm -rf "$RUNNER_ROOT"

if [ "$MODE" = "system" ] && [ "$PURGE_USER" -eq 1 ]; then
  run userdel "$RUNNER_USER"
fi

ci_log "Deinstallation abgeschlossen"
