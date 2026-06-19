package com.tomolog.room.dto;

import com.tomolog.room.domain.MemberRole;
import com.tomolog.room.domain.Presence;
import com.tomolog.room.domain.RoomMember;
import java.time.LocalDateTime;

/** A member entry within a room (SPEC §5). */
public record RoomMemberResponse(
    Long userId, MemberRole role, Presence presence, LocalDateTime joinedAt) {

  public static RoomMemberResponse from(RoomMember member) {
    return new RoomMemberResponse(
        member.getUserId(), member.getRole(), member.getPresence(), member.getJoinedAt());
  }
}
