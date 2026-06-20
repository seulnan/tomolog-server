package com.tomolog.domain.gamification;

import java.time.LocalDateTime;

/**
 * A badge a user has earned — a pure domain model. Earned once and never mutated; the unique
 * (userId, type) constraint at the persistence layer makes awarding idempotent (SPEC §3).
 */
public class Badge {

  private final Long id;
  private final Long userId;
  private final BadgeType type;
  private final LocalDateTime earnedAt;

  /** Creates a newly earned badge (no id yet — assigned on persist). */
  public Badge(Long userId, BadgeType type, LocalDateTime earnedAt) {
    this(null, userId, type, earnedAt);
  }

  /** Full constructor used by the persistence mapper. */
  public Badge(Long id, Long userId, BadgeType type, LocalDateTime earnedAt) {
    this.id = id;
    this.userId = userId;
    this.type = type;
    this.earnedAt = earnedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public BadgeType getType() {
    return type;
  }

  public LocalDateTime getEarnedAt() {
    return earnedAt;
  }
}
