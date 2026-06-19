package com.tomolog.presentation.realtime;

import com.tomolog.application.realtime.TimerAction;
import jakarta.validation.constraints.NotNull;

/** Host-only timer control command (client → server). */
public record TimerControl(@NotNull TimerAction action) {}
