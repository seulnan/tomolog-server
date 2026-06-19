package com.tomolog.application.realtime;

/** Payload for MEMBER_JOINED / MEMBER_LEFT events. */
public record MemberEventPayload(Long userId) {}
