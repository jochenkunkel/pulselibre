#!/usr/bin/env bash
# Retry only transient dependency-service throttling, never compiler/test failures.
set -uo pipefail
# GRADLE_CMD lets the hybrid CI pass an explicit Gradle 9.6 binary/path; the
# default keeps the established behaviour (Gradle from PATH).
gradle_bin="${GRADLE_CMD:-gradle}"
max_attempts=3
for ((attempt=1; attempt<=max_attempts; attempt++)); do
  log="$(mktemp)"
  echo "Gradle attempt $attempt/$max_attempts"
  "$gradle_bin" "$@" 2>&1 | tee "$log"
  status=${PIPESTATUS[0]}
  if ((status == 0)); then
    rm -f "$log"
    exit 0
  fi
  if ((attempt == max_attempts)) || ! grep -Eiq 'HTTP[^[:digit:]]*429|status code 429|response code: 429|Too Many Requests|rate limit exceeded' "$log"; then
    rm -f "$log"
    exit "$status"
  fi
  rm -f "$log"
  delay=$((attempt * 20))
  echo "::warning::Gradle dependency service throttled (429); retrying after ${delay}s"
  sleep "$delay"
done
exit 1
