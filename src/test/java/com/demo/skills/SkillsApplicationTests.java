package com.demo.skills;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class SkillsApplicationTests {

  @Test
  void isSpringBootApplication() {
    assertThat(SkillsApplication.class.getAnnotation(SpringBootApplication.class)).isNotNull();
  }

}
