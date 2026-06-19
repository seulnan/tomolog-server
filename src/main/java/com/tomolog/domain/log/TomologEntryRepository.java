package com.tomolog.domain.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TomologEntryRepository extends JpaRepository<TomologEntry, Long> {

  Page<TomologEntry> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

  long countByRoomIdAndUserId(Long roomId, Long userId);
}
