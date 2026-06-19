package com.tomolog.infrastructure.persistence.room;

import com.tomolog.domain.common.BaseTimeEntity;
import com.tomolog.domain.room.MemberRole;
import com.tomolog.domain.room.Presence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** JPA persistence model for a room membership (maps to {@code room_members}). */
@Entity
@Table(
    name = "room_members",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_room_members_room_user",
            columnNames = {"room_id", "user_id"}))
public class RoomMemberEntity extends BaseTimeEntity {

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Presence presence;

  @Column(name = "joined_at", nullable = false)
  private LocalDateTime joinedAt;

  protected RoomMemberEntity() {}

  public RoomMemberEntity(
      Long roomId, Long userId, MemberRole role, Presence presence, LocalDateTime joinedAt) {
    this.roomId = roomId;
    this.userId = userId;
    this.role = role;
    this.presence = presence;
    this.joinedAt = joinedAt;
  }

  public void updatePresence(Presence presence) {
    this.presence = presence;
  }

  public Long getRoomId() {
    return roomId;
  }

  public Long getUserId() {
    return userId;
  }

  public MemberRole getRole() {
    return role;
  }

  public Presence getPresence() {
    return presence;
  }

  public LocalDateTime getJoinedAt() {
    return joinedAt;
  }
}
