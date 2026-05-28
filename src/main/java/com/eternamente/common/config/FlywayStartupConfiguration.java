package com.eternamente.common.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Repara historial Flyway (p. ej. migración V7 fallida en Render) antes de migrar.
 */
@Configuration
public class FlywayStartupConfiguration {

  private static final Logger log = LoggerFactory.getLogger(FlywayStartupConfiguration.class);

  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return (Flyway flyway) -> {
      log.info("Flyway: ejecutando repair + migrate");
      flyway.repair();
      flyway.migrate();
    };
  }
}
