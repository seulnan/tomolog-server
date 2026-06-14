---
name: ask
description: Classify a decision as VERIFIED vs ASSUMED, isolate assumptions, and raise one pointed question per the two-bucket protocol. Use before asking a human anything.
---

# Two-bucket classification & pointed asks (CLAUDE.md §0)

Every decision lands in exactly one bucket. There is no third bucket and nothing is
decided silently.

## ✅ VERIFIED — implement, record evidence
Backed by machine verification the reviewer can re-run: it compiles, a **named** test
passes, the type system enforces it, a benchmark clears a threshold, or it matches an
explicit `SPEC.md` line. → Implement it. Record the evidence (test name / command / SPEC ref).

## ❓ ASSUMED — isolate, mark, log, expose
Depends on domain knowledge / product intent / business rules you could **not** verify.
Your confidence here is irrelevant. Do all four:
1. Isolate behind the smallest possible surface.
2. Mark in code: `// ASSUMPTION[LEDGER-id]: ...`.
3. Log it **OPEN** in `docs/DECISIONS.md` (use the `ledger` skill first — grep before asking).
4. Expose it as a pointed question (PR assumptions section + Slack).

## Forbidden
- Presenting an ASSUMED item as VERIFIED.
- Dropping an assumption because it "seems like a reasonable default."
- Resolving an assumption with your own confidence. Only a **human answer** or a
  **machine proof** removes something from ASSUMED.

## Safe-default escape hatch
For an internal-only assumption with a genuinely safe default where blocking would stall
the loop: proceed with the default — but log it OPEN and flag it for confirmation.
Proceeding is allowed; **hiding is never allowed.**

## Output for each pointed question (two audiences, §7)
- **도메인/평이판** (Korean) — for PR feature section + Slack.
- **Technical version** (English) — for PR technical decisions.
Same question, two audiences, so any reviewer can answer.
