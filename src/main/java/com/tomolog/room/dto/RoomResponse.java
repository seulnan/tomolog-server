package com.tomolog.room.dto;

import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomStatus;

/** Summary view of a room (SPEC §5). */
public record RoomResponse(
    Long id,
    String name,
    Long hostUserId,
    int capacity,
    RoomStatus status,
    String inviteCode,
    int currentMemberCount) {

  public static RoomResponse from(Room room) {
    return new RoomResponse(
        room.getId(),
        room.getName(),
        room.getHostUserId(),
        room.getCapacity(),
        room.getStatus(),
        room.getInviteCode(),
        room.getCurrentMemberCount());
  }
}
