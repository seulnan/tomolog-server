package com.tomolog.room.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.room.domain.Room;
import com.tomolog.room.repository.RoomMemberRepository;
import com.tomolog.room.repository.RoomRepository;
import com.tomolog.support.AbstractIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Headline concurrency acceptance test (SPEC §8): 100 threads released together attempt to join a
 * capacity-4 room; exactly 4 must succeed and the rest must get {@link RoomFullException}, with the
 * DB ending at exactly 4 members. NOT {@code @Transactional}: the room is committed before the
 * threads launch, and each join runs in its own transaction through the Spring proxy.
 */
class PessimisticJoinConcurrencyTest extends AbstractIntegrationTest {

  private static final int CAPACITY = 4;
  private static final int ATTEMPTS = 100;

  @Autowired private PessimisticLockJoinStrategy joinStrategy;
  @Autowired private RoomRepository roomRepository;
  @Autowired private RoomMemberRepository roomMemberRepository;

  @Test
  void join_with100SimultaneousAttempts_thenExactlyCapacitySucceeds() throws InterruptedException {
    String inviteCode = "c-" + UUID.randomUUID().toString().substring(0, 8);
    Long roomId = roomRepository.save(new Room("동시성 테스트", 1L, CAPACITY, inviteCode)).getId();

    ExecutorService pool = Executors.newFixedThreadPool(32);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(ATTEMPTS);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger full = new AtomicInteger();
    AtomicInteger other = new AtomicInteger();

    for (int i = 0; i < ATTEMPTS; i++) {
      long userId = 1000L + i;
      pool.submit(
          () -> {
            try {
              start.await();
              joinStrategy.join(roomId, userId);
              success.incrementAndGet();
            } catch (RoomFullException e) {
              full.incrementAndGet();
            } catch (Exception t) {
              other.incrementAndGet();
            } finally {
              done.countDown();
            }
          });
    }

    start.countDown();
    boolean finished = done.await(60, TimeUnit.SECONDS);
    pool.shutdownNow();

    assertThat(finished).isTrue();
    assertThat(other.get()).as("no unexpected errors").isZero();
    assertThat(success.get()).isEqualTo(CAPACITY);
    assertThat(full.get()).isEqualTo(ATTEMPTS - CAPACITY);
    assertThat(roomMemberRepository.countByRoomId(roomId)).isEqualTo(CAPACITY);
    assertThat(roomRepository.findById(roomId).orElseThrow().getCurrentMemberCount())
        .isEqualTo(CAPACITY);
  }
}
