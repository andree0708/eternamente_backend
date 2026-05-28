package com.eternamente.gameconfig;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSettingsRepository extends JpaRepository<GameSettings, String> {
}
