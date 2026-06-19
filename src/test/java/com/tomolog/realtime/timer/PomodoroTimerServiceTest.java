package com.tomolog.realtime.timer;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tomolog.application.realtime.PomodoroTimerService;
import com.tomolog.application.realtime.RoomEvent;
import com.tomolog.application.realtime.RoomEventBroadcaster;
import com.tomolog.application.realtime.RoomEventType;
import com.tomolog.application.realtime.RoomLiveStateRegistry;
import com.tomolog.application.realtime.TimerAction;
import com.tomolog.domain.realtime.RoomLiveState;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PomodoroTimerServiceTest {

  @Mock private RoomLiveStateRegistry registry;
  @Mock private RoomEventBroadcaster broadcaster;

  private boolean isType(Object event, RoomEventType type) {
    return event instanceof RoomEvent e && e.type() == type;
  }

  @Test
  void control_start_broadcastsTimerTick() {
    when(registry.get(1L)).thenReturn(new RoomLiveState());
    new PomodoroTimerService(registry, broadcaster).control(1L, TimerAction.START);

    verify(broadcaster).broadcast(eq(1L), argThat(e -> isType(e, RoomEventType.TIMER_TICK)));
  }

  @Test
  void control_skip_broadcastsPhaseChanged() {
    when(registry.get(2L)).thenReturn(new RoomLiveState());
    new PomodoroTimerService(registry, broadcaster).control(2L, TimerAction.SKIP);

    verify(broadcaster)
        .broadcast(eq(2L), argThat(e -> isType(e, RoomEventType.TIMER_PHASE_CHANGED)));
  }

  @Test
  void tickAll_running_broadcastsTick_paused_doesNot() {
    RoomLiveState running = new RoomLiveState();
    running.start();
    RoomLiveState paused = new RoomLiveState();
    when(registry.activeRoomIds()).thenReturn(Set.of(1L, 2L));
    when(registry.get(1L)).thenReturn(running);
    when(registry.get(2L)).thenReturn(paused);

    new PomodoroTimerService(registry, broadcaster).tickAll();

    verify(broadcaster).broadcast(eq(1L), argThat(e -> isType(e, RoomEventType.TIMER_TICK)));
    verify(broadcaster, never()).broadcast(eq(2L), argThat(e -> true));
  }
}
