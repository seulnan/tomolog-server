package com.tomolog.presentation.realtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inbound chat message (client → server). The sender is taken from the STOMP principal. */
public record ChatSend(@NotBlank @Size(max = 500) String text) {}
