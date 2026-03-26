package com.eternamente.user.api;

import com.eternamente.user.User;
import com.eternamente.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    UserRole role,
    String fullName,
    Instant createdAt
) {
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getRole(),
        user.getFullName(),
        user.getCreatedAt()
    );
  }
}
