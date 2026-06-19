package com.tomolog.application.realtime;

/** Outbound chat message payload (server → clients). */
public record ChatMessagePayload(Long userId, String text) {}
