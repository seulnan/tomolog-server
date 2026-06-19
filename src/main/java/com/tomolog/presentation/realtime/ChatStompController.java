package com.tomolog.presentation.realtime;

import com.tomolog.application.realtime.ChatMessagePayload;
import com.tomolog.application.realtime.RoomEvent;
import com.tomolog.application.realtime.RoomEventBroadcaster;
import com.tomolog.application.realtime.RoomEventType;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/** Receives chat messages on {@code /app/rooms/{roomId}/chat} and broadcasts them (SPEC §6). */
@Controller
public class ChatStompController {

  private final RoomEventBroadcaster broadcaster;

  public ChatStompController(RoomEventBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  /** Broadcasts the chat message to the room topic, tagged with the sender's id. */
  @MessageMapping("/rooms/{roomId}/chat")
  public void chat(
      @DestinationVariable Long roomId, @Payload @Valid ChatSend message, Principal principal) {
    Long userId = Long.valueOf(principal.getName());
    broadcaster.broadcast(
        roomId,
        RoomEvent.of(RoomEventType.CHAT, roomId, new ChatMessagePayload(userId, message.text())));
  }
}
