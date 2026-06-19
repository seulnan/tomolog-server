package com.tomolog.log.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.log.domain.TomologEntry;
import com.tomolog.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class TomologEntryRepositoryTest extends AbstractRepositoryTest {

  @Autowired private TomologEntryRepository tomologEntryRepository;

  @Test
  void save_givenSnapshot_thenPersistsAllFields() {
    TomologEntry saved =
        tomologEntryRepository.save(new TomologEntry(30L, 40L, 1, "🔥", "집중 잘됨", 25));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getRoomId()).isEqualTo(30L);
    assertThat(saved.getUserId()).isEqualTo(40L);
    assertThat(saved.getCycleNumber()).isEqualTo(1);
    assertThat(saved.getEmoji()).isEqualTo("🔥");
    assertThat(saved.getMemo()).isEqualTo("집중 잘됨");
    assertThat(saved.getStudiedMinutes()).isEqualTo(25);
  }

  @Test
  void findByRoomIdOrderByCreatedAtDesc_returnsRoomFeedNewestFirst() {
    tomologEntryRepository.save(new TomologEntry(31L, 40L, 1, "🔥", "1교시", 25));
    tomologEntryRepository.save(new TomologEntry(31L, 41L, 1, "📚", "2교시", 30));
    tomologEntryRepository.save(new TomologEntry(99L, 40L, 1, "💤", "다른방", 10));

    Page<TomologEntry> feed =
        tomologEntryRepository.findByRoomIdOrderByCreatedAtDesc(31L, PageRequest.of(0, 10));

    assertThat(feed.getContent()).extracting(TomologEntry::getRoomId).containsOnly(31L);
    assertThat(feed.getTotalElements()).isEqualTo(2);
  }

  @Test
  void countByRoomIdAndUserId_countsOnlyThatMembersSnapshots() {
    tomologEntryRepository.save(new TomologEntry(32L, 50L, 1, "🔥", null, 25));
    tomologEntryRepository.save(new TomologEntry(32L, 50L, 2, "🔥", null, 25));
    tomologEntryRepository.save(new TomologEntry(32L, 51L, 1, "🔥", null, 25));

    assertThat(tomologEntryRepository.countByRoomIdAndUserId(32L, 50L)).isEqualTo(2);
  }
}
