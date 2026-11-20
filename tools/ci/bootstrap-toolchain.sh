#!/usr/bin/env bash
# Install the exact CI toolchain (Temurin JDK 21 + Gradle 9.6.0) into a
# user-owned directory. No sudo, no system packages, no changes outside
# ~/.local/share/pulselibre-ci (or PULSELIBRE_TOOLCHAIN_DIR).
#
# Usage:
#   bash tools/ci/bootstrap-toolchain.sh            # install missing parts
#   bash tools/ci/bootstrap-toolchain.sh --force    # re-download
#
# The Android SDK is NOT installed here; see docs/ci/HYBRID_CI.md.
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

JDK_VERSION="21.0.13.1+2"
JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13.1%2B2/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz"
JDK_SHA256="bb8a93f9ae6e7a693b2b721e85dbeff26ae1a77447985a8d7afcdd9c7cf6222e"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${CI_GRADLE_VERSION}-bin.zip"
GRADLE_SHA256="bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01"

FORCE=0
for arg in "$@"; do
  case "$arg" in
    --force) FORCE=1 ;;
    -h|--help) sed -n '2,14p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) ci_die "Unbekannte Option: $arg" 2 ;;
  esac
done

ci_require_cmd curl
ci_require_cmd tar
ci_require_cmd unzip

downloads="$CI_TOOLCHAIN_DIR/downloads"
mkdir -p "$downloads"

download() { # url destination sha256
  local url="$1" destination="$2" sha="$3"
  if [ "$FORCE" -eq 0 ] && [ -f "$destination" ] &&
     printf '%s  %s\n' "$sha" "$destination" | sha256sum -c --status -; then
    ci_info "Bereits vorhanden und geprüft: $destination"
    return 0
  fi
  ci_info "Lade $(basename "$destination") ..."
  curl -fsSL --retry 3 --retry-delay 5 -o "$destination.part" "$url"
  printf '%s  %s\n' "$sha" "$destination.part" | sha256sum -c --status - ||
    ci_die "SHA-256-Prüfsumme stimmt nicht: $url"
  mv "$destination.part" "$destination"
}

if [ "$FORCE" -eq 1 ] || [ ! -x "$CI_TOOLCHAIN_DIR/jdk-21/bin/java" ]; then
  download "$JDK_URL" "$downloads/jdk21.tar.gz" "$JDK_SHA256"
  rm -rf "$CI_TOOLCHAIN_DIR/jdk-21"
  mkdir -p "$CI_TOOLCHAIN_DIR/jdk-21"
  tar -xzf "$downloads/jdk21.tar.gz" -C "$CI_TOOLCHAIN_DIR/jdk-21" --strip-components=1
  ci_info "JDK $JDK_VERSION installiert: $CI_TOOLCHAIN_DIR/jdk-21"
else
  ci_info "JDK vorhanden: $CI_TOOLCHAIN_DIR/jdk-21"
fi

if [ "$FORCE" -eq 1 ] || [ ! -x "$CI_TOOLCHAIN_DIR/gradle-$CI_GRADLE_VERSION/bin/gradle" ]; then
  download "$GRADLE_URL" "$downloads/gradle-$CI_GRADLE_VERSION-bin.zip" "$GRADLE_SHA256"
  rm -rf "$CI_TOOLCHAIN_DIR/gradle-$CI_GRADLE_VERSION"
  unzip -q -o "$downloads/gradle-$CI_GRADLE_VERSION-bin.zip" -d "$CI_TOOLCHAIN_DIR"
  ci_info "Gradle $CI_GRADLE_VERSION installiert: $CI_TOOLCHAIN_DIR/gradle-$CI_GRADLE_VERSION"
else
  ci_info "Gradle vorhanden: $CI_TOOLCHAIN_DIR/gradle-$CI_GRADLE_VERSION"
fi

ci_log "Toolchain bereit"
"$CI_TOOLCHAIN_DIR/jdk-21/bin/java" -version 2>&1 | head -n1
"$CI_TOOLCHAIN_DIR/gradle-$CI_GRADLE_VERSION/bin/gradle" --version | sed -n '1,6p'
