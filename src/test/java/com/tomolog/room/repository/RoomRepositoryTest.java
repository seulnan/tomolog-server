package com.tomolog.room.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomRepository;
import com.tomolog.domain.room.RoomStatus;
import com.tomolog.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class RoomRepositoryTest extends AbstractRepositoryTest {

  @Autowired private RoomRepository roomRepository;

  @Test
  void save_givenNewRoom_thenStartsWaitingEmptyAndVersioned() {
    Room saved = roomRepository.save(new Room("자정 스터디", 1L, 4, "abc123"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("자정 스터디");
    assertThat(saved.getHostUserId()).isEqualTo(1L);
    assertThat(saved.getCapacity()).isEqualTo(4);
    assertThat(saved.getStatus()).isEqualTo(RoomStatus.WAITING);
    assertThat(saved.getInviteCode()).isEqualTo("abc123");
    assertThat(saved.getCurrentMemberCount()).isZero();
    assertThat(saved.getVersion()).isNotNull();
  }

  @Test
  void findByInviteCode_givenExistingCode_thenReturnsRoom() {
    roomRepository.save(new Room("방", 1L, 4, "join-me"));

    assertThat(roomRepository.findByInviteCode("join-me")).isPresent();
    assertThat(roomRepository.findByInviteCode("nope")).isEmpty();
    assertThat(roomRepository.existsByInviteCode("join-me")).isTrue();
  }

  @Test
  void findByStatus_givenPageable_thenReturnsOnlyMatchingStatus() {
    roomRepository.save(new Room("대기방", 1L, 4, "w1"));
    Room active = roomRepository.save(new Room("진행방", 1L, 4, "a1"));
    active.changeStatus(RoomStatus.ACTIVE);
    roomRepository.save(active);

    Page<Room> waiting = roomRepository.findByStatus(RoomStatus.WAITING, PageRequest.of(0, 10));

    assertThat(waiting.getContent()).extracting(Room::getStatus).containsOnly(RoomStatus.WAITING);
  }

  @Test
  void memberCount_increaseDecreaseAndFullness_behaveAsExpected() {
    Room room = new Room("정원2", 1L, 2, "cap2");

    assertThat(room.isFull()).isFalse();
    room.increaseMemberCount();
    room.increaseMemberCount();
    assertThat(room.getCurrentMemberCount()).isEqualTo(2);
    assertThat(room.isFull()).isTrue();

    room.decreaseMemberCount();
    assertThat(room.getCurrentMemberCount()).isEqualTo(1);
    room.decreaseMemberCount();
    room.decreaseMemberCount();
    assertThat(room.getCurrentMemberCount()).isZero();
  }
}
