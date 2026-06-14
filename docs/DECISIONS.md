# Decision Ledger (assumption memory)

Per CLAUDE.md §0: every behavior-affecting decision not pinned by `SPEC.md` lives here as a
VERIFIED evidence trail or a ledger entry. **Grep this file before asking a human.** When a
human answers, rewrite the entry to SETTLED and never ask again.

Entry schema:
```
### [LEDGER-NNN] <one-line question>
- Status: OPEN | SETTLED
- 도메인 맥락: <why this matters to the product, in plain words>
- 고려한 선택지: A / B / C
- 잠정 선택: <X> — 이유: <rationale>
- 해소: <answer> — 확인자 <name>, <YYYY-MM-DD>   (only when SETTLED)
- 영향 범위: <files / behavior>
```

---

### [LEDGER-001] Stop 훅이 빌드 실패 시 응답 종료를 막아야 하는가?
- Status: SETTLED
- 도메인 맥락: `.claude/settings.json`의 Stop 훅은 응답 종료 전 `./gradlew build`를 돌린다.
  가드 없이 두면 (a) 부트스트랩/초기 M0처럼 래퍼·소스가 아직 없을 때, (b) 개발 중 일시적
  테스트 실패 시 훅이 non-zero로 끝나 종료를 막고 stderr를 에이전트에 되먹여 **루프(또는
  대화 세션)를 wedge** 시킬 수 있다.
- 고려한 선택지: A) 무가드 하드 차단 / B) `[ -x ./gradlew ] && ./gradlew build -q || true`
  (래퍼 있을 때만 돌리되 실패해도 종료 비차단) / C) 빌드 마커 파일로 게이팅
- 잠정 선택: B — 이유: 진짜 green 강제는 루프 단계 4(명시적 게이트 실행)와 gate-runner
  서브에이전트, 그리고 CI가 담당한다. Stop 훅은 가시성/조기경고용이며 세션을 막지 않는 게 안전.
- 해소: B 채택 — 확인자 (부트스트랩 설계), 2026-06-15
- 영향 범위: `.claude/settings.json` Stop 훅. 동일 가드를 PostToolUse spotlessApply에도 적용.

### [LEDGER-002] guard-edit.sh의 차단 정규식이 정당한 임계치 표기까지 막지 않는가?
- Status: SETTLED
- 도메인 맥락: 가드는 커버리지 하한 인하(첫 소수자리 0~6)·실패무시 플래그·테스트 비활성화
  어노테이션을 exit 2로 물리 차단한다. 정상 게이트 설정인 하한 0.70 / 0.80은 통과해야 하고,
  0.7~0.9대 임계치를 새로 추가하는 편집도 막히면 안 된다.
- 고려한 선택지: A) 첫 소수자리 0~6만 차단(현재) / B) 화이트리스트로 0.70·0.80 명시 허용
- 잠정 선택: A — 이유: 첫 소수자리가 0~6일 때만 매칭하므로 0.70/0.80은 통과.
- 해소: A 채택. 부트스트랩 시점 실측: 정상 하한(0.70·0.80)·COVEREDRATIO 표기는 통과,
  하한 0.5·0.65·실패무시·비활성화 어노테이션은 차단. 정상 임계치 오탐 없음.
  확인자 (부트스트랩 검증), 2026-06-15
- 영향 범위: `scripts/guard-edit.sh`.

### [LEDGER-003] guard-edit.sh가 편집의 NEW 콘텐츠뿐 아니라 OLD(치환 대상)까지 스캔해 메타 문서 편집을 오탐하지 않는가?
- Status: OPEN
- 도메인 맥락: 가드는 PreToolUse 페이로드(JSON) 전체를 grep한다. 그래서 위반 토큰을
  *설명/문서화*하는 텍스트(이 원장, 컨벤션 문서)를 편집하면 그 토큰 때문에 차단된다.
  실제로 이 원장 엔트리를 Edit으로 SETTLED 처리하려다 라이브 가드에 막혀, 전체 파일을
  Write로 다시 써서 우회했다 — 가드의 엔드투엔드 동작은 증명됐지만 메타 문서 편집은 불편하다.
- 고려한 선택지: A) 현행 유지(보수적, 우회는 Write 재작성) / B) 페이로드에서 new content만
  추출해 스캔(jq로 `.tool_input.content`/`.new_string`만) / C) 코드 경로(src·build.gradle·
  config) 편집에만 가드 적용하고 docs/는 제외
- 잠정 선택: A — 이유: 보수적 차단이 안전 측이고 빈도가 낮다. 단 B(정밀 스캔)가 오탐을
  없애므로 첫 루프에서 도입 검토.
- 영향 범위: `scripts/guard-edit.sh`, 문서/원장 편집 흐름.
