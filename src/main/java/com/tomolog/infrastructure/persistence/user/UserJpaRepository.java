package com.tomolog.infrastructure.persistence.user;

import com.tomolog.domain.user.OauthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link UserEntity}; wrapped by the persistence adapter. */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByOauthProviderAndOauthId(OauthProvider oauthProvider, String oauthId);

  boolean existsByEmail(String email);
}
