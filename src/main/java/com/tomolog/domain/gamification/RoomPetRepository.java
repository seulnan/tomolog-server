package com.tomolog.domain.gamification;

import java.util.Optional;

/**
 * Domain port for room-pet persistence. {@code growPet} carries persistence semantics by design —
 * it is the atomic, concurrent-safe growth counter (SPEC §3).
 */
public interface RoomPetRepository {

  Optional<RoomPet> findByRoomId(Long roomId);

  RoomPet save(RoomPet pet);

  /** Atomically adds growth points and recomputes the level. Returns rows affected. */
  int growPet(Long roomId, int points);
}
