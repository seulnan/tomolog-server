package com.tomolog.log.service;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.gamification.service.GamificationService;
import com.tomolog.log.domain.TomologEntry;
import com.tomolog.log.dto.LogResponse;
import com.tomolog.log.dto.SubmitLogRequest;
import com.tomolog.log.event.NewLogEvent;
import com.tomolog.log.repository.TomologEntryRepository;
import com.tomolog.room.repository.RoomMemberRepository;
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
  public LogResponse submit(Long roomId, Long userId, SubmitLogRequest request, LocalDateTime now) {
    if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
      throw new ApiException(ErrorCode.NOT_ROOM_MEMBER);
    }
    TomologEntry entry =
        entryRepository.saveAndFlush(
            new TomologEntry(
                roomId,
                userId,
                request.cycleNumber(),
                request.emoji(),
                request.memo(),
                request.studiedMinutes()));
    gamificationService.recordStudy(userId, roomId, request.studiedMinutes(), now);
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
