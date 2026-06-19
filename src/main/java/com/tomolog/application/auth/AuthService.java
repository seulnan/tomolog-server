package com.tomolog.application.auth;

import com.tomolog.application.user.UserService;
import com.tomolog.domain.user.OauthProvider;
import com.tomolog.domain.user.User;
import org.springframework.stereotype.Service;

/** Dev-login use case: upserts a seeded user and issues an access token (SPEC §7). */
@Service
public class AuthService {

  private final UserService userService;
  private final TokenIssuer tokenIssuer;

  public AuthService(UserService userService, TokenIssuer tokenIssuer) {
    this.userService = userService;
    this.tokenIssuer = tokenIssuer;
  }

  /** Upserts the (seeded) user for this oauth id and returns a fresh access token. */
  public String devLogin(String oauthId) {
    User user =
        userService.upsertOAuthUser(
            OauthProvider.GOOGLE, oauthId, oauthId + "@tomolog.local", "개발자");
    return tokenIssuer.createAccessToken(user.getId());
  }
}
