package com.tomolog.auth.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomolog.support.SharedContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SPEC §7 hard requirement: dev-login must be disabled outside local/test. Boots under the prod
 * profile (with infra supplied by Testcontainers) and asserts the route is absent → 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class DevLoginDisabledInProdTest {

  @Autowired private MockMvc mockMvc;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    SharedContainers.register(registry);
  }

  @Test
  void devLogin_underProdProfile_isNotMapped() throws Exception {
    mockMvc
        .perform(post("/api/auth/dev-login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isNotFound());
  }
}
