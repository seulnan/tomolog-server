package com.tomolog.infrastructure.persistence.gamification;

import com.tomolog.domain.gamification.RoomPet;
import org.springframework.stereotype.Component;

/** Maps between the domain {@link RoomPet} and its JPA {@link RoomPetEntity}. */
@Component
public class RoomPetMapper {

  public RoomPet toDomain(RoomPetEntity entity) {
    return new RoomPet(
        entity.getId(), entity.getRoomId(), entity.getGrowthPoints(), entity.getLevel());
  }

  public RoomPetEntity toNewEntity(RoomPet pet) {
    return new RoomPetEntity(pet.getRoomId(), pet.getGrowthPoints(), pet.getLevel());
  }
}
