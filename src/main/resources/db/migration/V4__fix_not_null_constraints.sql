ALTER TABLE assessment_session ALTER COLUMN user_external_id DROP NOT NULL;
ALTER TABLE assessment_session ALTER COLUMN user_id SET NOT NULL;