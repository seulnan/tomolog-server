package com.tomolog.presentation.user;

import com.tomolog.domain.user.AvatarType;
import com.tomolog.domain.user.User;

/** Current user's profile and study stats (SPEC §5: GET /api/users/me). */
public record UserProfileResponse(
    Long id,
    String email,
    String nickname,
    AvatarType avatarType,
    long totalStudyMinutes,
    int currentStreak,
    int longestStreak) {

  public static UserProfileResponse from(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getAvatarType(),
        user.getTotalStudyMinutes(),
        user.getCurrentStreak(),
        user.getLongestStreak());
  }
}
