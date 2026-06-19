package com.tomolog.domain.log;

import java.time.LocalDateTime;

/**
 * A "셋로그" study snapshot — a pure domain model. A member drops an emoji, a short memo and how many
 * minutes they studied at the end of a focus cycle (SPEC §3). Created once, never mutated.
 */
public class TomologEntry {

  private final Long id;
  private final Long roomId;
  private final Long userId;
  private final int cycleNumber;
  private final String emoji;
  private final String memo;
  private final int studiedMinutes;
  private final LocalDateTime createdAt;

  /** Creates a new snapshot (no id/timestamp yet). */
  public TomologEntry(
      Long roomId, Long userId, int cycleNumber, String emoji, String memo, int studiedMinutes) {
    this(null, roomId, userId, cycleNumber, emoji, memo, studiedMinutes, null);
  }

  /** Full constructor used by the persistence mapper. */
  public TomologEntry(
      Long id,
      Long roomId,
      Long userId,
      int cycleNumber,
      String emoji,
      String memo,
      int studiedMinutes,
      LocalDateTime createdAt) {
    this.id = id;
    this.roomId = roomId;
    this.userId = userId;
    this.cycleNumber = cycleNumber;
    this.emoji = emoji;
    this.memo = memo;
    this.studiedMinutes = studiedMinutes;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getRoomId() {
    return roomId;
  }

  public Long getUserId() {
    return userId;
  }

  public int getCycleNumber() {
    return cycleNumber;
  }

  public String getEmoji() {
    return emoji;
  }

  public String getMemo() {
    return memo;
  }

  public int getStudiedMinutes() {
    return studiedMinutes;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
