package com.tomolog.application.gamification;

import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;
import com.tomolog.domain.gamification.BadgeRepository;
import com.tomolog.domain.user.User;
import com.tomolog.domain.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a user's study stats and badges (SPEC §5: GET /api/stats/me). */
@Service
@Transactional(readOnly = true)
public class StatsService {

  private final UserRepository userRepository;
  private final BadgeRepository badgeRepository;

  public StatsService(UserRepository userRepository, BadgeRepository badgeRepository) {
    this.userRepository = userRepository;
    this.badgeRepository = badgeRepository;
  }

  /** Returns the user's streak, total minutes, and earned badges. */
  public StatsResponse getStats(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    List<BadgeResponse> badges =
        badgeRepository.findByUserId(userId).stream().map(BadgeResponse::from).toList();
    return new StatsResponse(
        user.getCurrentStreak(), user.getLongestStreak(), user.getTotalStudyMinutes(), badges);
  }
}
