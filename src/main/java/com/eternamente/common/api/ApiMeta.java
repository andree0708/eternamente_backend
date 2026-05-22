package com.eternamente.common.api;

import java.time.Instant;

public record ApiMeta(
    Instant timestamp,
    String version
) {
  public static ApiMeta now() {
    return new ApiMeta(Instant.now(), "v1");
  }
}
