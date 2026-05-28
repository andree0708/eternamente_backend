package com.eternamente.gameconfig.api;

import com.eternamente.common.api.ApiResponse;
import com.eternamente.gameconfig.GameConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game-config")
public class GameConfigController {

  private final GameConfigService gameConfigService;

  public GameConfigController(GameConfigService gameConfigService) {
    this.gameConfigService = gameConfigService;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<GameConfigResponse>>> getAll() {
    return ApiResponse.entity(gameConfigService.getAll());
  }

  @GetMapping("/{gameType}")
  public ResponseEntity<ApiResponse<GameConfigResponse>> getOne(@PathVariable String gameType) {
    return ApiResponse.entity(gameConfigService.getByGameType(gameType));
  }

  @PutMapping("/{gameType}")
  public ResponseEntity<ApiResponse<GameConfigResponse>> update(
      @PathVariable String gameType,
      @RequestBody Map<String, Object> settings
  ) {
    return ApiResponse.entity(gameConfigService.update(gameType, settings));
  }
}
