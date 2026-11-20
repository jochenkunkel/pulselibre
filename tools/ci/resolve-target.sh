#!/usr/bin/env bash
# Resolve which runner executes the PulseLibre Android CI and how much of the
# pipeline runs. Pure decision logic: no network, no build, fully testable.
#
# Inputs (environment variables):
#   EVENT_NAME              github.event_name            (workflow_dispatch|push|pull_request)
#   REQUESTED_ENVIRONMENT   inputs.run_environment        (LOCAL|CLOUD, dispatch only)
#   REQUESTED_SCOPE         inputs.test_scope             (QUICK|FULL, dispatch only)
#   LOCAL_RUNNER_ENABLED    vars.PULSELIBRE_LOCAL_RUNNER_ENABLED ("true" enables LOCAL)
#   PR_ON_LOCAL_ENABLED     vars.PULSELIBRE_PR_ON_LOCAL_ENABLED  ("true" allows PR on LOCAL)
#   PR_FROM_FORK            github.event.pull_request.head.repo.fork ("true" for forks)
#
# Outputs:
#   stdout              KEY=VALUE lines (also appended to $GITHUB_OUTPUT when set)
#   $GITHUB_STEP_SUMMARY  human readable table when set
#
# Safety rules:
#   * Pull requests never run on a self-hosted runner unless explicitly enabled
#     for non-fork PRs. Forks never run on LOCAL.
#   * LOCAL is only used when the operator enabled the local runner. Otherwise the
#     run falls back to CLOUD and says so loudly (no silent, uncontrolled cloud use).
#   * No expression can produce an empty or malformed runner label.
set -euo pipefail

normalize() {
  printf '%s' "${1:-}" | tr '[:lower:]' '[:upper:]' | tr -d '[:space:]'
}

event="$(normalize "${EVENT_NAME:-}")"
requested="$(normalize "${REQUESTED_ENVIRONMENT:-}")"
requested_scope="$(normalize "${REQUESTED_SCOPE:-}")"
local_enabled="$(normalize "${LOCAL_RUNNER_ENABLED:-}")"
pr_on_local="$(normalize "${PR_ON_LOCAL_ENABLED:-}")"
pr_from_fork="$(normalize "${PR_FROM_FORK:-}")"

# 1. Defaults per trigger.
case "$event" in
  WORKFLOW_DISPATCH)
    [ -n "$requested" ] || requested="LOCAL"
    [ -n "$requested_scope" ] || requested_scope="QUICK"
    ;;
  PUSH)
    # Pushes to main come from collaborators with write access = trusted code.
    requested="LOCAL"
    requested_scope="FULL"
    ;;
  PULL_REQUEST)
    # Desired default is LOCAL, but only where it is safe (see rule 3).
    requested="LOCAL"
    requested_scope="FULL"
    ;;
  *)
    # Unknown trigger: never guess, never spend cloud minutes silently.
    requested="CLOUD"
    requested_scope="QUICK"
    ;;
esac

# 2. Validate manual input.
case "$requested" in
  LOCAL|CLOUD) ;;
  *)
    requested="CLOUD"
    reason_invalid_env="Ungültiger Wert für run_environment ('${REQUESTED_ENVIRONMENT:-}') -> CLOUD"
    ;;
esac
case "$requested_scope" in
  QUICK|FULL) ;;
  *) requested_scope="QUICK" ;;
esac

# 3. Decide the effective environment.
environment="$requested"
reason="LOCAL ausgeführt auf dem Self-hosted Runner (Label pulselibre)"
if [ "$requested" = "LOCAL" ]; then
  if [ "$local_enabled" != "TRUE" ]; then
    environment="CLOUD"
    reason="LOCAL angefordert, aber der Self-hosted Runner ist nicht freigegeben (vars.PULSELIBRE_LOCAL_RUNNER_ENABLED != true) -> CLOUD-Fallback"
  elif [ "$event" = "PULL_REQUEST" ] && [ "$pr_from_fork" = "TRUE" ]; then
    environment="CLOUD"
    reason="Pull Request aus einem Fork: kein Self-hosted Runner für fremden Code -> CLOUD"
  elif [ "$event" = "PULL_REQUEST" ] && [ "$pr_on_local" != "TRUE" ]; then
    environment="CLOUD"
    reason="Pull Request: Self-hosted Runner ist für PRs nicht freigegeben (vars.PULSELIBRE_PR_ON_LOCAL_ENABLED != true) -> CLOUD"
  fi
fi
if [ -n "${reason_invalid_env:-}" ]; then
  reason="$reason_invalid_env"
fi

if [ "$environment" = "LOCAL" ]; then
  runs_on='["self-hosted","linux","x64","pulselibre"]'
  runner_label="self-hosted, linux, x64, pulselibre"
else
  runs_on='ubuntu-latest'
  runner_label="ubuntu-latest"
fi

emit() {
  printf '%s=%s\n' "$1" "$2"
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    printf '%s=%s\n' "$1" "$2" >>"$GITHUB_OUTPUT"
  fi
}

emit environment "$environment"
emit requested_environment "$requested"
emit scope "$requested_scope"
emit runs_on "$runs_on"
emit runner_label "$runner_label"
emit reason "$reason"
emit event_name "$event"

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    printf '## Android CI – Ausführungsziel\n\n'
    printf '| Feld | Wert |\n|---|---|\n'
    printf '| Auslöser | `%s` |\n' "$event"
    printf '| Angefordert | `%s` |\n' "$requested"
    printf '| Ausgeführt auf | **%s** |\n' "$environment"
    printf '| Runner | `%s` |\n' "$runner_label"
    printf '| Testumfang | **%s** |\n' "$requested_scope"
    printf '| Begründung | %s |\n' "$reason"
    printf '\n'
  } >>"$GITHUB_STEP_SUMMARY"
fi
