package com.tomolog.realtime.dto;

/** Payload for MEMBER_JOINED / MEMBER_LEFT events. */
public record MemberEventPayload(Long userId) {}
