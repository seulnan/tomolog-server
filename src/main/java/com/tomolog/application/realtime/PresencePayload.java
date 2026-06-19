package com.tomolog.application.realtime;

import com.tomolog.domain.room.Presence;

/** Outbound presence change payload (server → clients). */
public record PresencePayload(Long userId, Presence presence) {}
