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

## 설계 이야기 — 왜 이렇게 만들었나 (핵심 결정 3가지)

이 프로젝트에서 내가 가장 오래 고민한 세 가지 결정을, 실제 코드와 함께 정리한다. 채점하실 때
"무엇을 만들려고 어떻게 풀었고, 다른 방법은 왜 안 골랐는지"를 봐 주셨으면 한다.

### 1. "정원을 절대 못 넘는다"를 어떻게 보장할까

이 서비스의 진짜 어려운 점은 기능이 많은 게 아니라, **여러 명이 같은 순간에 마지막 한 자리를 두고
입장**할 때다. 순진하게 `if (방.isFull()) 거절; else 입장;`으로 짜면, 두 스레드가 동시에 `isFull()`을
통과한 뒤 둘 다 들어와 정원을 넘긴다(check-then-act 경쟁). 그래서 "검사와 증가가 쪼개지지 않게(원자적)"
만드는 게 전부였다.

처음엔 **비관적 락**으로 풀었다 — 입장할 때 방 행을 잠그고(`SELECT … FOR UPDATE`) 검사·삽입을 직렬화한다.
가장 직관적이고 정확하다. 하지만 테마방은 정원이 2000명이라, 모두가 **같은 방 행 하나**를 두고 줄을 서면
그 행이 병목이 된다. 그래서 대규모 방만 따로, 임계 구간을 **한 문장**으로 줄인 원자적 조건부 UPDATE를 썼다:

```java
// AtomicUpdateJoinStrategy — 카운터 증가 자체가 정원 검사다
int reserved = roomRepository.increaseMemberCountIfNotFull(roomId); // UPDATE … SET count=count+1 WHERE count<capacity
if (reserved == 0) throw new RoomFullException();                   // 0줄 갱신 = 자리가 없었다는 뜻
roomMemberRepository.save(new RoomMember(roomId, userId, MEMBER, now()));
```

여기서 한 가지를 크게 고쳐 배웠다. 원래는 멤버 insert가 실패하면 카운터를 `-1` 하는 **보상 로직**을
넣으려 했는데, 그건 롤백과 겹쳐 카운터가 틀어지는 함정이었다. 그래서 증가와 insert를 **같은 트랜잭션**에
묶어, 중복 입장으로 unique 제약이 깨지면 롤백이 카운터 증가까지 자동으로 되돌리게 했다(보상 코드 자체를
없앴다 — `docs/DECISIONS.md` LEDGER-008).

**다른 방법은 왜 전부로 안 갔나:** 낙관적 락(`@Version` + 재시도)은 경합이 심한 2000명 방에선 재시도가
폭증하고, 분산 락(Redisson)은 단일 서버에선 굳이 네트워크 락을 칠 이유가 없다. 그래서 버리지 않고
**둘 다 전략으로 남겨**, 방 종류에 따라 갈아끼우게 했다(친구방=설정값, 테마방=원자적). "락 하나로 다
해결"이 아니라 상황별 트레이드오프를 코드로 보여주고 싶었다:

```java
// RoomService.join — 방 타입으로 전략을 라우팅
RoomJoinStrategy strategy =
    type == RoomType.THEMED ? joinStrategyResolver.strategyFor(ATOMIC)  // 대규모: 한 문장 원자 갱신
                            : joinStrategyResolver.active();            // 친구방: 설정된 락 전략
```

전략 4개를 `RoomJoinStrategy` 인터페이스 하나로 묶은 덕에, **똑같은 합격 테스트**(정원 4짜리 방에 100명
동시 출발 → 정확히 4명 성공)를 4전략 모두에 돌려 비교할 수 있었다.

### 2. 핵심 규칙을 프레임워크에서 떼어내기 (헥사고날)

정원·스트릭 같은 규칙이 JPA 엔티티 안에 `@Column`과 섞여 있으면, 나중에 DB나 ORM을 바꿀 때 규칙까지
흔들린다. 그래서 도메인을 **순수 자바 객체**로 두고, 저장은 바깥(infrastructure)의 엔티티·매퍼·어댑터가
맡게 나눴다. 도메인엔 규칙만 남는다:

```java
// domain/room/Room — 스프링도 JPA도 모르는 순수 객체
public boolean isFull() { return currentMemberCount >= capacity; }
```

대신 함정이 하나 생긴다. JPA 엔티티는 트랜잭션 안에서 필드만 바꿔도 자동 저장(dirty checking)되지만,
순수 객체는 그게 안 된다. 게다가 비관적 락은 "잠근 그 행"을 끝까지 잡고 있어야 직렬화가 성립한다. 그래서
어댑터의 `save()`가 새 엔티티를 만드는 게 아니라, **이미 잠긴 managed 엔티티를 같은 트랜잭션에서 다시
찾아** 값만 입히도록 했다 — 락도 `@Version` 검사도 그대로 살아남는다:

```java
// RoomPersistenceAdapter.save — update 시 잠긴 엔티티를 재조회해 적용
RoomEntity entity = jpaRepository.findById(room.getId()).orElseThrow(...);
entity.apply(room.getStatus(), room.getCurrentMemberCount()); // FOR UPDATE 락 + @Version 유지
```

**다른 방법은 왜 안 썼나:** 도메인=엔티티로 그냥 두는 게(매퍼 없음) 코드는 적지만, 위의 분리 이점을
포기하는 것이라 택하지 않았다. 매퍼 자동 생성 라이브러리(MapStruct)도 6개뿐이라 빌드만 복잡해져, 손으로
쓴 매퍼가 더 명확하다고 봤다.

### 3. "잘 된다"를 말이 아니라 증거로

규칙이 진짜 동작하는지 스스로도 못 믿어서, **가드를 일부러 부숴 빌드가 빨개지는지** 확인하고 되돌리는
"깨보기"를 했다. 예를 들어 원자적 UPDATE에서 `WHERE count < capacity` 조건만 빼 보면, 2000명 방에
2500명이 그대로 들어와 테스트가 RED가 된다 — 그 조건이 정말 정원을 지키고 있었다는 증거다. 같은 식으로
도메인이 JPA에 의존하면 ArchUnit 게이트가 깨지는 것도 확인했다.

부하도 "어디까지 안 터지나"까지 밀어 봤다. 결론은 **정확성은 부하로 안 깨진다**는 것 — 단일 핫룸에 2만
동시 입장을 때려도 정원은 정확히 유지되고(에러 0), 다만 처리량 천장은 "그 방 행을 갱신하는 속도"
(~500~750 joins/s)였다. 방을 10개로 분산하면 ~4,163 joins/s로 약 6배 — 즉 한계는 정확성이 아니라
단일 행의 쓰기 속도이고, 방을 나누면 수평 확장된다는 걸 수치로 보였다.

## 고려했지만 구현하지 않은 것 (한 가지 예)

공유 타이머의 **다중 인스턴스 리더 선출(ShedLock 등)** 은 일부러 뺐다. 지금 타이머는 `@Scheduled` 1초
틱으로 도는데, 서버를 여러 대 띄우면 인스턴스마다 틱을 쏴서 같은 신호가 중복 브로드캐스트된다. ShedLock이나
Redisson 리더 선출로 "한 대만 틱"을 보장할 수 있지만, 과제 범위가 단일 서버 기준이라 지금 넣으면
오버엔지니어링이다. 그래서 **숨기지 않고 한계만 명시**해 두고, 멀티 인스턴스 운영이 필요해지는 순간
들어갈 자리만 비워 뒀다(`docs/DECISIONS.md` LEDGER-009).

## 본인이 구현한 부분 & AI 활용 여부 (바이브코딩)

솔직하게 적는다. 이 프로젝트는 **AI 에이전트(Claude Code)를 적극적으로 활용한 "바이브코딩"** 으로 만들었다.
코드 타이핑은 AI가 더 많이 했지만, **무엇을·왜·어떻게 만들지의 판단과 검증은 내가 했다.** 위 "설계 이야기"가
그 증거다 — 전략을 4개로 나눈 것도, 보상 로직의 롤백 함정을 잡아낸 것도, 헥사고날로 도메인을 분리한 것도
내가 내린 결정이고, 그 과정은 전부 `docs/DECISIONS.md`(의사결정 원장)에 남겼다.

- **내가 한 일**: 에이전트 운영 규칙(`CLAUDE.md`)과 빌드 스펙(`SPEC.md`)을 직접 설계해 AI가 게이트
  (포맷·린트·커버리지·ArchUnit) 안에서만 움직이게 만든 것, 핵심 의사결정(방 두 종류 LEDGER-008,
  PostgreSQL 전환 LEDGER-006, 4계층 LEDGER-007, 헥사고날 LEDGER-010, 부하 한계 탐색), 그리고 AI가 낸
  결과를 리뷰해 틀린 방향(보상 decrement 함정 등)을 잡아 다시 시킨 것.
- **AI가 한 일**: 정해진 방향대로의 실제 코드 작성·리팩토링·테스트·보일러플레이트.

<!-- TODO(제출 전 본인 확인): 위 비중 서술을 실제 작업에 맞게 한 번 더 다듬어 주세요. -->

- 원본 클론 URL: 해당 없음(빈 저장소에서 처음부터 구현).
