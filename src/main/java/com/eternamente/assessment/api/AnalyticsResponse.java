package com.eternamente.assessment.api;

import java.time.Instant;
import java.util.List;

public record AnalyticsResponse(
    CognitiveSummaryResponse summary,
    List<GameTypeStat> byGameType,
    List<RiskTrendPoint> riskTrend
) {
  public record GameTypeStat(
      String gameType,
      int sessions,
      double avgRiskScore,
      Double avgAccuracy
  ) {}

  public record RiskTrendPoint(
      Instant playedAt,
      double riskScore,
      String gameType
  ) {}
}
