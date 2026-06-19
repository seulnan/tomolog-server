package com.tomolog.room.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomolog.domain.room.MemberRole;
import com.tomolog.domain.room.Presence;
import com.tomolog.domain.room.RoomMember;
import com.tomolog.domain.room.RoomMemberRepository;
import com.tomolog.support.AbstractRepositoryTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class RoomMemberRepositoryTest extends AbstractRepositoryTest {

  @Autowired private RoomMemberRepository roomMemberRepository;

  @Test
  void save_givenNewMember_thenOnlineWithJoinTime() {
    RoomMember saved =
        roomMemberRepository.save(new RoomMember(10L, 20L, MemberRole.HOST, LocalDateTime.now()));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getRoomId()).isEqualTo(10L);
    assertThat(saved.getUserId()).isEqualTo(20L);
    assertThat(saved.getRole()).isEqualTo(MemberRole.HOST);
    assertThat(saved.getPresence()).isEqualTo(Presence.ONLINE);
    assertThat(saved.getJoinedAt()).isNotNull();
  }

  @Test
  void finders_byRoomAndUser_work() {
    roomMemberRepository.save(new RoomMember(11L, 21L, MemberRole.MEMBER, LocalDateTime.now()));
    roomMemberRepository.save(new RoomMember(11L, 22L, MemberRole.MEMBER, LocalDateTime.now()));

    assertThat(roomMemberRepository.findByRoomIdAndUserId(11L, 21L)).isPresent();
    assertThat(roomMemberRepository.findByRoomId(11L)).hasSize(2);
    assertThat(roomMemberRepository.countByRoomId(11L)).isEqualTo(2);
    assertThat(roomMemberRepository.existsByRoomIdAndUserId(11L, 99L)).isFalse();
  }

  @Test
  void updatePresence_changesPresenceState() {
    RoomMember member =
        roomMemberRepository.save(new RoomMember(12L, 23L, MemberRole.MEMBER, LocalDateTime.now()));

    member.updatePresence(Presence.AWAY);
    RoomMember reloaded = roomMemberRepository.saveAndFlush(member);

    assertThat(reloaded.getPresence()).isEqualTo(Presence.AWAY);
  }

  @Test
  void save_givenDuplicateRoomAndUser_thenRejectedByUniqueConstraint() {
    roomMemberRepository.save(new RoomMember(13L, 24L, MemberRole.HOST, LocalDateTime.now()));

    assertThatThrownBy(
            () ->
                roomMemberRepository.saveAndFlush(
                    new RoomMember(13L, 24L, MemberRole.MEMBER, LocalDateTime.now())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
