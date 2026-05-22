package com.eternamente.assessment.api;

import com.eternamente.assessment.UserCognitiveSummary;

import java.time.Instant;
import java.util.UUID;

public record CognitiveSummaryResponse(
    UUID userId,
    int totalSessions,
    double avgRiskScore,
    Instant lastPlayedAt,
    int memorySessions,
    int stroopSessions,
    int navigationSessions,
    int whackamoleSessions,
    Double avgAccuracy,
    Instant updatedAt
) {
  public static CognitiveSummaryResponse from(UserCognitiveSummary summary) {
    return new CognitiveSummaryResponse(
        summary.getUserId(),
        summary.getTotalSessions(),
        summary.getAvgRiskScore(),
        summary.getLastPlayedAt(),
        summary.getMemorySessions(),
        summary.getStroopSessions(),
        summary.getNavigationSessions(),
        summary.getWhackamoleSessions(),
        summary.getAvgAccuracy(),
        summary.getUpdatedAt()
    );
  }
}
