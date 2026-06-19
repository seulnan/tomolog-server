package com.tomolog.infrastructure.persistence.room;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link RoomMemberEntity}; wrapped by the persistence adapter. */
public interface RoomMemberJpaRepository extends JpaRepository<RoomMemberEntity, Long> {

  Optional<RoomMemberEntity> findByRoomIdAndUserId(Long roomId, Long userId);

  List<RoomMemberEntity> findByRoomId(Long roomId);

  long countByRoomId(Long roomId);

  boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
