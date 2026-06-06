package com.demo.skills;

import java.security.Security;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Application entrypoint. */
@SpringBootApplication
public class SkillsApplication {

  static {
    System.setProperty("user.timezone", "UTC");
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Security.setProperty("networkaddress.cache.ttl", "10");
    Security.setProperty("networkaddress.cache.negative.ttl", "10");
  }

  /** Starts the Spring Boot application. */
  static void main(final String[] args) {
    SpringApplication.run(SkillsApplication.class, args);
  }

}
