#!/usr/bin/env bash
# Shared helpers for the PulseLibre hybrid CI (LOCAL self-hosted + CLOUD GitHub-hosted).
#
# Sourced by tools/ci/*.sh and tools/ci-local.sh. Sourcing resolves the toolchain
# (JDK 21, Gradle 9.6.x, Android SDK) and must stay free of build side effects so
# that every phase of the pipeline uses the exact same resolution rules.
#
# shellcheck shell=bash

CI_LIB_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CI_REPO_ROOT="$(cd -- "$CI_LIB_DIR/../.." && pwd)"
export CI_REPO_ROOT

CI_LOG_PREFIX="${CI_LOG_PREFIX:-pulselibre-ci}"
CI_TOOLCHAIN_DIR="${PULSELIBRE_TOOLCHAIN_DIR:-$HOME/.local/share/pulselibre-ci}"
CI_GRADLE_VERSION="${PULSELIBRE_GRADLE_VERSION:-9.6.0}"
CI_MIN_JAVA_MAJOR="${PULSELIBRE_MIN_JAVA_MAJOR:-21}"
export CI_TOOLCHAIN_DIR CI_GRADLE_VERSION

ci_is_github() { [ "${GITHUB_ACTIONS:-}" = "true" ]; }

ci_log() {
  printf '\n[%s] %s\n' "$CI_LOG_PREFIX" "$*" >&2
}

ci_info() {
  printf '[%s] %s\n' "$CI_LOG_PREFIX" "$*" >&2
}

# Emit a GitHub Actions annotation when running inside Actions, plain text otherwise.
ci_annotate() {
  local level="$1" message="$2"
  if ci_is_github; then
    printf '::%s::%s\n' "$level" "$message"
  fi
}

ci_warn() {
  ci_annotate warning "$1"
  printf '[%s] WARNUNG: %s\n' "$CI_LOG_PREFIX" "$1" >&2
}

ci_error() {
  ci_annotate error "$1"
  printf '[%s] FEHLER: %s\n' "$CI_LOG_PREFIX" "$1" >&2
}

ci_die() {
  ci_error "$1"
  exit "${2:-1}"
}

ci_group() {
  if ci_is_github; then
    printf '::group::%s\n' "$1"
  else
    printf '\n=== %s ===\n' "$1"
  fi
}

ci_group_end() {
  if ci_is_github; then
    printf '::endgroup::\n'
  fi
}

ci_require_cmd() {
  command -v "$1" >/dev/null 2>&1 || ci_die "Benötigtes Programm fehlt: $1"
}

# --- Toolchain resolution -------------------------------------------------

_ci_java_major() {
  local java_bin="$1" version
  version="$("$java_bin" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9.]*\).*/\1/p' | head -n1)"
  case "$version" in
    1.*) printf '%s\n' "$version" | cut -d. -f2 ;;
    *) printf '%s\n' "$version" | cut -d. -f1 ;;
  esac
}

ci_resolve_java_home() {
  if [ -n "${PULSELIBRE_JAVA_RESOLVED:-}" ]; then
    return 0
  fi
  local candidate major
  local -a candidates=()
  [ -n "${PULSELIBRE_JDK21:-}" ] && candidates+=("$PULSELIBRE_JDK21")
  [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME")
  candidates+=("$CI_TOOLCHAIN_DIR/jdk-21")

  for candidate in "${candidates[@]}"; do
    [ -x "$candidate/bin/java" ] || continue
    major="$(_ci_java_major "$candidate/bin/java")"
    [ -n "$major" ] || continue
    if [ "$major" -ge "$CI_MIN_JAVA_MAJOR" ]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      export PULSELIBRE_JAVA_RESOLVED="$JAVA_HOME"
      ci_info "JDK: $JAVA_HOME (Version $major)"
      return 0
    fi
  done

  if command -v java >/dev/null 2>&1; then
    major="$(_ci_java_major "$(command -v java)")"
    if [ -n "$major" ] && [ "$major" -ge "$CI_MIN_JAVA_MAJOR" ]; then
      export JAVA_HOME="$(cd -- "$(dirname -- "$(readlink -f "$(command -v java)")")/.." && pwd)"
      export PULSELIBRE_JAVA_RESOLVED="$JAVA_HOME"
      ci_info "JDK: $JAVA_HOME (Version $major)"
      return 0
    fi
  fi

  ci_die "JDK ${CI_MIN_JAVA_MAJOR}+ nicht gefunden. Installiere es mit 'bash tools/ci/bootstrap-toolchain.sh' oder setze PULSELIBRE_JDK21."
}

_ci_version_ge() {
  # _ci_version_ge <actual> <minimum>, both MAJOR.MINOR[.PATCH]
  local IFS=. actual_parts min_parts i
  read -r -a actual_parts <<<"$1"
  read -r -a min_parts <<<"$2"
  for i in 0 1 2; do
    local a="${actual_parts[$i]:-0}" m="${min_parts[$i]:-0}"
    if [ "$a" -gt "$m" ]; then return 0; fi
    if [ "$a" -lt "$m" ]; then return 1; fi
  done
  return 0
}

ci_resolve_gradle() {
  if [ -n "${GRADLE_CMD:-}" ]; then
    export GRADLE_CMD
    ci_info "Gradle: $GRADLE_CMD (PULSELIBRE_GRADLE_CMD)"
    return 0
  fi

  local candidate version
  local -a candidates=()
  [ -x "$CI_REPO_ROOT/gradlew" ] && candidates+=("$CI_REPO_ROOT/gradlew")
  [ -x "$CI_TOOLCHAIN_DIR/gradle-$CI_GRADLE_VERSION/bin/gradle" ] &&
    candidates+=("$CI_TOOLCHAIN_DIR/gradle-$CI_GRADLE_VERSION/bin/gradle")
  command -v gradle >/dev/null 2>&1 && candidates+=("$(command -v gradle)")

  for candidate in "${candidates[@]}"; do
    version="$("$candidate" --version 2>/dev/null | sed -n 's/^Gradle \([0-9][0-9.]*\)$/\1/p' | head -n1)"
    [ -n "$version" ] || continue
    if _ci_version_ge "$version" "9.6"; then
      export GRADLE_CMD="$candidate"
      ci_info "Gradle: $candidate (Version $version)"
      return 0
    fi
    ci_info "Gradle $candidate ist zu alt ($version), wird übersprungen"
  done

  ci_die "Gradle 9.6+ nicht gefunden. Installiere es mit 'bash tools/ci/bootstrap-toolchain.sh' oder setze GRADLE_CMD."
}

ci_resolve_android_sdk() {
  if [ -n "${PULSELIBRE_SDK_RESOLVED:-}" ]; then
    return 0
  fi
  local candidate
  for candidate in \
    "${ANDROID_SDK_ROOT:-}" \
    "${ANDROID_HOME:-}" \
    "$HOME/Android/Sdk" \
    "/usr/local/lib/android/sdk" \
    "/opt/android-sdk"; do
    [ -n "$candidate" ] || continue
    if [ -d "$candidate/platforms" ] || [ -d "$candidate/platform-tools" ]; then
      export ANDROID_HOME="$candidate"
      export ANDROID_SDK_ROOT="$candidate"
      export PATH="$candidate/platform-tools:$candidate/cmdline-tools/latest/bin:$candidate/emulator:$PATH"
      export PULSELIBRE_SDK_RESOLVED="$candidate"
      ci_info "Android SDK: $candidate"
      return 0
    fi
  done
  ci_die "Android SDK nicht gefunden. Setze ANDROID_SDK_ROOT oder installiere das SDK (siehe docs/quality/ci/HYBRID_CI.md)."
}

# Prefer build-tools 35.0.0 (as used by the established CI), fall back to the newest.
ci_android_build_tools() {
  ci_resolve_android_sdk
  local dir
  if [ -d "$ANDROID_HOME/build-tools/35.0.0" ]; then
    printf '%s\n' "$ANDROID_HOME/build-tools/35.0.0"
    return 0
  fi
  dir="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort -V | tail -n1)"
  [ -n "$dir" ] || ci_die "Keine Android build-tools installiert ($ANDROID_HOME/build-tools)."
  printf '%s\n' "$dir"
}

ci_android_tools() {
  ci_resolve_android_sdk
  local sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
  [ -x "$sdkmanager" ] || sdkmanager="$(command -v sdkmanager 2>/dev/null || true)"
  printf '%s\n' "$sdkmanager"
}

# --- Gradle execution -----------------------------------------------------

# Run Gradle through the shared 429-retry wrapper so local and cloud runs behave
# identically. Never retries compiler or test failures.
ci_gradle() {
  ci_resolve_java_home
  ci_resolve_gradle
  ci_resolve_android_sdk
  local retry="$CI_REPO_ROOT/.github/scripts/gradle-retry.sh"
  if [ -f "$retry" ]; then
    bash "$retry" "$@"
  else
    "$GRADLE_CMD" "$@"
  fi
}

ci_version_code() {
  printf '%s\n' "${PULSELIBRE_VERSION_CODE:-${GITHUB_RUN_NUMBER:-1}}"
}

ci_artifact_dir() {
  local dir="${PULSELIBRE_CI_REPORT_DIR:-$CI_REPO_ROOT/build/ci-reports}"
  mkdir -p "$dir"
  printf '%s\n' "$dir"
}
