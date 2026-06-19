package com.tomolog.domain.room;

import java.time.LocalDateTime;

/**
 * Membership of a user in a room — a pure domain model. A user joins once (unique room+user at the
 * persistence layer) and their presence can change (SPEC §3).
 */
public class RoomMember {

  private final Long id;
  private final Long roomId;
  private final Long userId;
  private final MemberRole role;
  private Presence presence;
  private final LocalDateTime joinedAt;

  /** Creates a new membership (no id yet), starting ONLINE. */
  public RoomMember(Long roomId, Long userId, MemberRole role, LocalDateTime joinedAt) {
    this(null, roomId, userId, role, Presence.ONLINE, joinedAt);
  }

  /** Full constructor used by the persistence mapper. */
  public RoomMember(
      Long id,
      Long roomId,
      Long userId,
      MemberRole role,
      Presence presence,
      LocalDateTime joinedAt) {
    this.id = id;
    this.roomId = roomId;
    this.userId = userId;
    this.role = role;
    this.presence = presence;
    this.joinedAt = joinedAt;
  }

  public void updatePresence(Presence presence) {
    this.presence = presence;
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
