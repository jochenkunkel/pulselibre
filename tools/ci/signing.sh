#!/usr/bin/env bash
# Restore the established PulseLibre APK signing material for one CI run.
#
# The signing key is NEVER generated, rotated or replaced here. Two sources are
# supported and both use the same four values as the GitHub repository secrets:
#
#   PULSELIBRE_SIGNING_KEY_B64          base64 of the existing .keystore/.jks
#   PULSELIBRE_SIGNING_STORE_PASSWORD
#   PULSELIBRE_SIGNING_KEY_ALIAS
#   PULSELIBRE_SIGNING_KEY_PASSWORD
#
#   1. GitHub Actions: the workflow already decodes the secret into
#      $RUNNER_TEMP and exports PULSELIBRE_SIGNING_STORE (+ passwords).
#   2. Local runs: an env file outside the repository, default
#      ~/.config/pulselibre/ci-signing.env, mode 0600, containing the values above.
#
# Usage (sourced): ci_signing_restore ; trap ci_signing_cleanup EXIT
# Never prints secrets, passwords or key material.
# shellcheck shell=bash

CI_SIGNING_ENV_FILE="${PULSELIBRE_CI_SIGNING_ENV:-$HOME/.config/pulselibre/ci-signing.env}"
CI_SIGNING_TMP_DIR=""

ci_signing_ready() {
  [ -n "${PULSELIBRE_SIGNING_STORE:-}" ] &&
    [ -f "${PULSELIBRE_SIGNING_STORE}" ] &&
    [ -n "${PULSELIBRE_SIGNING_STORE_PASSWORD:-}" ] &&
    [ -n "${PULSELIBRE_SIGNING_KEY_ALIAS:-}" ] &&
    [ -n "${PULSELIBRE_SIGNING_KEY_PASSWORD:-}" ]
}

ci_signing_restore() {
  if ci_signing_ready; then
    ci_info "Signierung: vorhandener Keystore aus der Umgebung wird verwendet"
    return 0
  fi

  # Local path: optional env file, never inside the repository.
  if [ -f "$CI_SIGNING_ENV_FILE" ]; then
    local mode
    mode="$(stat -c '%a' "$CI_SIGNING_ENV_FILE" 2>/dev/null || echo '')"
    if [ -n "$mode" ] && [ "$mode" != "600" ] && [ "$mode" != "400" ]; then
      ci_warn "Signatur-Datei $CI_SIGNING_ENV_FILE hat Rechte $mode; erwartet 600. Bitte 'chmod 600' ausführen."
    fi
    # shellcheck disable=SC1090
    set -a
    . "$CI_SIGNING_ENV_FILE"
    set +a
  fi

  if [ -z "${PULSELIBRE_SIGNING_KEY_B64:-}" ]; then
    ci_warn "Keine stabilen Signaturschlüssel verfügbar: Tests laufen, aber es wird KEINE updatefähige APK veröffentlicht."
    return 0
  fi
  if [ -z "${PULSELIBRE_SIGNING_STORE_PASSWORD:-}" ] ||
     [ -z "${PULSELIBRE_SIGNING_KEY_ALIAS:-}" ] ||
     [ -z "${PULSELIBRE_SIGNING_KEY_PASSWORD:-}" ]; then
    ci_warn "Signaturschlüssel unvollständig (Store-Passwort, Alias oder Key-Passwort fehlt): keine updatefähige APK."
    return 0
  fi

  CI_SIGNING_TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/pulselibre-signing.XXXXXX")"
  chmod 700 "$CI_SIGNING_TMP_DIR"
  local store="$CI_SIGNING_TMP_DIR/pulselibre-ci.keystore"
  printf '%s' "$PULSELIBRE_SIGNING_KEY_B64" | base64 --decode >"$store"
  chmod 600 "$store"
  export PULSELIBRE_SIGNING_STORE="$store"
  ci_info "Signierung: Keystore aus $CI_SIGNING_ENV_FILE wiederhergestellt (temporär, wird nach dem Lauf gelöscht)"
}

ci_signing_cleanup() {
  if [ -n "$CI_SIGNING_TMP_DIR" ] && [ -d "$CI_SIGNING_TMP_DIR" ]; then
    rm -rf "$CI_SIGNING_TMP_DIR"
  fi
  CI_SIGNING_TMP_DIR=""
}

# Print a non-secret summary line for logs and job summaries.
ci_signing_status() {
  if ci_signing_ready; then
    printf 'configured\n'
  else
    printf 'not-configured\n'
  fi
}
