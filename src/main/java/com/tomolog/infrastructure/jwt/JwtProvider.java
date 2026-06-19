package com.tomolog.infrastructure.jwt;

import com.tomolog.application.auth.TokenIssuer;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Issues and validates HS256 access tokens whose subject is the user id (SPEC §7). */
@Component
public class JwtProvider implements TokenIssuer {

  private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

  private final SecretKey key;
  private final long accessTokenValiditySeconds;

  public JwtProvider(JwtProperties properties) {
    this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenValiditySeconds = properties.getAccessTokenValiditySeconds();
  }

  /** Issues an access token for the given user id. */
  public String createAccessToken(Long userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessTokenValiditySeconds * 1000);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  /** Returns true if the token's signature and expiry are valid. */
  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      log.debug("Rejected JWT: {}", ex.getMessage());
      return false;
    }
  }

  /** Extracts the user id from a valid token. */
  public Long getUserId(String token) {
    return Long.valueOf(parse(token));
  }

  private String parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }
}
