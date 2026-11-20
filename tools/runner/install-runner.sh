#!/usr/bin/env bash
# Install the official GitHub Actions self-hosted runner for PulseLibre.
#
# Default mode is a hardened SYSTEM installation with a dedicated, non-login
# user (recommended). `--user` installs into the current user's home instead.
#
# The runner is NOT registered while the repository is public: that would let
# fork code execute on this machine. Pass --allow-public only if you have
# consciously accepted that risk.
#
# Usage:
#   sudo bash tools/runner/install-runner.sh --system [--android-sdk DIR]
#   bash tools/runner/install-runner.sh --user [--android-sdk DIR]
#
# Common options:
#   --repo OWNER/NAME        default: jochenkunkel/pulselibre
#   --labels LABELS          default: pulselibre
#   --name NAME              default: <hostname>-pulselibre
#   --runner-version V       default: latest release
#   --sha256 HEX             optional pinned checksum of the runner archive
#   --android-sdk DIR        existing Android SDK to expose to jobs
#   --java-home DIR          existing JDK 21 to expose to jobs
#   --token-file FILE        registration token (otherwise fetched via gh)
#   --allow-public           permit registration while the repo is public
#   --replace                replace an existing runner with the same name
#   --dry-run                print actions without changing anything
#
# Registration tokens are short-lived (~1h) and are never written to disk or
# logged by this script.
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)/ci/lib.sh"

REPO="jochenkunkel/pulselibre"
LABELS="pulselibre"
RUNNER_NAME="$(hostname -s 2>/dev/null || hostname)-pulselibre"
RUNNER_VERSION=""
SHA256_ARG=""
ANDROID_SDK=""
JAVA_HOME_ARG=""
TOKEN_FILE=""
MODE="system"
ALLOW_PUBLIC=0
REPLACE=0
DRY_RUN=0

need_value() {
  [ "$#" -ge 2 ] && [ -n "${2:-}" ] || ci_die "Option $1 benötigt einen Wert." 2
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --system) MODE="system" ;;
    --user) MODE="user" ;;
    --repo) need_value "$@"; REPO="$2"; shift ;;
    --labels) need_value "$@"; LABELS="$2"; shift ;;
    --name) need_value "$@"; RUNNER_NAME="$2"; shift ;;
    --runner-version) need_value "$@"; RUNNER_VERSION="$2"; shift ;;
    --sha256) need_value "$@"; SHA256_ARG="$2"; shift ;;
    --android-sdk) need_value "$@"; ANDROID_SDK="$2"; shift ;;
    --java-home) need_value "$@"; JAVA_HOME_ARG="$2"; shift ;;
    --token-file) need_value "$@"; TOKEN_FILE="$2"; shift ;;
    --allow-public) ALLOW_PUBLIC=1 ;;
    --replace) REPLACE=1 ;;
    --dry-run) DRY_RUN=1 ;;
    -h|--help) sed -n '2,33p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) ci_die "Unbekannte Option: $1" 2 ;;
  esac
  shift
done

run() {
  if [ "$DRY_RUN" -eq 1 ]; then
    printf '[dry-run] %s\n' "$*"
  else
    "$@"
  fi
}

if [ "$MODE" = "system" ]; then
  if [ "$(id -u)" -ne 0 ] && [ "$DRY_RUN" -ne 1 ]; then
    ci_die "System-Installation benötigt root: sudo bash tools/runner/install-runner.sh --system"
  fi
  RUNNER_ROOT="/opt/pulselibre-runner"
  RUNNER_USER="ghrunner"
else
  RUNNER_ROOT="${PULSELIBRE_RUNNER_ROOT:-$HOME/.local/share/pulselibre-runner}"
  RUNNER_USER="$(id -un)"
fi

# --- Safety gate: never expose a self-hosted runner to public fork code -----
visibility=""
if command -v gh >/dev/null 2>&1; then
  visibility="$(gh api "repos/$REPO" --jq '.visibility' 2>/dev/null || true)"
fi
if [ "$visibility" = "public" ] && [ "$ALLOW_PUBLIC" -ne 1 ]; then
  ci_die "Repository $REPO ist ÖFFENTLICH. Ein Self-hosted Runner darf keinen fremden Code ausführen. Erst privat schalten oder bewusst --allow-public setzen (siehe docs/ci/HYBRID_CI.md#sicherheit)."
fi
if [ -z "$visibility" ]; then
  ci_warn "Repository-Sichtbarkeit konnte nicht geprüft werden (gh nicht authentifiziert?). Vor dem Start manuell prüfen."
fi

# --- Registration token -----------------------------------------------------
token=""
if [ -n "${RUNNER_TOKEN:-}" ]; then
  token="$RUNNER_TOKEN"
elif [ -n "$TOKEN_FILE" ]; then
  [ -r "$TOKEN_FILE" ] || ci_die "Token-Datei nicht lesbar: $TOKEN_FILE"
  token="$(tr -d '[:space:]' <"$TOKEN_FILE")"
elif command -v gh >/dev/null 2>&1; then
  token="$(gh api -X POST "repos/$REPO/actions/runners/registration-token" --jq .token 2>/dev/null || true)"
fi
[ -n "$token" ] || ci_die "Kein Registrierungs-Token. 'gh auth login' ausführen oder --token-file/RUNNER_TOKEN verwenden."
ci_info "Registrierungs-Token vorhanden (wird nicht protokolliert)"

# --- Dedicated system user --------------------------------------------------
if [ "$MODE" = "system" ]; then
  if ! id -u "$RUNNER_USER" >/dev/null 2>&1; then
    ci_info "Lege Systembenutzer $RUNNER_USER an (keine Login-Shell)"
    run useradd --system --create-home --home-dir "$RUNNER_ROOT" \
      --shell /usr/sbin/nologin "$RUNNER_USER"
  fi
fi

if [ "$MODE" = "system" ] && [ "$DRY_RUN" -eq 1 ] && [ "$(id -u)" -ne 0 ]; then
  # Ohne root lässt sich der Ablauf vollständig anzeigen, ohne /opt anzufassen.
  ci_warn "Dry-Run ohne root: $RUNNER_ROOT wird nicht angelegt; die echte Installation benötigt sudo."
else
  mkdir -p "$RUNNER_ROOT"
fi
if [ "$MODE" = "system" ]; then
  run chown -R "$RUNNER_USER:$RUNNER_USER" "$RUNNER_ROOT"
  run chmod 750 "$RUNNER_ROOT"
fi

# --- Runner package ---------------------------------------------------------
if [ -z "$RUNNER_VERSION" ]; then
  RUNNER_VERSION="$(curl -fsSL https://api.github.com/repos/actions/runner/releases/latest |
    sed -n 's/.*"tag_name": *"v\([^"]*\)".*/\1/p' | head -n1)"
fi
[ -n "$RUNNER_VERSION" ] || ci_die "Runner-Version konnte nicht ermittelt werden."
archive="actions-runner-linux-x64-$RUNNER_VERSION.tar.gz"
url="https://github.com/actions/runner/releases/download/v$RUNNER_VERSION/$archive"

ci_info "Runner-Version: $RUNNER_VERSION"
if [ ! -x "$RUNNER_ROOT/bin/Runner.Listener" ] || [ ! -f "$RUNNER_ROOT/.runner-version" ] ||
   [ "$(cat "$RUNNER_ROOT/.runner-version" 2>/dev/null)" != "$RUNNER_VERSION" ]; then
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  ci_info "Lade $archive"
  curl -fsSL --retry 3 -o "$tmp/$archive" "$url"

  expected="$SHA256_ARG"
  if [ -z "$expected" ] && curl -fsSL --retry 2 -o "$tmp/$archive.sha256" "$url.sha256" 2>/dev/null; then
    expected="$(cut -d' ' -f1 "$tmp/$archive.sha256")"
  fi
  actual="$(sha256sum "$tmp/$archive" | cut -d' ' -f1)"
  if [ -n "$expected" ]; then
    [ "$actual" = "$expected" ] || ci_die "SHA-256 des Runner-Pakets stimmt nicht (erwartet $expected, erhalten $actual)."
    ci_info "SHA-256 geprüft: $actual"
  else
    # GitHub veröffentlicht für manche Releases keine .sha256-Datei. Dann wird
    # der Hash angezeigt und im Runner-Verzeichnis zur späteren Prüfung abgelegt.
    ci_warn "Keine Hersteller-Prüfsumme verfügbar. SHA-256 des Archivs: $actual (mit --sha256 <hex> festnageln)"
    if [ "$DRY_RUN" -eq 0 ]; then printf '%s  %s\n' "$actual" "$archive" >"$RUNNER_ROOT/.runner-archive.sha256"; fi
  fi
  run tar -xzf "$tmp/$archive" -C "$RUNNER_ROOT"
  if [ "$DRY_RUN" -eq 0 ]; then printf '%s\n' "$RUNNER_VERSION" >"$RUNNER_ROOT/.runner-version"; fi
fi

# --- Runtime dependencies ---------------------------------------------------
missing_libs="$(ldd "$RUNNER_ROOT/bin/Runner.Listener" 2>/dev/null | grep 'not found' || true)"
if [ -n "$missing_libs" ]; then
  ci_warn "Fehlende Bibliotheken: $missing_libs"
  ci_info "Installieren: apt-get install -y libicu-dev libssl-dev (oder passende Pakete der Distribution)"
fi

# --- Runner environment (read by every job) ---------------------------------
env_file="$RUNNER_ROOT/.env"
env_lines=()
[ -n "$ANDROID_SDK" ] && env_lines+=("ANDROID_HOME=$ANDROID_SDK" "ANDROID_SDK_ROOT=$ANDROID_SDK")
[ -n "$JAVA_HOME_ARG" ] && env_lines+=("JAVA_HOME=$JAVA_HOME_ARG")
if [ "${#env_lines[@]}" -gt 0 ]; then
  ci_info "Schreibe Runner-Umgebung: $env_file"
  if [ "$DRY_RUN" -eq 1 ]; then
    printf '[dry-run] %s\n' "${env_lines[@]}"
  else
    printf '%s\n' "${env_lines[@]}" >"$env_file"
    chmod 640 "$env_file"
    [ "$MODE" = "system" ] && chown "$RUNNER_USER:$RUNNER_USER" "$env_file"
  fi
fi

# --- Configure --------------------------------------------------------------
configure=(
  ./config.sh
  --url "https://github.com/$REPO"
  --token "$token"
  --name "$RUNNER_NAME"
  --labels "$LABELS"
  --work _work
  --unattended
)
[ "$REPLACE" -eq 1 ] && configure+=(--replace)

if [ -f "$RUNNER_ROOT/.runner" ]; then
  ci_info "Runner ist bereits konfiguriert ($RUNNER_ROOT/.runner vorhanden) – Registrierung wird nicht überschrieben."
else
  ci_info "Registriere Runner '$RUNNER_NAME' für $REPO mit Labels '$LABELS'"
  if [ "$MODE" = "system" ]; then
    if [ "$DRY_RUN" -eq 1 ]; then
      printf '[dry-run] sudo -u %s ./config.sh --url https://github.com/%s --token *** --name %s --labels %s --work _work --unattended\n' \
        "$RUNNER_USER" "$REPO" "$RUNNER_NAME" "$LABELS"
    else
      (cd "$RUNNER_ROOT" && sudo -u "$RUNNER_USER" -H "${configure[@]}")
    fi
  elif [ "$DRY_RUN" -eq 1 ]; then
    printf '[dry-run] (cd %s && ./config.sh --url https://github.com/%s --token *** --name %s --labels %s --work _work --unattended)\n' \
      "$RUNNER_ROOT" "$REPO" "$RUNNER_NAME" "$LABELS"
  else
    (cd "$RUNNER_ROOT" && "${configure[@]}")
  fi
fi

# --- Service ----------------------------------------------------------------
if [ "$MODE" = "system" ]; then
  ci_info "Installiere systemd-Dienst für Benutzer $RUNNER_USER"
  run bash -c "cd '$RUNNER_ROOT' && ./svc.sh install '$RUNNER_USER'"
  run bash -c "cd '$RUNNER_ROOT' && ./svc.sh start"
  ci_info "Status: systemctl status 'actions.runner.*'"
else
  unit_dir="${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user"
  unit_file="$unit_dir/pulselibre-runner.service"
  ci_info "Installiere systemd-Benutzerdienst: $unit_file"
  if [ "$DRY_RUN" -eq 0 ]; then
    mkdir -p "$unit_dir"
    cat >"$unit_file" <<EOF
[Unit]
Description=PulseLibre self-hosted GitHub Actions runner
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=$RUNNER_ROOT
ExecStart=$RUNNER_ROOT/run.sh
Restart=always
RestartSec=5
KillMode=process
KillSignal=SIGTERM
TimeoutStopSec=300

[Install]
WantedBy=default.target
EOF
    systemctl --user daemon-reload
    systemctl --user enable --now pulselibre-runner.service
  fi
  ci_info "Autostart beim Hochfahren: sudo loginctl enable-linger $RUNNER_USER"
  ci_info "Status: systemctl --user status pulselibre-runner"
fi

ci_log "Fertig"
ci_info "Runner-Label: $LABELS"
ci_info "Arbeitsverzeichnis: $RUNNER_ROOT/_work (keine Secrets dort ablegen)"
ci_info "Aktivierung in GitHub: Repository-Variable PULSELIBRE_LOCAL_RUNNER_ENABLED=true setzen"
