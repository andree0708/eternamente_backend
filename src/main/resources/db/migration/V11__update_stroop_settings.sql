UPDATE game_settings
SET settings = jsonb_build_object(
  'rounds', settings->>'rounds',
  'timeLimitSeconds', '5'
)
WHERE game_type = 'stroop';
