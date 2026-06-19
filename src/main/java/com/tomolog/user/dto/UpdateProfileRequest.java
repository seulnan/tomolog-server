package com.tomolog.user.dto;

import com.tomolog.user.domain.AvatarType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Editable profile fields (SPEC §5: PATCH /api/users/me). */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 30) String nickname, @NotNull AvatarType avatarType) {}
