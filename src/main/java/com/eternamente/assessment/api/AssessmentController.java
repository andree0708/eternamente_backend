package com.eternamente.assessment.api;

import com.eternamente.assessment.service.AssessmentService;
import com.eternamente.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

  private final AssessmentService assessmentService;

  public AssessmentController(AssessmentService assessmentService) {
    this.assessmentService = assessmentService;
  }

  /** POST /api/assessments — Guardar resultados de una partida */
  @PostMapping
  public ResponseEntity<ApiResponse<AssessmentResponse>> create(
      @Valid @RequestBody CreateAssessmentRequest request,
      @AuthenticationPrincipal UUID userId
  ) {
    return ApiResponse.created(assessmentService.create(request, userId));
  }

  /** GET /api/assessments/summary — Resumen cognitivo acumulado */
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<CognitiveSummaryResponse>> getSummary(
      @AuthenticationPrincipal UUID userId
  ) {
    return ApiResponse.entity(assessmentService.getSummary(userId));
  }

  /** GET /api/assessments — Listado de evaluaciones del usuario */
  @GetMapping
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getMyAssessments(
      @AuthenticationPrincipal UUID userId
  ) {
    return ApiResponse.entity(assessmentService.getByUser(userId));
  }

  /** GET /api/assessments/{id} — Detalle de una evaluación */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AssessmentResponse>> get(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId
  ) {
    return ApiResponse.entity(assessmentService.get(id, userId));
  }

  /** GET /api/assessments/{id}/analysis — Análisis detallado con IA */
  @GetMapping("/{id}/analysis")
  public ResponseEntity<ApiResponse<AssessmentAnalysisResponse>> getDetailedAnalysis(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId
  ) {
    return ApiResponse.entity(assessmentService.getDetailedAnalysis(id, userId));
  }
}
