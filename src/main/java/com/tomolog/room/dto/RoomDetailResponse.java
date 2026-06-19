package com.tomolog.room.dto;

import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomMember;
import java.util.List;

/** Room detail including its members (SPEC §5: GET /api/rooms/{id}). */
public record RoomDetailResponse(RoomResponse room, List<RoomMemberResponse> members) {

  public static RoomDetailResponse of(Room room, List<RoomMember> members) {
    return new RoomDetailResponse(
        RoomResponse.from(room), members.stream().map(RoomMemberResponse::from).toList());
  }
}
