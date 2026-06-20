package com.tomolog.infrastructure.persistence.gamification;

import com.tomolog.domain.gamification.Badge;
import org.springframework.stereotype.Component;

/** Maps between the domain {@link Badge} and its JPA {@link BadgeEntity}. */
@Component
public class BadgeMapper {

  public Badge toDomain(BadgeEntity entity) {
    return new Badge(entity.getId(), entity.getUserId(), entity.getType(), entity.getEarnedAt());
  }

  public BadgeEntity toNewEntity(Badge badge) {
    return new BadgeEntity(badge.getUserId(), badge.getType(), badge.getEarnedAt());
  }
}
