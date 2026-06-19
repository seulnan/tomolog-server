package com.tomolog.infrastructure.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomolog.application.auth.TokenResponse;
import com.tomolog.infrastructure.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * After a successful OAuth login, issues our own JWT for the upserted user and writes it as JSON. A
 * real frontend would instead receive the token via redirect; that is out of scope here.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;
  private final ObjectMapper objectMapper;

  public OAuth2LoginSuccessHandler(JwtProvider jwtProvider, ObjectMapper objectMapper) {
    this.jwtProvider = jwtProvider;
    this.objectMapper = objectMapper;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    Long userId = Long.valueOf(principal.getName());
    String accessToken = jwtProvider.createAccessToken(userId);

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), TokenResponse.of(accessToken));
  }
}
