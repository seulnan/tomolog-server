package com.tomolog.infrastructure.persistence.log;

import com.tomolog.domain.log.TomologEntry;
import com.tomolog.domain.log.TomologEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** JPA adapter for the {@link TomologEntryRepository} port. Snapshots are create-only. */
@Repository
public class TomologEntryPersistenceAdapter implements TomologEntryRepository {

  private final TomologEntryJpaRepository jpaRepository;
  private final TomologEntryMapper mapper;

  public TomologEntryPersistenceAdapter(
      TomologEntryJpaRepository jpaRepository, TomologEntryMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public TomologEntry save(TomologEntry entry) {
    return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toNewEntity(entry)));
  }

  @Override
  public Page<TomologEntry> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable) {
    return jpaRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable).map(mapper::toDomain);
  }

  @Override
  public long countByRoomIdAndUserId(Long roomId, Long userId) {
    return jpaRepository.countByRoomIdAndUserId(roomId, userId);
  }
}
