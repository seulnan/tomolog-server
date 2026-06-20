package com.tomolog.infrastructure.persistence.room;

import com.tomolog.domain.common.BaseTimeEntity;
import com.tomolog.domain.room.RoomStatus;
import com.tomolog.domain.room.RoomType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA persistence model for a room (maps to {@code rooms}). {@code version} backs optimistic lock.
 */
@Entity
@Table(name = "rooms")
public class RoomEntity extends BaseTimeEntity {

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "host_user_id")
  private Long hostUserId;

  @Column(nullable = false)
  private int capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoomType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoomStatus status;

  @Column(name = "invite_code", nullable = false, unique = true, length = 12)
  private String inviteCode;

  @Column(name = "current_member_count", nullable = false)
  private int currentMemberCount;

  @Version
  @Column(nullable = false)
  private Long version;

  protected RoomEntity() {}

  public RoomEntity(String name, Long hostUserId, int capacity, RoomType type, String inviteCode) {
    this.name = name;
    this.hostUserId = hostUserId;
    this.capacity = capacity;
    this.type = type;
    this.inviteCode = inviteCode;
    this.status = RoomStatus.WAITING;
    this.currentMemberCount = 0;
  }

  /** Applies a domain room's mutable fields onto this managed entity (for updates). */
  public void apply(RoomStatus status, int currentMemberCount) {
    this.status = status;
    this.currentMemberCount = currentMemberCount;
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
}
