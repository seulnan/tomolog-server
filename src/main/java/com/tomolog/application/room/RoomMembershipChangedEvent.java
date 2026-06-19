package com.tomolog.application.room;

/**
 * Application event published when a user joins or leaves a room. The realtime layer listens for it
 * and broadcasts to the room topic — this keeps the room service decoupled from messaging (SPEC
 * §6).
 */
public record RoomMembershipChangedEvent(Long roomId, Long userId, boolean joined) {}
