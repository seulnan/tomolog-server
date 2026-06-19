package com.tomolog.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * One PostgreSQL and one Redis container shared by the {@code @SpringBootTest} integration tests,
 * so the whole suite starts them once rather than per class.
 */
public final class SharedContainers {

  public static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
  public static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  static {
    POSTGRES.start();
    REDIS.start();
  }

  private SharedContainers() {}

  /** Wires datasource, redis and a JWT secret so any profile (incl. prod) can boot under test. */
  public static void register(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("tomolog.jwt.secret", () -> "integration-test-secret-0123456789-abcdef");
  }
}
