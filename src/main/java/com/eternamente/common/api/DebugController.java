package com.eternamente.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class DebugController {

  private static final Logger log = LoggerFactory.getLogger(DebugController.class);

  @GetMapping("/debug/ping")
  public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
    log.info("GET /debug/ping");
    return ApiResponse.entity(Map.of(
        "message", "pong",
        "time", Instant.now().toString()
    ));
  }

  @PostMapping("/debug/echo")
  public ResponseEntity<ApiResponse<Map<String, Object>>> echo(@RequestBody Map<String, Object> body) {
    log.info("POST /debug/echo body={}", body);
    return ApiResponse.entity(Map.of(
        "received", body,
        "time", Instant.now().toString()
    ));
  }
}
