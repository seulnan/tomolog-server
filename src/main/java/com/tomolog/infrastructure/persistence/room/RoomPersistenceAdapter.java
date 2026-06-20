package com.tomolog.infrastructure.persistence.room;

import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomRepository;
import com.tomolog.domain.room.RoomStatus;
import com.tomolog.domain.room.RoomType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter for the {@link RoomRepository} port. The lock/atomic operations delegate straight to
 * the JPA queries (exact same SQL/lock/version semantics). On update, {@code save} re-fetches the
 * managed entity in the current transaction — so a row loaded via {@code findByIdForUpdate} stays
 * locked and the {@code @Version} optimistic check still fires on flush.
 */
@Repository
public class RoomPersistenceAdapter implements RoomRepository {

  private final RoomJpaRepository jpaRepository;
  private final RoomMapper mapper;

  public RoomPersistenceAdapter(RoomJpaRepository jpaRepository, RoomMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Room> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public boolean existsById(Long id) {
    return jpaRepository.existsById(id);
  }

  @Override
  public Page<Room> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable).map(mapper::toDomain);
  }

  @Override
  public Optional<Room> findByIdForUpdate(Long id) {
    return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Room> findByInviteCode(String inviteCode) {
    return jpaRepository.findByInviteCode(inviteCode).map(mapper::toDomain);
  }

  @Override
  public Page<Room> findByStatus(RoomStatus status, Pageable pageable) {
    return jpaRepository.findByStatus(status, pageable).map(mapper::toDomain);
  }

  @Override
  public boolean existsByInviteCode(String inviteCode) {
    return jpaRepository.existsByInviteCode(inviteCode);
  }

  @Override
  public Optional<RoomType> findTypeById(Long id) {
    return jpaRepository.findTypeById(id);
  }

  @Override
  public Room save(Room room) {
    if (room.getId() == null) {
      return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toNewEntity(room)));
    }
    RoomEntity entity =
        jpaRepository
            .findById(room.getId())
            .orElseThrow(() -> new IllegalStateException("Room not found: " + room.getId()));
    entity.apply(room.getStatus(), room.getCurrentMemberCount());
    return mapper.toDomain(jpaRepository.saveAndFlush(entity));
  }

  @Override
  public int increaseMemberCountIfNotFull(Long id) {
    return jpaRepository.increaseMemberCountIfNotFull(id);
  }

  @Override
  public int decreaseMemberCount(Long id) {
    return jpaRepository.decreaseMemberCount(id);
  }
}
