package com.eternamente.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, UUID> {

    @Query("SELECT s FROM AssessmentSession s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<AssessmentSession> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM AssessmentSession s WHERE s.id = :id AND s.user.id = :userId")
    java.util.Optional<AssessmentSession> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT s FROM AssessmentSession s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<AssessmentSession> findTop10ByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("""
        SELECT s FROM AssessmentSession s
        WHERE s.user.id = :userId AND s.createdAt >= :after
        ORDER BY s.createdAt ASC
        """)
    List<AssessmentSession> findRecentByUserId(@Param("userId") UUID userId, @Param("after") Instant after);
}

