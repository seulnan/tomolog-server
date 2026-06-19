package com.tomolog.room.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.application.room.RoomService;
import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomFullException;
import com.tomolog.domain.room.RoomMemberRepository;
import com.tomolog.domain.room.RoomRepository;
import com.tomolog.domain.room.RoomType;
import com.tomolog.support.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Heavy load/stress exploration for THEMED rooms — NOT part of the default build (run via {@code
 * ./gradlew stressTest}). Pushes far past the correctness range to find the ceiling: a single hot
 * room is row-lock-bound (throughput plateaus no matter the concurrency), while spreading load
 * across many rooms scales the aggregate. The hard invariant — capacity is NEVER exceeded — must
 * hold at every level (LEDGER-008).
 */
@Tag("stress")
class StressTest extends AbstractIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(StressTest.class);
  private static final int THREADS = 128;

  @Autowired private RoomService roomService;
  @Autowired private RoomRepository roomRepository;
  @Autowired private RoomMemberRepository roomMemberRepository;

  private Long newThemedRoom(int capacity) {
    String code = "st-" + UUID.randomUUID().toString().substring(0, 8);
    return roomRepository.save(new Room("부하 카페", null, capacity, code, RoomType.THEMED)).getId();
  }

  @Test
  void singleHotRoom_rampUp_capHoldsAndThroughputPlateaus() throws InterruptedException {
    int capacity = 2000;
    for (int attempts : new int[] {5_000, 10_000, 20_000}) {
      long roomId = newThemedRoom(capacity);
      int[] r = runJoins(List.of(roomId), attempts);
      long ms = r[3];
      log.info(
          "[single hot room] cap={} attempts={} joined={} rejected={} other={} in {} ms ({} joins/s)",
          capacity,
          attempts,
          r[0],
          r[1],
          r[2],
          ms,
          r[0] * 1000L / Math.max(ms, 1));
      assertThat(roomRepository.findById(roomId).orElseThrow().getCurrentMemberCount())
          .as("cap never exceeded at attempts=%d", attempts)
          .isEqualTo(capacity);
      assertThat(r[0]).isEqualTo(capacity);
    }
  }

  @Test
  void manyRooms_spreadLoad_scalesAggregateThroughput() throws InterruptedException {
    int rooms = 10;
    int capacity = 2000;
    int attempts = 30_000;
    List<Long> roomIds = new ArrayList<>();
    for (int i = 0; i < rooms; i++) {
      roomIds.add(newThemedRoom(capacity));
    }

    int[] r = runJoins(roomIds, attempts);
    long ms = r[3];
    log.info(
        "[{} rooms] attempts={} joined={} rejected={} other={} in {} ms ({} joins/s aggregate)",
        rooms,
        attempts,
        r[0],
        r[1],
        r[2],
        ms,
        r[0] * 1000L / Math.max(ms, 1));

    assertThat(r[0]).isEqualTo(rooms * capacity);
    for (Long roomId : roomIds) {
      assertThat(roomMemberRepository.countByRoomId(roomId)).isEqualTo(capacity);
    }
  }

  /** Returns [success, full, other, elapsedMs]; spreads attempts round-robin over the rooms. */
  private int[] runJoins(List<Long> roomIds, int attempts) throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(attempts);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger full = new AtomicInteger();
    AtomicInteger other = new AtomicInteger();
    for (int i = 0; i < attempts; i++) {
      long roomId = roomIds.get(i % roomIds.size());
      long userId = 1_000_000L + i;
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
    boolean finished = done.await(180, TimeUnit.SECONDS);
    pool.shutdownNow();
    assertThat(finished).as("all attempts completed").isTrue();
    return new int[] {
      success.get(), full.get(), other.get(), (int) ((System.nanoTime() - t0) / 1_000_000)
    };
  }
}
