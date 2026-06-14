---
name: pr
description: Write a dual-audience PR body (Korean feature section + English technical section) and a paste-ready Slack note. Use when a milestone is complete.
allowed-tools: Bash(git:*), Bash(gh:*)
---

# Open / update a PR (CLAUDE.md §5 & §6)

A PR is the human review surface. Make it readable, tight, dual-audience, and complete.
Use the structure in `.github/PULL_REQUEST_TEMPLATE.md` verbatim.

## Required sections
- **TL;DR** — 1–2 lines: what shipped, why it matters.
- **기능 관점 변경사항** — Korean, plain language, domain view (this text goes to Slack).
- **Technical changes** — English: components, endpoints, data model, algorithms, key files.
- **Why** — problem / motivation, reference SPEC.md sections.
- **Decisions & trade-offs**:
  - ✅ **Verified** — claim → evidence (test name / command / SPEC ref). Re-runnable.
  - ❓ **확인이 필요한 가정** — `[LEDGER-id]` question, provisional choice + reason. **Domain
    wording AND technical wording**, so any reviewer can answer.
  - 🔀 Considered but rejected · 🧭 Decision-path changes · 🚫 Intentionally not implemented.
- **AI reasoning trail** — compressed reasoning for non-obvious fixes (audit, not re-derive).
- **Settled this PR** — `[LEDGER-id]` OPEN → SETTLED moves.
- **Test evidence** — build green · coverage % · headline concurrency test (3 strategies): PASS · CI link.

## Rules
- The ONLY section a reviewer must act on is **가정 (assumptions)**. Keep it short and pointed.
- If zero open assumptions: state *"열린 가정 없음 — 모든 결정은 VERIFIED 또는 SETTLED."*
  Never imply trust where there is no proof.

## Slack note (§6) — also produce this, paste-ready:
```
📦 <마일스톤> 작업 올렸어요
무엇: <한 줄, 사용자 입장에서 뭐가 달라지는지>
왜: <한 줄>
✅ 검증됨: <기계가 증명한 핵심 1~2개>
❓ 확인 필요: <도메인 가정 1~2개를 비개발자 언어로> — 이거 맞을까요?
🔗 PR: <링크>
```

## Steps
1. Push the branch (`feat/m<N>-<slug>`).
2. Create/update the PR with `gh pr create` / `gh pr edit` (or GitHub MCP if connected).
3. Pull the ❓ assumptions straight from `docs/DECISIONS.md` OPEN entries.
4. Print the Slack note for the user to paste.
