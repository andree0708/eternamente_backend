ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS game_type VARCHAR(32);

UPDATE assessment_session
SET game_type = COALESCE(metrics->>'gameType', 'memory')
WHERE game_type IS NULL;

CREATE INDEX IF NOT EXISTS idx_assessment_session_user_created
  ON assessment_session (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_assessment_session_game_type
  ON assessment_session (game_type);

CREATE TABLE IF NOT EXISTS user_cognitive_summary (
  user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
  total_sessions INT NOT NULL DEFAULT 0,
  avg_risk_score DOUBLE PRECISION NOT NULL DEFAULT 0,
  last_played_at TIMESTAMPTZ,
  memory_sessions INT NOT NULL DEFAULT 0,
  stroop_sessions INT NOT NULL DEFAULT 0,
  navigation_sessions INT NOT NULL DEFAULT 0,
  whackamole_sessions INT NOT NULL DEFAULT 0,
  avg_accuracy DOUBLE PRECISION,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
