package com.tomolog.presentation.room;

import com.tomolog.domain.room.MemberRole;
import com.tomolog.domain.room.Presence;
import com.tomolog.domain.room.RoomMember;
import java.time.LocalDateTime;

/** A member entry within a room (SPEC §5). */
public record RoomMemberResponse(
    Long userId, MemberRole role, Presence presence, LocalDateTime joinedAt) {

  public static RoomMemberResponse from(RoomMember member) {
    return new RoomMemberResponse(
        member.getUserId(), member.getRole(), member.getPresence(), member.getJoinedAt());
  }
}
