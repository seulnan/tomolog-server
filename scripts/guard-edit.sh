#!/usr/bin/env bash
# PreToolUse(Edit|MultiEdit|Write) guard — CLAUDE.md §2.3 / §0.
#
# The harness sells "deterministic enforcement": the loop must not be able to pass the
# gates by weakening tests or lowering thresholds. This guard makes that physical — on a
# match it exits 2, which Claude Code treats as a BLOCK: the tool call never runs and the
# stderr below is fed back to the agent.
#
# Payload arrives as JSON on stdin. We inspect the proposed new content / command string.
set -euo pipefail

payload="$(cat)"

# Patterns that weaken the safety net:
#   - @Disabled / @Ignore        → silenced tests
#   - lowering JaCoCo minimum     → minimum = 0.0..0.6 (anything below the §3 floor)
#   - ignoreFailures = true       → turning a gate into a warning
#   - maxWarnings bump on checkstyle, deleting the concurrency acceptance test name
violations='@Disabled|@Ignore|ignoreFailures *= *true|minimum *= *0\.[0-6][0-9]*|jacoco.*0\.[0-6]'

if echo "$payload" | grep -Eiq "$violations"; then
  echo "BLOCKED: disabling tests or lowering coverage/lint thresholds violates CLAUDE.md §0/§3." >&2
  echo "If a gate genuinely cannot be met, that is an OPEN ledger item (docs/DECISIONS.md), not an edit." >&2
  exit 2
fi

exit 0
