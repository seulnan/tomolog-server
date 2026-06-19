package com.tomolog.room.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.application.room.JoinStrategyResolver;
import com.tomolog.domain.room.JoinStrategyType;
import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomFullException;
import com.tomolog.domain.room.RoomJoinStrategy;
import com.tomolog.domain.room.RoomMemberRepository;
import com.tomolog.domain.room.RoomRepository;
import com.tomolog.support.AbstractIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Headline concurrency acceptance test (SPEC §8), run against ALL three strategies: 100 threads
 * released together attempt to join a capacity-4 room; exactly 4 must succeed, the rest must get
 * {@link RoomFullException}, no unexpected errors, and the DB must end at exactly 4 members. NOT
 * {@code @Transactional}: the room is committed before launch and each join runs in its own
 * transaction through the Spring-managed strategy bean.
 */
class AllStrategiesConcurrencyTest extends AbstractIntegrationTest {

  private static final int CAPACITY = 4;
  private static final int ATTEMPTS = 100;

  @Autowired private JoinStrategyResolver resolver;
  @Autowired private RoomRepository roomRepository;
  @Autowired private RoomMemberRepository roomMemberRepository;

  @ParameterizedTest
  @EnumSource(JoinStrategyType.class)
  void join_with100SimultaneousAttempts_thenExactlyCapacitySucceeds(JoinStrategyType type)
      throws InterruptedException {
    RoomJoinStrategy strategy = resolver.strategyFor(type);
    String inviteCode = "c-" + UUID.randomUUID().toString().substring(0, 8);
    Long roomId = roomRepository.save(new Room(type + " 방", 1L, CAPACITY, inviteCode)).getId();

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
              strategy.join(roomId, userId);
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

    start.countDown();
    boolean finished = done.await(60, TimeUnit.SECONDS);
    pool.shutdownNow();

    assertThat(finished).as("all attempts completed for %s", type).isTrue();
    assertThat(other.get()).as("no unexpected errors for %s", type).isZero();
    assertThat(success.get()).as("successes for %s", type).isEqualTo(CAPACITY);
    assertThat(full.get()).as("rejections for %s", type).isEqualTo(ATTEMPTS - CAPACITY);
    assertThat(roomMemberRepository.countByRoomId(roomId)).isEqualTo(CAPACITY);
    assertThat(roomRepository.findById(roomId).orElseThrow().getCurrentMemberCount())
        .isEqualTo(CAPACITY);
  }
}
