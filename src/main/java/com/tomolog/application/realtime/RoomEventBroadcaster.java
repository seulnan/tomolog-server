package com.tomolog.application.realtime;

import com.tomolog.application.gamification.PetGrewEvent;
import com.tomolog.application.log.NewLogEvent;
import com.tomolog.application.room.RoomMembershipChangedEvent;
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

  @EventListener
  public void onNewLog(NewLogEvent event) {
    broadcast(event.roomId(), RoomEvent.of(RoomEventType.NEW_LOG, event.roomId(), event.log()));
  }

  @EventListener
  public void onPetGrew(PetGrewEvent event) {
    broadcast(
        event.roomId(),
        RoomEvent.of(
            RoomEventType.PET_GREW,
            event.roomId(),
            new PetSnapshot(event.roomId(), event.growthPoints(), event.level())));
  }
}
