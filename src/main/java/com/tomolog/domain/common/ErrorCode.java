package com.tomolog.domain.common;

import org.springframework.http.HttpStatus;

/** Catalogue of API error codes and their HTTP status (SPEC §5). */
public enum ErrorCode {
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."),
  ROOM_FULL(HttpStatus.CONFLICT, "방 정원이 가득 찼습니다."),
  ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여한 방입니다."),
  NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "방의 멤버가 아닙니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  public HttpStatus status() {
    return status;
  }

  public String defaultMessage() {
    return message;
  }
}
