package com.tomolog.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

class AuthUserApiTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String devLoginToken(String oauthId) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/auth/dev-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"oauthId\":\"" + oauthId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("accessToken").asText();
  }

  @Test
  void getMe_withValidToken_returnsProfile() throws Exception {
    String token = devLoginToken("api-test-1");

    mockMvc
        .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("api-test-1@tomolog.local"))
        .andExpect(jsonPath("$.currentStreak").value(0));
  }

  @Test
  void getMe_withoutToken_returns401WithErrorEnvelope() throws Exception {
    mockMvc
        .perform(get("/api/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/users/me"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void patchMe_withValidBody_updatesNicknameAndAvatar() throws Exception {
    String token = devLoginToken("api-test-2");

    mockMvc
        .perform(
            patch("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"새이름\",\"avatarType\":\"FROG\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("새이름"))
        .andExpect(jsonPath("$.avatarType").value("FROG"));
  }

  @Test
  void patchMe_withBlankNickname_returns400WithErrorEnvelope() throws Exception {
    String token = devLoginToken("api-test-3");

    mockMvc
        .perform(
            patch("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"\",\"avatarType\":\"CAT\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void getMe_withGarbageToken_returns401() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).contains("UNAUTHORIZED");
  }
}
