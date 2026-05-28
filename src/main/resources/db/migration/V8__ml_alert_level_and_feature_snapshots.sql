ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS alert_level VARCHAR(16);

CREATE TABLE IF NOT EXISTS user_ml_feature_snapshot (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  features JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_ml_feature_user_created
  ON user_ml_feature_snapshot (user_id, created_at DESC);
