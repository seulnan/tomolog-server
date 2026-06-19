package com.tomolog.domain.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByOauthProviderAndOauthId(OauthProvider oauthProvider, String oauthId);

  boolean existsByEmail(String email);
}
