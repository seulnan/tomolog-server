package com.tomolog.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for full-stack tests: boots the whole application (real security chain, real JWT filter)
 * with MockMvc against shared Postgres + Redis containers under the {@code test} profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    SharedContainers.register(registry);
  }
}
