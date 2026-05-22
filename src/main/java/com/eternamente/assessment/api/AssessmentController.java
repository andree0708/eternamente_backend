package com.eternamente.assessment.api;

import com.eternamente.assessment.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

  @PostMapping
  public ResponseEntity<AssessmentResponse> create(
      @Valid @RequestBody CreateAssessmentRequest request,
      @AuthenticationPrincipal UUID userId
  ) {
    AssessmentResponse created = assessmentService.create(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/summary")
  public CognitiveSummaryResponse getSummary(@AuthenticationPrincipal UUID userId) {
    return assessmentService.getSummary(userId);
  }

  @GetMapping("/{id}")
  public AssessmentResponse get(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId
  ) {
    return assessmentService.get(id, userId);
  }

  @GetMapping
  public List<AssessmentResponse> getMyAssessments(@AuthenticationPrincipal UUID userId) {
    return assessmentService.getByUser(userId);
  }

  @GetMapping("/{id}/analysis")
  public AssessmentAnalysisResponse getDetailedAnalysis(
      @PathVariable UUID id,
      @AuthenticationPrincipal UUID userId
  ) {
    return assessmentService.getDetailedAnalysis(id, userId);
  }
}
