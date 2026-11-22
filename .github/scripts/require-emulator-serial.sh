#!/usr/bin/env bash
# Fail closed before instrumentation tests if they could run on a physical device.
# Instrumentation tests overwrite profile/settings values, so they are only
# allowed on an emulator. This guard is called by tools/ci/run-ci.sh before
# :app:connectedDebugAndroidTest and is covered by tools/test_device_safety_gate.py.
set -euo pipefail

if [ -n "${ANDROID_SERIAL:-}" ]; then
  case "$ANDROID_SERIAL" in
    emulator-*)
      echo "Instrumentation-Coverage: ANDROID_SERIAL=$ANDROID_SERIAL (Emulator)"
      exit 0
      ;;
    *)
      echo "::error::ANDROID_SERIAL=$ANDROID_SERIAL ist kein Emulator; Instrumentationstests nur mit emulator-... zulässig."
      exit 1
      ;;
  esac
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "::error::adb nicht gefunden: Instrumentationstests können nicht auf einen Emulator begrenzt werden."
  exit 1
fi

physical="$(adb devices | awk 'NR > 1 && $2 == "device" && $1 !~ /^emulator-/ { print $1; exit }')"
if [ -n "$physical" ]; then
  echo "::error::Physisches Android-Gerät sichtbar ($physical): Instrumentationstests nur mit ANDROID_SERIAL=emulator-... ausführen."
  exit 1
fi

echo "Instrumentation-Coverage: kein physisches Gerät sichtbar"
exit 0
