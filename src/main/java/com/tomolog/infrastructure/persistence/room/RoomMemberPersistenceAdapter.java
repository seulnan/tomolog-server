package com.tomolog.infrastructure.persistence.room;

import com.tomolog.domain.room.RoomMember;
import com.tomolog.domain.room.RoomMemberRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** JPA adapter for the {@link RoomMemberRepository} port. */
@Repository
public class RoomMemberPersistenceAdapter implements RoomMemberRepository {

  private final RoomMemberJpaRepository jpaRepository;
  private final RoomMemberMapper mapper;

  public RoomMemberPersistenceAdapter(
      RoomMemberJpaRepository jpaRepository, RoomMemberMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId) {
    return jpaRepository.findByRoomIdAndUserId(roomId, userId).map(mapper::toDomain);
  }

  @Override
  public List<RoomMember> findByRoomId(Long roomId) {
    return jpaRepository.findByRoomId(roomId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public long countByRoomId(Long roomId) {
    return jpaRepository.countByRoomId(roomId);
  }

  @Override
  public boolean existsByRoomIdAndUserId(Long roomId, Long userId) {
    return jpaRepository.existsByRoomIdAndUserId(roomId, userId);
  }

  @Override
  public RoomMember save(RoomMember member) {
    if (member.getId() == null) {
      return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toNewEntity(member)));
    }
    RoomMemberEntity entity =
        jpaRepository
            .findById(member.getId())
            .orElseThrow(
                () -> new IllegalStateException("RoomMember not found: " + member.getId()));
    entity.updatePresence(member.getPresence());
    return mapper.toDomain(jpaRepository.saveAndFlush(entity));
  }

  @Override
  public void delete(RoomMember member) {
    jpaRepository.deleteById(member.getId());
    jpaRepository.flush();
  }
}
