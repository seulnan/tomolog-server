package com.tomolog.presentation.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a PRIVATE room (SPEC §5: POST /api/rooms). Capacity 2~50. */
public record CreateRoomRequest(
    @NotBlank @Size(max = 50) String name, @Min(2) @Max(50) int capacity) {}
