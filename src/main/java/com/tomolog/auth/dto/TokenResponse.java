package com.tomolog.auth.dto;

/** Access token returned after login (dev-login or OAuth success). */
public record TokenResponse(String accessToken, String tokenType) {

  public static TokenResponse of(String accessToken) {
    return new TokenResponse(accessToken, "Bearer");
  }
}
