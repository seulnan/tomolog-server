---
name: commit
description: Split work into atomic commits and write Conventional-Commit messages that answer What/Why. Use when ready to commit a finished slice.
allowed-tools: Bash(git add:*), Bash(git status:*), Bash(git diff:*), Bash(git commit:*), Bash(git log:*)
---

# Atomic commit (CLAUDE.md §4)

Current state:
- `!git status --short`
- `!git diff --stat`

## Rules
1. **One logical change = one commit.** Never mix a refactor with a behavior change —
   split them so each is independently reviewable and revertible.
2. Stage deliberately (`git add -p` / explicit paths), not `git add -A` blindly.
3. Subject: `<type>(<scope>): <imperative summary ≤ 72 chars>`.
   Types: `feat fix refactor test docs chore perf build ci`.
4. Body answers **What / Why** (the diff already shows How):

```
<type>(<scope>): <summary>

What:  <what this commit concretely changes>
Why:   <reason / problem solved — not how>
Notes: <trade-offs, refs, touched [LEDGER-id], BREAKING CHANGE: ...>
```

## Steps
1. Group the working tree into the smallest set of atomic changes.
2. For each: stage only its paths, then commit with the template above.
3. The `scripts/commit-msg` hook validates the subject — if it rejects, fix the subject, don't bypass.
4. If a change relies on an ASSUMED decision, reference its `[LEDGER-id]` in `Notes:`.
