package com.tomolog.presentation.room;

import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomStatus;
import com.tomolog.domain.room.RoomType;

/** Summary view of a room (SPEC §5). {@code hostUserId} is null for THEMED rooms. */
public record RoomResponse(
    Long id,
    String name,
    Long hostUserId,
    RoomType type,
    int capacity,
    RoomStatus status,
    String inviteCode,
    int currentMemberCount) {

  public static RoomResponse from(Room room) {
    return new RoomResponse(
        room.getId(),
        room.getName(),
        room.getHostUserId(),
        room.getType(),
        room.getCapacity(),
        room.getStatus(),
        room.getInviteCode(),
        room.getCurrentMemberCount());
  }
}
