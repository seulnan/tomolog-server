package com.tomolog.domain.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Domain port for study-snapshot persistence (plain interface, domain models). {@code Page}/ {@code
 * Pageable} are pagination value types, not JPA coupling.
 */
public interface TomologEntryRepository {

  TomologEntry save(TomologEntry entry);

  Page<TomologEntry> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

  long countByRoomIdAndUserId(Long roomId, Long userId);
}
