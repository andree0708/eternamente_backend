package com.eternamente.user.api;

public record AuthResponse(String token, String type) {
  public AuthResponse(String token) {
    this(token, "Bearer");
  }
}
