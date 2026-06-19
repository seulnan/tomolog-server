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

```
com.tomolog
├── config        # WebSocket / OpenAPI 설정
├── auth          # OAuth2(구글·카카오) + 자체 JWT, 시큐리티 체인, STOMP CONNECT 인증
├── user          # 회원 도메인/리포지토리/서비스/프로필 API
├── room          # 방 도메인 + concurrency(입장 전략 4종) + CRUD/입장/퇴장 API
├── realtime      # STOMP: presence·채팅·공유 타이머 + 이벤트 브로드캐스트(인메모리 레지스트리)
├── log           # 공부 스냅샷 제출/피드 (+ NEW_LOG 브로드캐스트)
├── gamification  # 스트릭·뱃지·방펫(동시성 안전 성장) + 통계 API
└── common        # 통일 에러 envelope, 감사(auditing) 베이스 엔티티
```

- **계층**: Controller → Service → Repository, 경계는 DTO(record)만. 생성자 주입만. ArchUnit으로 강제.
- **실시간**: STOMP over WebSocket(`/ws`), 브로커 `/topic`·`/queue`, 앱 `/app`. 입장/스냅샷/펫 성장은
  Spring ApplicationEvent로 발행 → realtime이 받아 토픽에 브로드캐스트(룸/로그/게임 로직이 메시징에 비의존).
- **방 두 종류**: 친구방(PRIVATE, 호스트 생성·정원 ≤50) / 테마방(THEMED, 고정 5곳·무기명·정원 ≤2000).

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
