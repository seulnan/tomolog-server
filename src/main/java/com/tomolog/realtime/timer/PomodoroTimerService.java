package com.tomolog.realtime.timer;

import com.tomolog.realtime.RoomEventBroadcaster;
import com.tomolog.realtime.dto.TimerControl.TimerAction;
import com.tomolog.realtime.dto.TimerSnapshot;
import com.tomolog.realtime.event.RoomEvent;
import com.tomolog.realtime.event.RoomEventType;
import com.tomolog.realtime.registry.RoomLiveState;
import com.tomolog.realtime.registry.RoomLiveStateRegistry;
import com.tomolog.realtime.registry.TimerTickResult;
import org.springframework.stereotype.Service;

/**
 * Drives the shared Pomodoro timer for each room: applies host controls and the per-second tick,
 * broadcasting TIMER_TICK / TIMER_PHASE_CHANGED to the room topic (SPEC §6).
 */
@Service
public class PomodoroTimerService {

  private final RoomLiveStateRegistry registry;
  private final RoomEventBroadcaster broadcaster;

  public PomodoroTimerService(RoomLiveStateRegistry registry, RoomEventBroadcaster broadcaster) {
    this.registry = registry;
    this.broadcaster = broadcaster;
  }

  public void control(Long roomId, TimerAction action) {
    RoomLiveState state = registry.get(roomId);
    switch (action) {
      case START -> broadcastTick(roomId, state.start());
      case PAUSE -> broadcastTick(roomId, state.pause());
      case SKIP -> broadcastPhaseChanged(roomId, state.skip());
    }
  }

  public void tickAll() {
    for (Long roomId : registry.activeRoomIds()) {
      RoomLiveState state = registry.get(roomId);
      if (!state.isRunning()) {
        continue;
      }
      TimerTickResult result = state.tick();
      if (result.phaseChanged()) {
        broadcastPhaseChanged(roomId, result.snapshot());
      } else {
        broadcastTick(roomId, result.snapshot());
      }
    }
  }

  private void broadcastTick(Long roomId, TimerSnapshot snapshot) {
    broadcaster.broadcast(roomId, RoomEvent.of(RoomEventType.TIMER_TICK, roomId, snapshot));
  }

  private void broadcastPhaseChanged(Long roomId, TimerSnapshot snapshot) {
    broadcaster.broadcast(
        roomId, RoomEvent.of(RoomEventType.TIMER_PHASE_CHANGED, roomId, snapshot));
  }
}
