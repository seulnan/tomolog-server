package com.tomolog.presentation.log;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to submit a study snapshot (SPEC §5: POST /api/rooms/{id}/logs). */
public record SubmitLogRequest(
    @Min(1) int cycleNumber,
    @NotBlank @Size(max = 8) String emoji,
    @Size(max = 100) String memo,
    @Min(1) @Max(600) int studiedMinutes) {}
