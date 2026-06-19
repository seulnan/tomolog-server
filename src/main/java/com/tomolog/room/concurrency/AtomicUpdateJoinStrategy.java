package com.tomolog.room.concurrency;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.room.domain.MemberRole;
import com.tomolog.room.domain.RoomMember;
import com.tomolog.room.repository.RoomMemberRepository;
import com.tomolog.room.repository.RoomRepository;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * High-throughput join for large THEMED rooms. A single conditional UPDATE ({@code ... SET count =
 * count + 1 WHERE count < capacity}) is the capacity gate, so the critical section is one short
 * statement rather than a read-check-insert round trip. The counter increment and the member insert
 * live in the same transaction: if the user is already a member the unique constraint fails and the
 * rollback undoes the increment too — no counter drift, no compensation (SPEC §4, LEDGER-008).
 */
@Component
public class AtomicUpdateJoinStrategy implements RoomJoinStrategy {

  private final RoomRepository roomRepository;
  private final RoomMemberRepository roomMemberRepository;

  public AtomicUpdateJoinStrategy(
      RoomRepository roomRepository, RoomMemberRepository roomMemberRepository) {
    this.roomRepository = roomRepository;
    this.roomMemberRepository = roomMemberRepository;
  }

  @Override
  @Transactional
  public RoomMember join(Long roomId, Long userId) {
    int reserved = roomRepository.increaseMemberCountIfNotFull(roomId);
    if (reserved == 0) {
      if (!roomRepository.existsById(roomId)) {
        throw new ApiException(ErrorCode.ROOM_NOT_FOUND);
      }
      throw new RoomFullException();
    }
    try {
      return roomMemberRepository.saveAndFlush(
          new RoomMember(roomId, userId, MemberRole.MEMBER, LocalDateTime.now()));
    } catch (DataIntegrityViolationException duplicate) {
      // Already a member: the unique (room_id, user_id) constraint tripped. The transaction is now
      // rollback-only, so the reserved slot is released automatically — just translate the error.
      throw new ApiException(
          ErrorCode.ALREADY_JOINED, ErrorCode.ALREADY_JOINED.defaultMessage(), duplicate);
    }
  }

  @Override
  public JoinStrategyType type() {
    return JoinStrategyType.ATOMIC;
  }
}
