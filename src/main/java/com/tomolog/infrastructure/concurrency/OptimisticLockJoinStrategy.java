package com.tomolog.infrastructure.concurrency;

import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;
import com.tomolog.domain.room.JoinStrategyType;
import com.tomolog.domain.room.MemberRole;
import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomFullException;
import com.tomolog.domain.room.RoomJoinStrategy;
import com.tomolog.domain.room.RoomMember;
import com.tomolog.domain.room.RoomMemberRepository;
import com.tomolog.domain.room.RoomRepository;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Join strategy backed by the room's {@code @Version} column. Each attempt runs in its own
 * transaction; on a version conflict it backs off and retries a fresh read, so capacity is honored
 * without holding a DB lock. Under heavy contention losers eventually read a full room and get
 * {@link RoomFullException} (SPEC §4).
 */
@Component
public class OptimisticLockJoinStrategy implements RoomJoinStrategy {

  private static final int MAX_RETRIES = 50;

  private final RoomRepository roomRepository;
  private final RoomMemberRepository roomMemberRepository;
  private final TransactionTemplate transactionTemplate;

  public OptimisticLockJoinStrategy(
      RoomRepository roomRepository,
      RoomMemberRepository roomMemberRepository,
      PlatformTransactionManager transactionManager) {
    this.roomRepository = roomRepository;
    this.roomMemberRepository = roomMemberRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public RoomMember join(Long roomId, Long userId) {
    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
      try {
        return transactionTemplate.execute(status -> doJoin(roomId, userId));
      } catch (OptimisticLockingFailureException conflict) {
        backoff();
      }
    }
    throw new ApiException(ErrorCode.INTERNAL_ERROR, "입장 처리가 경합으로 실패했습니다.");
  }

  private RoomMember doJoin(Long roomId, Long userId) {
    Room room =
        roomRepository
            .findById(roomId)
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

  private void backoff() {
    try {
      Thread.sleep(ThreadLocalRandom.current().nextInt(2, 10));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "입장 대기 중 인터럽트되었습니다.", e);
    }
  }

  @Override
  public JoinStrategyType type() {
    return JoinStrategyType.OPTIMISTIC;
  }
}
