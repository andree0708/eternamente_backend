package com.eternamente.assessment;

import com.eternamente.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_session")
public class AssessmentSession {

  @Id
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "age", nullable = false)
  private Integer age;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metrics", nullable = false, columnDefinition = "jsonb")
  private String metricsJson;

  @Column(name = "model_version", nullable = false, length = 64)
  private String modelVersion;

  @Column(name = "risk_score", nullable = false)
  private Double riskScore;

  @Column(name = "predicted_dcl", nullable = false)
  private Boolean predictedDcl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "ml_run_at", nullable = false)
  private Instant mlRunAt;

  public AssessmentSession() {
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (mlRunAt == null) {
      mlRunAt = Instant.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getMetricsJson() {
    return metricsJson;
  }

  public void setMetricsJson(String metricsJson) {
    this.metricsJson = metricsJson;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  public Double getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(Double riskScore) {
    this.riskScore = riskScore;
  }

  public Boolean getPredictedDcl() {
    return predictedDcl;
  }

  public void setPredictedDcl(Boolean predictedDcl) {
    this.predictedDcl = predictedDcl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getMlRunAt() {
    return mlRunAt;
  }

  public void setMlRunAt(Instant mlRunAt) {
    this.mlRunAt = mlRunAt;
  }
}

