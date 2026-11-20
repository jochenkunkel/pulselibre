#!/usr/bin/env bash
# Boot a headless API-35 emulator, run the instrumentation tests on it and shut
# it down again. Used by `tools/ci-local.sh --full`; GitHub Actions uses
# reactivecircus/android-emulator-runner with the same Gradle phase.
#
# Usage: tools/ci/emulator.sh [--keep-running]
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

KEEP=0
for arg in "$@"; do
  case "$arg" in
    --keep-running) KEEP=1 ;;
    -h|--help) sed -n '2,9p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) ci_die "Unbekannte Option: $arg" 2 ;;
  esac
done

ci_resolve_android_sdk
ci_resolve_java_home

AVD_NAME="${PULSELIBRE_AVD_NAME:-pulselibre-ci-api35}"
EMULATOR_PORT="${PULSELIBRE_EMULATOR_PORT:-5560}"
SERIAL="emulator-$EMULATOR_PORT"
SYSTEM_IMAGE="system-images;android-35;google_apis;x86_64"
BOOT_TIMEOUT="${PULSELIBRE_EMULATOR_BOOT_TIMEOUT:-420}"
WIPE="${PULSELIBRE_EMULATOR_WIPE:-1}"

ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR_BIN="$ANDROID_HOME/emulator/emulator"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
[ -x "$AVDMANAGER" ] || AVDMANAGER="$(command -v avdmanager 2>/dev/null || true)"

[ -x "$ADB" ] || ci_die "adb fehlt: $ADB (platform-tools installieren)"
[ -x "$EMULATOR_BIN" ] || ci_die "Android Emulator fehlt: $EMULATOR_BIN"

if ! { [ -r /dev/kvm ] && [ -w /dev/kvm ]; }; then
  ci_die "KVM nicht nutzbar. Ohne Hardwarevirtualisierung werden keine Emulator-Tests vorgetäuscht. Siehe docs/ci/HYBRID_CI.md#kvm."
fi

REPORT_DIR="$(ci_artifact_dir)"
EMULATOR_LOG="$REPORT_DIR/emulator-$SERIAL.log"

if [ ! -f "$HOME/.android/avd/$AVD_NAME.ini" ]; then
  [ -n "$AVDMANAGER" ] && [ -x "$AVDMANAGER" ] || ci_die "avdmanager nicht gefunden (cmdline-tools installieren)."
  if [ ! -d "$ANDROID_HOME/system-images/android-35/google_apis/x86_64" ]; then
    ci_die "System-Image $SYSTEM_IMAGE fehlt. 'bash tools/ci/ensure-android-sdk.sh --install' ausführen."
  fi
  ci_info "Erzeuge AVD $AVD_NAME ($SYSTEM_IMAGE)"
  printf 'no\n' | "$AVDMANAGER" create avd --force --name "$AVD_NAME" \
    --package "$SYSTEM_IMAGE" --device "pixel_6" >/dev/null
fi

cleanup() {
  if [ "$KEEP" -eq 0 ]; then
    ci_info "Beende Emulator $SERIAL"
    "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1 || true
    sleep 2
    pkill -f "emulator.*-avd $AVD_NAME" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

if "$ADB" devices | grep -q "^$SERIAL[[:space:]]*device"; then
  ci_info "Emulator $SERIAL läuft bereits"
else
  ci_group "Emulator starten ($AVD_NAME, API 35)"
  local_args=(
    -avd "$AVD_NAME"
    -port "$EMULATOR_PORT"
    -no-window
    -no-audio
    -no-boot-anim
    -no-snapshot
    -gpu swiftshader_indirect
    -camera-back none
    -camera-front none
    -netdelay none
    -netspeed full
  )
  if [ "$WIPE" = "1" ]; then
    local_args+=(-wipe-data)
  fi
  nohup "$EMULATOR_BIN" "${local_args[@]}" >"$EMULATOR_LOG" 2>&1 &
  ci_info "Emulator-Log: $EMULATOR_LOG"
  ci_group_end
fi

ci_info "Warte auf Boot von $SERIAL (max. ${BOOT_TIMEOUT}s)"
deadline=$((SECONDS + BOOT_TIMEOUT))
until "$ADB" devices | grep -q "^$SERIAL[[:space:]]*device"; do
  if ! ps -eo args= | grep -q "[e]mulator.*-avd $AVD_NAME"; then
    ci_die "Emulator-Prozess ist beendet. Log: $EMULATOR_LOG"
  fi
  if [ "$SECONDS" -ge "$deadline" ]; then
    ci_die "Emulator wurde nicht innerhalb von ${BOOT_TIMEOUT}s sichtbar. Log: $EMULATOR_LOG"
  fi
  sleep 3
done
until [ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  if [ "$SECONDS" -ge "$deadline" ]; then
    ci_die "Emulator ist nicht innerhalb von ${BOOT_TIMEOUT}s gebootet. Log: $EMULATOR_LOG"
  fi
  sleep 3
done

"$ADB" -s "$SERIAL" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
"$ADB" -s "$SERIAL" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
ci_info "Emulator gebootet: $SERIAL"

export ANDROID_SERIAL="$SERIAL"
bash "$CI_REPO_ROOT/tools/ci/run-ci.sh" emulator --scope FULL
