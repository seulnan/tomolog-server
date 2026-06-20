package com.tomolog.domain.room;

import java.time.LocalDateTime;

/**
 * A study room — a pure domain model. {@code currentMemberCount} is the denormalized counter the
 * join strategies guard; {@code version} mirrors the optimistic-lock version (the actual check
 * happens on the JPA entity in the adapter). PRIVATE rooms are host-created (≤50); THEMED rooms are
 * fixed, anonymous and large (≤2000) (SPEC §3, §4, LEDGER-008).
 */
public class Room {

  public static final int PRIVATE_MAX_CAPACITY = 50;
  public static final int THEMED_MAX_CAPACITY = 2000;

  private final Long id;
  private String name;
  private final Long hostUserId;
  private final int capacity;
  private final RoomType type;
  private RoomStatus status;
  private final String inviteCode;
  private int currentMemberCount;
  private final Long version;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  /** Creates a new PRIVATE room. */
  public Room(String name, Long hostUserId, int capacity, String inviteCode) {
    this(name, hostUserId, capacity, inviteCode, RoomType.PRIVATE);
  }

  /** Creates a new room of the given type. */
  public Room(String name, Long hostUserId, int capacity, String inviteCode, RoomType type) {
    this(
        null,
        name,
        hostUserId,
        capacity,
        type,
        RoomStatus.WAITING,
        inviteCode,
        0,
        null,
        null,
        null);
  }

  /** Full constructor used by the persistence mapper. */
  public Room(
      Long id,
      String name,
      Long hostUserId,
      int capacity,
      RoomType type,
      RoomStatus status,
      String inviteCode,
      int currentMemberCount,
      Long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.hostUserId = hostUserId;
    this.capacity = capacity;
    this.type = type;
    this.status = status;
    this.inviteCode = inviteCode;
    this.currentMemberCount = currentMemberCount;
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public boolean isFull() {
    return currentMemberCount >= capacity;
  }

  public void increaseMemberCount() {
    this.currentMemberCount++;
  }

  public void decreaseMemberCount() {
    if (currentMemberCount > 0) {
      this.currentMemberCount--;
    }
  }

  public void changeStatus(RoomStatus status) {
    this.status = status;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Long getHostUserId() {
    return hostUserId;
  }

  public int getCapacity() {
    return capacity;
  }

  public RoomType getType() {
    return type;
  }

  public RoomStatus getStatus() {
    return status;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public int getCurrentMemberCount() {
    return currentMemberCount;
  }

  public Long getVersion() {
    return version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
