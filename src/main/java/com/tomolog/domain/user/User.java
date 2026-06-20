package com.tomolog.domain.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A registered user — a pure domain model with no persistence framework coupling. Identity comes
 * from an OAuth provider; study stats (minutes, streaks) advance as snapshots are submitted (SPEC
 * §3). The mapper reconstitutes this from the JPA entity and back.
 */
public class User {

  private final Long id;
  private final OauthProvider oauthProvider;
  private final String oauthId;
  private String email;
  private String nickname;
  private AvatarType avatarType;
  private long totalStudyMinutes;
  private int currentStreak;
  private int longestStreak;
  private LocalDate lastStudyDate;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  /** Creates a brand-new user (no id/timestamps yet — assigned on persist). */
  public User(
      OauthProvider oauthProvider,
      String oauthId,
      String email,
      String nickname,
      AvatarType avatarType) {
    this(null, oauthProvider, oauthId, email, nickname, avatarType, 0L, 0, 0, null, null, null);
  }

  /** Full constructor used by the persistence mapper to reconstitute a stored user. */
  public User(
      Long id,
      OauthProvider oauthProvider,
      String oauthId,
      String email,
      String nickname,
      AvatarType avatarType,
      long totalStudyMinutes,
      int currentStreak,
      int longestStreak,
      LocalDate lastStudyDate,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.oauthProvider = oauthProvider;
    this.oauthId = oauthId;
    this.email = email;
    this.nickname = nickname;
    this.avatarType = avatarType;
    this.totalStudyMinutes = totalStudyMinutes;
    this.currentStreak = currentStreak;
    this.longestStreak = longestStreak;
    this.lastStudyDate = lastStudyDate;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
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

  public Long getId() {
    return id;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
