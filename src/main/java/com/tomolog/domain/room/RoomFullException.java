package com.tomolog.domain.room;

import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;

/** Thrown when a join attempt hits a room that is already at capacity (HTTP 409). */
public class RoomFullException extends ApiException {

  public RoomFullException() {
    super(ErrorCode.ROOM_FULL);
  }
}
