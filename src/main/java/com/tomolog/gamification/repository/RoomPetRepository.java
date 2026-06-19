package com.tomolog.gamification.repository;

import com.tomolog.gamification.domain.RoomPet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPetRepository extends JpaRepository<RoomPet, Long> {

  Optional<RoomPet> findByRoomId(Long roomId);
}
