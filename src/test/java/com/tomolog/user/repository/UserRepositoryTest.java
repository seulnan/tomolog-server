package com.tomolog.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.support.AbstractRepositoryTest;
import com.tomolog.user.domain.AvatarType;
import com.tomolog.user.domain.OauthProvider;
import com.tomolog.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryTest extends AbstractRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void save_givenNewUser_thenPersistsWithIdAuditAndDefaults() {
    User saved =
        userRepository.save(
            new User(OauthProvider.GOOGLE, "google-123", "a@example.com", "냥냥", AvatarType.CAT));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getOauthProvider()).isEqualTo(OauthProvider.GOOGLE);
    assertThat(saved.getOauthId()).isEqualTo("google-123");
    assertThat(saved.getEmail()).isEqualTo("a@example.com");
    assertThat(saved.getNickname()).isEqualTo("냥냥");
    assertThat(saved.getAvatarType()).isEqualTo(AvatarType.CAT);
    assertThat(saved.getTotalStudyMinutes()).isZero();
    assertThat(saved.getCurrentStreak()).isZero();
    assertThat(saved.getLongestStreak()).isZero();
    assertThat(saved.getLastStudyDate()).isNull();
  }

  @Test
  void findByOauthProviderAndOauthId_givenExistingUser_thenReturnsIt() {
    userRepository.save(
        new User(OauthProvider.KAKAO, "kakao-777", "b@example.com", "곰곰", AvatarType.BEAR));

    Optional<User> found =
        userRepository.findByOauthProviderAndOauthId(OauthProvider.KAKAO, "kakao-777");

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("b@example.com");
  }

  @Test
  void findByOauthProviderAndOauthId_givenWrongProvider_thenEmpty() {
    userRepository.save(
        new User(OauthProvider.KAKAO, "kakao-888", "c@example.com", "개굴", AvatarType.FROG));

    Optional<User> found =
        userRepository.findByOauthProviderAndOauthId(OauthProvider.GOOGLE, "kakao-888");

    assertThat(found).isEmpty();
  }

  @Test
  void existsByEmail_reflectsWhetherEmailIsTaken() {
    userRepository.save(
        new User(OauthProvider.GOOGLE, "google-999", "taken@example.com", "토끼", AvatarType.RABBIT));

    assertThat(userRepository.existsByEmail("taken@example.com")).isTrue();
    assertThat(userRepository.existsByEmail("free@example.com")).isFalse();
  }
}
