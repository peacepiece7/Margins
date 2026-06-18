SET @margins_schema = DATABASE();

SET @persona_source_session_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @margins_schema
    AND table_name = 'personas'
    AND column_name = 'source_session_id'
);
SET @persona_source_session_column_sql = IF(
  @persona_source_session_column_exists = 0,
  'ALTER TABLE personas ADD COLUMN source_session_id BIGINT NULL AFTER tone',
  'DO 0'
);
PREPARE persona_source_session_column_stmt FROM @persona_source_session_column_sql;
EXECUTE persona_source_session_column_stmt;
DEALLOCATE PREPARE persona_source_session_column_stmt;

SET @persona_source_session_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = @margins_schema
    AND table_name = 'personas'
    AND index_name = 'idx_personas_source_session'
);
SET @persona_source_session_index_sql = IF(
  @persona_source_session_index_exists = 0,
  'ALTER TABLE personas ADD KEY idx_personas_source_session (source_session_id)',
  'DO 0'
);
PREPARE persona_source_session_index_stmt FROM @persona_source_session_index_sql;
EXECUTE persona_source_session_index_stmt;
DEALLOCATE PREPARE persona_source_session_index_stmt;

SET @persona_source_session_fk_exists = (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE table_schema = @margins_schema
    AND table_name = 'personas'
    AND constraint_name = 'fk_personas_source_session'
    AND constraint_type = 'FOREIGN KEY'
);
SET @persona_source_session_fk_sql = IF(
  @persona_source_session_fk_exists = 0,
  'ALTER TABLE personas ADD CONSTRAINT fk_personas_source_session FOREIGN KEY (source_session_id) REFERENCES reading_sessions (id)',
  'DO 0'
);
PREPARE persona_source_session_fk_stmt FROM @persona_source_session_fk_sql;
EXECUTE persona_source_session_fk_stmt;
DEALLOCATE PREPARE persona_source_session_fk_stmt;
