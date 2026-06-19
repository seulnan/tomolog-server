package com.tomolog.room.domain;

import com.tomolog.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A study room. {@code currentMemberCount} is a denormalized counter the join strategies guard, and
 * {@code version} backs the optimistic-lock strategy (SPEC §3, §4).
 */
@Entity
@Table(name = "rooms")
public class Room extends BaseTimeEntity {

  public static final int MAX_CAPACITY = 6;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "host_user_id", nullable = false)
  private Long hostUserId;

  @Column(nullable = false)
  private int capacity;

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

  protected Room() {}

  public Room(String name, Long hostUserId, int capacity, String inviteCode) {
    this.name = name;
    this.hostUserId = hostUserId;
    this.capacity = capacity;
    this.inviteCode = inviteCode;
    this.status = RoomStatus.WAITING;
    this.currentMemberCount = 0;
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

  public String getName() {
    return name;
  }

  public Long getHostUserId() {
    return hostUserId;
  }

  public int getCapacity() {
    return capacity;
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
