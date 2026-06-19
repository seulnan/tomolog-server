package com.tomolog.user.domain;

import com.tomolog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/**
 * A registered user. Identity comes from an OAuth provider; study stats (minutes, streaks) are
 * maintained as gamification events happen (SPEC §3).
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_users_provider_oauth_id",
            columnNames = {"oauth_provider", "oauth_id"}))
public class User extends BaseTimeEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "oauth_provider", nullable = false, length = 20)
  private OauthProvider oauthProvider;

  @Column(name = "oauth_id", nullable = false, length = 100)
  private String oauthId;

  @Column(nullable = false, length = 100)
  private String email;

  @Column(nullable = false, length = 30)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(name = "avatar_type", nullable = false, length = 20)
  private AvatarType avatarType;

  @Column(name = "total_study_minutes", nullable = false)
  private long totalStudyMinutes;

  @Column(name = "current_streak", nullable = false)
  private int currentStreak;

  @Column(name = "longest_streak", nullable = false)
  private int longestStreak;

  @Column(name = "last_study_date")
  private LocalDate lastStudyDate;

  protected User() {}

  public User(
      OauthProvider oauthProvider,
      String oauthId,
      String email,
      String nickname,
      AvatarType avatarType) {
    this.oauthProvider = oauthProvider;
    this.oauthId = oauthId;
    this.email = email;
    this.nickname = nickname;
    this.avatarType = avatarType;
  }

  public void updateProfile(String nickname, AvatarType avatarType) {
    this.nickname = nickname;
    this.avatarType = avatarType;
  }

  /**
   * Records a study session: adds minutes and advances the daily streak. Studying the next calendar
   * day extends the streak; a gap resets it to 1; multiple sessions the same day keep it.
   */
  public void recordStudy(int minutes, LocalDate today) {
    this.totalStudyMinutes += minutes;
    if (lastStudyDate == null || lastStudyDate.plusDays(1).equals(today)) {
      this.currentStreak = lastStudyDate == null ? 1 : currentStreak + 1;
    } else if (!lastStudyDate.equals(today)) {
      this.currentStreak = 1;
    }
    this.longestStreak = Math.max(longestStreak, currentStreak);
    this.lastStudyDate = today;
  }

  public OauthProvider getOauthProvider() {
    return oauthProvider;
  }

  public String getOauthId() {
    return oauthId;
  }

  public String getEmail() {
    return email;
  }

  public String getNickname() {
    return nickname;
  }

  public AvatarType getAvatarType() {
    return avatarType;
  }

  public long getTotalStudyMinutes() {
    return totalStudyMinutes;
  }

  public int getCurrentStreak() {
    return currentStreak;
  }

  public int getLongestStreak() {
    return longestStreak;
  }

  public LocalDate getLastStudyDate() {
    return lastStudyDate;
  }
}
