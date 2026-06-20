# tomolog — 실시간 스터디 룸 서버

```
학번:    <YOUR_ID>
이름:    <YOUR_NAME>
과제명:  tomolog — 실시간 스터디 룸 서버
GitHub:  https://github.com/seulnan/tomolog-server
```

친구들끼리 또는 무기명의 사람들과 함께 공부하는 실시간 스터디 룸 서버다. 친구들이 **방(로그)** 을
만들어 **공유 포모도로 타이머**로 같이 집중하고, 서로의 **접속 상태(presence)** 를 실시간으로 보며,
한 사이클이 끝나면 **공부 스냅샷**(이모지 + 한 줄 메모 + 분)을 남겨 방의 피드에 쌓는다. 가벼운
게이미피케이션(스트릭·뱃지·방마다 같이 키우는 펫)이 다시 오게 만든다. **서버 전용**(프론트엔드는 범위 밖).

## 아키텍처 개요

코드는 **4계층 + 헥사고날(ports & adapters)** 로 짰다. 위에서 아래로만 의존하고, 가장 중요한
**도메인(공부 규칙·정원 규칙)** 은 스프링이나 JPA를 전혀 모른다 — 순수 자바 객체만 둔다. 이렇게 한
이유는 단순하다. "방 정원은 절대 못 넘는다", "공부하면 스트릭이 오른다" 같은 핵심 규칙이 데이터베이스
기술과 엉키면, 나중에 DB나 프레임워크를 바꿀 때 규칙까지 같이 흔들리기 때문이다.

```
com.tomolog
├── presentation    # REST 컨트롤러 + STOMP 핸들러. 바깥과 만나는 DTO(record)만 주고받음.
├── application     # 서비스(유스케이스). 트랜잭션 경계, 도메인 조립, 입장 라우팅.
├── domain          # 순수 도메인 모델 + 포트(리포지토리 인터페이스). 프레임워크 의존 0.
│   └── user/ room/ log/ gamification/ realtime/ common/
└── infrastructure  # 바깥쪽 어댑터(안으로만 의존).
    ├── persistence # JPA 엔티티 + 매퍼 + 영속성 어댑터(포트 구현)
    ├── concurrency # 입장 전략 4종 구현(아래 표)
    └── jwt/ oauth/ security/ config
```

- **의존 방향**: `presentation → application → domain`, `infrastructure`는 안쪽으로만. `domain`은
  아무 데도 의존하지 않는다. 이 규칙은 **ArchUnit 테스트로 강제**한다 — 어기면 빌드가 깨진다.
- **도메인↔DB 분리(헥사고날)**: 애그리거트마다 `순수 모델` / `포트(인터페이스)` / `JPA 엔티티` /
  `매퍼` / `영속성 어댑터` 5조각으로 나눴다. 예를 들어 `domain/room/Room`은 규칙만 담고, 실제 저장은
  `infrastructure/persistence/room`의 엔티티·어댑터가 책임진다.
- **실시간**: STOMP over WebSocket(`/ws`), 브로커 `/topic`·`/queue`, 앱 `/app`. 입장·스냅샷·펫 성장은
  이벤트로 발행 → `realtime`이 받아 토픽에 브로드캐스트(방/로그/게임 로직이 메시징에 직접 안 묶임).
- **방 두 종류**: 친구방(PRIVATE, 호스트 생성·정원 ≤50) / 테마방(THEMED, 고정 5곳·무기명·정원 ≤2000).

## 주요 기능

- **로그인**: OAuth2(구글·카카오)로 로그인 → 서버가 자체 JWT를 발급. 이 토큰 하나로 REST와 STOMP
  연결을 둘 다 인증한다(로컬·테스트는 `dev-login`으로 키 없이 토큰 발급).
- **방(로그)**: 친구방을 만들거나 테마방에 들어가 같이 공부한다. **입장은 정원을 절대 못 넘는다** —
  이 동시성 보장이 이 프로젝트의 핵심(아래 표).
- **공유 포모도로 타이머**: 한 방의 사람들이 같은 타이머로 집중/휴식을 함께 탄다. 1초 틱을 방 토픽으로
  브로드캐스트.
- **접속 상태(presence) · 채팅**: 누가 들어오고 나갔는지, 지금 누가 있는지 실시간으로 보인다.
- **공부 스냅샷(tomolog)**: 한 사이클이 끝나면 이모지 + 한 줄 메모 + 공부한 분을 남겨 방 피드에 쌓는다.
- **게이미피케이션**: 매일 공부하면 스트릭이 오르고, 조건을 채우면 뱃지를 받고, 방마다 같이 키우는
  **방펫**이 모두의 제출을 동시성-안전하게 합산해 성장한다.

## 클래스 구성 (핵심만)

| 영역 | 핵심 클래스 | 역할 |
|------|------------|------|
| 입장 전략(포트) | `domain/room/RoomJoinStrategy` | 정원-안전 입장의 공통 인터페이스(전략 패턴) |
| 입장 전략(구현) | `infrastructure/concurrency/{Pessimistic,Optimistic,DistributedLock,AtomicUpdate}JoinStrategy` | 4가지 동시성 메커니즘 |
| 방 도메인 | `domain/room/Room`, `RoomMember`, `RoomType`, `RoomStatus` | 정원·상태·멤버 규칙(순수 POJO) |
| 영속성 어댑터 | `infrastructure/persistence/room/Room{Entity,Mapper,PersistenceAdapter,JpaRepository}` | 도메인↔DB 매핑, `@Lock`·원자적 UPDATE 보유 |
| 유스케이스 | `application/room/RoomService` 등 | 트랜잭션 경계 + 타입별 입장 라우팅 |
| 실시간 | `application/realtime/{PomodoroTimerService,RoomEventBroadcaster,TimerScheduler}` | 타이머 틱·이벤트 브로드캐스트 |
| 게임 | `application/gamification/GamificationService`, `domain/gamification/{Badge,RoomPet}` | 스트릭·뱃지·방펫 |
| 인증 | `infrastructure/{jwt,oauth,security}` + `application/auth/{AuthService,TokenIssuer}` | OAuth2 + JWT(포트로 도메인과 분리) |

## 사용한 주요 자바 개념

실제 코드에서 쓴 것들만 적는다(버즈워드 나열이 아니라).

- **`record`로 만든 불변 DTO** — 컨트롤러 경계의 요청/응답은 전부 `record`(11개). 엔티티를 바깥에 노출하지 않는다.
- **전략 패턴(Strategy) + 다형성** — `RoomJoinStrategy` 인터페이스 하나에 입장 메커니즘 4개를 구현하고,
  친구방은 설정값으로, 테마방은 자동으로 골라 끼운다.
- **인터페이스 기반 포트/어댑터** — 리포지토리를 `JpaRepository` 상속이 아닌 **순수 인터페이스(포트)** 로 두고
  infrastructure에서 구현. 도메인이 JPA를 모르게 만드는 핵심.
- **동시성 제어** — `synchronized`가 아니라 DB·분산 레벨: 비관적 락(`SELECT … FOR UPDATE`), 낙관적 락
  (`@Version` + 재시도), 분산 락(Redisson `RLock`), 원자적 조건부 `UPDATE`. 멀티스레드 테스트는
  `ExecutorService` + `CountDownLatch`로 "동시 출발"을 만든다.
- **`Optional`·제네릭·`enum`** — null 대신 `Optional`, 타입(`RoomType`)·상태(`RoomStatus`)·전략 종류는 `enum`.
- **스트림/람다** — 매퍼·집계에서 사용.
- **Bean Validation** — 인바운드 DTO는 `@Valid`로 검증(클라이언트 입력 불신).

## 동시성 전략 비교 ⭐ (핵심)

입장은 정원을 **절대 초과하지 않도록** `RoomJoinStrategy`로 구현했고, 친구방은 설정값
(`tomolog.join-strategy`)으로 3전략 중 선택, 대규모 테마방은 자동으로 원자적 전략을 쓴다.

| 전략 | 메커니즘 | 실패 모드 / 특징 | 처리량 | 언제 쓰나 |
|------|----------|------------------|--------|-----------|
| **Pessimistic** | `SELECT ... FOR UPDATE`로 방 행을 잠그고 검사·삽입 | 같은 방 입장이 직렬화(락 대기). phantom read 없음 | 중 | 정확성 최우선, 일반 친구방 |
| **Optimistic** | `@Version` + 충돌 시 백오프 재시도 | 경합 심하면 재시도 폭증. DB 락 없음 | 낮은 경합에서 높음 | 경합 낮은 환경 |
| **Distributed** | Redisson `RLock`(`lock:room:{id}`) | 여러 인스턴스에서도 직렬화. 락 대기/리스 타임아웃 | 중 | 다중 인스턴스 배포 |
| **Atomic** (테마방) | 조건부 단일 UPDATE `SET count=count+1 WHERE count<capacity` | 임계 구간이 한 문장(짧은 행 락) | **높음** | 대규모(2000명) 테마방 |

**부하 실측(로컬, `./gradlew stressTest`)**
- 단일 핫룸: 2만 동시 입장에도 정원 정확히 유지·에러 0. 처리량은 ~500~750 joins/s에서 천장(같은 방
  행 갱신이 직렬화되므로 — 정확성은 부하로 깨지지 않는다).
- 10개 방 분산: ~4,163 joins/s(약 6배) — 방 분산으로 수평 확장.
- **방펫 성장**도 동시 제출에 원자적으로 합산(50명 동시 → 정확히 합산, 유실 0).

## 기술 스택

Java 21 · Spring Boot 3.3 · Spring Web / WebSocket(STOMP) / Data JPA / Security / OAuth2 Client ·
**PostgreSQL 16** · Redis + Redisson · JWT(jjwt) · Flyway · springdoc(Swagger) · JUnit5 · AssertJ ·
Mockito · Testcontainers · Gradle · Docker / docker-compose · GitHub Actions.

> 참고: 과제 원본 스펙은 MySQL이었으나, 매니지드 호스팅 가능성을 열어두려 PostgreSQL로 변경했다
> (의사결정 기록: `docs/DECISIONS.md` LEDGER-006).

## 로컬 실행

```bash
docker compose up --build      # app + postgres + redis 가 healthy 하게 기동
```

- 앱: http://localhost:8080
- 실제 구글/카카오 키가 없어도 개발용 로그인으로 토큰 발급(로컬·테스트 프로필 한정):
  ```bash
  curl -X POST localhost:8080/api/auth/dev-login -H 'Content-Type: application/json' -d '{"oauthId":"demo"}'
  # → {"accessToken":"...","tokenType":"Bearer"}  이후 Authorization: Bearer <token>
  ```
- 환경변수는 `.env.example` 참고(시크릿은 절대 커밋하지 않음).

## API 요약 (Swagger)

전체 명세와 실행은 **Swagger UI**: http://localhost:8080/swagger-ui.html

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/dev-login` | (local/test) 개발용 JWT 발급 |
| GET/PATCH | `/api/users/me` | 내 프로필 조회/수정 |
| POST/GET | `/api/rooms` | 방 생성 / 목록 |
| GET | `/api/rooms/{id}` | 방 상세(+멤버) |
| POST | `/api/rooms/{id}/join` | **동시성-보호 입장** |
| DELETE | `/api/rooms/{id}/members/me` | 퇴장 |
| POST/GET | `/api/rooms/{id}/logs` | 스냅샷 제출 / 피드 |
| GET | `/api/stats/me` | 스트릭·총시간·뱃지 |

실시간(STOMP): 구독 `/topic/rooms/{id}`(MEMBER_JOINED·PRESENCE_UPDATED·NEW_LOG·TIMER_TICK 등),
전송 `/app/rooms/{id}/{chat|presence|timer/control}`.

## 테스트

```bash
./gradlew build         # 전체 게이트: 포맷·체크스타일·PMD·테스트·커버리지·ArchUnit (Docker 필요)
./gradlew stressTest    # 대규모 부하 탐색(단일 핫룸 램프 + 멀티룸) — 기본 빌드엔 미포함
```

- **헤드라인 동시성 합격 테스트**: capacity-4 방에 100스레드를 동시 출발 → 정확히 4명 성공·96명
  RoomFull·DB 4. **3전략(+원자적 4전략) 모두** 통과(`AllStrategiesConcurrencyTest`).
- WebSocket 통합테스트, 방펫 동시성, 스트릭/뱃지, REST 슬라이스 등 포함. 라인 커버리지 ~90%+.

## 스크린샷 (제출용)

- [ ] Swagger UI (`/swagger-ui.html`)
- [ ] 동시성 합격 테스트 통과 로그
- [ ] `docker compose up` 기동 로그(app/postgres/redis healthy)

## CI

GitHub Actions(`.github/workflows/ci.yml`): JDK 21에서 전체 게이트 실행 + JaCoCo 리포트 업로드.
기본 브랜치(`main`)에서 green.

## 본인이 구현한 부분 & AI 활용 여부 (바이브코딩)

솔직하게 적는다. 이 프로젝트는 **AI 에이전트(Claude Code)를 적극적으로 활용한 "바이브코딩"** 으로 만들었다.

- **AI가 한 일**: 대부분의 실제 코드 타이핑(엔티티·서비스·컨트롤러·테스트·게이트 설정 보일러플레이트)과,
  내가 정한 방향대로의 구현·리팩토링.
- **내가 한 일(= 본인 기여)**: 무엇을 어떻게 만들지에 대한 **모든 결정과 설계**.
  - 에이전트 운영 규칙(헌장 `CLAUDE.md`)과 빌드 스펙(`SPEC.md`)을 직접 설계해, AI가 멋대로 안 하고
    게이트(포맷·린트·커버리지·ArchUnit) 안에서만 움직이게 만들었다. **"시스템을 만드는 시스템"을 설계한 셈.**
  - 핵심 의사결정을 직접 내리고 전부 `docs/DECISIONS.md`(원장)에 남겼다 — 방을 두 종류로 나눈 것
    (LEDGER-008), DB를 PostgreSQL로 바꾼 것(LEDGER-006), 4계층(LEDGER-007)·헥사고날 매퍼 분리
    (LEDGER-010) 적용, 부하 테스트를 "어디까지 안 터지나"까지 밀어붙인 것 등.
  - AI가 낸 결과를 리뷰하고 틀린 방향(예: 보상 decrement 롤백 함정, 스택 PR을 close한 실수 등)을 잡아
    다시 시켰다.

> 정리하면 키보드는 AI가 더 많이 쳤지만, **"무엇을·왜·어떻게"의 모든 판단과 검증은 내가 했고 그 흔적이
> `docs/DECISIONS.md`에 그대로 남아 있다.** 코드보다 원장을 보면 내 기여가 더 잘 보인다.

<!-- TODO(제출 전 본인 확인/보완): 위 'AI가 한 일/내가 한 일' 구분은 실제 작업 비중에 맞게 직접 다듬어 주세요.
     클론한 원본 저장소가 없다면(처음부터 빌드함) 아래 '원본 URL' 줄은 지워도 됩니다. -->

- 원본 클론 URL: 해당 없음(빈 저장소에서 처음부터 구현).

## 고려했지만 구현하지 않은 것 + 설계에서 신경 쓴 점

**실제로 빼기로 한 것 (한 가지 예):** 공유 타이머의 **다중 인스턴스 리더 선출(ShedLock 등)**.
지금 타이머는 `@Scheduled` 1초 틱으로 도는데, 서버를 여러 대 띄우면 인스턴스마다 틱을 쏴서 중복
브로드캐스트가 난다. ShedLock이나 Redisson 리더 선출로 "한 대만 틱"을 보장할 수 있지만, 과제 범위는
단일 서버 기준이라 **지금은 의도적으로 안 넣고 한계만 명시**했다(은폐하지 않는다 — `docs/DECISIONS.md`
LEDGER-009). 멀티 인스턴스 운영이 필요해지는 순간 들어갈 자리만 비워뒀다.

**설계에서 특히 신경 쓴 고급 기법:**
- **정원 초과 0 보장을 4가지 다른 동시성 모델로** 구현하고 같은 합격 테스트로 비교(전략 패턴). 단순히
  "락 걸었다"가 아니라, 비관적/낙관적/분산/원자적의 트레이드오프를 코드로 보여준다.
- **헥사고날로 도메인을 프레임워크에서 떼어내고**, 그게 정말 지켜지는지 ArchUnit 게이트로 강제
  (도메인이 `jakarta.persistence`에 손대면 빌드가 깨진다).
- **게이트가 죽은 규칙이 아님을 "깨보기"로 증명**: 가드를 일부러 부숴 빌드가 RED가 되는지 확인하고 원복.
  예) 원자적 cap 조건을 빼면 2000명 방에 2500명이 들어와 버린다(→ 가드가 진짜 일하는 증거).
- **정확성과 처리량을 분리해서 측정**: 부하를 끝까지 밀어도 정원 정확성은 안 깨지고, 천장은 "그 방의 행
  갱신 속도"라는 걸 수치로 보였다(단일 방 ~500~750 joins/s, 10방 분산 ~4,163 joins/s).
