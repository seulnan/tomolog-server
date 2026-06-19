package com.tomolog.infrastructure.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings. {@code secret} must be at least 256 bits (32 bytes) for HS256; prod supplies it via
 * {@code JWT_SECRET} with no fallback so a misconfigured prod fails closed (SPEC §7).
 */
@ConfigurationProperties(prefix = "tomolog.jwt")
public class JwtProperties {

  private String secret;
  private long accessTokenValiditySeconds = 3600;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getAccessTokenValiditySeconds() {
    return accessTokenValiditySeconds;
  }

  public void setAccessTokenValiditySeconds(long accessTokenValiditySeconds) {
    this.accessTokenValiditySeconds = accessTokenValiditySeconds;
  }
}
