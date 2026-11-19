#!/usr/bin/env bash
# Fail the full emulator gate if Android instrumentation produced no coverage data.
set -euo pipefail
root="${1:-app/build}"
coverage_dir="$root/outputs/code_coverage"
if [[ ! -d "$coverage_dir" ]]; then
  echo "::error::No instrumentation coverage directory: $coverage_dir"
  exit 1
fi
found=0
while IFS= read -r -d '' path; do
  if [[ -s "$path" ]]; then
    echo "Instrumentation coverage data: $path"
    found=1
  fi
done < <(find "$coverage_dir" -type f \( -name '*.ec' -o -name '*.exec' \) -print0)
if (( found == 0 )); then
  echo "::error::Instrumentation tests ran without a nonempty JaCoCo .ec/.exec file"
  exit 1
fi
