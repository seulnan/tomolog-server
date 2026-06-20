package com.tomolog.infrastructure.persistence.room;

import com.tomolog.domain.room.Room;
import org.springframework.stereotype.Component;

/** Maps between the domain {@link Room} and its JPA {@link RoomEntity}. */
@Component
public class RoomMapper {

  public Room toDomain(RoomEntity entity) {
    return new Room(
        entity.getId(),
        entity.getName(),
        entity.getHostUserId(),
        entity.getCapacity(),
        entity.getType(),
        entity.getStatus(),
        entity.getInviteCode(),
        entity.getCurrentMemberCount(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public RoomEntity toNewEntity(Room room) {
    return new RoomEntity(
        room.getName(),
        room.getHostUserId(),
        room.getCapacity(),
        room.getType(),
        room.getInviteCode());
  }
}
