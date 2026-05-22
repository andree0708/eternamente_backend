package com.eternamente.common.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints públicos para health checks (Render, UptimeRobot, etc.).
 * Evita 401 en GET / que generaba ruido en los logs.
 */
@RestController
public class HealthController {

  @GetMapping({"/", "/health"})
  public ResponseEntity<ApiResponse<Map<String, String>>> health() {
    return ApiResponse.entity(Map.of(
        "status", "UP",
        "service", "EternaMenteBackend"
    ));
  }
}
