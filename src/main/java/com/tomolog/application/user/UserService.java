package com.tomolog.application.user;

import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;
import com.tomolog.domain.user.AvatarType;
import com.tomolog.domain.user.OauthProvider;
import com.tomolog.domain.user.User;
import com.tomolog.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and maintains user records, including upsert on OAuth login (SPEC §7). */
@Service
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** Returns the user or throws {@link ErrorCode#USER_NOT_FOUND}. */
  public User getById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
  }

  /** Finds the user for this OAuth identity, creating one on first login. */
  @Transactional
  public User upsertOAuthUser(
      OauthProvider provider, String oauthId, String email, String nickname) {
    return userRepository
        .findByOauthProviderAndOauthId(provider, oauthId)
        .orElseGet(
            () ->
                userRepository.save(
                    new User(
                        provider,
                        oauthId,
                        emailOrFallback(email, oauthId),
                        nicknameOrFallback(nickname, oauthId),
                        AvatarType.CAT)));
  }

  /** Updates the user's editable profile fields. */
  @Transactional
  public User updateProfile(Long userId, String nickname, AvatarType avatarType) {
    User user = getById(userId);
    user.updateProfile(nickname, avatarType);
    return user;
  }

  private static String emailOrFallback(String email, String oauthId) {
    return email != null && !email.isBlank() ? email : oauthId + "@tomolog.local";
  }

  private static String nicknameOrFallback(String nickname, String oauthId) {
    return nickname != null && !nickname.isBlank() ? nickname : "user-" + oauthId;
  }
}
