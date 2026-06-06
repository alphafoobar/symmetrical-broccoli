package com.demo.skills;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class SkillsApplicationTests {

  @Test
  @DisplayName("SkillsApplication is annotated as a Spring Boot application")
  void isSpringBootApplication() {
    assertThat(SkillsApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
  }

}
