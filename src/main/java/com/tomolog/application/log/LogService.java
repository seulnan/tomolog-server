package com.tomolog.application.log;

import com.tomolog.application.gamification.GamificationService;
import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;
import com.tomolog.domain.log.TomologEntry;
import com.tomolog.domain.log.TomologEntryRepository;
import com.tomolog.domain.room.RoomMemberRepository;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Submits study snapshots (with gamification + NEW_LOG broadcast) and serves the room feed. */
@Service
public class LogService {

  private final TomologEntryRepository entryRepository;
  private final RoomMemberRepository roomMemberRepository;
  private final GamificationService gamificationService;
  private final ApplicationEventPublisher eventPublisher;

  public LogService(
      TomologEntryRepository entryRepository,
      RoomMemberRepository roomMemberRepository,
      GamificationService gamificationService,
      ApplicationEventPublisher eventPublisher) {
    this.entryRepository = entryRepository;
    this.roomMemberRepository = roomMemberRepository;
    this.gamificationService = gamificationService;
    this.eventPublisher = eventPublisher;
  }

  /** Persists a snapshot (members only), applies gamification, and broadcasts NEW_LOG. */
  @Transactional
  public LogResponse submit(
      Long roomId,
      Long userId,
      int cycleNumber,
      String emoji,
      String memo,
      int studiedMinutes,
      LocalDateTime now) {
    if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
      throw new ApiException(ErrorCode.NOT_ROOM_MEMBER);
    }
    TomologEntry entry =
        entryRepository.saveAndFlush(
            new TomologEntry(roomId, userId, cycleNumber, emoji, memo, studiedMinutes));
    gamificationService.recordStudy(userId, roomId, studiedMinutes, now);
    LogResponse response = LogResponse.from(entry);
    eventPublisher.publishEvent(new NewLogEvent(roomId, response));
    return response;
  }

  /** Returns the room's snapshot feed, newest first. */
  @Transactional(readOnly = true)
  public Page<LogResponse> feed(Long roomId, Pageable pageable) {
    return entryRepository
        .findByRoomIdOrderByCreatedAtDesc(roomId, pageable)
        .map(LogResponse::from);
  }
}
