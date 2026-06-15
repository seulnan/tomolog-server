package com.studylog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Application entry point for the StudyLog real-time study-room server (SPEC §9). */
@EnableJpaAuditing
@SpringBootApplication
public class StudyLogApplication {

  public static void main(String[] args) {
    SpringApplication.run(StudyLogApplication.class, args);
  }
}
