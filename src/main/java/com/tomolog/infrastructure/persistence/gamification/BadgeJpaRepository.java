package com.tomolog.infrastructure.persistence.gamification;

import com.tomolog.domain.gamification.BadgeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link BadgeEntity}; wrapped by the persistence adapter. */
public interface BadgeJpaRepository extends JpaRepository<BadgeEntity, Long> {

  List<BadgeEntity> findByUserId(Long userId);

  boolean existsByUserIdAndType(Long userId, BadgeType type);
}
