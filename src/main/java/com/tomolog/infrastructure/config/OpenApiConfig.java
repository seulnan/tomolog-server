package com.tomolog.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI metadata, with a Bearer-JWT scheme so the UI can call authenticated endpoints.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  OpenAPI tomologOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("tomolog API")
                .version("v1")
                .description("실시간 스터디 룸 서버 — 친구방/테마방, 동시성 입장, 실시간, 게이미피케이션"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER));
  }
}
