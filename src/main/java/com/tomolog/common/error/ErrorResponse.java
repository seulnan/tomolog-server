package com.tomolog.common.error;

import java.time.LocalDateTime;

/** Unified error envelope returned for every failed request (SPEC §5). */
public record ErrorResponse(
    LocalDateTime timestamp, int status, String code, String message, String path) {

  public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
    return new ErrorResponse(
        LocalDateTime.now(), errorCode.status().value(), errorCode.name(), message, path);
  }
}
