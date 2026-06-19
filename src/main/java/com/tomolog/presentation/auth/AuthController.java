package com.tomolog.presentation.auth;

import com.tomolog.application.auth.AuthService;
import com.tomolog.application.auth.TokenResponse;
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

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** Upserts a seeded user for the given (or default) oauth id and returns a fresh access token. */
  @PostMapping("/dev-login")
  public TokenResponse devLogin(@RequestBody(required = false) DevLoginRequest request) {
    String oauthId =
        request != null && request.oauthId() != null && !request.oauthId().isBlank()
            ? request.oauthId()
            : DEFAULT_DEV_OAUTH_ID;
    return TokenResponse.of(authService.devLogin(oauthId));
  }
}
