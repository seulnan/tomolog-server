package com.tomolog.gamification.domain;

import com.tomolog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * A badge a user has earned. The unique constraint on (user_id, type) makes earning idempotent — a
 * user holds each badge type at most once (SPEC §3).
 */
@Entity
@Table(
    name = "badges",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_badges_user_type",
            columnNames = {"user_id", "type"}))
public class Badge extends BaseTimeEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BadgeType type;

  @Column(name = "earned_at", nullable = false)
  private LocalDateTime earnedAt;

  protected Badge() {}

  public Badge(Long userId, BadgeType type, LocalDateTime earnedAt) {
    this.userId = userId;
    this.type = type;
    this.earnedAt = earnedAt;
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
