package com.tomolog.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.gamification.domain.RoomPet;
import com.tomolog.gamification.repository.RoomPetRepository;
import com.tomolog.room.domain.Room;
import com.tomolog.room.repository.RoomRepository;
import com.tomolog.support.AbstractIntegrationTest;
import com.tomolog.user.domain.AvatarType;
import com.tomolog.user.domain.OauthProvider;
import com.tomolog.user.domain.User;
import com.tomolog.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * SPEC §3: the shared room pet must count concurrent feeds correctly. 50 members submit study at
 * once; the pet's growth must equal the exact sum, with no lost updates (the atomic counter update
 * is the guard). NOT {@code @Transactional}.
 */
class PetGrowthConcurrencyTest extends AbstractIntegrationTest {

  private static final int MEMBERS = 50;
  private static final int MINUTES_EACH = 10;

  @Autowired private GamificationService gamificationService;
  @Autowired private UserRepository userRepository;
  @Autowired private RoomRepository roomRepository;
  @Autowired private RoomPetRepository roomPetRepository;

  @Test
  void concurrentFeeds_areAllCountedInThePet() throws InterruptedException {
    String code = "pet-" + UUID.randomUUID().toString().substring(0, 6);
    Long roomId = roomRepository.save(new Room("펫방", 1L, MEMBERS, code)).getId();
    roomPetRepository.save(new RoomPet(roomId));
    List<Long> userIds = new ArrayList<>();
    for (int i = 0; i < MEMBERS; i++) {
      userIds.add(
          userRepository
              .save(
                  new User(
                      OauthProvider.GOOGLE,
                      "pet-" + i,
                      "p" + i + "@x.com",
                      "u" + i,
                      AvatarType.CAT))
              .getId());
    }

    ExecutorService pool = Executors.newFixedThreadPool(32);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(MEMBERS);
    LocalDateTime now = LocalDateTime.of(2026, 3, 1, 14, 0);
    for (Long userId : userIds) {
      pool.submit(
          () -> {
            try {
              start.await();
              gamificationService.recordStudy(userId, roomId, MINUTES_EACH, now);
            } catch (Exception ignored) {
              // counted by the assertion below (growth would be short)
            } finally {
              done.countDown();
            }
          });
    }
    start.countDown();
    assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
    pool.shutdownNow();

    RoomPet pet = roomPetRepository.findByRoomId(roomId).orElseThrow();
    assertThat(pet.getGrowthPoints()).isEqualTo(MEMBERS * MINUTES_EACH);
    assertThat(pet.getLevel()).isEqualTo((MEMBERS * MINUTES_EACH) / 100 + 1);
  }
}
