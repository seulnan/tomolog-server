package com.tomolog.realtime.dto;

/** Outbound chat message payload (server → clients). */
public record ChatMessagePayload(Long userId, String text) {}
