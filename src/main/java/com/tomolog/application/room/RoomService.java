package com.tomolog.application.room;

import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;
import com.tomolog.domain.gamification.RoomPet;
import com.tomolog.domain.gamification.RoomPetRepository;
import com.tomolog.domain.room.JoinStrategyType;
import com.tomolog.domain.room.MemberRole;
import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomJoinStrategy;
import com.tomolog.domain.room.RoomMember;
import com.tomolog.domain.room.RoomMemberRepository;
import com.tomolog.domain.room.RoomRepository;
import com.tomolog.domain.room.RoomStatus;
import com.tomolog.domain.room.RoomType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Room lifecycle and membership. The concurrency-guarded join is delegated to a strategy, which
 * owns its own transaction — so {@code join} here is deliberately non-transactional, and the read
 * methods are individually marked read-only.
 */
@Service
public class RoomService {

  private static final int MAX_INVITE_CODE_ATTEMPTS = 5;

  private final RoomRepository roomRepository;
  private final RoomMemberRepository roomMemberRepository;
  private final RoomPetRepository roomPetRepository;
  private final JoinStrategyResolver joinStrategyResolver;
  private final ApplicationEventPublisher eventPublisher;

  public RoomService(
      RoomRepository roomRepository,
      RoomMemberRepository roomMemberRepository,
      RoomPetRepository roomPetRepository,
      JoinStrategyResolver joinStrategyResolver,
      ApplicationEventPublisher eventPublisher) {
    this.roomRepository = roomRepository;
    this.roomMemberRepository = roomMemberRepository;
    this.roomPetRepository = roomPetRepository;
    this.joinStrategyResolver = joinStrategyResolver;
    this.eventPublisher = eventPublisher;
  }

  /** Creates a PRIVATE room and joins the host as its first member. */
  @Transactional
  public Room createRoom(Long hostUserId, String name, int capacity) {
    if (capacity < 2 || capacity > Room.PRIVATE_MAX_CAPACITY) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "정원은 2~" + Room.PRIVATE_MAX_CAPACITY + " 사이여야 합니다.");
    }
    Room room = roomRepository.save(new Room(name, hostUserId, capacity, generateInviteCode()));
    room.increaseMemberCount();
    roomMemberRepository.save(
        new RoomMember(room.getId(), hostUserId, MemberRole.HOST, LocalDateTime.now()));
    roomPetRepository.save(new RoomPet(room.getId()));
    return room;
  }

  /** Lists rooms, optionally filtered by status, newest first. */
  @Transactional(readOnly = true)
  public Page<Room> listRooms(RoomStatus status, Pageable pageable) {
    return status == null
        ? roomRepository.findAll(pageable)
        : roomRepository.findByStatus(status, pageable);
  }

  /** Returns the room or throws {@link ErrorCode#ROOM_NOT_FOUND}. */
  @Transactional(readOnly = true)
  public Room getRoom(Long roomId) {
    return roomRepository
        .findById(roomId)
        .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
  }

  /** Returns the room's current members. */
  @Transactional(readOnly = true)
  public List<RoomMember> getMembers(Long roomId) {
    return roomMemberRepository.findByRoomId(roomId);
  }

  /**
   * Joins a user, routing by room type: large THEMED rooms use the high-throughput atomic strategy,
   * PRIVATE rooms use the configured strategy. Not transactional here — the selected strategy owns
   * its own transaction (optimistic retries each need a fresh one).
   */
  public RoomMember join(Long roomId, Long userId) {
    RoomType type =
        roomRepository
            .findTypeById(roomId)
            .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
    RoomJoinStrategy strategy =
        type == RoomType.THEMED
            ? joinStrategyResolver.strategyFor(JoinStrategyType.ATOMIC)
            : joinStrategyResolver.active();
    RoomMember member = strategy.join(roomId, userId);
    // Published after the strategy's transaction has committed; the realtime layer broadcasts it.
    eventPublisher.publishEvent(new RoomMembershipChangedEvent(roomId, userId, true));
    return member;
  }

  /**
   * Removes the user's membership and decrements the counter. THEMED rooms use an atomic counter
   * update (no row lock); PRIVATE rooms decrement under a row lock, consistent with their join
   * path.
   */
  @Transactional
  public void leave(Long roomId, Long userId) {
    RoomMember member =
        roomMemberRepository
            .findByRoomIdAndUserId(roomId, userId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_ROOM_MEMBER));
    RoomType type =
        roomRepository
            .findTypeById(roomId)
            .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
    if (type == RoomType.THEMED) {
      // Flush the membership delete before the bulk counter update: that query clears the
      // persistence context, which would otherwise discard the still-pending delete.
      roomMemberRepository.delete(member);
      roomMemberRepository.flush();
      roomRepository.decreaseMemberCount(roomId);
    } else {
      roomMemberRepository.delete(member);
      Room room =
          roomRepository
              .findByIdForUpdate(roomId)
              .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
      room.decreaseMemberCount();
    }
    eventPublisher.publishEvent(new RoomMembershipChangedEvent(roomId, userId, false));
  }

  private String generateInviteCode() {
    for (int attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
      String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
      if (!roomRepository.existsByInviteCode(code)) {
        return code;
      }
    }
    throw new ApiException(ErrorCode.INTERNAL_ERROR, "초대 코드 생성에 실패했습니다.");
  }
}
