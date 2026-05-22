package com.eternamente.assessment;

import com.eternamente.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_cognitive_summary")
public class UserCognitiveSummary {

  @Id
  @Column(name = "user_id", columnDefinition = "uuid")
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "total_sessions", nullable = false)
  private int totalSessions;

  @Column(name = "avg_risk_score", nullable = false)
  private double avgRiskScore;

  @Column(name = "last_played_at")
  private Instant lastPlayedAt;

  @Column(name = "memory_sessions", nullable = false)
  private int memorySessions;

  @Column(name = "stroop_sessions", nullable = false)
  private int stroopSessions;

  @Column(name = "navigation_sessions", nullable = false)
  private int navigationSessions;

  @Column(name = "whackamole_sessions", nullable = false)
  private int whackamoleSessions;

  @Column(name = "avg_accuracy")
  private Double avgAccuracy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UserCognitiveSummary() {
  }

  @PrePersist
  @PreUpdate
  void touchTimestamps() {
    updatedAt = Instant.now();
  }

  public UUID getUserId() {
    return userId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
    if (user != null) {
      this.userId = user.getId();
    }
  }

  public int getTotalSessions() {
    return totalSessions;
  }

  public void setTotalSessions(int totalSessions) {
    this.totalSessions = totalSessions;
  }

  public double getAvgRiskScore() {
    return avgRiskScore;
  }

  public void setAvgRiskScore(double avgRiskScore) {
    this.avgRiskScore = avgRiskScore;
  }

  public Instant getLastPlayedAt() {
    return lastPlayedAt;
  }

  public void setLastPlayedAt(Instant lastPlayedAt) {
    this.lastPlayedAt = lastPlayedAt;
  }

  public int getMemorySessions() {
    return memorySessions;
  }

  public void setMemorySessions(int memorySessions) {
    this.memorySessions = memorySessions;
  }

  public int getStroopSessions() {
    return stroopSessions;
  }

  public void setStroopSessions(int stroopSessions) {
    this.stroopSessions = stroopSessions;
  }

  public int getNavigationSessions() {
    return navigationSessions;
  }

  public void setNavigationSessions(int navigationSessions) {
    this.navigationSessions = navigationSessions;
  }

  public int getWhackamoleSessions() {
    return whackamoleSessions;
  }

  public void setWhackamoleSessions(int whackamoleSessions) {
    this.whackamoleSessions = whackamoleSessions;
  }

  public Double getAvgAccuracy() {
    return avgAccuracy;
  }

  public void setAvgAccuracy(Double avgAccuracy) {
    this.avgAccuracy = avgAccuracy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
