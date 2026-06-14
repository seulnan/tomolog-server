## TL;DR
<1~2줄: 무엇이 출하됐고 왜 중요한가.>

## 기능 관점 변경사항 (비개발자도 이해 · 슬랙 공유용)
<한국어, 평범한 말, 도메인 관점. 사용자나 기획자가 알아차리고 신경 쓸 것. 전문용어 없이.
 이 텍스트가 그대로 슬랙으로 간다.>

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
<비자명한 수정의 압축된 사고 과정 — 리뷰어가 *재유도*가 아니라 *추론을 감사*할 수 있을 만큼.>

## Settled this PR (메모리 갱신)
- [LEDGER-id] <질문> → <답> (확인자 <누구>, <날짜>) — OPEN → SETTLED 이동.

## Test evidence
- build: green · coverage: <N>% · 헤드라인 동시성 테스트(3전략): PASS · CI: <링크>

---
> 리뷰어가 반드시 행동해야 하는 유일한 곳은 **가정** 섹션이다. 열린 가정이 0개면 명시하라:
> *"열린 가정 없음 — 모든 결정은 VERIFIED 또는 SETTLED."*
