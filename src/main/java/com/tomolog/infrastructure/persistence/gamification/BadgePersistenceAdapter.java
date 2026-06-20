package com.tomolog.infrastructure.persistence.gamification;

import com.tomolog.domain.gamification.Badge;
import com.tomolog.domain.gamification.BadgeRepository;
import com.tomolog.domain.gamification.BadgeType;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter for the {@link BadgeRepository} port. Badges are only ever created, never updated.
 */
@Repository
public class BadgePersistenceAdapter implements BadgeRepository {

  private final BadgeJpaRepository jpaRepository;
  private final BadgeMapper mapper;

  public BadgePersistenceAdapter(BadgeJpaRepository jpaRepository, BadgeMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public List<Badge> findByUserId(Long userId) {
    return jpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public boolean existsByUserIdAndType(Long userId, BadgeType type) {
    return jpaRepository.existsByUserIdAndType(userId, type);
  }

  @Override
  public Badge save(Badge badge) {
    // Flush on save so the unique (user_id, type) constraint surfaces immediately as a
    // DataIntegrityViolationException rather than being deferred to commit.
    return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toNewEntity(badge)));
  }
}
