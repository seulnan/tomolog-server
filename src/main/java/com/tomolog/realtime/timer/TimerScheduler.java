package com.tomolog.realtime.timer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ticks every running room timer once a second. Single-instance only — in a multi-instance
 * deployment this would need leader election to avoid duplicate ticks (see LEDGER-009).
 */
@Component
public class TimerScheduler {

  private final PomodoroTimerService timerService;

  public TimerScheduler(PomodoroTimerService timerService) {
    this.timerService = timerService;
  }

  @Scheduled(fixedRate = 1000)
  public void tick() {
    timerService.tickAll();
  }
}
