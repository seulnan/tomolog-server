package com.tomolog.realtime.event;

import java.time.Instant;

/**
 * Shared envelope for every server→client room event (SPEC §6): {@code { type, roomId, payload,
 * serverTime }}. Payloads are DTOs, never entities.
 */
public record RoomEvent(RoomEventType type, Long roomId, Object payload, Instant serverTime) {

  public static RoomEvent of(RoomEventType type, Long roomId, Object payload) {
    return new RoomEvent(type, roomId, payload, Instant.now());
  }
}
