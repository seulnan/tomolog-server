package com.tomolog.room.service;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.room.concurrency.JoinStrategyResolver;
import com.tomolog.room.domain.MemberRole;
import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomMember;
import com.tomolog.room.domain.RoomStatus;
import com.tomolog.room.repository.RoomMemberRepository;
import com.tomolog.room.repository.RoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
  private final JoinStrategyResolver joinStrategyResolver;

  public RoomService(
      RoomRepository roomRepository,
      RoomMemberRepository roomMemberRepository,
      JoinStrategyResolver joinStrategyResolver) {
    this.roomRepository = roomRepository;
    this.roomMemberRepository = roomMemberRepository;
    this.joinStrategyResolver = joinStrategyResolver;
  }

  /** Creates a room and joins the host as its first member. */
  @Transactional
  public Room createRoom(Long hostUserId, String name, int capacity) {
    if (capacity < 2 || capacity > Room.MAX_CAPACITY) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "정원은 2~" + Room.MAX_CAPACITY + " 사이여야 합니다.");
    }
    Room room = roomRepository.save(new Room(name, hostUserId, capacity, generateInviteCode()));
    room.increaseMemberCount();
    roomMemberRepository.save(
        new RoomMember(room.getId(), hostUserId, MemberRole.HOST, LocalDateTime.now()));
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
   * Joins a user via the configured concurrency strategy. Intentionally not transactional here: the
   * selected strategy opens and owns its own transaction (optimistic retries each need a fresh
   * one).
   */
  public RoomMember join(Long roomId, Long userId) {
    return joinStrategyResolver.active().join(roomId, userId);
  }

  /** Removes the user's membership and decrements the room's counter under a row lock. */
  @Transactional
  public void leave(Long roomId, Long userId) {
    RoomMember member =
        roomMemberRepository
            .findByRoomIdAndUserId(roomId, userId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_ROOM_MEMBER));
    Room room =
        roomRepository
            .findByIdForUpdate(roomId)
            .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
    roomMemberRepository.delete(member);
    room.decreaseMemberCount();
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
