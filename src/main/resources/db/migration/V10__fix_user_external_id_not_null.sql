-- Render/legacy: Hibernate puede omitir user_external_id en INSERT; la columna no debe bloquear el guardado.

ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS user_external_id VARCHAR(255);
ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS game_type VARCHAR(32);
ALTER TABLE assessment_session ADD COLUMN IF NOT EXISTS alert_level VARCHAR(16);

ALTER TABLE assessment_session ALTER COLUMN user_external_id DROP NOT NULL;

UPDATE assessment_session s
SET user_external_id = COALESCE(
  NULLIF(TRIM(s.user_external_id), ''),
  u.email,
  s.user_id::text
)
FROM app_user u
WHERE s.user_id = u.id
  AND (s.user_external_id IS NULL OR TRIM(s.user_external_id) = '');

UPDATE assessment_session
SET user_external_id = user_id::text
WHERE user_external_id IS NULL OR TRIM(user_external_id) = '';

ALTER TABLE assessment_session ALTER COLUMN user_external_id SET DEFAULT '';
