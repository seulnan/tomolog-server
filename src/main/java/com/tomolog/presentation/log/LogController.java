package com.tomolog.presentation.log;

import com.tomolog.application.log.LogResponse;
import com.tomolog.application.log.LogService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Study snapshot submit + feed for a room (SPEC §5). */
@RestController
@RequestMapping("/api/rooms/{roomId}/logs")
public class LogController {

  private final LogService logService;

  public LogController(LogService logService) {
    this.logService = logService;
  }

  /** Submits a study snapshot; members only. Also broadcasts NEW_LOG. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LogResponse submit(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long roomId,
      @Valid @RequestBody SubmitLogRequest request) {
    return logService.submit(
        roomId,
        userId,
        request.cycleNumber(),
        request.emoji(),
        request.memo(),
        request.studiedMinutes(),
        LocalDateTime.now());
  }

  /** Returns the room's snapshot feed, newest first. */
  @GetMapping
  public Page<LogResponse> feed(
      @PathVariable Long roomId, @PageableDefault(size = 20) Pageable pageable) {
    return logService.feed(roomId, pageable);
  }
}
