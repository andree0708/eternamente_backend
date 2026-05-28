package com.eternamente.assessment.ml;

import java.util.Map;
import java.util.UUID;

public interface MlAnalysisService {
  MlPrediction analyze(UUID userId, Map<String, Object> metrics);
}
