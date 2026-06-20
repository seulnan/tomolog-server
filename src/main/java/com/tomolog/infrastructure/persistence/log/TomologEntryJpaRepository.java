package com.tomolog.infrastructure.persistence.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link TomologEntryEntity}; wrapped by the persistence adapter. */
public interface TomologEntryJpaRepository extends JpaRepository<TomologEntryEntity, Long> {

  Page<TomologEntryEntity> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

  long countByRoomIdAndUserId(Long roomId, Long userId);
}
