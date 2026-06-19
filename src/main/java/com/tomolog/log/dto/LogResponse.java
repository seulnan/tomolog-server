package com.tomolog.log.dto;

import com.tomolog.log.domain.TomologEntry;
import java.time.LocalDateTime;

/** A study snapshot in the feed / NEW_LOG event (SPEC §5, §6). */
public record LogResponse(
    Long id,
    Long roomId,
    Long userId,
    int cycleNumber,
    String emoji,
    String memo,
    int studiedMinutes,
    LocalDateTime createdAt) {

  public static LogResponse from(TomologEntry entry) {
    return new LogResponse(
        entry.getId(),
        entry.getRoomId(),
        entry.getUserId(),
        entry.getCycleNumber(),
        entry.getEmoji(),
        entry.getMemo(),
        entry.getStudiedMinutes(),
        entry.getCreatedAt());
  }
}
