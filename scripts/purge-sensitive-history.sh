#!/usr/bin/env bash
# IRREVERSIBLE: run only on a disposable, freshly cloned mirror after making the repo PRIVATE.
# Requires git-filter-repo (https://github.com/newren/git-filter-repo).
set -euo pipefail
if [[ "${1:-}" != "--i-understand-history-rewrite" ]]; then
  echo "Usage: $0 --i-understand-history-rewrite" >&2
  echo "Run in a disposable --mirror clone; coordinate all collaborators and backups." >&2
  exit 2
fi
command -v git-filter-repo >/dev/null || { echo "Install git-filter-repo first" >&2; exit 2; }
[[ "$(git rev-parse --is-bare-repository)" == true ]] || { echo "Use a fresh git clone --mirror, not a working tree" >&2; exit 2; }
[[ "$(git remote get-url origin)" == *jochenkunkel/pulselibre* ]] || { echo "Unexpected origin" >&2; exit 2; }
echo "WARNING: Removes all historical versions of the private recording and derived report."
echo "Press Ctrl-C within 10 seconds if this is not a disposable mirror clone."
sleep 10
git filter-repo --force --path data/overnight/pulselibre-cirqa-overnight.jsonl --path docs/CIRQA_OVERNIGHT_FIRST_RESULTS.md --invert-paths
echo "History rewritten locally. Inspect results, restore origin if removed, and push only after coordinating."
echo "Example (review remote carefully): git remote add origin <original-repo-url>"
echo "Then: git push --force --mirror origin"
echo "GitHub Support may need to purge cached views/PR refs; forks and external copies remain separate."
