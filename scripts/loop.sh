#!/usr/bin/env bash
# Ralph loop (CLAUDE.md §1 / §2.8) — headless, repeated `claude -p` invocations.
# Each cycle: pick the top unchecked milestone, ship the smallest coherent slice, drive
# the full gate green, commit atomically, update the ledger, open/refresh the PR.
# Stops when the agent prints ALL_DONE.
#
# Usage:  bash scripts/loop.sh [MAX_ITERATIONS]   (default 50)
#
# Verify the CLI flags for your installed version with `claude --help` — `-p`,
# `--permission-mode acceptEdits`, and `--dangerously-skip-permissions` have changed
# across versions.
set -euo pipefail

MAX=${1:-50}
mkdir -p logs

for ((i = 1; i <= MAX; i++)); do
  echo "=== Ralph iteration $i / $MAX ==="
  claude -p "$(cat PROMPT.md)" --permission-mode acceptEdits 2>&1 | tee "logs/iter-$i.log"

  if grep -q "ALL_DONE" "logs/iter-$i.log"; then
    echo "ALL_DONE detected — Definition of Done met. Stopping."
    break
  fi
done
