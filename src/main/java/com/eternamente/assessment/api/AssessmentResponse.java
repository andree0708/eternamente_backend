package com.eternamente.assessment.api;

import com.eternamente.assessment.AssessmentSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record AssessmentResponse(
    UUID id,
    UUID userId,
    Integer age,
    double riskScore,
    boolean predictedDcl,
    String alertLevel,
    String modelVersion,
    String gameType,
    Map<String, Object> metrics,
    Instant createdAt,
    Instant mlRunAt
) {
  public static AssessmentResponse from(
      AssessmentSession session,
      UUID userId,
      ObjectMapper objectMapper
  ) {
    Map<String, Object> metrics = parseMetrics(session.getMetricsJson(), objectMapper);
    String gameType = session.getGameType();
    if (gameType == null || gameType.isBlank()) {
      Object fromMetrics = metrics.get("gameType");
      gameType = fromMetrics != null ? String.valueOf(fromMetrics) : "memory";
    }
    return new AssessmentResponse(
        session.getId(),
        userId,
        session.getAge(),
        session.getRiskScore() == null ? 0d : session.getRiskScore(),
        session.getPredictedDcl() != null && session.getPredictedDcl(),
        session.getAlertLevel() != null ? session.getAlertLevel() : "NORMAL",
        session.getModelVersion(),
        gameType,
        metrics,
        session.getCreatedAt(),
        session.getMlRunAt()
    );
  }

  private static Map<String, Object> parseMetrics(String metricsJson, ObjectMapper objectMapper) {
    if (metricsJson == null || metricsJson.isBlank()) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(metricsJson, new TypeReference<>() {});
    } catch (Exception ignored) {
      return Collections.emptyMap();
    }
  }
}
