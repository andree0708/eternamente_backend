package com.eternamente.user.service;

/**
 * El cliente envía la contraseña como SHA-256 en hexadecimal (64 caracteres).
 * El servidor aplica BCrypt sobre ese valor para no almacenar texto plano ni el hash crudo.
 */
final class ClientPasswordSupport {

  private static final int SHA256_HEX_LENGTH = 64;

  private ClientPasswordSupport() {
  }

  static String normalizeForStorage(String passwordFromClient) {
    if (passwordFromClient == null) {
      return "";
    }
    String trimmed = passwordFromClient.trim();
    if (isSha256Hex(trimmed)) {
      return trimmed.toLowerCase();
    }
    return trimmed;
  }

  private static boolean isSha256Hex(String value) {
    if (value.length() != SHA256_HEX_LENGTH) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      boolean hex = (c >= '0' && c <= '9')
          || (c >= 'a' && c <= 'f')
          || (c >= 'A' && c <= 'F');
      if (!hex) {
        return false;
      }
    }
    return true;
  }
}
