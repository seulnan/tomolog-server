package com.tomolog.room.dto;

import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomStatus;
import com.tomolog.room.domain.RoomType;

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
