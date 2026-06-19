package com.tomolog.domain.realtime;

/** Timer payload for TIMER_TICK / TIMER_PHASE_CHANGED events. */
public record TimerSnapshot(TimerPhase phase, int remainingSeconds, boolean running) {}
