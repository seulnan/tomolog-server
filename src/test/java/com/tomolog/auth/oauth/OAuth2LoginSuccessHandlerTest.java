package com.tomolog.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomolog.infrastructure.jwt.JwtProvider;
import com.tomolog.infrastructure.oauth.OAuth2LoginSuccessHandler;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

  @Mock private JwtProvider jwtProvider;
  @Mock private Authentication authentication;
  @Mock private OAuth2User principal;

  @Test
  void onAuthenticationSuccess_writesAccessTokenAsJson() throws IOException {
    when(authentication.getPrincipal()).thenReturn(principal);
    when(principal.getName()).thenReturn("42");
    when(jwtProvider.createAccessToken(42L)).thenReturn("signed-token");

    OAuth2LoginSuccessHandler handler =
        new OAuth2LoginSuccessHandler(jwtProvider, new ObjectMapper());
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("signed-token").contains("Bearer");
  }
}
