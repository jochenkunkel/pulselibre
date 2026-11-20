#!/usr/bin/env bash
# Run the PulseLibre Android CI directly on this machine – no GitHub, no token,
# no self-hosted runner required.
#
# This wrapper only orchestrates the same phases that GitHub Actions runs
# (tools/ci/run-ci.sh), so LOCAL, CLOUD and offline builds cannot drift apart.
#
# Usage:
#   ./tools/ci-local.sh --quick          # Build, R8 release, unit tests, APK checks
#   ./tools/ci-local.sh --full           # + lint, instrumentation APK, API-35 emulator
#   ./tools/ci-local.sh --full --keep-emulator
#   ./tools/ci-local.sh --bootstrap      # install JDK 21 + Gradle 9.6.0 first
#
# Exit code is 0 only if every executed phase succeeded.
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/ci/lib.sh"

SCOPE="QUICK"
SKIP_PYTHON=0
KEEP_EMULATOR=0
BOOTSTRAP=0

usage() {
  sed -n '2,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

for arg in "$@"; do
  case "$arg" in
    --quick) SCOPE="QUICK" ;;
    --full) SCOPE="FULL" ;;
    --skip-python) SKIP_PYTHON=1 ;;
    --keep-emulator) KEEP_EMULATOR=1 ;;
    --bootstrap) BOOTSTRAP=1 ;;
    -h|--help) usage; exit 0 ;;
    *) ci_die "Unbekannte Option: $arg (siehe --help)" 2 ;;
  esac
done

if [ "$BOOTSTRAP" -eq 1 ]; then
  bash "$CI_REPO_ROOT/tools/ci/bootstrap-toolchain.sh"
fi

START=$SECONDS
PHASES_OK=()

run_phase() {
  local name="$1"
  shift
  if "$@"; then
    PHASES_OK+=("$name")
  else
    local status=$?
    ci_error "Phase '$name' fehlgeschlagen (Exit $status)."
    ci_log "Abgebrochen nach $((SECONDS - START))s. Berichte: $(ci_artifact_dir)"
    exit "$status"
  fi
}

ci_log "PulseLibre CI lokal · Umfang $SCOPE · $(date -Is)"
ci_log "Repository: $CI_REPO_ROOT"

python_phase() {
  if [ "$SKIP_PYTHON" -eq 1 ]; then
    ci_info "Python-Regressionstests übersprungen (--skip-python)"
    return 0
  fi
  bash "$CI_REPO_ROOT/tools/ci/run-ci.sh" python-tests
}

run_phase "preflight" bash "$CI_REPO_ROOT/tools/ci/run-ci.sh" preflight --scope "$SCOPE"
run_phase "python-tests" python_phase
run_phase "build" bash "$CI_REPO_ROOT/tools/ci/run-ci.sh" build --scope "$SCOPE"
run_phase "verify" bash "$CI_REPO_ROOT/tools/ci/run-ci.sh" verify --scope "$SCOPE"
run_phase "diagnostics" bash "$CI_REPO_ROOT/tools/ci/run-ci.sh" diagnostics --scope "$SCOPE"

if [ "$SCOPE" = "FULL" ]; then
  if [ "$KEEP_EMULATOR" -eq 1 ]; then
    run_phase "emulator" bash "$CI_REPO_ROOT/tools/ci/emulator.sh" --keep-running
  else
    run_phase "emulator" bash "$CI_REPO_ROOT/tools/ci/emulator.sh"
  fi
fi

DURATION=$((SECONDS - START))
ci_log "Lokal CI erfolgreich · Umfang $SCOPE · Dauer ${DURATION}s"
ci_info "Phasen: ${PHASES_OK[*]}"
ci_info "Berichte: $(ci_artifact_dir)"
ci_info "APK: app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$CI_REPO_ROOT/app/build/outputs/apk/release/app-release.apk" ]; then
  ci_info "Release-APK: app/build/outputs/apk/release/app-release.apk"
fi
