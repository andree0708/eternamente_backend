-- Idempotente para Render/PostgreSQL 18: corrige esquema aunque V7 haya fallado parcialmente

ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS user_external_id VARCHAR(255);
ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS game_type VARCHAR(32);
ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS alert_level VARCHAR(16);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'assessment_session'
      AND column_name = 'user_external_id'
  ) THEN
    ALTER TABLE assessment_session ALTER COLUMN user_external_id DROP NOT NULL;
  END IF;
END $$;

UPDATE assessment_session s
SET user_external_id = u.email
FROM app_user u
WHERE s.user_id = u.id
  AND (s.user_external_id IS NULL OR s.user_external_id = '');

CREATE TABLE IF NOT EXISTS user_ml_feature_snapshot (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  features JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_ml_feature_user_created
  ON user_ml_feature_snapshot (user_id, created_at DESC);
