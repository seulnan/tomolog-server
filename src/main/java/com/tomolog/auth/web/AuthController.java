package com.tomolog.auth.web;

import com.tomolog.auth.dto.TokenResponse;
import com.tomolog.auth.jwt.JwtProvider;
import com.tomolog.user.domain.OauthProvider;
import com.tomolog.user.domain.User;
import com.tomolog.user.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only login that mints a JWT for a seeded user so the test suite and the harness can run
 * without real Google/Kakao credentials. Active only under the {@code local} and {@code test}
 * profiles — absent in prod, where the route 404s (SPEC §7).
 */
@RestController
@RequestMapping("/api/auth")
@Profile({"local", "test"})
public class AuthController {

  private static final String DEFAULT_DEV_OAUTH_ID = "dev-user";

  private final UserService userService;
  private final JwtProvider jwtProvider;

  public AuthController(UserService userService, JwtProvider jwtProvider) {
    this.userService = userService;
    this.jwtProvider = jwtProvider;
  }

  /** Upserts a seeded user for the given (or default) oauth id and returns a fresh access token. */
  @PostMapping("/dev-login")
  public TokenResponse devLogin(@RequestBody(required = false) DevLoginRequest request) {
    String oauthId =
        request != null && request.oauthId() != null && !request.oauthId().isBlank()
            ? request.oauthId()
            : DEFAULT_DEV_OAUTH_ID;
    User user =
        userService.upsertOAuthUser(
            OauthProvider.GOOGLE, oauthId, oauthId + "@tomolog.local", "개발자");
    return TokenResponse.of(jwtProvider.createAccessToken(user.getId()));
  }
}
