package com.tomolog.room.concurrency;

import com.tomolog.common.error.ApiException;
import com.tomolog.common.error.ErrorCode;

/** Thrown when a join attempt hits a room that is already at capacity (HTTP 409). */
public class RoomFullException extends ApiException {

  public RoomFullException() {
    super(ErrorCode.ROOM_FULL);
  }
}
