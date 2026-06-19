package com.tomolog.realtime.dto;

import com.tomolog.room.domain.Presence;
import jakarta.validation.constraints.NotNull;

/** Inbound presence heartbeat / AWAY toggle (client → server). */
public record PresenceUpdate(@NotNull Presence presence) {}
