package com.tomolog.domain.room;

/**
 * Kind of study room. PRIVATE rooms are host-created for people who know each other (small,
 * host-set capacity). THEMED rooms are a fixed set of public "places" where anonymous people study
 * together at large scale (SPEC extension, LEDGER-008).
 */
public enum RoomType {
  PRIVATE,
  THEMED
}
