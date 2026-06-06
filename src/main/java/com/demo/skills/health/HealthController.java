package com.demo.skills.health;

import com.demo.skills.api.HealthApi;
import com.demo.skills.api.model.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller implementing the health-check endpoint. */
@RestController
@RequestMapping("/api/v1")
public class HealthController implements HealthApi {

  @Override
  public ResponseEntity<HealthResponse> getHealth() {
    return ResponseEntity.ok(new HealthResponse("UP"));
  }
}
