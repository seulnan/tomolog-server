package com.tomolog.gamification.service;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.gamification.domain.Badge;
import com.tomolog.gamification.domain.BadgeType;
import com.tomolog.gamification.event.PetGrewEvent;
import com.tomolog.gamification.repository.BadgeRepository;
import com.tomolog.gamification.repository.RoomPetRepository;
import com.tomolog.user.domain.User;
import com.tomolog.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies gamification when a study snapshot is submitted: advances the user's streak/minutes,
 * grows the room pet (concurrent-safe), and awards badges (SPEC §3, §6).
 */
@Service
public class GamificationService {

  private static final int STREAK_BADGE_DAYS = 7;
  private static final int NIGHT_OWL_END_HOUR = 5;
  private static final int MARATHON_MINUTES = 120;

  private final UserRepository userRepository;
  private final RoomPetRepository roomPetRepository;
  private final BadgeRepository badgeRepository;
  private final ApplicationEventPublisher eventPublisher;

  public GamificationService(
      UserRepository userRepository,
      RoomPetRepository roomPetRepository,
      BadgeRepository badgeRepository,
      ApplicationEventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.roomPetRepository = roomPetRepository;
    this.badgeRepository = badgeRepository;
    this.eventPublisher = eventPublisher;
  }

  /** Records study minutes, grows the pet, and evaluates badges for the user. */
  @Transactional
  public void recordStudy(Long userId, Long roomId, int studiedMinutes, LocalDateTime now) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    user.recordStudy(studiedMinutes, now.toLocalDate());
    growPet(roomId, studiedMinutes);
    awardBadges(user, studiedMinutes, now);
  }

  private void growPet(Long roomId, int points) {
    if (roomPetRepository.growPet(roomId, points) > 0) {
      roomPetRepository
          .findByRoomId(roomId)
          .ifPresent(
              pet ->
                  eventPublisher.publishEvent(
                      new PetGrewEvent(roomId, pet.getGrowthPoints(), pet.getLevel())));
    }
  }

  private void awardBadges(User user, int minutes, LocalDateTime now) {
    award(user.getId(), BadgeType.FIRST_LOG);
    if (user.getCurrentStreak() >= STREAK_BADGE_DAYS) {
      award(user.getId(), BadgeType.STREAK_7);
    }
    if (now.getHour() < NIGHT_OWL_END_HOUR) {
      award(user.getId(), BadgeType.NIGHT_OWL);
    }
    if (minutes >= MARATHON_MINUTES) {
      award(user.getId(), BadgeType.MARATHON);
    }
  }

  private void award(Long userId, BadgeType type) {
    if (!badgeRepository.existsByUserIdAndType(userId, type)) {
      badgeRepository.save(new Badge(userId, type, LocalDateTime.now()));
    }
  }
}
