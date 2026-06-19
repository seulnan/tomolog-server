package com.tomolog.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a room (SPEC §5: POST /api/rooms). */
public record CreateRoomRequest(
    @NotBlank @Size(max = 50) String name, @Min(2) @Max(6) int capacity) {}
