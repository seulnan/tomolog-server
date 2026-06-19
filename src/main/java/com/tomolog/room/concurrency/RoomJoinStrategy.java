package com.tomolog.room.concurrency;

import com.tomolog.room.domain.RoomMember;

/**
 * Strategy for the concurrency-guarded join-room operation (SPEC §4). Implementations must enforce
 * {@code currentMemberCount < capacity} atomically so capacity is never exceeded, even under many
 * simultaneous join attempts.
 */
public interface RoomJoinStrategy {

  /**
   * Joins the user to the room, or throws {@link RoomFullException} when the room is at capacity.
   *
   * @param roomId the room to join
   * @param userId the joining user
   * @return the created membership
   */
  RoomMember join(Long roomId, Long userId);
}
