package com.tomolog.infrastructure.persistence.room;

import com.tomolog.domain.room.RoomStatus;
import com.tomolog.domain.room.RoomType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository over {@link RoomEntity}; wrapped by the persistence adapter. */
public interface RoomJpaRepository extends JpaRepository<RoomEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from RoomEntity r where r.id = :id")
  Optional<RoomEntity> findByIdForUpdate(@Param("id") Long id);

  Optional<RoomEntity> findByInviteCode(String inviteCode);

  Page<RoomEntity> findByStatus(RoomStatus status, Pageable pageable);

  boolean existsByInviteCode(String inviteCode);

  @Query("select r.type from RoomEntity r where r.id = :id")
  Optional<RoomType> findTypeById(@Param("id") Long id);

  @Modifying(clearAutomatically = true)
  @Query(
      "update RoomEntity r set r.currentMemberCount = r.currentMemberCount + 1 "
          + "where r.id = :id and r.currentMemberCount < r.capacity")
  int increaseMemberCountIfNotFull(@Param("id") Long id);

  @Modifying(clearAutomatically = true)
  @Query(
      "update RoomEntity r set r.currentMemberCount = r.currentMemberCount - 1 "
          + "where r.id = :id and r.currentMemberCount > 0")
  int decreaseMemberCount(@Param("id") Long id);
}
