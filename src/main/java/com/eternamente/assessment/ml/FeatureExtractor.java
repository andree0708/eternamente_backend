package com.eternamente.assessment.ml;

import com.eternamente.assessment.AssessmentSession;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;

@Component
public class FeatureExtractor {

  private static final int WEEKS_BACK = 4;
  private static final int MIN_DATA_POINTS = 3;

  private final SessionMetricsReader metricsReader;

  public FeatureExtractor(SessionMetricsReader metricsReader) {
    this.metricsReader = metricsReader;
  }

  public record SessionPoint(Instant at, String gameType, Map<String, Object> metrics) {}

  public float[] extractRaw(List<SessionPoint> sessions) {
    Instant cutoff = Instant.now().minus(WEEKS_BACK * 7L, ChronoUnit.DAYS);
    List<SessionPoint> window = sessions.stream()
        .filter(s -> s.at() == null || !s.at().isBefore(cutoff))
        .sorted(Comparator.comparing(SessionPoint::at, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();

    float[] raw = new float[FeatureVector.SIZE];
    for (int i = 0; i < raw.length; i++) {
      raw[i] = 0.5f;
    }

    if (window.isEmpty()) {
      return raw;
    }

    raw[0] = (float) defaultIfNaN(meanRt(window, GameDomainMapper.Domain.MEMORY), 1500d);
    raw[1] = (float) defaultIfNaN(meanRt(window, GameDomainMapper.Domain.ATTENTION), 1500d);
    raw[2] = (float) defaultIfNaN(meanRt(window, GameDomainMapper.Domain.EXECUTIVE), 1500d);
    raw[3] = 1500f;

    raw[4] = (float) defaultIfNaN(meanAccuracy(window, GameDomainMapper.Domain.MEMORY), 50d);
    raw[5] = (float) defaultIfNaN(meanAccuracy(window, GameDomainMapper.Domain.ATTENTION), 50d);
    raw[6] = (float) defaultIfNaN(meanAccuracy(window, GameDomainMapper.Domain.EXECUTIVE), 50d);
    raw[7] = 50f;
    raw[8] = (float) defaultIfNaN(meanAccuracy(window, GameDomainMapper.Domain.ORIENTATION), 50d);

    raw[9] = (float) defaultIfNaN(accuracyTrend(window, GameDomainMapper.Domain.MEMORY), 0d);
    raw[10] = (float) defaultIfNaN(accuracyTrend(window, GameDomainMapper.Domain.ATTENTION), 0d);

    raw[11] = (float) sessionCompletionRate(window);
    raw[12] = (float) defaultIfNaN(rtVariability(window), 0.3d);
    raw[13] = (float) defaultIfNaN(deltaFromBaseline(window), 0d);

    return raw;
  }

  public boolean hasMinimumData(List<SessionPoint> sessions) {
    Instant cutoff = Instant.now().minus(WEEKS_BACK * 7L, ChronoUnit.DAYS);
    long count = sessions.stream()
        .filter(s -> s.at() == null || !s.at().isBefore(cutoff))
        .count();
    return count >= MIN_DATA_POINTS;
  }

  private static double meanRt(List<SessionPoint> sessions, GameDomainMapper.Domain domain) {
    return sessions.stream()
        .filter(s -> GameDomainMapper.domainOf(s.gameType()) == domain)
        .map(s -> SessionMetricsReader.reactionTimeMs(s.metrics()))
        .filter(rt -> !Double.isNaN(rt))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(Double.NaN);
  }

  private static double defaultIfNaN(double value, double fallback) {
    return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
  }

  private static double meanAccuracy(List<SessionPoint> sessions, GameDomainMapper.Domain domain) {
    return sessions.stream()
        .filter(s -> GameDomainMapper.domainOf(s.gameType()) == domain)
        .map(s -> SessionMetricsReader.accuracyPct(s.metrics()))
        .filter(a -> !Double.isNaN(a))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(Double.NaN);
  }

  private static double accuracyTrend(List<SessionPoint> sessions, GameDomainMapper.Domain domain) {
    List<Double> accuracies = sessions.stream()
        .filter(s -> GameDomainMapper.domainOf(s.gameType()) == domain)
        .map(s -> SessionMetricsReader.accuracyPct(s.metrics()))
        .filter(a -> !Double.isNaN(a))
        .toList();
    if (accuracies.size() < MIN_DATA_POINTS) {
      return Double.NaN;
    }
    return olsSlope(accuracies);
  }

  private static double olsSlope(List<Double> values) {
    int n = values.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumX2 = 0;
    for (int i = 0; i < n; i++) {
      double x = i;
      double y = values.get(i);
      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumX2 += x * x;
    }
    double denom = n * sumX2 - sumX * sumX;
    if (denom == 0) {
      return 0;
    }
    return (n * sumXY - sumX * sumY) / denom;
  }

  private static double sessionCompletionRate(List<SessionPoint> sessions) {
    int expected = WEEKS_BACK * 7;
    return Math.min(1d, (double) sessions.size() / expected);
  }

  private static double rtVariability(List<SessionPoint> sessions) {
    List<Double> rts = new ArrayList<>();
    for (SessionPoint s : sessions) {
      double rt = SessionMetricsReader.reactionTimeMs(s.metrics());
      if (!Double.isNaN(rt)) {
        rts.add(rt);
      }
    }
    if (rts.size() < MIN_DATA_POINTS) {
      return Double.NaN;
    }
    DoubleSummaryStatistics stats = rts.stream().mapToDouble(Double::doubleValue).summaryStatistics();
    if (stats.getAverage() == 0) {
      return 0;
    }
    double variance = rts.stream()
        .mapToDouble(v -> Math.pow(v - stats.getAverage(), 2))
        .average()
        .orElse(0);
    return Math.sqrt(variance) / stats.getAverage();
  }

  private static double deltaFromBaseline(List<SessionPoint> sessions) {
    List<Double> scores = sessions.stream()
        .map(s -> SessionMetricsReader.accuracyPct(s.metrics()))
        .filter(a -> !Double.isNaN(a))
        .toList();
    if (scores.size() < MIN_DATA_POINTS) {
      return Double.NaN;
    }
    int chunk = Math.max(1, scores.size() / 4);
    double baseline = scores.subList(0, chunk).stream().mapToDouble(Double::doubleValue).average().orElse(0);
    double recent = scores.subList(scores.size() - chunk, scores.size()).stream()
        .mapToDouble(Double::doubleValue).average().orElse(0);
    double std = Math.sqrt(scores.stream()
        .mapToDouble(v -> Math.pow(v - baseline, 2))
        .average()
        .orElse(1));
    if (std < 1e-6) {
      std = 1;
    }
    return (recent - baseline) / std;
  }
}
