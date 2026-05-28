-- Render/legacy: user_external_id quedó NOT NULL pero la app solo usa user_id
ALTER TABLE assessment_session ALTER COLUMN user_external_id DROP NOT NULL;

UPDATE assessment_session s
SET user_external_id = u.email
FROM app_user u
WHERE s.user_id = u.id
  AND (s.user_external_id IS NULL OR s.user_external_id = '');
