package com.tomolog.room.repository;

import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
