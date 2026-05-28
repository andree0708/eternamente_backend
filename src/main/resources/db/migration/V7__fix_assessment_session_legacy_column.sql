-- Render/legacy: user_external_id puede seguir NOT NULL en BD antiguas
ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS user_external_id VARCHAR(255);

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
