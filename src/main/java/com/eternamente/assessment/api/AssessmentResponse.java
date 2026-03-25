package com.eternamente.assessment.api;

import com.eternamente.assessment.AssessmentSession;

import java.time.Instant;
import java.util.UUID;

public record AssessmentResponse(
    UUID id,
    String userExternalId,
    Integer age,
    double riskScore,
    boolean predictedDcl,
    String modelVersion,
    Instant createdAt,
    Instant mlRunAt
) {
  public static AssessmentResponse from(AssessmentSession session) {
    return new AssessmentResponse(
        session.getId(),
        session.getUserExternalId(),
        session.getAge(),
        session.getRiskScore() == null ? 0d : session.getRiskScore(),
        session.getPredictedDcl() != null && session.getPredictedDcl(),
        session.getModelVersion(),
        session.getCreatedAt(),
        session.getMlRunAt()
    );
  }
}

