package com.eternamente.assessment.ml;

import java.util.Set;

/**
 * Mapeo de los 8 juegos web a dominios cognitivos (documento de migración §3).
 */
public final class GameDomainMapper {

  public enum Domain {
    MEMORY,
    ATTENTION,
    EXECUTIVE,
    LANGUAGE,
    ORIENTATION,
    PROCESSING_SPEED
  }

  private static final Set<String> MEMORY_GAMES = Set.of("memory", "digitspan", "corsi");
  private static final Set<String> ATTENTION_GAMES = Set.of("whackamole");
  private static final Set<String> EXECUTIVE_GAMES = Set.of("stroop", "navigation");
  private static final Set<String> ORIENTATION_GAMES = Set.of("orientation");
  private static final Set<String> SPEED_GAMES = Set.of("arithmetic");

  private GameDomainMapper() {
  }

  public static Domain domainOf(String gameType) {
    if (gameType == null) {
      return Domain.MEMORY;
    }
    String g = gameType.toLowerCase();
    if (MEMORY_GAMES.contains(g)) return Domain.MEMORY;
    if (ATTENTION_GAMES.contains(g)) return Domain.ATTENTION;
    if (EXECUTIVE_GAMES.contains(g)) return Domain.EXECUTIVE;
    if (ORIENTATION_GAMES.contains(g)) return Domain.ORIENTATION;
    if (SPEED_GAMES.contains(g)) return Domain.PROCESSING_SPEED;
    return Domain.MEMORY;
  }
}
