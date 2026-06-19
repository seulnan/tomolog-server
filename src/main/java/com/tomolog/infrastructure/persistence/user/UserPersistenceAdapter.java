package com.tomolog.infrastructure.persistence.user;

import com.tomolog.domain.user.OauthProvider;
import com.tomolog.domain.user.User;
import com.tomolog.domain.user.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter for the {@link UserRepository} port. Translates between domain models and entities;
 * on update it re-fetches the managed entity and applies the domain's mutable fields.
 */
@Repository
public class UserPersistenceAdapter implements UserRepository {

  private final UserJpaRepository jpaRepository;
  private final UserMapper mapper;

  public UserPersistenceAdapter(UserJpaRepository jpaRepository, UserMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<User> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findByOauthProviderAndOauthId(OauthProvider oauthProvider, String oauthId) {
    return jpaRepository
        .findByOauthProviderAndOauthId(oauthProvider, oauthId)
        .map(mapper::toDomain);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpaRepository.existsByEmail(email);
  }

  @Override
  public User save(User user) {
    if (user.getId() == null) {
      return mapper.toDomain(jpaRepository.save(mapper.toNewEntity(user)));
    }
    UserEntity entity =
        jpaRepository
            .findById(user.getId())
            .orElseThrow(() -> new IllegalStateException("User not found: " + user.getId()));
    entity.apply(
        user.getEmail(),
        user.getNickname(),
        user.getAvatarType(),
        user.getTotalStudyMinutes(),
        user.getCurrentStreak(),
        user.getLongestStreak(),
        user.getLastStudyDate());
    return mapper.toDomain(jpaRepository.save(entity));
  }
}
