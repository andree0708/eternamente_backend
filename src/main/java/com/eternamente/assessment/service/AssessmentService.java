package com.eternamente.assessment.service;

import com.eternamente.assessment.AssessmentSession;
import com.eternamente.assessment.AssessmentSessionRepository;
import com.eternamente.assessment.api.AssessmentResponse;
import com.eternamente.assessment.api.CreateAssessmentRequest;
import com.eternamente.assessment.ml.MlAnalysisService;
import com.eternamente.assessment.ml.MlPrediction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentService {

  private final AssessmentSessionRepository repository;
  private final MlAnalysisService mlAnalysisService;
  private final ObjectMapper objectMapper;

  public AssessmentService(
      AssessmentSessionRepository repository,
      MlAnalysisService mlAnalysisService,
      ObjectMapper objectMapper
  ) {
    this.repository = repository;
    this.mlAnalysisService = mlAnalysisService;
    this.objectMapper = objectMapper;
  }

  public AssessmentResponse create(CreateAssessmentRequest request) {
    MlPrediction prediction = mlAnalysisService.analyze(request.metrics());
    String metricsJson = toMetricsJson(request.metrics());

    AssessmentSession session = new AssessmentSession();
    session.setUserExternalId(request.userExternalId());
    session.setAge(request.age());
    session.setMetricsJson(metricsJson);
    session.setModelVersion(prediction.modelVersion());
    session.setRiskScore(prediction.riskScore());
    session.setPredictedDcl(prediction.predictedDcl());
    session.setMlRunAt(Instant.now());

    AssessmentSession saved = repository.save(session);
    return AssessmentResponse.from(saved);
  }

  public AssessmentResponse get(UUID id) {
    return repository.findById(id)
        .map(AssessmentResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment no encontrada"));
  }

  private String toMetricsJson(Map<String, Object> metrics) {
    try {
      return objectMapper.writeValueAsString(metrics);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metrics inválidos: no se pueden serializar");
    }
  }
}

