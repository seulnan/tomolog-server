package com.tomolog.room.concurrency;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.room.domain.MemberRole;
import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomMember;
import com.tomolog.room.repository.RoomMemberRepository;
import com.tomolog.room.repository.RoomRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Join strategy backed by a row-level write lock: it selects the room row {@code FOR UPDATE}, which
 * serializes concurrent joins on the same room, then checks the counter and inserts the member
 * inside the one transaction. The counter on the locked row is the source of truth, so there is no
 * phantom-read window (SPEC §4).
 */
@Component
public class PessimisticLockJoinStrategy implements RoomJoinStrategy {

  private final RoomRepository roomRepository;
  private final RoomMemberRepository roomMemberRepository;

  public PessimisticLockJoinStrategy(
      RoomRepository roomRepository, RoomMemberRepository roomMemberRepository) {
    this.roomRepository = roomRepository;
    this.roomMemberRepository = roomMemberRepository;
  }

  @Override
  @Transactional
  public RoomMember join(Long roomId, Long userId) {
    Room room =
        roomRepository
            .findByIdForUpdate(roomId)
            .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
    if (roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
      throw new ApiException(ErrorCode.ALREADY_JOINED);
    }
    if (room.isFull()) {
      throw new RoomFullException();
    }
    room.increaseMemberCount();
    return roomMemberRepository.save(
        new RoomMember(roomId, userId, MemberRole.MEMBER, LocalDateTime.now()));
  }

  @Override
  public JoinStrategyType type() {
    return JoinStrategyType.PESSIMISTIC;
  }
}
