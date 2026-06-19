package com.tomolog.realtime.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.domain.realtime.RoomLiveState;
import com.tomolog.domain.realtime.TimerPhase;
import com.tomolog.domain.realtime.TimerSnapshot;
import com.tomolog.domain.realtime.TimerTickResult;
import com.tomolog.domain.room.Presence;
import org.junit.jupiter.api.Test;

class RoomLiveStateTest {

  @Test
  void start_thenRunningInFocusPhase() {
    RoomLiveState state = new RoomLiveState();

    TimerSnapshot snapshot = state.start();

    assertThat(snapshot.running()).isTrue();
    assertThat(snapshot.phase()).isEqualTo(TimerPhase.FOCUS);
    assertThat(snapshot.remainingSeconds()).isEqualTo(RoomLiveState.FOCUS_SECONDS);
  }

  @Test
  void pause_thenNotRunning() {
    RoomLiveState state = new RoomLiveState();
    state.start();

    assertThat(state.pause().running()).isFalse();
  }

  @Test
  void skip_flipsPhaseFocusToBreakToFocus() {
    RoomLiveState state = new RoomLiveState();

    assertThat(state.skip().phase()).isEqualTo(TimerPhase.BREAK);
    assertThat(state.skip().phase()).isEqualTo(TimerPhase.FOCUS);
  }

  @Test
  void tick_whenPaused_doesNotDecrement() {
    RoomLiveState state = new RoomLiveState();

    TimerTickResult result = state.tick();

    assertThat(result.phaseChanged()).isFalse();
    assertThat(result.snapshot().remainingSeconds()).isEqualTo(RoomLiveState.FOCUS_SECONDS);
  }

  @Test
  void tick_whenRunning_decrementsBySecond() {
    RoomLiveState state = new RoomLiveState();
    state.start();

    assertThat(state.tick().snapshot().remainingSeconds())
        .isEqualTo(RoomLiveState.FOCUS_SECONDS - 1);
  }

  @Test
  void tick_whenFocusElapses_flipsToBreak() {
    RoomLiveState state = new RoomLiveState();
    state.start();

    TimerTickResult last = null;
    for (int i = 0; i < RoomLiveState.FOCUS_SECONDS; i++) {
      last = state.tick();
    }

    assertThat(last).isNotNull();
    assertThat(last.phaseChanged()).isTrue();
    assertThat(last.snapshot().phase()).isEqualTo(TimerPhase.BREAK);
    assertThat(last.snapshot().remainingSeconds()).isEqualTo(RoomLiveState.BREAK_SECONDS);
  }

  @Test
  void presence_isStoredAndSnapshotted() {
    RoomLiveState state = new RoomLiveState();

    state.updatePresence(7L, Presence.AWAY);

    assertThat(state.presenceSnapshot()).containsEntry(7L, Presence.AWAY);
  }
}
