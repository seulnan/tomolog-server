package com.tomolog.room.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomType;
import com.tomolog.room.repository.RoomMemberRepository;
import com.tomolog.room.repository.RoomRepository;
import com.tomolog.room.service.RoomService;
import com.tomolog.support.AbstractIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Load test for large THEMED rooms (LEDGER-008): a capacity-2000 room is hit by 2500 concurrent
 * join attempts routed through the atomic-counter strategy. Proves capacity holds exactly at 2000
 * under heavy contention, and logs throughput. NOT {@code @Transactional} — the room is committed
 * before launch and each join is its own transaction.
 */
class ThemedRoomLoadTest extends AbstractIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(ThemedRoomLoadTest.class);
  private static final int CAPACITY = 2000;
  private static final int ATTEMPTS = 2500;

  @Autowired private RoomService roomService;
  @Autowired private RoomRepository roomRepository;
  @Autowired private RoomMemberRepository roomMemberRepository;

  private record Counts(int success, int full, int other, long elapsedMs) {}

  private Long newThemedRoom() {
    String code = "load-" + UUID.randomUUID().toString().substring(0, 6);
    return roomRepository
        .save(new Room("부하 테스트 카페", null, CAPACITY, code, RoomType.THEMED))
        .getId();
  }

  private Counts runConcurrentJoins(Long roomId, int attempts) throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(64);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(attempts);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger full = new AtomicInteger();
    AtomicInteger other = new AtomicInteger();
    for (int i = 0; i < attempts; i++) {
      long userId = 100_000L + i;
      pool.submit(
          () -> {
            try {
              start.await();
              roomService.join(roomId, userId);
              success.incrementAndGet();
            } catch (RoomFullException e) {
              full.incrementAndGet();
            } catch (Exception e) {
              other.incrementAndGet();
            } finally {
              done.countDown();
            }
          });
    }
    long t0 = System.nanoTime();
    start.countDown();
    boolean finished = done.await(120, TimeUnit.SECONDS);
    pool.shutdownNow();
    assertThat(finished).as("all attempts completed").isTrue();
    return new Counts(success.get(), full.get(), other.get(), (System.nanoTime() - t0) / 1_000_000);
  }

  @Test
  void themedRoom_under2500ConcurrentJoins_capsAtExactly2000() throws InterruptedException {
    Long roomId = newThemedRoom();

    Counts c = runConcurrentJoins(roomId, ATTEMPTS);
    log.info(
        "THEMED load: {} attempts, {} joined, {} rejected in {} ms ({} joins/s)",
        ATTEMPTS,
        c.success(),
        c.full(),
        c.elapsedMs(),
        c.success() * 1000L / Math.max(c.elapsedMs(), 1));

    assertThat(c.other()).as("no unexpected errors").isZero();
    assertThat(c.success()).isEqualTo(CAPACITY);
    assertThat(c.full()).isEqualTo(ATTEMPTS - CAPACITY);
    assertThat(roomMemberRepository.countByRoomId(roomId)).isEqualTo(CAPACITY);
    assertThat(roomRepository.findById(roomId).orElseThrow().getCurrentMemberCount())
        .isEqualTo(CAPACITY);
  }

  @Test
  void themedRoom_duplicateJoin_thenAlreadyJoinedAndCounterNotDrifted() {
    Long roomId = newThemedRoom();
    roomService.join(roomId, 777L);

    assertThatThrownBy(() -> roomService.join(roomId, 777L))
        .isInstanceOf(ApiException.class)
        .extracting(e -> ((ApiException) e).getErrorCode())
        .isEqualTo(ErrorCode.ALREADY_JOINED);
    assertThat(roomRepository.findById(roomId).orElseThrow().getCurrentMemberCount()).isEqualTo(1);
  }

  @Test
  void themedRoom_leaveDecrementsCounter() {
    Long roomId = newThemedRoom();
    roomService.join(roomId, 888L);

    roomService.leave(roomId, 888L);

    assertThat(roomRepository.findById(roomId).orElseThrow().getCurrentMemberCount()).isZero();
    assertThat(roomMemberRepository.countByRoomId(roomId)).isZero();
  }
}
