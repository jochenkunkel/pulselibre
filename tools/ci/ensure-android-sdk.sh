#!/usr/bin/env bash
# Verify (and optionally install) the Android SDK components the CI needs.
#
# CLOUD runners ship a preinstalled SDK: this script only verifies it.
# LOCAL runners can install missing packages with --install.
#
# Usage: tools/ci/ensure-android-sdk.sh [--install]
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

INSTALL=0
for arg in "$@"; do
  case "$arg" in
    --install) INSTALL=1 ;;
    -h|--help) sed -n '2,10p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) ci_die "Unbekannte Option: $arg" 2 ;;
  esac
done

ci_resolve_android_sdk

PACKAGES=(
  "platform-tools"
  "platforms;android-37.0"
  "platforms;android-35"
  "build-tools;35.0.0"
  "build-tools;36.0.0"
  "emulator"
  "system-images;android-35;google_apis;x86_64"
)

missing=()
for package in "${PACKAGES[@]}"; do
  path=""
  case "$package" in
    "platforms;"*) path="platforms/${package#platforms;}" ;;
    "build-tools;"*) path="build-tools/${package#build-tools;}" ;;
    "system-images;"*)
      path="system-images/${package#system-images;}"
      path="${path//;//}"
      ;;
    "platform-tools") path="platform-tools" ;;
    "emulator") path="emulator" ;;
  esac
  if [ ! -e "$ANDROID_HOME/$path" ]; then
    missing+=("$package")
  fi
done

if [ "${#missing[@]}" -eq 0 ]; then
  ci_info "Android SDK vollständig: $ANDROID_HOME"
  exit 0
fi

ci_warn "Fehlende Android-SDK-Pakete: ${missing[*]}"
if [ "$INSTALL" -ne 1 ]; then
  ci_info "Nicht installiert (kein --install). Der Build kann fortgesetzt werden; der Emulator-Job installiert fehlende Pakete bei Bedarf."
  exit 0
fi

sdkmanager="$(ci_android_tools)"
[ -x "$sdkmanager" ] || ci_die "sdkmanager nicht gefunden. Android cmdline-tools installieren."

yes | "$sdkmanager" --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
"$sdkmanager" --sdk_root="$ANDROID_HOME" "${missing[@]}"
ci_info "Android SDK ergänzt: $ANDROID_HOME"
