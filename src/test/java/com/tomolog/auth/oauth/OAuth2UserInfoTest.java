package com.tomolog.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomolog.domain.user.OauthProvider;
import com.tomolog.infrastructure.oauth.OAuth2UserInfo;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OAuth2UserInfoTest {

  @Test
  void of_givenGoogleAttributes_thenMapsSubEmailName() {
    OAuth2UserInfo info =
        OAuth2UserInfo.of(
            "google", Map.of("sub", "g-123", "email", "g@example.com", "name", "구글이"));

    assertThat(info.provider()).isEqualTo(OauthProvider.GOOGLE);
    assertThat(info.oauthId()).isEqualTo("g-123");
    assertThat(info.email()).isEqualTo("g@example.com");
    assertThat(info.nickname()).isEqualTo("구글이");
  }

  @Test
  void of_givenKakaoNestedAttributes_thenMapsIdEmailNickname() {
    Map<String, Object> attributes =
        Map.of(
            "id",
            9999L,
            "kakao_account",
            Map.of("email", "k@example.com", "profile", Map.of("nickname", "카카오")));

    OAuth2UserInfo info = OAuth2UserInfo.of("kakao", attributes);

    assertThat(info.provider()).isEqualTo(OauthProvider.KAKAO);
    assertThat(info.oauthId()).isEqualTo("9999");
    assertThat(info.email()).isEqualTo("k@example.com");
    assertThat(info.nickname()).isEqualTo("카카오");
  }

  @Test
  void of_givenUnknownProvider_thenThrows() {
    assertThatThrownBy(() -> OAuth2UserInfo.of("naver", Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
