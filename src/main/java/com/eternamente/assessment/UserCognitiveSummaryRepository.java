package com.eternamente.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserCognitiveSummaryRepository extends JpaRepository<UserCognitiveSummary, UUID> {
}
