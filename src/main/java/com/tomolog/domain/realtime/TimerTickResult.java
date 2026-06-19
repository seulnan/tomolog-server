package com.tomolog.domain.realtime;

/** Result of advancing a room timer by one tick: the new snapshot and whether the phase flipped. */
public record TimerTickResult(TimerSnapshot snapshot, boolean phaseChanged) {}
