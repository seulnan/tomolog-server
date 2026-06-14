# Conventions (human-readable copy of CLAUDE.md §3–§6)

This is the team-facing summary. The machine-enforced source of truth is the gate config
(`build.gradle`, `config/checkstyle`, `config/pmd`, ArchUnit test). A build that violates
these is a failed build.

## Architecture (ArchUnit-enforced)
- Controllers depend on services only — never on repositories or other controllers.
- Domain/service layers must not depend on the web layer (`jakarta.servlet`, controllers).
- No cyclic dependencies between packages.
- **Constructor injection only.** No field `@Autowired`, no setter injection.

## Boundaries & types
- Controllers exchange **DTOs** (Java `record`) only; never expose entities. Use explicit mappers.
- Prefer immutability: `final` fields, avoid domain setters.
- Validate every inbound DTO with Bean Validation. Never trust client input.

## Size & complexity (Checkstyle/PMD-enforced)
- Method ≤ 40 lines · cyclomatic complexity ≤ 10 · class/file ≤ 300 lines. Refactor, don't suppress.

## Errors & logging
- No swallowed exceptions, no empty catch. Wrap with context or rethrow as a domain exception.
- SLF4J only. No `System.out` / `printStackTrace`. Proper levels, no secrets in logs.

## Naming & hygiene
- Standard Java naming, no obscure abbreviations.
- No committed `TODO`/`FIXME` unless tagged with a `[LEDGER-id]` reference.
- No dead or commented-out code.

## Testing
- Test names: `methodUnderTest_givenX_thenY` or BDD given/when/then. One behavior per test. AssertJ.
- Coverage gate: `service` / `concurrency` / `gamification` line ≥ 80%, overall ≥ 70%.
- The headline concurrency acceptance test (SPEC §8) is mandatory and must never be skipped.

## Git (§4)
- Atomic commits: one logical change each; never mix refactor + behavior change.
- Conventional Commits with a What/Why body. Branch per milestone: `feat/m3-room-join`.

## PR & Slack (§5–§6)
- Dual-audience PR: Korean feature section (→ Slack) + English technical section.
- The only section a reviewer must act on is **assumptions**. State explicitly when there are none.
- Every assumption is written twice: plain/domain wording + technical wording.
