package com.tomolog.domain.room;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Domain port for room persistence. The lock-flavored and atomic operations ({@code
 * findByIdForUpdate}, {@code increaseMemberCountIfNotFull}, {@code decreaseMemberCount}) carry
 * persistence semantics by design — they are the headline concurrency guards (SPEC §4).
 */
public interface RoomRepository {

  Optional<Room> findById(Long id);

  boolean existsById(Long id);

  Page<Room> findAll(Pageable pageable);

  /** Loads the room under a write lock so concurrent joins on it are serialized (pessimistic). */
  Optional<Room> findByIdForUpdate(Long id);

  Optional<Room> findByInviteCode(String inviteCode);

  Page<Room> findByStatus(RoomStatus status, Pageable pageable);

  boolean existsByInviteCode(String inviteCode);

  Optional<RoomType> findTypeById(Long id);

  Room save(Room room);

  /** Atomic capacity gate: increments only while below capacity. Returns rows affected (1/0). */
  int increaseMemberCountIfNotFull(Long id);

  /** Atomic counter decrement (never below zero). Returns rows affected. */
  int decreaseMemberCount(Long id);
}
