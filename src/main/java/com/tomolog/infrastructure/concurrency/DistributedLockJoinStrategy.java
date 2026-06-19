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
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Join strategy backed by a Redisson distributed lock keyed {@code lock:room:{roomId}}. The lock
 * serializes joins even across multiple application instances; the check-and-insert runs in a
 * committed transaction before the lock is released, so the next holder reads the updated counter
 * (SPEC §4).
 */
@Component
public class DistributedLockJoinStrategy implements RoomJoinStrategy {

  private static final long WAIT_SECONDS = 5;
  private static final long LEASE_SECONDS = 3;

  private final RedissonClient redissonClient;
  private final RoomRepository roomRepository;
  private final RoomMemberRepository roomMemberRepository;
  private final TransactionTemplate transactionTemplate;

  public DistributedLockJoinStrategy(
      RedissonClient redissonClient,
      RoomRepository roomRepository,
      RoomMemberRepository roomMemberRepository,
      PlatformTransactionManager transactionManager) {
    this.redissonClient = redissonClient;
    this.roomRepository = roomRepository;
    this.roomMemberRepository = roomMemberRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public RoomMember join(Long roomId, Long userId) {
    RLock lock = redissonClient.getLock("lock:room:" + roomId);
    boolean acquired = false;
    try {
      acquired = lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS);
      if (!acquired) {
        throw new ApiException(ErrorCode.INTERNAL_ERROR, "입장 락 획득에 실패했습니다.");
      }
      return transactionTemplate.execute(status -> doJoin(roomId, userId));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "입장 락 대기 중 인터럽트되었습니다.", e);
    } finally {
      if (acquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
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

  @Override
  public JoinStrategyType type() {
    return JoinStrategyType.DISTRIBUTED;
  }
}
