package com.eternamente.user.api;

import com.eternamente.user.User;

import java.util.UUID;

public record AuthResponse(String token, String type, UUID userId, String email, String fullName) {
  public AuthResponse(String token, User user) {
    this(token, "Bearer", user.getId(), user.getEmail(), user.getFullName());
  }
  
  public AuthResponse(String token) {
    this(token, "Bearer", null, null, null);
  }
}
