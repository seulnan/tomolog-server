package com.tomolog.realtime;

import com.tomolog.realtime.dto.MemberEventPayload;
import com.tomolog.realtime.event.RoomEvent;
import com.tomolog.realtime.event.RoomEventType;
import com.tomolog.room.event.RoomMembershipChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends room events to {@code /topic/rooms/{roomId}}. Membership changes arrive as Spring
 * application events (published by the room service after a join/leave commits), keeping the room
 * service free of any messaging dependency (SPEC §6).
 */
@Component
public class RoomEventBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;

  public RoomEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /** Broadcasts an event envelope to the room's topic. */
  public void broadcast(Long roomId, RoomEvent event) {
    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
  }

  @EventListener
  public void onMembershipChanged(RoomMembershipChangedEvent event) {
    RoomEventType type = event.joined() ? RoomEventType.MEMBER_JOINED : RoomEventType.MEMBER_LEFT;
    broadcast(
        event.roomId(), RoomEvent.of(type, event.roomId(), new MemberEventPayload(event.userId())));
  }
}
