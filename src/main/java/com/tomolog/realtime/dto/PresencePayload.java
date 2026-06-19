package com.tomolog.realtime.dto;

import com.tomolog.room.domain.Presence;

/** Outbound presence change payload (server → clients). */
public record PresencePayload(Long userId, Presence presence) {}
