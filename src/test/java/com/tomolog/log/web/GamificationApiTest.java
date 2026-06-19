package com.tomolog.log.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomolog.application.gamification.GamificationService;
import com.tomolog.domain.gamification.BadgeRepository;
import com.tomolog.domain.gamification.BadgeType;
import com.tomolog.domain.gamification.RoomPet;
import com.tomolog.domain.gamification.RoomPetRepository;
import com.tomolog.domain.room.Room;
import com.tomolog.domain.room.RoomRepository;
import com.tomolog.domain.user.AvatarType;
import com.tomolog.domain.user.OauthProvider;
import com.tomolog.domain.user.User;
import com.tomolog.domain.user.UserRepository;
import com.tomolog.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class GamificationApiTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private GamificationService gamificationService;
  @Autowired private UserRepository userRepository;
  @Autowired private BadgeRepository badgeRepository;
  @Autowired private RoomRepository roomRepository;
  @Autowired private RoomPetRepository roomPetRepository;

  private String token(String oauthId) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/dev-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"oauthId\":\"" + oauthId + "\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("accessToken").asText();
  }

  private long createRoom(String token) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/rooms")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"기록방\",\"capacity\":4}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("id").asLong();
  }

  private ResultActions submitLog(String token, long roomId, int minutes) throws Exception {
    return mockMvc.perform(
        post("/api/rooms/" + roomId + "/logs")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"cycleNumber\":1,\"emoji\":\"🔥\",\"memo\":\"집중\",\"studiedMinutes\":"
                    + minutes
                    + "}"));
  }

  private record SeedIds(long userId, long roomId) {}

  private SeedIds seedUserWithRoomPet() {
    String code = "g-" + UUID.randomUUID().toString().substring(0, 6);
    Long roomId = roomRepository.save(new Room("g방", 1L, 4, code)).getId();
    roomPetRepository.save(new RoomPet(roomId));
    Long userId =
        userRepository
            .save(new User(OauthProvider.GOOGLE, code, code + "@x.com", "u", AvatarType.CAT))
            .getId();
    return new SeedIds(userId, roomId);
  }

  @Test
  void submitLog_asMember_thenCreatedAndAppearsInFeed() throws Exception {
    String host = token("log-host");
    long roomId = createRoom(host);

    submitLog(host, roomId, 25)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.studiedMinutes").value(25));
    mockMvc
        .perform(
            get("/api/rooms/" + roomId + "/logs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + host))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].emoji").value("🔥"));
  }

  @Test
  void submitLog_asNonMember_thenForbidden() throws Exception {
    long roomId = createRoom(token("log-host2"));

    submitLog(token("outsider"), roomId, 25)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_ROOM_MEMBER"));
  }

  @Test
  void submitLog_invalid_thenBadRequest() throws Exception {
    String host = token("log-host3");
    long roomId = createRoom(host);

    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/logs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + host)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cycleNumber\":1,\"emoji\":\"\",\"studiedMinutes\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void stats_afterSubmit_reflectMinutesAndFirstLogBadge() throws Exception {
    String host = token("stats-host");
    long roomId = createRoom(host);
    submitLog(host, roomId, 40).andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/stats/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + host))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalStudyMinutes").value(40))
        .andExpect(jsonPath("$.currentStreak").value(1))
        .andExpect(jsonPath("$.badges[?(@.type=='FIRST_LOG')]").exists());
  }

  @Test
  void recordStudy_atNight_awardsNightOwl() {
    SeedIds ids = seedUserWithRoomPet();

    gamificationService.recordStudy(
        ids.userId(), ids.roomId(), 30, LocalDateTime.of(2026, 3, 1, 2, 0));

    assertThat(badgeRepository.existsByUserIdAndType(ids.userId(), BadgeType.NIGHT_OWL)).isTrue();
  }

  @Test
  void recordStudy_longSession_awardsMarathon() {
    SeedIds ids = seedUserWithRoomPet();

    gamificationService.recordStudy(
        ids.userId(), ids.roomId(), 120, LocalDateTime.of(2026, 3, 1, 14, 0));

    assertThat(badgeRepository.existsByUserIdAndType(ids.userId(), BadgeType.MARATHON)).isTrue();
  }

  @Test
  void recordStudy_sevenConsecutiveDays_awardsStreak7() {
    SeedIds ids = seedUserWithRoomPet();
    LocalDateTime base = LocalDateTime.of(2026, 3, 1, 14, 0);

    for (int day = 0; day < 7; day++) {
      gamificationService.recordStudy(ids.userId(), ids.roomId(), 10, base.plusDays(day));
    }

    assertThat(badgeRepository.existsByUserIdAndType(ids.userId(), BadgeType.STREAK_7)).isTrue();
  }
}
