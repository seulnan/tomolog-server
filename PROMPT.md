CLAUDE.md와 SPEC.md에 따라 tomolog 빌드를 계속하라.

1. SPEC.md §11 마일스톤 체크리스트에서 최상단 미체크 마일스톤의 가장 작은 코히어런트 조각을 고른다.
2. docs/DECISIONS.md를 읽어 SETTLED 답을 적용하고, 새 가정은 OPEN으로 기록한다(ledger 스킬).
3. 그 조각을 구현한다.
4. 전체 게이트를 green으로 통과시킨다:
   ./gradlew spotlessCheck checkstyleMain pmdMain test jacocoTestCoverageVerification
   (= ./gradlew build). 테스트 약화·임계치 인하로 통과시키지 마라(§0).
5. 원자적으로 커밋한다(commit 스킬). 원장을 갱신한다.
6. 마일스톤이 끝나면 §5 템플릿으로 PR을 열거나 갱신하고(pr 스킬), OPEN 가정만 사람에게 콕 집어 묻는다.

SPEC.md §13 Definition of Done이 전부 충족되면 ALL_DONE 을 출력하라.
