package com.tomolog.domain.user;

import java.util.Optional;

/**
 * Domain port for user persistence — a plain interface returning domain models, with no framework
 * coupling. The JPA adapter lives in the infrastructure layer.
 */
public interface UserRepository {

  Optional<User> findById(Long id);

  Optional<User> findByOauthProviderAndOauthId(OauthProvider oauthProvider, String oauthId);

  boolean existsByEmail(String email);

  User save(User user);
}
