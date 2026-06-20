package com.tomolog.domain.gamification;

import java.util.List;

/** Domain port for badge persistence (plain interface, domain models). */
public interface BadgeRepository {

  List<Badge> findByUserId(Long userId);

  boolean existsByUserIdAndType(Long userId, BadgeType type);

  Badge save(Badge badge);
}
