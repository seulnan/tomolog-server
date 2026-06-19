package com.tomolog.presentation.realtime;

import com.tomolog.application.realtime.PomodoroTimerService;
import com.tomolog.application.room.RoomService;
import com.tomolog.domain.room.Room;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/** Host-only control of the shared Pomodoro timer on {@code /app/rooms/{roomId}/timer/control}. */
@Controller
public class TimerStompController {

  private final RoomService roomService;
  private final PomodoroTimerService timerService;

  public TimerStompController(RoomService roomService, PomodoroTimerService timerService) {
    this.roomService = roomService;
    this.timerService = timerService;
  }

  /** Applies START/PAUSE/SKIP, but only when the sender is the room's host (otherwise ignored). */
  @MessageMapping("/rooms/{roomId}/timer/control")
  public void control(
      @DestinationVariable Long roomId, @Payload @Valid TimerControl control, Principal principal) {
    Long userId = Long.valueOf(principal.getName());
    Room room = roomService.getRoom(roomId);
    if (userId.equals(room.getHostUserId())) {
      timerService.control(roomId, control.action());
    }
  }
}
