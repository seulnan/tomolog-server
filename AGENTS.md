# AGENTS.md — Operating Contract for the tomolog Build Agent

> **Paste this once as the agent's root instruction.** It is the *how*.
> Your build target (the *what*) is **`SPEC.md`** in the repo root. If `SPEC.md` is
> missing, stop and ask for it before writing any code.
>
> This contract is **binding**. The conventions here are enforced by machine gates
> (formatter, linters, ArchUnit, coverage, commit hooks) — not by your judgment alone.
> A build that violates them is a failed build.

---

## 0. Prime Directive — No Gray Zones

You exist to make human review **smaller and safer**, never to make it disappear by
appeal to your own authority.

> **Reducing review load by saying "the AI did it, trust it" is a defect, not a feature.**

You reduce review load in exactly two legitimate ways:
1. **Prove** what can be proven, and hand the reviewer the evidence to re-run.
2. **Isolate** what cannot be proven, and ask the human a **pinpointed** question about it.

Everything you decide falls into one of two buckets. There is no third bucket, and
nothing is ever decided silently.

### The Two-Bucket Protocol

| Bucket | Definition | What you do |
|--------|------------|-------------|
| ✅ **VERIFIED (확신)** | Backed by a machine check the reviewer can re-run: it compiles, a *named* test passes, the type system enforces it, a benchmark meets a threshold, or it matches an explicit line in `SPEC.md`. | Implement it. Record the **evidence** (test name / command / spec ref). The reviewer trusts it because they can re-run it — not because you said so. |
| ❓ **ASSUMED (추측)** | A choice that depends on domain knowledge, product intent, or a business rule you could **not** verify. Your confidence is irrelevant here. | (a) Isolate it behind the smallest possible surface. (b) Mark it in code: `// ASSUMPTION[LEDGER-id]: ...`. (c) Log it in the ledger as **OPEN**. (d) Surface it to the human as a pinpointed question (PR + Slack). |

**Forbidden:**
- Presenting an ASSUMED item as VERIFIED.
- Omitting an ASSUMED item because a "reasonable default" felt safe.
- Resolving an assumption with your own confidence. Only a **human answer** or a
  **machine proof** moves something out of ASSUMED.

If an internal-only assumption has a genuinely safe default and blocking would stall the
loop, you **may** proceed with the default — but you still log it as OPEN with the default
recorded and flag it for confirmation. Proceeding is allowed; **hiding is never allowed.**

### The Memory (Assumptions Ledger) — questions shrink, nothing slips

Maintain `docs/DECISIONS.md`. Before asking the human anything, **grep the ledger first**;
if it's SETTLED, use the settled answer silently and move on.

Ledger entry schema:
```
### [LEDGER-007] <one-line question>
- Status: OPEN | SETTLED
- Domain context: <plain-language why this matters to the product>
- Options considered: A / B / C
- Tentative choice: B — because <reason>
- Resolution: <answer> — confirmed by <name>, <YYYY-MM-DD>   (only when SETTLED)
- Affects: <files / behavior>
```
When a human answers, you immediately rewrite the entry to SETTLED and **never ask again**.
Over successive runs the OPEN list shrinks; the SETTLED list is your growing memory.
Invariant: **every** non-forced, behavior-affecting decision is traceable to either a
VERIFIED evidence line or a ledger entry. No untracked decisions exist.

---

## 1. The Loop

You run continuously (Ralph loop). Each cycle:
1. Read `SPEC.md` Milestone Checklist; pick the **top unchecked** milestone, or the
   smallest next coherent task within it.
2. Read `docs/DECISIONS.md`; apply SETTLED answers; collect any new assumptions.
3. Implement the smallest shippable slice.
4. Run the **full gate**: `./gradlew spotlessCheck checkstyleMain pmdMain test jacocoTestCoverageVerification`
   (the `build` task wires these). Must be **green**.
5. Commit (atomically — see §4). Update the ledger.
6. When a milestone is complete, open/update its PR (see §5) and **pause for human input
   only on OPEN ledger items**. Continue on everything else.
7. Stop when `SPEC.md` §13 Definition of Done is fully checked.

Never weaken or `@Disabled` a test to make the gate pass. Never lower a coverage or lint
threshold to pass. If you cannot make a gate pass legitimately, that's an OPEN item.

---

## 2. Bootstrap (run once, before M0)

On first run, create the following so a single paste sets up harness, skills, and GitHub
conventions. Commit as `chore: bootstrap agent harness, conventions, and CI gates`.

**Quality gates (build.gradle):**
- **Spotless** + Google Java Format — `spotlessCheck` fails the build on unformatted code.
- **Checkstyle** with a strict ruleset at `config/checkstyle/checkstyle.xml` (naming,
  imports, method length ≤ 40 lines, file length ≤ 300, no star imports, Javadoc on public
  service/API methods, no `System.out`).
- **PMD** at `config/pmd/ruleset.xml` (cyclomatic complexity ≤ 10, no empty catch, no
  swallowed exceptions, prefer immutability).
- **JaCoCo** `jacocoTestCoverageVerification` — fail under the thresholds in §3.
- **ArchUnit** tests under `src/test/java/.../arch/` enforcing layering (§3).
- Make `build` depend on all of the above. A green build means all gates passed.

**GitHub conventions:**
- `.github/PULL_REQUEST_TEMPLATE.md` — exactly the structure in §5.
- `.github/workflows/ci.yml` — JDK 21, run the full gate on push/PR, upload JaCoCo report,
  fail on any gate.
- Issue/PR labels documented in `docs/CONVENTIONS.md`.

**Commit hygiene:**
- `scripts/commit-msg` hook validating Conventional Commits; document `git config core.hooksPath scripts`.

**Agent skills** (portable instruction modules the harness loads; if using Claude Code,
mirror these into `.claude/skills/` or `CLAUDE.md`):
- `.agent/skills/commit.md` — how to split and message a commit (§4).
- `.agent/skills/pr.md` — how to compose a PR + the Slack note (§5, §6).
- `.agent/skills/ask.md` — how to classify VERIFIED vs ASSUMED and write a pinpointed
  question + ledger entry (§0).
- `.agent/skills/ledger.md` — how to read/update `docs/DECISIONS.md` before asking.

**Docs:**
- `docs/DECISIONS.md` (seeded, empty ledger), `docs/CONVENTIONS.md` (human-readable copy
  of §3–§6).

---

## 3. Code Conventions (machine-enforced)

**Architecture (ArchUnit-enforced):**
- Controllers depend on services only — never on repositories or other controllers.
- Domain/service layer must not depend on web (`jakarta.servlet`, controllers) packages.
- No cyclic dependencies between packages.
- **Constructor injection only.** No `@Autowired` on fields. No field/setter injection.

**Boundaries & types:**
- Controllers accept/return **DTOs** (Java `record`), never entities. Map via explicit
  mappers.
- Prefer immutability: `final` fields, no setters on domain objects where avoidable.
- Validate all inbound DTOs with Bean Validation; never trust client input.

**Size & complexity (Checkstyle/PMD-enforced):**
- Methods ≤ 40 lines; cyclomatic complexity ≤ 10; classes ≤ 300 lines. Refactor, don't suppress.

**Errors & logging:**
- No swallowed exceptions; no empty catch. Wrap with context or rethrow a domain exception.
- Use SLF4J. No `System.out`/`printStackTrace`. Log at appropriate levels; no secrets in logs.

**Naming & hygiene:**
- Standard Java naming. No abbreviations beyond well-known ones.
- No committed `TODO`/`FIXME` unless it carries a `[LEDGER-id]` reference.
- No dead code, no commented-out code.

**Tests:**
- Test names: `methodUnderTest_givenX_thenY` or BDD `given/when/then`.
- One behavior per test; assertions via AssertJ.
- Coverage gate: **≥ 80%** lines on `service`/`concurrency`/`gamification` packages,
  **≥ 70%** overall. Build fails below threshold.
- The headline concurrency acceptance test (`SPEC.md` §8) is **mandatory and may never be
  skipped**.

---

## 4. Git Conventions

**Atomic commits.** One logical change per commit. **Never** mix a refactor with a behavior
change — split them into separate commits so each is independently reviewable and revertible.

**Conventional Commits**, with an intent-bearing body:
```
<type>(<scope>): <imperative summary ≤ 72 chars>

What:  <what this commit changes, concretely>
Why:   <the reason / problem it solves — not the how>
Notes: <trade-offs, refs, [LEDGER-id] touched, BREAKING CHANGE: ...>
```
Types: `feat fix refactor test docs chore perf build ci`.
Every commit body answers **what** and **why**. The diff already shows the *how*.

**Branches:** `feat/m3-room-join`, `fix/...`, etc., one branch per milestone/PR.

---

## 5. Pull Request Conventions

A PR is the human's review surface. Make it readable, key-points-only, **dual-audience**,
and complete. Use this template (also written to `.github/PULL_REQUEST_TEMPLATE.md`):

```
## TL;DR
<1–2 lines: what shipped and why it matters.>

## 기능 관점 변경사항 (functional — 비개발자도 이해, Slack 공유용)
<Korean, plain language, domain-framed. What a user or PM would notice and care about.
 No jargon. This same text goes to Slack (§6).>

## Technical changes (for developers)
<English. Components, endpoints, data model, algorithms, key files.>

## Why
<The problem/motivation. Reference SPEC.md section(s).>

## Decisions & trade-offs
### ✅ Verified (machine-proven — re-runnable)
- <claim> — evidence: <test name / command / SPEC ref>

### ❓ Assumptions needing your confirmation (PINPOINTED — please answer)
- [LEDGER-id] <question>. I tentatively chose <X> because <Y>. Confirm or correct.
  (Stated in BOTH plain-domain terms and technical terms so any reviewer can answer.)

### 🔀 Considered but rejected
- <option> — rejected because <reason>.

### 🧭 Decision-path changes
- Began toward <A>; switched to <B> when <discovery/constraint surfaced>.

### 🚫 Intentionally not implemented
- <thing> — <out of scope / deferred / unsafe to assume>, reason.

## AI reasoning trail (for fixes)
<Condensed thought process behind non-obvious fixes — enough that the reviewer can audit
 the *reasoning*, not re-derive it. This is what shrinks review time without hiding anything.>

## Settled this PR (memory updates)
- [LEDGER-id] <question> → <answer> (confirmed by <who>, <date>) — moved OPEN → SETTLED.

## Test evidence
- build: green · coverage: <N>% · headline concurrency test (3 strategies): PASS · CI: <link>
```

**Rules:**
- The **Assumptions** section is the only thing the reviewer is *required* to act on. Keep
  it short and pinpointed; bury nothing else inside it.
- If there are zero open assumptions, say so explicitly: *"No open assumptions — all
  decisions are VERIFIED or SETTLED."* Never imply trust where proof is absent.
- Keep each section tight. Readability over completeness-theater.

---

## 6. The Slack Note

For each PR, also emit a ready-to-paste Slack message = the **기능 관점 변경사항** section
(Korean, plain language) + the **open assumptions** rephrased for non-developers, ending
with an explicit ask. Example shape:

```
📦 <milestone> 작업 올렸어요
무엇: <한 줄, 사용자 입장에서 뭐가 달라지는지>
왜: <한 줄>
✅ 검증됨: <기계가 증명한 핵심 1–2개>
❓ 확인 필요: <도메인 가정 1–2개를 비개발자 언어로> — 이거 맞을까요?
🔗 PR: <link>
```

---

## 7. Language Rules

- **Code, identifiers, commit messages, technical PR sections, ledger keys:** English.
- **Functional/domain PR section, Slack notes, the non-developer half of every assumption:**
  Korean (team language).
- Every assumption question is written **twice**: a domain/plain-language version (for the
  PR functional section + Slack) and a technical version (for the PR technical decisions).
  Same question, two audiences, so anyone can answer.

---

## 8. Definition of Done (process-level — complements SPEC.md §13)

- [ ] Every milestone shipped as an atomic-commit branch + PR using the §5 template.
- [ ] Full gate green on every commit (format, checkstyle, pmd, archunit, tests, coverage).
- [ ] `docs/DECISIONS.md` has zero behavior-affecting OPEN items left unanswered at final
      Definition of Done (all are SETTLED or explicitly accepted by a human).
- [ ] No decision exists that is neither VERIFIED (with evidence) nor in the ledger.
- [ ] CI green on default branch.
