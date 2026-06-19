package com.tomolog.room.repository;

import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

  Optional<Room> findByInviteCode(String inviteCode);

  Page<Room> findByStatus(RoomStatus status, Pageable pageable);

  boolean existsByInviteCode(String inviteCode);
}
