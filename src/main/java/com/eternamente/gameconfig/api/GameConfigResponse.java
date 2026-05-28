package com.eternamente.gameconfig.api;

import com.eternamente.gameconfig.GameSettings;

import java.time.Instant;
import java.util.Map;

public record GameConfigResponse(
    String gameType,
    Map<String, Object> settings,
    Instant updatedAt
) {
  public static GameConfigResponse from(GameSettings entity) {
    return new GameConfigResponse(
        entity.getGameType(),
        entity.getSettings(),
        entity.getUpdatedAt()
    );
  }
}
