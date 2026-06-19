package com.tomolog.domain.room;

import java.util.List;
import java.util.Optional;

/** Domain port for room-membership persistence (plain interface, domain models). */
public interface RoomMemberRepository {

  Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

  List<RoomMember> findByRoomId(Long roomId);

  long countByRoomId(Long roomId);

  boolean existsByRoomIdAndUserId(Long roomId, Long userId);

  RoomMember save(RoomMember member);

  void delete(RoomMember member);
}
