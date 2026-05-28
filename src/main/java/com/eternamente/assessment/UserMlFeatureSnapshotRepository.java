package com.eternamente.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserMlFeatureSnapshotRepository extends JpaRepository<UserMlFeatureSnapshot, UUID> {
}
