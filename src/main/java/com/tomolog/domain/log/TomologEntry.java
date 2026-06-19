package com.tomolog.domain.log;

import com.tomolog.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A "셋로그" study snapshot a member drops at the end of a focus cycle: an emoji, a short memo and how
 * many minutes they studied (SPEC §3).
 */
@Entity
@Table(name = "tomolog_entries")
public class TomologEntry extends BaseTimeEntity {

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "cycle_number", nullable = false)
  private int cycleNumber;

  @Column(nullable = false, length = 8)
  private String emoji;

  @Column(length = 100)
  private String memo;

  @Column(name = "studied_minutes", nullable = false)
  private int studiedMinutes;

  protected TomologEntry() {}

  public TomologEntry(
      Long roomId, Long userId, int cycleNumber, String emoji, String memo, int studiedMinutes) {
    this.roomId = roomId;
    this.userId = userId;
    this.cycleNumber = cycleNumber;
    this.emoji = emoji;
    this.memo = memo;
    this.studiedMinutes = studiedMinutes;
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
}
