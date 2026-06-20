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

## 만들면서 부딪힌 문제들

이 프로젝트에서 정말 어려웠던 건 기능을 많이 붙이는 게 아니라, 눈에 잘 안 보이는 동시성과
구조 문제였다. 만들면서 실제로 막혔던 지점 세 군데를, 그때 무슨 생각을 했고 어떻게 풀었는지
순서대로 적는다.

### 정원을 절대 못 넘기기

처음엔 단순하게 짰다. 방에 들어올 때 정원이 찼는지 보고, 안 찼으면 넣는다. 코드로 쓰면
`if (방이 꽉 찼나?) 거절; 아니면 입장;` 정도다. 혼자 테스트할 땐 잘 됐다.

문제는 여러 명이 같은 순간에 마지막 한 자리를 두고 들어올 때다. 두 요청이 거의 동시에 "아직
안 찼네"를 통과한 뒤, 둘 다 들어와 정원을 넘겨버린다. 검사하는 순간과 늘리는 순간 사이가
벌어져 있어서 생기는 일이다.

그래서 "검사와 증가가 쪼개지지 않게" 만드는 게 핵심이었다. 처음 택한 방법은 비관적 락이다.
입장할 때 그 방 행을 잠그고(`SELECT … FOR UPDATE`) 한 명씩 줄 세우면, 끼어들 틈이 사라진다.
가장 직관적이고 정확해서 친구방(정원 50명)에는 이걸 쓴다.

그런데 테마방은 정원이 2000명이다. 2000명이 전부 같은 방 행 하나를 잠그려고 줄을 서면, 그 행
하나가 병목이 된다. 그래서 큰 방만 따로, 잠그는 구간을 아예 SQL 한 문장으로 줄였다. "정원보다
적을 때만 카운트를 1 올려라"를 조건부 UPDATE 한 방으로 보내고, 그게 0줄을 바꿨으면 자리가
없었다는 뜻으로 처리한다.

```java
// AtomicUpdateJoinStrategy — 카운트를 올리는 UPDATE 자체가 정원 검사 역할을 한다
int reserved = roomRepository.increaseMemberCountIfNotFull(roomId); // SET count=count+1 WHERE count<capacity
if (reserved == 0) {                 // 0줄 갱신 = 그 사이 정원이 찼다
  throw new RoomFullException();
}
roomMemberRepository.save(new RoomMember(roomId, userId, MEMBER, now()));
```

여기서 한 번 크게 헤맸다. 원래는 멤버를 넣다가 실패하면(이미 들어와 있던 사람이면) 올려둔
카운트를 다시 1 빼주는 "보상" 코드를 넣었었다. 그런데 이게 트랜잭션 롤백과 겹치면서 카운트가
오히려 틀어졌다. 한참 들여다보고 나서야, 카운트 증가와 멤버 추가를 같은 트랜잭션에 묶어두면
중복 입장으로 실패할 때 롤백이 카운트까지 알아서 되돌린다는 걸 알았다. 그래서 보상 코드를
넣는 게 아니라 오히려 지웠다. 이 결정은 `docs/DECISIONS.md`의 LEDGER-008에 적어뒀다.

낙관적 락(버전 번호로 충돌을 잡고 재시도)이나 분산 락(Redisson)도 만들어는 뒀다. 다만
낙관적 락은 2000명이 몰리는 방에선 재시도가 너무 잦아지고, 분산 락은 서버가 한 대뿐인 지금은
굳이 네트워크 락을 칠 이유가 없어서 기본으로 쓰진 않는다. 버리는 대신 전략으로 남겨두고, 방
종류에 따라 갈아끼우게 했다.

```java
// RoomService.join — 방 타입을 보고 입장 전략을 고른다
RoomJoinStrategy strategy =
    type == RoomType.THEMED ? joinStrategyResolver.strategyFor(ATOMIC)  // 큰 방: SQL 한 문장
                            : joinStrategyResolver.active();            // 친구방: 설정된 락 전략
```

이렇게 네 전략을 `RoomJoinStrategy` 인터페이스 하나로 맞춰둔 덕에, 똑같은 테스트(정원 4명 방에
100명이 동시에 달려들면 정확히 4명만 성공) 하나로 네 전략을 전부 돌려 비교할 수 있었다.

### 헥사고날로 옮기다 만난 함정

다음으로 구조를 손봤다. 정원이나 스트릭 같은 "규칙"이 JPA 엔티티 안에 `@Column`이랑 뒤섞여
있으면, 나중에 DB나 프레임워크를 바꿀 때 규칙까지 흔들린다. 그래서 규칙만 담은 순수 자바 객체를
따로 두고, 실제 저장은 바깥쪽(엔티티·매퍼·어댑터)에 맡기도록 나눴다. 도메인 쪽엔 이런 규칙만
남는다.

```java
// domain/room/Room — 스프링도 JPA도 모르는 순수 객체
public boolean isFull() {
  return currentMemberCount >= capacity;
}
```

깔끔해 보였는데, 옮기고 나서 함정을 하나 만났다. 원래 JPA 엔티티는 트랜잭션 안에서 값만 바꿔도
끝날 때 알아서 저장된다(더티 체킹). 그런데 순수 객체로 떼어내면 그게 안 돼서, 값을 바꾼 곳마다
저장을 직접 호출해줘야 했다. 더 까다로운 건 비관적 락이었다. 락은 "잠근 바로 그 행"을 끝까지
들고 있어야 의미가 있는데, 저장할 때 새 엔티티를 만들어 버리면 잠금이 끊긴다. 그래서 어댑터의
저장 로직이 새로 만드는 대신, 이미 잠겨 있는 그 엔티티를 같은 트랜잭션에서 다시 찾아 값만
덮어쓰게 했다. 그래야 락도, 버전 충돌 검사도 그대로 살아남는다.

```java
// RoomPersistenceAdapter.save — 이미 잠긴 엔티티를 다시 찾아 값만 적용
RoomEntity entity = jpaRepository.findById(room.getId()).orElseThrow(...);
entity.apply(room.getStatus(), room.getCurrentMemberCount()); // FOR UPDATE 락·버전 검사 유지
```

매퍼를 자동으로 만들어주는 라이브러리(MapStruct)도 생각해봤지만, 애그리거트가 6개뿐이라
빌드만 복잡해질 것 같아서 손으로 썼다. 이쪽이 무슨 일이 일어나는지 더 잘 보인다.

### 초록불을 너무 믿지 않기

마지막은 검증 이야기다. 만들면서 제일 자주 한 생각이 "테스트가 통과한다고 이게 진짜 맞는 건가?"
였다. 정원 가드가 정말 일을 하고 있는지, 아니면 테스트가 운 좋게 통과하는 건지 확신이 안 섰다.

그래서 일부러 가드를 부숴봤다. 위의 조건부 UPDATE에서 `WHERE count < capacity`만 빼고 돌리니,
2000명짜리 방에 2500명이 그대로 들어오면서 테스트가 빨갛게 떴다. 그 조건 한 줄이 진짜로 정원을
지키고 있었다는 증거다. 확인하고 나서 원래대로 되돌렸다. 도메인이 JPA에 손대면 빌드가 깨지는
규칙(ArchUnit)도 똑같이 한 번 부숴서 확인했다.

부하도 "어디까지 버티나" 끝까지 밀어봤다. 결론은, 정확성은 부하로 안 깨진다는 거였다. 방 하나에
2만 명을 동시에 때려넣어도 정원은 정확히 지켜졌고(초과 0), 대신 처리량은 그 방 행을 갱신하는
속도(~500~750건/초)에서 천장을 쳤다. 방을 10개로 나누면 ~4,163건/초로 약 6배가 됐다. 즉
한계는 "정확성"이 아니라 "한 행을 얼마나 빨리 고치느냐"였고, 방을 나누면 늘어난다는 걸 숫자로
확인할 수 있었다.

## 고려했지만 안 만든 것

공유 타이머의 다중 인스턴스 리더 선출(ShedLock 같은 것)은 일부러 안 넣었다. 지금 타이머는 1초마다
틱을 쏘는데, 서버를 여러 대 띄우면 대마다 틱을 쏴서 같은 신호가 중복으로 나간다. 한 대만 틱을
쏘게 만드는 방법은 있지만, 과제는 서버 한 대 기준이라 지금 넣으면 과한 설계다. 그래서 숨기지 않고
"이건 단일 서버 전제"라고 한계만 명시해두고, 나중에 필요하면 들어갈 자리만 비워뒀다
(`docs/DECISIONS.md` LEDGER-009).

## 본인이 구현한 부분 & AI 활용 여부 (바이브코딩)

솔직하게 적으면, 이 프로젝트는 AI 에이전트(Claude Code)를 적극적으로 쓴 "바이브코딩"으로 만들었다.
코드 타이핑은 AI가 더 많이 했다. 대신 무엇을 왜 어떻게 만들지를 정하고, 결과가 맞는지 따지고
틀린 걸 잡아낸 건 내 몫이었다. 위에 적은 보상 코드의 롤백 함정을 찾아내고, 전략을 네 개로 나누고,
도메인을 프레임워크에서 떼어내기로 한 판단들이 그렇다.

특히 AI가 멋대로 가지 못하게, 운영 규칙(`CLAUDE.md`)과 빌드 스펙(`SPEC.md`)을 먼저 짜서 포맷·린트·
커버리지·아키텍처 검사라는 자동 게이트 안에서만 움직이게 만들었다. 그리고 방을 두 종류로 나눌지,
DB를 PostgreSQL로 바꿀지, 4계층·헥사고날을 적용할지 같은 결정은 전부 직접 내리고 그 이유까지
`docs/DECISIONS.md`에 남겼다. 코드만 보면 AI가 쓴 것 같지만, 그 코드가 왜 그렇게 생겼는지는 이
기록을 따라오면 보인다.

<!-- TODO(제출 전 본인 확인): 위 'AI가 더 많이 / 판단은 내가' 비중을 실제에 맞게 한 번 더 다듬어 주세요. -->

원본을 클론한 게 아니라 빈 저장소에서 처음부터 만들었기 때문에, 참고한 원본 URL은 없다.
