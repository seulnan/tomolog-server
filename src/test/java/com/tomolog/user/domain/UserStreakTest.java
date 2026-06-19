package com.tomolog.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserStreakTest {

  private static final LocalDate DAY1 = LocalDate.of(2026, 3, 1);

  private User newUser() {
    return new User(OauthProvider.GOOGLE, "oid", "e@x.com", "닉", AvatarType.CAT);
  }

  @Test
  void firstStudy_startsStreakAtOneAndAddsMinutes() {
    User user = newUser();

    user.recordStudy(30, DAY1);

    assertThat(user.getCurrentStreak()).isEqualTo(1);
    assertThat(user.getLongestStreak()).isEqualTo(1);
    assertThat(user.getTotalStudyMinutes()).isEqualTo(30);
  }

  @Test
  void consecutiveDays_extendStreak() {
    User user = newUser();

    user.recordStudy(10, DAY1);
    user.recordStudy(10, DAY1.plusDays(1));
    user.recordStudy(10, DAY1.plusDays(2));

    assertThat(user.getCurrentStreak()).isEqualTo(3);
    assertThat(user.getLongestStreak()).isEqualTo(3);
    assertThat(user.getTotalStudyMinutes()).isEqualTo(30);
  }

  @Test
  void sameDayAgain_keepsStreakButAddsMinutes() {
    User user = newUser();

    user.recordStudy(25, DAY1);
    user.recordStudy(25, DAY1);

    assertThat(user.getCurrentStreak()).isEqualTo(1);
    assertThat(user.getTotalStudyMinutes()).isEqualTo(50);
  }

  @Test
  void gap_resetsStreakToOneButKeepsLongest() {
    User user = newUser();

    user.recordStudy(10, DAY1);
    user.recordStudy(10, DAY1.plusDays(1)); // streak 2
    user.recordStudy(10, DAY1.plusDays(5)); // gap -> reset

    assertThat(user.getCurrentStreak()).isEqualTo(1);
    assertThat(user.getLongestStreak()).isEqualTo(2);
  }
}
