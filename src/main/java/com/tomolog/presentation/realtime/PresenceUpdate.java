package com.tomolog.presentation.realtime;

import com.tomolog.domain.room.Presence;
import jakarta.validation.constraints.NotNull;

/** Inbound presence heartbeat / AWAY toggle (client → server). */
public record PresenceUpdate(@NotNull Presence presence) {}
