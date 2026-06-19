package com.tomolog.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.infrastructure.jwt.JwtProperties;
import com.tomolog.infrastructure.jwt.JwtProvider;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

  private static final String SECRET = "unit-test-secret-0123456789-abcdefghij";

  private JwtProvider providerWithValidity(long seconds) {
    JwtProperties properties = new JwtProperties();
    properties.setSecret(SECRET);
    properties.setAccessTokenValiditySeconds(seconds);
    return new JwtProvider(properties);
  }

  @Test
  void createAccessToken_thenTokenIsValidAndCarriesUserId() {
    JwtProvider provider = providerWithValidity(3600);

    String token = provider.createAccessToken(42L);

    assertThat(provider.isValid(token)).isTrue();
    assertThat(provider.getUserId(token)).isEqualTo(42L);
  }

  @Test
  void isValid_givenTamperedToken_thenFalse() {
    JwtProvider provider = providerWithValidity(3600);
    String token = provider.createAccessToken(1L);

    assertThat(provider.isValid(token + "tampered")).isFalse();
  }

  @Test
  void isValid_givenExpiredToken_thenFalse() {
    JwtProvider provider = providerWithValidity(-3600);
    String expired = provider.createAccessToken(1L);

    assertThat(provider.isValid(expired)).isFalse();
  }

  @Test
  void isValid_givenTokenSignedWithDifferentSecret_thenFalse() {
    String foreign = providerWithValidity(3600).createAccessToken(1L);

    JwtProperties other = new JwtProperties();
    other.setSecret("a-totally-different-secret-9876543210-zyxwv");
    JwtProvider verifier = new JwtProvider(other);

    assertThat(verifier.isValid(foreign)).isFalse();
  }
}
