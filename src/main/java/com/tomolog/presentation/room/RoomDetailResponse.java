package com.tomolog.presentation.room;

import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomMember;
import java.util.List;

/** Room detail including its members (SPEC §5: GET /api/rooms/{id}). */
public record RoomDetailResponse(RoomResponse room, List<RoomMemberResponse> members) {

  public static RoomDetailResponse of(Room room, List<RoomMember> members) {
    return new RoomDetailResponse(
        RoomResponse.from(room), members.stream().map(RoomMemberResponse::from).toList());
  }
}
