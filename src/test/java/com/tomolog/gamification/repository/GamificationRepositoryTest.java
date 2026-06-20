package com.tomolog.gamification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomolog.domain.gamification.Badge;
import com.tomolog.domain.gamification.BadgeRepository;
import com.tomolog.domain.gamification.BadgeType;
import com.tomolog.domain.gamification.RoomPet;
import com.tomolog.domain.gamification.RoomPetRepository;
import com.tomolog.infrastructure.persistence.gamification.BadgeMapper;
import com.tomolog.infrastructure.persistence.gamification.BadgePersistenceAdapter;
import com.tomolog.infrastructure.persistence.gamification.RoomPetMapper;
import com.tomolog.infrastructure.persistence.gamification.RoomPetPersistenceAdapter;
import com.tomolog.support.AbstractRepositoryTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@Import({
  BadgePersistenceAdapter.class,
  BadgeMapper.class,
  RoomPetPersistenceAdapter.class,
  RoomPetMapper.class
})
class GamificationRepositoryTest extends AbstractRepositoryTest {

  @Autowired private RoomPetRepository roomPetRepository;
  @Autowired private BadgeRepository badgeRepository;

  @Test
  void roomPet_startsAtLevelOneAndLevelsUpAsItGrows() {
    RoomPet pet = roomPetRepository.save(new RoomPet(60L));
    assertThat(pet.getLevel()).isEqualTo(1);
    assertThat(pet.getGrowthPoints()).isZero();

    pet.addGrowth(250);
    RoomPet grown = roomPetRepository.save(pet);

    assertThat(grown.getGrowthPoints()).isEqualTo(250);
    assertThat(grown.getLevel()).isEqualTo(3);
    assertThat(roomPetRepository.findByRoomId(60L)).isPresent();
  }

  @Test
  void roomPet_isOnePerRoom() {
    roomPetRepository.save(new RoomPet(61L));

    assertThatThrownBy(() -> roomPetRepository.save(new RoomPet(61L)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void badge_savedAndFoundByUser() {
    badgeRepository.save(new Badge(70L, BadgeType.FIRST_LOG, LocalDateTime.now()));
    badgeRepository.save(new Badge(70L, BadgeType.STREAK_7, LocalDateTime.now()));

    assertThat(badgeRepository.findByUserId(70L)).hasSize(2);
    assertThat(badgeRepository.existsByUserIdAndType(70L, BadgeType.FIRST_LOG)).isTrue();
    assertThat(badgeRepository.existsByUserIdAndType(70L, BadgeType.MARATHON)).isFalse();
  }

  @Test
  void badge_sameTypeForSameUserIsRejected() {
    badgeRepository.save(new Badge(71L, BadgeType.NIGHT_OWL, LocalDateTime.now()));

    assertThatThrownBy(
            () -> badgeRepository.save(new Badge(71L, BadgeType.NIGHT_OWL, LocalDateTime.now())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
