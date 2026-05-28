package com.eternamente.gameconfig;

import com.eternamente.gameconfig.api.GameConfigResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class GameConfigService {

  private final GameSettingsRepository repository;

  public GameConfigService(GameSettingsRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<GameConfigResponse> getAll() {
    return repository.findAll().stream()
        .map(GameConfigResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public GameConfigResponse getByGameType(String gameType) {
    return repository.findById(gameType)
        .map(GameConfigResponse::from)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No hay configuración para el juego: " + gameType
        ));
  }

  @Transactional
  public GameConfigResponse update(String gameType, Map<String, Object> settings) {
    GameSettings entity = repository.findById(gameType)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No hay configuración para el juego: " + gameType
        ));
    entity.setSettings(settings);
    return GameConfigResponse.from(repository.save(entity));
  }
}
