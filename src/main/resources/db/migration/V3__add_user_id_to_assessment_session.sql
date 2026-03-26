ALTER TABLE assessment_session ADD COLUMN user_id UUID REFERENCES app_user(id);

CREATE INDEX IF NOT EXISTS idx_assessment_session_user_id ON assessment_session (user_id);
