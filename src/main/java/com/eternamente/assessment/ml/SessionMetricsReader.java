package com.eternamente.assessment.ml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SessionMetricsReader {

  private final ObjectMapper objectMapper;

  public SessionMetricsReader(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> parse(String metricsJson) {
    if (metricsJson == null || metricsJson.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(metricsJson, new TypeReference<>() {});
    } catch (Exception e) {
      return Map.of();
    }
  }

  public static double accuracyPct(Map<String, Object> metrics) {
    Object v = metrics.get("accuracy");
    if (v instanceof Number n) {
      double a = n.doubleValue();
      return a <= 1d ? a * 100d : a;
    }
    Object matched = metrics.get("matchedPairs");
    Object total = metrics.get("totalPairs");
    if (matched instanceof Number m && total instanceof Number t && t.doubleValue() > 0) {
      return (m.doubleValue() / t.doubleValue()) * 100d;
    }
    return Double.NaN;
  }

  public static double reactionTimeMs(Map<String, Object> metrics) {
    Object[] keys = {"averageReactionTimeMs", "reactionTimeMs", "averageRevealMs"};
    for (Object key : keys) {
      Object v = metrics.get(String.valueOf(key));
      if (v instanceof Number n && n.doubleValue() > 0) {
        return n.doubleValue();
      }
    }
    return Double.NaN;
  }
}
