package com.tomolog.user.repository;

import com.tomolog.user.domain.OauthProvider;
import com.tomolog.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByOauthProviderAndOauthId(OauthProvider oauthProvider, String oauthId);

  boolean existsByEmail(String email);
}
