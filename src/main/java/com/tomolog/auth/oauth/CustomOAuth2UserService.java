package com.tomolog.auth.oauth;

import com.tomolog.user.domain.User;
import com.tomolog.user.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Loads the provider profile, upserts our {@link User}, and returns a principal whose name is our
 * own user id (so the success handler can mint a JWT for it). SPEC §7.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  /** Attribute key used to carry our DB user id on the returned principal. */
  public static final String USER_ID_ATTRIBUTE = "userId";

  private final UserService userService;

  public CustomOAuth2UserService(UserService userService) {
    this.userService = userService;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    OAuth2UserInfo info = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

    User user =
        userService.upsertOAuthUser(info.provider(), info.oauthId(), info.email(), info.nickname());

    Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
    attributes.put(USER_ID_ATTRIBUTE, user.getId());
    return new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, USER_ID_ATTRIBUTE);
  }
}
