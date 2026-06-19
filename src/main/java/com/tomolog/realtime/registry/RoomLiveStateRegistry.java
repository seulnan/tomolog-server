package com.tomolog.realtime.registry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Holds the live state of active rooms, keyed by room id (in-memory, single instance). */
@Component
public class RoomLiveStateRegistry {

  private final Map<Long, RoomLiveState> states = new ConcurrentHashMap<>();

  public RoomLiveState get(Long roomId) {
    return states.computeIfAbsent(roomId, id -> new RoomLiveState());
  }

  /** Room ids that currently have live state (used by the timer scheduler). */
  public Set<Long> activeRoomIds() {
    return Set.copyOf(states.keySet());
  }
}
