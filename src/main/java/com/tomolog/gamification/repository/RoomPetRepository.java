package com.tomolog.gamification.repository;

import com.tomolog.gamification.domain.RoomPet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomPetRepository extends JpaRepository<RoomPet, Long> {

  Optional<RoomPet> findByRoomId(Long roomId);

  /**
   * Atomically adds growth points and recomputes the level in one statement, so concurrent
   * snapshots from different members are all counted correctly (SPEC §3). Returns rows affected.
   */
  @Modifying(flushAutomatically = true)
  @Query(
      "update RoomPet p set p.growthPoints = p.growthPoints + :points, "
          + "p.level = (p.growthPoints + :points) / 100 + 1 where p.roomId = :roomId")
  int growPet(@Param("roomId") Long roomId, @Param("points") int points);
}
