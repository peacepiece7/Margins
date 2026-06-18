SET @margins_schema = DATABASE();

SET @persona_role_key_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @margins_schema
    AND table_name = 'personas'
    AND column_name = 'role_key'
);
SET @persona_role_key_column_sql = IF(
  @persona_role_key_column_exists = 0,
  'ALTER TABLE personas ADD COLUMN role_key VARCHAR(80) NULL AFTER tone',
  'DO 0'
);
PREPARE persona_role_key_column_stmt FROM @persona_role_key_column_sql;
EXECUTE persona_role_key_column_stmt;
DEALLOCATE PREPARE persona_role_key_column_stmt;

SET @persona_session_role_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @margins_schema
    AND table_name = 'personas'
    AND index_name = 'idx_personas_session_role'
);
SET @persona_session_role_index_sql = IF(
  @persona_session_role_index_exists = 0,
  'ALTER TABLE personas ADD KEY idx_personas_session_role (source_session_id, role_key)',
  'DO 0'
);
PREPARE persona_session_role_index_stmt FROM @persona_session_role_index_sql;
EXECUTE persona_session_role_index_stmt;
DEALLOCATE PREPARE persona_session_role_index_stmt;
