# CLAUDE.md — StudyLog 빌드 에이전트 운영 헌장 (Claude Code 판)

> **이 파일을 저장소 루트에 두면 클로드 코드가 매 세션 자동으로 읽는다.** 이것은 *어떻게
> 일하는가*(헌법)이고, *무엇을 만드는가*는 루트의 **`SPEC.md`**다. `SPEC.md`가 없으면
> 코드를 쓰기 전에 멈추고 요청하라.
>
> **하네스 엔지니어링 원칙:** 결정론적으로 보장할 수 있는 것은 모델 판단이 아니라
> **훅·스크립트·게이트**로 강제한다. 모델의 확률적 판단은 도메인 가정에만 쓰고, 그 가정은
> 반드시 사람에게 노출한다. 아래 컨벤션은 포매터·린터·ArchUnit·커버리지·커밋훅으로
> 기계가 강제한다. 게이트를 위반한 빌드는 실패한 빌드다.

---

## 0. 최우선 지침 — 회색지대 없음

너의 존재 이유는 사람의 리뷰를 **더 작고 더 안전하게** 만드는 것이지, 너의 권위에 기대
리뷰를 사라지게 하는 것이 아니다.

> **"AI가 했으니 믿어라"로 리뷰량을 줄이는 것은 기능이 아니라 결함이다.**

리뷰량을 줄이는 합법적인 방법은 정확히 두 가지뿐이다.
1. 증명할 수 있는 것은 **증명**하고, 리뷰어가 재실행할 증거를 넘긴다.
2. 증명할 수 없는 것은 **고립**시키고, 그것에 대해 **콕 집어** 사람에게 묻는다.

네가 내리는 모든 결정은 둘 중 하나의 버킷에 들어간다. 제3의 버킷은 없고, 조용히 결정되는
것도 없다.

### 두 버킷 프로토콜 (Two-Bucket Protocol)

| 버킷 | 정의 | 행동 |
|------|------|------|
| ✅ **VERIFIED (확신)** | 리뷰어가 재실행할 수 있는 기계 검증으로 뒷받침됨: 컴파일되거나, *이름 붙은* 테스트가 통과하거나, 타입 시스템이 강제하거나, 벤치마크가 임계치를 넘거나, `SPEC.md`의 명시적 줄과 일치한다. | 구현한다. **증거**(테스트 이름 / 명령 / SPEC 참조)를 기록한다. 리뷰어는 네가 그렇다고 해서가 아니라, 직접 재실행할 수 있어서 신뢰한다. |
| ❓ **ASSUMED (추측)** | 네가 검증할 수 **없었던** 도메인 지식·제품 의도·비즈니스 규칙에 의존하는 선택. 여기서 너의 확신 정도는 무의미하다. | (a) 가능한 가장 작은 표면 뒤로 고립시킨다. (b) 코드에 표시: `// ASSUMPTION[LEDGER-id]: ...`. (c) 원장에 **OPEN**으로 기록한다. (d) 콕 집은 질문으로 사람에게 노출한다(PR + 슬랙). |

**금지 사항:**
- ASSUMED 항목을 VERIFIED인 것처럼 제시하기.
- "합리적 기본값" 같아 보인다는 이유로 ASSUMED 항목을 누락하기.
- 너 자신의 확신으로 가정을 해소하기. ASSUMED에서 빼낼 수 있는 것은 **사람의 답** 또는
  **기계의 증명**뿐이다.

내부 전용 가정에 진짜로 안전한 기본값이 있고 막으면 루프가 멈출 상황이라면, 기본값으로
**진행해도 된다**. 단, 선택한 기본값과 함께 OPEN으로 기록하고 확인 대상으로 표시한다.
진행은 허용되지만 **숨기는 것은 절대 허용되지 않는다.**

### 메모리 (가정 원장) — 질문은 줄고, 빠지는 것은 없다

`docs/DECISIONS.md`를 유지한다. 사람에게 무언가를 묻기 **전에 원장부터 grep**한다.
SETTLED면 그 답을 조용히 적용하고 넘어간다.

원장 엔트리 스키마:
```
### [LEDGER-007] <한 줄 질문>
- Status: OPEN | SETTLED
- 도메인 맥락: <이게 제품에 왜 중요한지 평범한 말로>
- 고려한 선택지: A / B / C
- 잠정 선택: B — 이유: <근거>
- 해소: <답> — 확인자 <이름>, <YYYY-MM-DD>   (SETTLED일 때만)
- 영향 범위: <파일 / 동작>
```
사람이 답하면 즉시 SETTLED로 다시 쓰고 **다시는 묻지 않는다**. 사이클을 거듭할수록 OPEN
목록은 줄고, SETTLED 목록이 곧 너의 성장하는 메모리다.
**불변식:** 강제되지 않은(=스펙이 못박지 않은) 모든 동작 영향 결정은 VERIFIED 증거 줄 또는
원장 엔트리 중 하나로 추적 가능해야 한다. 추적되지 않는 결정은 존재할 수 없다.

---

## 1. 루프 (Ralph Loop, 헤드리스)

루프는 `scripts/loop.sh`(§2에서 생성)가 `claude -p` 헤드리스 모드를 반복 호출해 돈다.
각 사이클에서 너는:
1. `SPEC.md`의 마일스톤 체크리스트를 읽고 **최상단 미체크** 마일스톤(또는 그 안의 가장 작은
   다음 코히어런트 작업)을 고른다.
2. `docs/DECISIONS.md`를 읽어 SETTLED 답을 적용하고, 새 가정을 수집한다.
3. 출하 가능한 가장 작은 조각을 구현한다.
4. **전체 게이트**를 돌린다:
   `./gradlew spotlessCheck checkstyleMain pmdMain test jacocoTestCoverageVerification`
   (`build` 태스크가 이들을 묶는다). 반드시 **green**.
5. 원자적으로 커밋한다(§4). 원장을 갱신한다.
6. 마일스톤이 끝나면 §5 템플릿으로 PR을 열거나 갱신하고, **OPEN 원장 항목에 대해서만**
   사람 입력을 기다린다. 나머지는 계속 진행한다.
7. `SPEC.md` §13 Definition of Done이 전부 체크되면 로그에 `ALL_DONE`을 출력하고 멈춘다.

테스트를 `@Disabled` 처리하거나 약화시켜 게이트를 통과시키지 마라. 커버리지·린트 임계치를
낮춰 통과시키지 마라. 정당하게 통과시킬 수 없다면 그것은 OPEN 항목이다.

---

## 2. 부트스트랩 (첫 실행 1회, M0 이전)

첫 실행에서 아래 파일들을 **이 내용 그대로** 생성하라. 한 번의 붙여넣기로 하네스·스킬·깃허브
컨벤션이 모두 셋업되게 하기 위함이다. 커밋: `chore: bootstrap agent harness, gates, conventions`.

### 2.1 결정론 게이트 (build.gradle)
- **Spotless** + Google Java Format → `spotlessCheck`가 미포맷 코드에서 빌드 실패.
- **Checkstyle** (`config/checkstyle/checkstyle.xml`): 네이밍, 별표 임포트 금지, 메서드 ≤ 40줄,
  파일 ≤ 300줄, 공개 service/API 메서드 Javadoc, `System.out` 금지.
- **PMD** (`config/pmd/ruleset.xml`): 순환복잡도 ≤ 10, 빈 catch 금지, 예외 삼킴 금지.
- **JaCoCo** `jacocoTestCoverageVerification` → §3 임계치 미달 시 빌드 실패.
- **ArchUnit** 테스트(`src/test/java/.../arch/`): §3 레이어링 강제.
- `build`가 위 전부에 의존하게 한다. green = 모든 게이트 통과.

### 2.2 `.claude/settings.json` — 훅(결정론적 강제)
```json
{
  "hooks": {
    "PostToolUse": [
      { "matcher": "Edit|MultiEdit|Write",
        "hooks": [{ "type": "command", "command": "./gradlew spotlessApply -q || true" }] }
    ],
    "PreToolUse": [
      { "matcher": "Edit|MultiEdit|Write",
        "hooks": [{ "type": "command", "command": "bash scripts/guard-edit.sh" }] }
    ],
    "Stop": [
      { "hooks": [{ "type": "command", "command": "./gradlew build -q" }] }
    ]
  }
}
```
> 훅 스키마는 클코 버전에 따라 다를 수 있으니 `/hooks` 또는 공식 문서로 확인하라.
> PostToolUse = 편집 후 자동 포맷. Stop = 응답 종료 전 전체 빌드 검증. PreToolUse 가드는
> 아래 스크립트로 테스트 약화/임계치 조작을 **물리적으로 차단**한다.

### 2.3 `scripts/guard-edit.sh` — 테스트 약화·게이트 조작 차단
```bash
#!/usr/bin/env bash
# PreToolUse(Edit|Write) 가드. payload는 stdin(JSON)으로 들어온다.
# 위반 시 exit 2 → 클로드 코드가 도구 호출을 차단하고 stderr를 에이전트에 되돌린다.
payload="$(cat)"
if echo "$payload" | grep -Eiq '@Disabled|@Ignore|jacoco.*0\.[0-6]|minimum *= *0\.[0-6]'; then
  echo "BLOCKED: 테스트 비활성화/커버리지 임계치 인하는 CLAUDE.md §0/§3 위반." >&2
  exit 2
fi
exit 0
```

### 2.4 `.claude/skills/` — 재사용 루틴(호출 시에만 로드)
각 파일은 frontmatter(`name`, `description`)로 시작한다.
- `commit/SKILL.md` — 커밋 분할 + 메시지 작성(§4).
- `pr/SKILL.md` — PR 본문 + 슬랙 노트 작성(§5, §6).
- `ask/SKILL.md` — VERIFIED vs ASSUMED 분류 + 콕 집은 질문 + 원장 엔트리 작성(§0).
- `ledger/SKILL.md` — 묻기 전에 `docs/DECISIONS.md` 읽고 갱신하기(§0).

### 2.5 `.claude/agents/` — 서브에이전트(컨텍스트 격리)
- `gate-runner.md` — 전체 게이트를 돌리고 실패만 요약해 반환(메인 컨텍스트 오염 방지).
- `reviewer.md` — 변경분을 §0 두 버킷 기준으로 점검해 VERIFIED/ASSUMED 초안을 반환.
  (빌트인 Explore/Plan은 탐색·계획에 사용한다.)

### 2.6 깃허브·전달
- `.github/PULL_REQUEST_TEMPLATE.md` — §5 구조 그대로.
- `.github/workflows/ci.yml` — JDK 21, 전체 게이트 실행, JaCoCo 리포트 업로드, 게이트 실패 시 실패.
- PR 생성·슬랙 전송은 **GitHub MCP / Slack MCP**(연결돼 있으면) 또는 `gh` CLI로 한다.

### 2.7 커밋 위생
- `scripts/commit-msg` 훅: Conventional Commits 검증. `git config core.hooksPath scripts` 안내.

### 2.8 루프 하네스
- `PROMPT.md`(루프 1회 킥오프, 얇게):
  ```
  CLAUDE.md와 SPEC.md에 따라 StudyLog 빌드를 계속하라. 최상단 미체크 마일스톤의 가장 작은
  조각을 구현하고, 전체 게이트를 green으로 통과시키고, 원자적으로 커밋하고,
  docs/DECISIONS.md를 갱신하고, PR을 열거나 갱신하라. Definition of Done이 모두 충족되면
  ALL_DONE 을 출력하라.
  ```
- `scripts/loop.sh`:
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail
  MAX=${1:-50}; mkdir -p logs
  for ((i=1;i<=MAX;i++)); do
    echo "=== Ralph iteration $i ==="
    claude -p "$(cat PROMPT.md)" --permission-mode acceptEdits 2>&1 | tee "logs/iter-$i.log"
    grep -q "ALL_DONE" "logs/iter-$i.log" && { echo "Done."; break; }
  done
  ```
  > CLI 플래그(`-p`, `--permission-mode`, 필요 시 `--dangerously-skip-permissions`)는
  > `claude --help`로 현재 버전에서 확인하라.

### 2.9 문서
- `docs/DECISIONS.md`(빈 원장 시드), `docs/CONVENTIONS.md`(§3~§6 사람용 사본).

---

## 3. 코드 컨벤션 (기계 강제)

**아키텍처 (ArchUnit 강제)**
- 컨트롤러는 service에만 의존 — 리포지토리/다른 컨트롤러에 직접 의존 금지.
- 도메인/서비스 레이어는 web(`jakarta.servlet`, 컨트롤러) 패키지에 의존 금지.
- 패키지 간 순환 의존 금지.
- **생성자 주입만.** 필드 `@Autowired` 금지, 세터 주입 금지.

**경계 & 타입**
- 컨트롤러는 **DTO**(Java `record`)만 주고받고, 엔티티를 노출하지 않는다. 명시적 매퍼 사용.
- 불변 선호: `final` 필드, 도메인 객체 세터 지양.
- 모든 인바운드 DTO는 Bean Validation으로 검증. 클라이언트 입력을 신뢰하지 않는다.

**크기 & 복잡도 (Checkstyle/PMD 강제)**
- 메서드 ≤ 40줄, 순환복잡도 ≤ 10, 클래스 ≤ 300줄. 억제(suppress) 말고 리팩터링한다.

**오류 & 로깅**
- 예외 삼킴 금지, 빈 catch 금지. 맥락을 붙여 감싸거나 도메인 예외로 재던진다.
- SLF4J 사용. `System.out`/`printStackTrace` 금지. 적절한 레벨, 로그에 시크릿 금지.

**네이밍 & 위생**
- 표준 자바 네이밍. 잘 알려진 것 외 약어 금지.
- 커밋된 `TODO`/`FIXME` 금지(단, `[LEDGER-id]` 참조를 달면 허용).
- 죽은 코드·주석처리 코드 금지.

**테스트**
- 테스트 이름: `methodUnderTest_givenX_thenY` 또는 BDD `given/when/then`.
- 테스트당 하나의 동작. 단언은 AssertJ.
- 커버리지 게이트: `service`/`concurrency`/`gamification` 패키지 라인 **≥ 80%**, 전체 **≥ 70%**.
  미달 시 빌드 실패.
- 헤드라인 동시성 합격 테스트(`SPEC.md` §8)는 **필수이며 절대 건너뛸 수 없다.**

---

## 4. 깃 컨벤션

**원자적 커밋.** 하나의 논리 변경 = 하나의 커밋. 리팩터링과 동작 변경을 **절대** 한 커밋에
섞지 말고 분리하라 — 각각 독립적으로 리뷰·되돌리기 가능해야 한다.

**Conventional Commits**, 의도를 담은 본문과 함께:
```
<type>(<scope>): <명령형 요약 ≤ 72자>

What:  <이 커밋이 구체적으로 무엇을 바꾸는지>
Why:   <이유 / 해결하는 문제 — 방법(how)이 아니라>
Notes: <트레이드오프, 참조, 건드린 [LEDGER-id], BREAKING CHANGE: ...>
```
타입: `feat fix refactor test docs chore perf build ci`. 모든 커밋 본문은 **무엇/왜**를 답한다.
방법(how)은 diff가 이미 보여준다.

**브랜치:** `feat/m3-room-join`처럼 마일스톤/PR당 하나.

---

## 5. PR 컨벤션 (이중 청중)

PR은 사람의 리뷰 표면이다. 가독성 좋게, 핵심만, **이중 청중**으로, 빠짐없이.
아래 템플릿을 `.github/PULL_REQUEST_TEMPLATE.md`에도 그대로 쓴다.

```
## TL;DR
<1~2줄: 무엇이 출하됐고 왜 중요한가.>

## 기능 관점 변경사항 (비개발자도 이해 · 슬랙 공유용)
<한국어, 평범한 말, 도메인 관점. 사용자나 기획자가 알아차리고 신경 쓸 것. 전문용어 없이.
 이 텍스트가 그대로 슬랙으로 간다(§6).>

## Technical changes (for developers)
<영어. 컴포넌트, 엔드포인트, 데이터 모델, 알고리즘, 핵심 파일.>

## Why
<문제/동기. SPEC.md 섹션 참조.>

## Decisions & trade-offs
### ✅ Verified (기계 증명 · 재실행 가능)
- <주장> — 증거: <테스트 이름 / 명령 / SPEC 참조>

### ❓ 확인이 필요한 가정 (콕 집음 · 답변 요망)
- [LEDGER-id] <질문>. 잠정적으로 <X>를 골랐다, 이유는 <Y>. 확인 또는 정정 바람.
  (도메인 언어판 + 기술판 둘 다 적어 어느 리뷰어든 답할 수 있게.)

### 🔀 Considered but rejected
- <선택지> — 기각 이유 <근거>.

### 🧭 Decision-path changes
- <A>로 시작했다가 <발견/제약>이 드러나 <B>로 전환.

### 🚫 Intentionally not implemented
- <항목> — <범위 밖 / 보류 / 가정 불가>, 이유.

## AI reasoning trail (수정 사고 과정)
<비자명한 수정의 압축된 사고 과정 — 리뷰어가 *재유도*가 아니라 *추론을 감사*할 수 있을 만큼.
 이것이 아무것도 숨기지 않으면서 리뷰 시간을 줄이는 핵심이다.>

## Settled this PR (메모리 갱신)
- [LEDGER-id] <질문> → <답> (확인자 <누구>, <날짜>) — OPEN → SETTLED 이동.

## Test evidence
- build: green · coverage: <N>% · 헤드라인 동시성 테스트(3전략): PASS · CI: <링크>
```

**규칙:**
- 리뷰어가 *반드시 행동해야 하는* 유일한 곳은 **가정** 섹션이다. 짧고 콕 집어서, 다른 건
  여기 묻지 마라.
- 열린 가정이 0개면 명시하라: *"열린 가정 없음 — 모든 결정은 VERIFIED 또는 SETTLED."*
  증명이 없는 곳에 신뢰를 암시하지 마라.
- 각 섹션은 타이트하게. 완성도 연기(theater)보다 가독성.

---

## 6. 슬랙 노트

각 PR마다 붙여넣기용 슬랙 메시지도 만든다 = §5의 **기능 관점 변경사항** + **열린 가정**을
비개발자 언어로 다시 쓴 것 + 명시적 요청으로 끝맺기.
```
📦 <마일스톤> 작업 올렸어요
무엇: <한 줄, 사용자 입장에서 뭐가 달라지는지>
왜: <한 줄>
✅ 검증됨: <기계가 증명한 핵심 1~2개>
❓ 확인 필요: <도메인 가정 1~2개를 비개발자 언어로> — 이거 맞을까요?
🔗 PR: <링크>
```

---

## 7. 언어 규칙

- **코드, 식별자, 커밋 메시지, PR 기술 섹션, 원장 키:** 영어.
- **PR 기능 섹션, 슬랙 노트, 모든 가정의 비개발자판:** 한국어(팀 언어).
- 모든 가정 질문은 **두 번** 쓴다: 도메인/평이판(PR 기능 섹션 + 슬랙) + 기술판(PR 기술 결정).
  같은 질문, 두 청중, 누구든 답할 수 있게.

---

## 8. Claude Code 기능 매핑 (어떤 규칙을 무엇으로 강제하는가)

| 규칙/필요 | 클로드 코드 메커니즘 | 비고 |
|-----------|----------------------|------|
| 헌법(항상 적용) | **CLAUDE.md** (이 파일) | 매 세션 자동 로드 |
| 반복 루틴(commit/pr/ask/ledger) | **Skills** (`.claude/skills/`) | 호출 시에만 로드 → 컨텍스트 절약 |
| 포맷 자동화·테스트약화 차단·종료 전 빌드 | **Hooks** (`.claude/settings.json`) | 결정론적(확률적 지시보다 강함) |
| 게이트 실행·리뷰 점검의 컨텍스트 격리 | **Subagents** (`.claude/agents/`) | 메인 컨텍스트 오염 방지 |
| PR 생성·슬랙 전송 | **MCP** (GitHub/Slack) 또는 `gh` CLI | 연결된 도구 사용 |
| 자율 반복 실행 | **헤드리스 `claude -p`** + `scripts/loop.sh` | Ralph 루프 |
| 긴 세션 컨텍스트 관리 | 빌트인 `/compact`, 잘못된 상태 복구는 `/rewind` | 필요 시 |

---

## 9. Definition of Done (프로세스 레벨 · SPEC.md §13 보완)

- [ ] 모든 마일스톤이 원자적 커밋 브랜치 + §5 템플릿 PR로 출하됨.
- [ ] 모든 커밋에서 전체 게이트 green(format, checkstyle, pmd, archunit, test, coverage).
- [ ] 최종 시점에 `docs/DECISIONS.md`의 동작 영향 OPEN 항목이 0개(전부 SETTLED 또는 사람이
      명시 수락).
- [ ] VERIFIED(증거 포함)도 아니고 원장에도 없는 결정이 존재하지 않음.
- [ ] 기본 브랜치 CI green.
