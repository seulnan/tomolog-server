package com.tomolog.realtime.dto;

import jakarta.validation.constraints.NotNull;

/** Host-only timer control command (client → server). */
public record TimerControl(@NotNull TimerAction action) {

  /** Supported control actions for the shared Pomodoro timer. */
  public enum TimerAction {
    START,
    PAUSE,
    SKIP
  }
}
