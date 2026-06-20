package com.tomolog.infrastructure.persistence.gamification;

import com.tomolog.domain.gamification.RoomPet;
import com.tomolog.domain.gamification.RoomPetRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** JPA adapter for the {@link RoomPetRepository} port. */
@Repository
public class RoomPetPersistenceAdapter implements RoomPetRepository {

  private final RoomPetJpaRepository jpaRepository;
  private final RoomPetMapper mapper;

  public RoomPetPersistenceAdapter(RoomPetJpaRepository jpaRepository, RoomPetMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<RoomPet> findByRoomId(Long roomId) {
    return jpaRepository.findByRoomId(roomId).map(mapper::toDomain);
  }

  @Override
  public RoomPet save(RoomPet pet) {
    if (pet.getId() == null) {
      return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toNewEntity(pet)));
    }
    RoomPetEntity entity =
        jpaRepository
            .findById(pet.getId())
            .orElseThrow(() -> new IllegalStateException("RoomPet not found: " + pet.getId()));
    entity.apply(pet.getGrowthPoints(), pet.getLevel());
    return mapper.toDomain(jpaRepository.saveAndFlush(entity));
  }

  @Override
  public int growPet(Long roomId, int points) {
    return jpaRepository.growPet(roomId, points);
  }
}
