package com.tomolog.application.log;

/** Published when a study snapshot is submitted; the realtime layer broadcasts NEW_LOG. */
public record NewLogEvent(Long roomId, LogResponse log) {}
