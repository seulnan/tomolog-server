package com.tomolog.domain.room;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

  /**
   * Selects the room row with a write lock ({@code SELECT ... FOR UPDATE}) so concurrent joins on
   * the same room are serialized. Used by the pessimistic and (later) other join strategies.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Room r where r.id = :id")
  Optional<Room> findByIdForUpdate(@Param("id") Long id);

  Optional<Room> findByInviteCode(String inviteCode);

  Page<Room> findByStatus(RoomStatus status, Pageable pageable);

  boolean existsByInviteCode(String inviteCode);

  /** Lightweight type lookup so a join can be routed by room type without loading the entity. */
  @Query("select r.type from Room r where r.id = :id")
  Optional<RoomType> findTypeById(@Param("id") Long id);

  /**
   * Atomic capacity gate for large THEMED rooms: increments the counter only while below capacity,
   * in a single statement. Returns 1 if a slot was taken, 0 if the room was full. The DB still
   * row-locks briefly, but the critical section is one statement instead of read-check-insert.
   */
  @Modifying(clearAutomatically = true)
  @Query(
      "update Room r set r.currentMemberCount = r.currentMemberCount + 1 "
          + "where r.id = :id and r.currentMemberCount < r.capacity")
  int increaseMemberCountIfNotFull(@Param("id") Long id);

  /** Atomic counter decrement for THEMED rooms on leave (never goes below zero). */
  @Modifying(clearAutomatically = true)
  @Query(
      "update Room r set r.currentMemberCount = r.currentMemberCount - 1 "
          + "where r.id = :id and r.currentMemberCount > 0")
  int decreaseMemberCount(@Param("id") Long id);
}
