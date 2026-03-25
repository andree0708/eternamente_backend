CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS assessment_session (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_external_id VARCHAR(255) NOT NULL,
  age INT NOT NULL,
  metrics JSONB NOT NULL,
  model_version VARCHAR(64) NOT NULL,
  risk_score DOUBLE PRECISION NOT NULL,
  predicted_dcl BOOLEAN NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ml_run_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_assessment_session_created_at
  ON assessment_session (created_at);

