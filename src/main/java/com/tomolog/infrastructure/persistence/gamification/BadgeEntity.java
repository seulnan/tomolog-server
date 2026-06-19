package com.tomolog.infrastructure.persistence.gamification;

import com.tomolog.domain.common.BaseTimeEntity;
import com.tomolog.domain.gamification.BadgeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** JPA persistence model for a badge (maps to the {@code badges} table). */
@Entity
@Table(
    name = "badges",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_badges_user_type",
            columnNames = {"user_id", "type"}))
public class BadgeEntity extends BaseTimeEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private BadgeType type;

  @Column(name = "earned_at", nullable = false)
  private LocalDateTime earnedAt;

  protected BadgeEntity() {}

  public BadgeEntity(Long userId, BadgeType type, LocalDateTime earnedAt) {
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
