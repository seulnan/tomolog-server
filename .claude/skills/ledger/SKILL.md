---
name: ledger
description: Read docs/DECISIONS.md before asking a human, apply SETTLED answers silently, and record new assumptions as OPEN. Use whenever a decision is not nailed down by SPEC.md.
allowed-tools: Bash(grep:*), Bash(cat:*)
---

# Assumption ledger (CLAUDE.md §0)

`docs/DECISIONS.md` is your growing memory. **Grep it before asking a human anything.**

## Before asking
1. `!grep -n "LEDGER" docs/DECISIONS.md` — scan existing entries.
2. If the question is already **SETTLED**: apply that answer silently and move on. Do not re-ask.
3. If **OPEN**: don't duplicate it — reference the existing id.
4. If absent: add a new entry (next id) as **OPEN**.

## Entry schema
```
### [LEDGER-007] <one-line question>
- Status: OPEN | SETTLED
- 도메인 맥락: <why this matters to the product, in plain words>
- 고려한 선택지: A / B / C
- 잠정 선택: B — 이유: <rationale>
- 해소: <answer> — 확인자 <name>, <YYYY-MM-DD>   (only when SETTLED)
- 영향 범위: <files / behavior>
```

## When a human answers
Rewrite the entry to **SETTLED** immediately with the answer, confirmer, and date — and
**never ask again**. Each cycle the OPEN list shrinks; the SETTLED list is your memory.

## Invariant
Every behavior-affecting decision not pinned by SPEC.md must be traceable to either a
VERIFIED evidence line or a ledger entry. An untracked decision must not exist.
