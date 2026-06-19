package com.tomolog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tomolog.application.user.UserService;
import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;
import com.tomolog.domain.user.AvatarType;
import com.tomolog.domain.user.OauthProvider;
import com.tomolog.domain.user.User;
import com.tomolog.domain.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @InjectMocks private UserService userService;

  @Test
  void getById_givenMissingUser_thenThrowsUserNotFound() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getById(1L))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void upsertOAuthUser_givenExistingUser_thenReturnsItWithoutSaving() {
    User existing = new User(OauthProvider.GOOGLE, "g-1", "e@x.com", "닉", AvatarType.CAT);
    when(userRepository.findByOauthProviderAndOauthId(OauthProvider.GOOGLE, "g-1"))
        .thenReturn(Optional.of(existing));

    User result = userService.upsertOAuthUser(OauthProvider.GOOGLE, "g-1", "e@x.com", "닉");

    assertThat(result).isSameAs(existing);
    verify(userRepository, never()).save(any());
  }

  @Test
  void upsertOAuthUser_givenNewUserWithMissingFields_thenSavesWithFallbacks() {
    when(userRepository.findByOauthProviderAndOauthId(OauthProvider.KAKAO, "k-9"))
        .thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    userService.upsertOAuthUser(OauthProvider.KAKAO, "k-9", null, null);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getEmail()).isEqualTo("k-9@tomolog.local");
    assertThat(saved.getNickname()).isEqualTo("user-k-9");
    assertThat(saved.getAvatarType()).isEqualTo(AvatarType.CAT);
  }

  @Test
  void updateProfile_changesNicknameAndAvatar() {
    User user = new User(OauthProvider.GOOGLE, "g-2", "e@x.com", "old", AvatarType.CAT);
    when(userRepository.findById(5L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.updateProfile(5L, "new", AvatarType.FROG);

    assertThat(result.getNickname()).isEqualTo("new");
    assertThat(result.getAvatarType()).isEqualTo(AvatarType.FROG);
  }
}
