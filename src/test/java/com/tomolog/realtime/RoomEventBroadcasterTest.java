package com.tomolog.realtime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.tomolog.realtime.dto.MemberEventPayload;
import com.tomolog.realtime.event.RoomEvent;
import com.tomolog.realtime.event.RoomEventType;
import com.tomolog.room.event.RoomMembershipChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RoomEventBroadcasterTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @Test
  void onMembershipChanged_joined_broadcastsMemberJoinedToRoomTopic() {
    new RoomEventBroadcaster(messagingTemplate)
        .onMembershipChanged(new RoomMembershipChangedEvent(5L, 9L, true));

    verify(messagingTemplate)
        .convertAndSend(
            eq("/topic/rooms/5"),
            (Object)
                argThat(
                    payload ->
                        payload instanceof RoomEvent event
                            && event.type() == RoomEventType.MEMBER_JOINED
                            && event.payload() instanceof MemberEventPayload p
                            && p.userId().equals(9L)));
  }

  @Test
  void onMembershipChanged_left_broadcastsMemberLeft() {
    new RoomEventBroadcaster(messagingTemplate)
        .onMembershipChanged(new RoomMembershipChangedEvent(5L, 9L, false));

    verify(messagingTemplate)
        .convertAndSend(
            eq("/topic/rooms/5"),
            (Object)
                argThat(
                    payload ->
                        payload instanceof RoomEvent event
                            && event.type() == RoomEventType.MEMBER_LEFT));
  }
}
