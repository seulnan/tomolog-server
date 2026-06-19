package com.tomolog.realtime.registry;

import com.tomolog.realtime.dto.TimerSnapshot;
import com.tomolog.room.domain.Presence;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory live state for one room: a thread-safe presence map and a shared Pomodoro timer. The
 * timer's compound state (phase + remaining + running) is mutated under a {@link ReentrantLock} so
 * ticks and host controls never interleave. This is the language-level {@code java.util.concurrent}
 * showcase that complements the DB-level locks (SPEC §4, §6).
 */
public class RoomLiveState {

  public static final int FOCUS_SECONDS = 25 * 60;
  public static final int BREAK_SECONDS = 5 * 60;

  private final Map<Long, Presence> presenceByUser = new ConcurrentHashMap<>();

  private final ReentrantLock timerLock = new ReentrantLock();
  private TimerPhase phase = TimerPhase.FOCUS;
  private int remainingSeconds = FOCUS_SECONDS;
  private boolean running;

  public void updatePresence(Long userId, Presence presence) {
    presenceByUser.put(userId, presence);
  }

  public Map<Long, Presence> presenceSnapshot() {
    return Map.copyOf(presenceByUser);
  }

  /** Starts (or resumes) the timer. */
  public TimerSnapshot start() {
    return withTimerLock(
        () -> {
          running = true;
          return snapshotUnsafe();
        });
  }

  /** Pauses the timer, keeping the remaining time. */
  public TimerSnapshot pause() {
    return withTimerLock(
        () -> {
          running = false;
          return snapshotUnsafe();
        });
  }

  /** Skips to the next phase immediately. */
  public TimerSnapshot skip() {
    return withTimerLock(
        () -> {
          switchPhase();
          return snapshotUnsafe();
        });
  }

  /** Advances one second; flips phase when the current one elapses. */
  public TimerTickResult tick() {
    return withTimerLock(
        () -> {
          if (!running) {
            return new TimerTickResult(snapshotUnsafe(), false);
          }
          remainingSeconds--;
          boolean phaseChanged = false;
          if (remainingSeconds <= 0) {
            switchPhase();
            phaseChanged = true;
          }
          return new TimerTickResult(snapshotUnsafe(), phaseChanged);
        });
  }

  public TimerSnapshot timerSnapshot() {
    return withTimerLock(this::snapshotUnsafe);
  }

  public boolean isRunning() {
    return withTimerLock(() -> running);
  }

  private void switchPhase() {
    phase = phase == TimerPhase.FOCUS ? TimerPhase.BREAK : TimerPhase.FOCUS;
    remainingSeconds = phase == TimerPhase.FOCUS ? FOCUS_SECONDS : BREAK_SECONDS;
  }

  private TimerSnapshot snapshotUnsafe() {
    return new TimerSnapshot(phase, remainingSeconds, running);
  }

  private <T> T withTimerLock(java.util.function.Supplier<T> action) {
    timerLock.lock();
    try {
      return action.get();
    } finally {
      timerLock.unlock();
    }
  }
}
