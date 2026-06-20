package com.tomolog.infrastructure.persistence.user;

import com.tomolog.domain.user.User;
import org.springframework.stereotype.Component;

/** Maps between the domain {@link User} and its JPA {@link UserEntity}. */
@Component
public class UserMapper {

  public User toDomain(UserEntity entity) {
    return new User(
        entity.getId(),
        entity.getOauthProvider(),
        entity.getOauthId(),
        entity.getEmail(),
        entity.getNickname(),
        entity.getAvatarType(),
        entity.getTotalStudyMinutes(),
        entity.getCurrentStreak(),
        entity.getLongestStreak(),
        entity.getLastStudyDate(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public UserEntity toNewEntity(User user) {
    return new UserEntity(
        user.getOauthProvider(),
        user.getOauthId(),
        user.getEmail(),
        user.getNickname(),
        user.getAvatarType(),
        user.getTotalStudyMinutes(),
        user.getCurrentStreak(),
        user.getLongestStreak(),
        user.getLastStudyDate());
  }
}
