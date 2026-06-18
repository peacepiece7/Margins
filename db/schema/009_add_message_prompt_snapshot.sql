SET @margins_schema = DATABASE();

SET @message_prompt_snapshot_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @margins_schema
    AND table_name = 'messages'
    AND column_name = 'prompt_snapshot'
);
SET @message_prompt_snapshot_column_sql = IF(
  @message_prompt_snapshot_column_exists = 0,
  'ALTER TABLE messages ADD COLUMN prompt_snapshot JSON NULL AFTER question_id',
  'DO 0'
);
PREPARE message_prompt_snapshot_column_stmt FROM @message_prompt_snapshot_column_sql;
EXECUTE message_prompt_snapshot_column_stmt;
DEALLOCATE PREPARE message_prompt_snapshot_column_stmt;
