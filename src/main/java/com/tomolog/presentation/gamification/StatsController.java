package com.tomolog.presentation.gamification;

import com.tomolog.application.gamification.StatsResponse;
import com.tomolog.application.gamification.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Stats endpoint (SPEC §5). */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

  private final StatsService statsService;

  public StatsController(StatsService statsService) {
    this.statsService = statsService;
  }

  /** Returns the authenticated user's streak, total minutes, and badges. */
  @GetMapping("/me")
  public StatsResponse myStats(@AuthenticationPrincipal Long userId) {
    return statsService.getStats(userId);
  }
}
