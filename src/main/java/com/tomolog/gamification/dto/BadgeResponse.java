package com.tomolog.gamification.dto;

import com.tomolog.gamification.domain.Badge;
import com.tomolog.gamification.domain.BadgeType;
import java.time.LocalDateTime;

/** A badge a user holds (SPEC §5: GET /api/stats/me). */
public record BadgeResponse(BadgeType type, LocalDateTime earnedAt) {

  public static BadgeResponse from(Badge badge) {
    return new BadgeResponse(badge.getType(), badge.getEarnedAt());
  }
}
