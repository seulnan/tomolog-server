package com.tomolog.presentation.user;

import com.tomolog.application.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Current-user profile endpoints (SPEC §5). */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** Returns the authenticated user's profile and study stats. */
  @GetMapping("/me")
  public UserProfileResponse getMe(@AuthenticationPrincipal Long userId) {
    return UserProfileResponse.from(userService.getById(userId));
  }

  /** Updates the authenticated user's nickname and avatar. */
  @PatchMapping("/me")
  public UserProfileResponse updateMe(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody UpdateProfileRequest request) {
    return UserProfileResponse.from(
        userService.updateProfile(userId, request.nickname(), request.avatarType()));
  }
}
