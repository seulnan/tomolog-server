package com.tomolog.infrastructure.persistence.room;

import com.tomolog.domain.room.RoomMember;
import org.springframework.stereotype.Component;

/** Maps between the domain {@link RoomMember} and its JPA {@link RoomMemberEntity}. */
@Component
public class RoomMemberMapper {

  public RoomMember toDomain(RoomMemberEntity entity) {
    return new RoomMember(
        entity.getId(),
        entity.getRoomId(),
        entity.getUserId(),
        entity.getRole(),
        entity.getPresence(),
        entity.getJoinedAt());
  }

  public RoomMemberEntity toNewEntity(RoomMember member) {
    return new RoomMemberEntity(
        member.getRoomId(),
        member.getUserId(),
        member.getRole(),
        member.getPresence(),
        member.getJoinedAt());
  }
}
