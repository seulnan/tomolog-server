package com.tomolog.realtime.dto;

import com.tomolog.realtime.registry.TimerPhase;

/** Timer payload for TIMER_TICK / TIMER_PHASE_CHANGED events. */
public record TimerSnapshot(TimerPhase phase, int remainingSeconds, boolean running) {}
