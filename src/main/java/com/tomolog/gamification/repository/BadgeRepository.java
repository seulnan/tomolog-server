package com.tomolog.gamification.repository;

import com.tomolog.gamification.domain.Badge;
import com.tomolog.gamification.domain.BadgeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

  List<Badge> findByUserId(Long userId);

  boolean existsByUserIdAndType(Long userId, BadgeType type);
}
