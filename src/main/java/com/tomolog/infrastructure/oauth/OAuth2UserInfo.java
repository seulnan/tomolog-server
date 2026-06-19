package com.tomolog.infrastructure.oauth;

import com.tomolog.domain.user.OauthProvider;
import java.util.Map;

/**
 * Normalizes the provider-specific attribute maps from Google and Kakao into the few fields we
 * actually store. Pure mapping, so it is unit-testable without hitting a provider.
 */
public record OAuth2UserInfo(
    OauthProvider provider, String oauthId, String email, String nickname) {

  @SuppressWarnings("unchecked")
  public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
    return switch (registrationId) {
      case "google" ->
          new OAuth2UserInfo(
              OauthProvider.GOOGLE,
              String.valueOf(attributes.get("sub")),
              (String) attributes.get("email"),
              (String) attributes.get("name"));
      case "kakao" -> {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile =
            account == null ? Map.of() : (Map<String, Object>) account.get("profile");
        yield new OAuth2UserInfo(
            OauthProvider.KAKAO,
            String.valueOf(attributes.get("id")),
            account == null ? null : (String) account.get("email"),
            profile == null ? null : (String) profile.get("nickname"));
      }
      default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    };
  }
}
