package com.tomolog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Application entry point for the tomolog real-time study-room server (SPEC §9). */
@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class TomologApplication {

  public static void main(String[] args) {
    SpringApplication.run(TomologApplication.class, args);
  }
}
