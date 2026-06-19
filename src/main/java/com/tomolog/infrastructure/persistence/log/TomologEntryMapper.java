package com.tomolog.infrastructure.persistence.log;

import com.tomolog.domain.log.TomologEntry;
import org.springframework.stereotype.Component;

/** Maps between the domain {@link TomologEntry} and its JPA {@link TomologEntryEntity}. */
@Component
public class TomologEntryMapper {

  public TomologEntry toDomain(TomologEntryEntity entity) {
    return new TomologEntry(
        entity.getId(),
        entity.getRoomId(),
        entity.getUserId(),
        entity.getCycleNumber(),
        entity.getEmoji(),
        entity.getMemo(),
        entity.getStudiedMinutes(),
        entity.getCreatedAt());
  }

  public TomologEntryEntity toNewEntity(TomologEntry entry) {
    return new TomologEntryEntity(
        entry.getRoomId(),
        entry.getUserId(),
        entry.getCycleNumber(),
        entry.getEmoji(),
        entry.getMemo(),
        entry.getStudiedMinutes());
  }
}
