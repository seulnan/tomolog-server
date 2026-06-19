package com.tomolog.application.gamification;

import com.tomolog.domain.gamification.Badge;
import com.tomolog.domain.gamification.BadgeType;
import java.time.LocalDateTime;

/** A badge a user holds (SPEC §5: GET /api/stats/me). */
public record BadgeResponse(BadgeType type, LocalDateTime earnedAt) {

  public static BadgeResponse from(Badge badge) {
    return new BadgeResponse(badge.getType(), badge.getEarnedAt());
  }
}
