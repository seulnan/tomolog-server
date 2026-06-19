package com.tomolog.application.realtime;

/** Types of server→client event broadcast to a room topic (SPEC §6). */
public enum RoomEventType {
  MEMBER_JOINED,
  MEMBER_LEFT,
  PRESENCE_UPDATED,
  CHAT,
  TIMER_TICK,
  TIMER_PHASE_CHANGED,
  NEW_LOG,
  PET_GREW
}
