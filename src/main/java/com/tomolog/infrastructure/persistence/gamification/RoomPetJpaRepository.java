package com.tomolog.infrastructure.persistence.gamification;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository over {@link RoomPetEntity}; wrapped by the persistence adapter. */
public interface RoomPetJpaRepository extends JpaRepository<RoomPetEntity, Long> {

  Optional<RoomPetEntity> findByRoomId(Long roomId);

  @Modifying(flushAutomatically = true)
  @Query(
      "update RoomPetEntity p set p.growthPoints = p.growthPoints + :points, "
          + "p.level = (p.growthPoints + :points) / 100 + 1 where p.roomId = :roomId")
  int growPet(@Param("roomId") Long roomId, @Param("points") int points);
}
