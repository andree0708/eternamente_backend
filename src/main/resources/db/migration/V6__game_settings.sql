CREATE TABLE IF NOT EXISTS game_settings (
  game_type VARCHAR(32) PRIMARY KEY,
  settings JSONB NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO game_settings (game_type, settings) VALUES
  ('memory', '{"pairsEasy":6,"pairsMedium":8,"pairsHard":10,"colsEasy":3,"colsMedium":4,"colsHard":4}'),
  ('stroop', '{"rounds":20,"wordDisplayMs":4000}'),
  ('navigation', '{"maxLevel":5,"gridSize":5}'),
  ('whackamole', '{"rounds":30,"gridSize":3,"showMs":1400,"distractorChance":0.35}'),
  ('digitspan', '{"maxLevel":5,"sequenceStart":3,"displayMsPerDigit":900,"responseTimeoutMs":12000}'),
  ('corsi', '{"maxLevel":5,"gridSize":3,"flashMs":600,"gapMs":400}'),
  ('orientation', '{"questionsPerSession":5}'),
  ('arithmetic', '{"rounds":15,"timeLimitSeconds":8,"maxOperand":20}')
ON CONFLICT (game_type) DO NOTHING;
