package com.tomolog.realtime.registry;

import com.tomolog.realtime.dto.TimerSnapshot;

/** Result of advancing a room timer by one tick: the new snapshot and whether the phase flipped. */
public record TimerTickResult(TimerSnapshot snapshot, boolean phaseChanged) {}
