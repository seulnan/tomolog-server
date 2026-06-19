package com.tomolog.log.event;

import com.tomolog.log.dto.LogResponse;

/** Published when a study snapshot is submitted; the realtime layer broadcasts NEW_LOG. */
public record NewLogEvent(Long roomId, LogResponse log) {}
