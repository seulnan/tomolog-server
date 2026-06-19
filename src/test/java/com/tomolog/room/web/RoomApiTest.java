package com.tomolog.room.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomolog.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class RoomApiTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

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

  private long createRoom(String token, String name, int capacity) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/rooms")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\",\"capacity\":" + capacity + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("id").asLong();
  }

  @Test
  void createRoom_thenHostIsFirstMember() throws Exception {
    String host = token("room-host-1");
    long roomId = createRoom(host, "심야 스터디", 4);

    mockMvc
        .perform(get("/api/rooms/" + roomId).header(HttpHeaders.AUTHORIZATION, "Bearer " + host))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.room.currentMemberCount").value(1))
        .andExpect(jsonPath("$.members[0].role").value("HOST"));
  }

  @Test
  void join_thenMemberCountIncrements() throws Exception {
    long roomId = createRoom(token("room-host-2"), "방", 4);
    String guest = token("room-guest-1");

    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guest))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("MEMBER"));
    mockMvc
        .perform(get("/api/rooms/" + roomId).header(HttpHeaders.AUTHORIZATION, "Bearer " + guest))
        .andExpect(jsonPath("$.room.currentMemberCount").value(2));
  }

  @Test
  void join_whenRoomFull_thenConflict() throws Exception {
    long roomId = createRoom(token("room-host-3"), "정원2", 2);
    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("guest-a")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("guest-b")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROOM_FULL"));
  }

  @Test
  void join_whenAlreadyMember_thenConflict() throws Exception {
    long roomId = createRoom(token("room-host-4"), "중복", 4);
    String guest = token("dup-guest");
    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guest))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/rooms/" + roomId + "/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guest))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ALREADY_JOINED"));
  }

  @Test
  void leave_thenMemberCountDecrements() throws Exception {
    long roomId = createRoom(token("room-host-5"), "퇴장방", 4);
    String guest = token("leave-guest");
    mockMvc.perform(
        post("/api/rooms/" + roomId + "/join")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + guest));

    mockMvc
        .perform(
            delete("/api/rooms/" + roomId + "/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guest))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/rooms/" + roomId).header(HttpHeaders.AUTHORIZATION, "Bearer " + guest))
        .andExpect(jsonPath("$.room.currentMemberCount").value(1));
  }

  @Test
  void leave_whenNotMember_thenForbidden() throws Exception {
    long roomId = createRoom(token("room-host-6"), "비멤버", 4);

    mockMvc
        .perform(
            delete("/api/rooms/" + roomId + "/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("stranger")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOT_ROOM_MEMBER"));
  }

  @Test
  void getRoom_whenMissing_thenNotFound() throws Exception {
    mockMvc
        .perform(
            get("/api/rooms/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("any")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
  }

  @Test
  void createRoom_withInvalidBody_thenBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/rooms")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("bad-host"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"capacity\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void listRooms_returnsCreatedRoom() throws Exception {
    String host = token("list-host");
    createRoom(host, "목록방", 4);

    mockMvc
        .perform(get("/api/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer " + host))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }
}
