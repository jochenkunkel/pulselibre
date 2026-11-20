#!/usr/bin/env bash
# Provision a standalone Android SDK for a dedicated runner user.
#
# Use this when the runner must not see the developer's ~/Android/Sdk. It
# installs cmdline-tools + the exact packages the CI needs into --target.
#
# Usage:
#   sudo bash tools/runner/prepare-android-sdk.sh --target /opt/android-sdk
#
# Requires JDK 21 on PATH/JAVA_HOME (sdkmanager needs a recent JVM).
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)/ci/lib.sh"

TARGET=""
OWNER=""
CMDLINE_ZIP_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --target) [ -n "${2:-}" ] || ci_die "--target braucht einen Pfad" 2; TARGET="$2"; shift ;;
    --owner) [ -n "${2:-}" ] || ci_die "--owner braucht einen Benutzer" 2; OWNER="$2"; shift ;;
    -h|--help) sed -n '2,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) ci_die "Unbekannte Option: $1" 2 ;;
  esac
  shift
done

[ -n "$TARGET" ] || ci_die "--target fehlt (z. B. /opt/android-sdk)." 2
ci_resolve_java_home
ci_require_cmd unzip

mkdir -p "$TARGET/cmdline-tools"
if [ ! -x "$TARGET/cmdline-tools/latest/bin/sdkmanager" ]; then
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  ci_info "Lade Android command-line tools"
  curl -fsSL --retry 3 -o "$tmp/tools.zip" "$CMDLINE_ZIP_URL"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$TARGET/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$TARGET/cmdline-tools/latest"
fi

export ANDROID_HOME="$TARGET"
export ANDROID_SDK_ROOT="$TARGET"
SDKMANAGER="$TARGET/cmdline-tools/latest/bin/sdkmanager"

yes | "$SDKMANAGER" --sdk_root="$TARGET" --licenses >/dev/null || true
"$SDKMANAGER" --sdk_root="$TARGET" \
  "platform-tools" \
  "platforms;android-37.0" \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "build-tools;36.0.0" \
  "emulator" \
  "system-images;android-35;google_apis;x86_64"

if [ -n "$OWNER" ]; then
  chown -R "$OWNER:$OWNER" "$TARGET"
fi

ci_info "Android SDK bereit: $TARGET"
ci_info "Runner-Installation: tools/runner/install-runner.sh --android-sdk $TARGET"
