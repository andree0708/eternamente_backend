package com.eternamente.assessment.ml;

import java.util.Map;

public interface MlAnalysisService {
  MlPrediction analyze(Map<String, Object> metrics);
}

