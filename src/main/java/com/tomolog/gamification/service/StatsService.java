package com.tomolog.gamification.service;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.gamification.dto.BadgeResponse;
import com.tomolog.gamification.dto.StatsResponse;
import com.tomolog.gamification.repository.BadgeRepository;
import com.tomolog.user.domain.User;
import com.tomolog.user.repository.UserRepository;
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
