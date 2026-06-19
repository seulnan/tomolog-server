package com.tomolog.presentation.realtime;

import com.tomolog.application.realtime.PresencePayload;
import com.tomolog.application.realtime.RoomEvent;
import com.tomolog.application.realtime.RoomEventBroadcaster;
import com.tomolog.application.realtime.RoomEventType;
import com.tomolog.application.realtime.RoomLiveStateRegistry;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/** Receives presence heartbeats on {@code /app/rooms/{roomId}/presence} (SPEC §6). */
@Controller
public class PresenceStompController {

  private final RoomLiveStateRegistry registry;
  private final RoomEventBroadcaster broadcaster;

  public PresenceStompController(RoomLiveStateRegistry registry, RoomEventBroadcaster broadcaster) {
    this.registry = registry;
    this.broadcaster = broadcaster;
  }

  /** Updates the sender's presence in the room's live state and broadcasts the change. */
  @MessageMapping("/rooms/{roomId}/presence")
  public void updatePresence(
      @DestinationVariable Long roomId,
      @Payload @Valid PresenceUpdate update,
      Principal principal) {
    Long userId = Long.valueOf(principal.getName());
    registry.get(roomId).updatePresence(userId, update.presence());
    broadcaster.broadcast(
        roomId,
        RoomEvent.of(
            RoomEventType.PRESENCE_UPDATED,
            roomId,
            new PresencePayload(userId, update.presence())));
  }
}
