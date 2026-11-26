#!/usr/bin/env bash
# The single implementation of the PulseLibre Android CI pipeline.
#
# GitHub Actions (LOCAL and CLOUD) and tools/ci-local.sh call exactly these
# phases, so both execution environments run the same commands, in the same
# order, with the same diagnostics.
#
# Usage: tools/ci/run-ci.sh <phase> [--scope QUICK|FULL]
#
#   preflight      verify toolchain, SDK components and (for FULL) KVM
#   python-tests   offline validator, workflow structure, runner safety tests
#   build          unit tests + debug APK + R8 release APK + JVM coverage
#   verify         Android lint + instrumentation test APK (FULL only)
#   diagnostics    non-secret APK/signing/R8 checks
#   emulator       connected instrumentation tests + coverage gate + combined coverage (FULL only)
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
# shellcheck source=tools/ci/signing.sh
source "$CI_REPO_ROOT/tools/ci/signing.sh"
trap ci_signing_cleanup EXIT

# Gradle and the coverage gate expect the repository root as working directory,
# no matter where the caller started the script.
cd "$CI_REPO_ROOT"

PHASE="${1:-}"
[ -n "$PHASE" ] || ci_die "Aufruf: tools/ci/run-ci.sh <preflight|python-tests|build|verify|diagnostics|emulator> [--scope QUICK|FULL]" 2
shift || true

SCOPE="${CI_SCOPE:-QUICK}"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --scope) SCOPE="$(printf '%s' "${2:-}" | tr '[:lower:]' '[:upper:]')"; shift 2 ;;
    *) ci_die "Unbekannte Option: $1" 2 ;;
  esac
done
case "$SCOPE" in
  QUICK|FULL) ;;
  *) ci_die "Ungültiger Testumfang: $SCOPE (erlaubt: QUICK, FULL)" 2 ;;
esac
export CI_SCOPE="$SCOPE"

VERSION_CODE="$(ci_version_code)"
export PULSELIBRE_VERSION_CODE="$VERSION_CODE"

preflight() {
  ci_group "Vorprüfung (JDK, Gradle, Android SDK)"
  ci_resolve_java_home
  ci_resolve_gradle
  ci_resolve_android_sdk
  local build_tools
  build_tools="$(ci_android_build_tools)"
  ci_info "Android build-tools: $build_tools"

  local missing=0 component
  for component in "platforms/android-37.0" "platforms/android-35" "platform-tools"; do
    if [ ! -d "$ANDROID_HOME/$component" ]; then
      ci_warn "Android SDK-Komponente fehlt: $component"
      missing=1
    fi
  done
  if [ "$missing" -ne 0 ]; then
    ci_info "Fehlende Komponenten lassen sich mit 'bash tools/ci/ensure-android-sdk.sh' installieren."
  fi

  if [ "$SCOPE" = "FULL" ]; then
    if [ -r /dev/kvm ] && [ -w /dev/kvm ]; then
      ci_info "KVM: verfügbar (/dev/kvm beschreibbar)"
    else
      ci_warn "KVM nicht beschreibbar: Emulator-Tests benötigen /dev/kvm (Gruppe 'kvm' oder udev-Regel)."
    fi
  fi

  local free_gb
  free_gb="$(df -Pk "$CI_REPO_ROOT" | awk 'NR==2 {printf "%d", $4/1024/1024}')"
  ci_info "Freier Speicher im Arbeitsverzeichnis: ${free_gb} GB"
  if [ "${free_gb:-0}" -lt 5 ]; then
    ci_warn "Weniger als 5 GB freier Speicher: Build/Emulator kann fehlschlagen."
  fi
  ci_group_end
}

python_tests() {
  ci_group "Python-Regressionstests (Offline-Validatoren, Workflow-Struktur, Runner-Sicherheit)"
  if ! python3 -c 'import yaml' >/dev/null 2>&1; then
    ci_die "PyYAML fehlt (python3 -c 'import yaml'). Installation: sudo apt-get install python3-yaml" 2
  fi
  python3 -m unittest discover -s "$CI_REPO_ROOT/tools" -p 'test_*.py' -v
  ci_group_end
}

build() {
  ci_group "Build + JVM-Unit-Tests (Scope $SCOPE)"
  ci_signing_restore
  ci_info "Signierung: $(ci_signing_status)"
  ci_gradle \
    :app:testDebugUnitTest \
    :app:assembleDebug \
    :app:assembleRelease \
    :app:createDebugUnitTestCoverageReport \
    "-PpulselibreVersionCode=$VERSION_CODE" \
    --stacktrace
  ci_group_end
}

verify() {
  if [ "$SCOPE" != "FULL" ]; then
    ci_info "Scope QUICK: Lint und Instrumentation-Test-APK werden übersprungen (kein Test wird umgangen, sie gehören zu FULL)."
    return 0
  fi
  ci_group "Android Lint + Instrumentation-Test-APK"
  ci_gradle :app:lintDebug :app:assembleDebugAndroidTest --stacktrace
  ci_group_end
}

diagnostics() {
  ci_group "APK-Diagnose (ohne Secrets)"
  local build_tools apksigner aapt debug_apk release_apk release_cert debug_cert
  # build and diagnostics are separate processes in local CI; restore the
  # local signing environment again before comparing APK certificates.
  ci_signing_restore
  build_tools="$(ci_android_build_tools)"
  apksigner="$build_tools/apksigner"
  aapt="$build_tools/aapt"
  debug_apk="$CI_REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"
  [ -f "$debug_apk" ] || ci_die "Debug-APK fehlt: $debug_apk"
  release_apk="$(find "$CI_REPO_ROOT/app/build/outputs/apk/release" -maxdepth 1 -type f -name 'app-release*.apk' -print -quit 2>/dev/null || true)"
  [ -n "$release_apk" ] || ci_die "Release-APK fehlt (R8-Build nicht erzeugt)."

  echo "Package: de.pulselibre"
  echo "Version code: $VERSION_CODE"
  echo "Signierung konfiguriert: $(ci_signing_status)"

  "$apksigner" verify --print-certs "$debug_apk" | grep -E 'Signer #1 certificate SHA-256 digest|Verified using'
  "$aapt" dump badging "$debug_apk" | grep '^package:'

  local mapping="$CI_REPO_ROOT/app/build/outputs/mapping/release/mapping.txt"
  [ -f "$mapping" ] || ci_die "R8-Mapping fehlt: $mapping"
  "$aapt" dump badging "$release_apk" | grep '^package:'
  if "$aapt" dump badging "$release_apk" | grep -q 'application-debuggable'; then
    ci_die "Release-APK ist debuggable."
  fi

  if ci_signing_ready; then
    release_cert="$("$apksigner" verify --print-certs "$release_apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
    debug_cert="$("$apksigner" verify --print-certs "$debug_apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
    if [ -z "$release_cert" ] || [ "$release_cert" != "$debug_cert" ]; then
      ci_die "Release-APK-Zertifikat weicht vom etablierten Debug-Update-Zertifikat ab."
    fi
    echo "APK_SIGNING_READY=true"
    if [ -n "${GITHUB_ENV:-}" ]; then echo "APK_SIGNING_READY=true" >>"$GITHUB_ENV"; fi
  else
    echo "APK_SIGNING_READY=false"
    if [ -n "${GITHUB_ENV:-}" ]; then echo "APK_SIGNING_READY=false" >>"$GITHUB_ENV"; fi
  fi
  ci_group_end
}

emulator() {
  if [ "$SCOPE" != "FULL" ]; then
    ci_die "Emulator-Phase nur mit --scope FULL zulässig." 2
  fi
  ci_group "Emulator-Instrumentationstests (API 35)"
  if ! { [ -r /dev/kvm ] && [ -w /dev/kvm ]; }; then
    ci_die "KVM nicht verfügbar: Emulator-Tests können nicht ehrlich ausgeführt werden. Siehe docs/quality/ci/HYBRID_CI.md#kvm."
  fi
  # Fail-closed safety gate: instrumentation tests must never overwrite a
  # physical test device's profile/settings. Extracted into its own script so
  # tools/test_device_safety_gate.py can exercise every branch offline.
  bash "$CI_REPO_ROOT/.github/scripts/require-emulator-serial.sh" || \
    ci_die "Instrumentationstests auf physischem Gerät verhindert."
  # On failure name the failing tests in the log (the emulator's GPU "bad color buffer" lines are noise, also in green runs).
  ci_gradle :app:connectedDebugAndroidTest --stacktrace || {
    rc=$?
    python3 "$CI_REPO_ROOT/tools/ci/failed_instrumentation_tests.py" "$CI_REPO_ROOT/app/build/outputs/androidTest-results" || true
    exit "$rc"
  }
  bash "$CI_REPO_ROOT/.github/scripts/verify-instrumentation-coverage.sh"
  ci_group_end

  # #6: record both coverage sources with the classes compiled in this job.
  # The build job's JVM exec file cannot be reused here (different compilation
  # identities would silently produce a 0 % combined report).
  ci_group "Kombinierte JVM/Emulator-Coverage (#6)"
  ci_gradle :app:testDebugUnitTest :app:createCombinedCoverageReport --stacktrace
  ci_group_end
}

case "$PHASE" in
  preflight) preflight ;;
  python-tests) python_tests ;;
  build) build ;;
  verify) verify ;;
  diagnostics) diagnostics ;;
  emulator) emulator ;;
  *) ci_die "Unbekannte Phase: $PHASE" 2 ;;
esac
