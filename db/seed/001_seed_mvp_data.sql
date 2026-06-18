INSERT INTO users (id, username, display_name, auth_provider, is_test_data)
VALUES (1, 'test-reader', 'Test Reader', 'local', TRUE)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  auth_provider = VALUES(auth_provider),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO books (
  id,
  user_id,
  title,
  author,
  publisher,
  published_year,
  isbn,
  language_code,
  description,
  source,
  raw_metadata,
  is_test_data
)
VALUES (
  1,
  1,
  'The Left Hand of Darkness',
  'Ursula K. Le Guin',
  'Ace',
  1969,
  '9780441478125',
  'en',
  'Seed book for Margins MVP reading session flows.',
  'seed',
  JSON_OBJECT('seedKey', 'mvp-db-schema'),
  TRUE
)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  author = VALUES(author),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO book_candidates (
  id,
  user_id,
  query_text,
  candidate_rank,
  title,
  author,
  confidence_score,
  ai_model,
  response_snapshot,
  selected_book_id,
  is_test_data
)
VALUES (
  1,
  1,
  'left hand darkness',
  1,
  'The Left Hand of Darkness',
  'Ursula K. Le Guin',
  0.98000,
  'seed',
  JSON_OBJECT('reason', 'deterministic seed candidate'),
  1,
  TRUE
)
ON DUPLICATE KEY UPDATE
  selected_book_id = VALUES(selected_book_id),
  is_test_data = VALUES(is_test_data);

INSERT INTO reading_sessions (
  id,
  user_id,
  book_id,
  title,
  status,
  is_pinned,
  reading_goal,
  start_page,
  current_page,
  target_page,
  progress_note,
  summary,
  context_snapshot,
  is_test_data
)
VALUES (
  1,
  1,
  1,
  'Seed reflection session',
  'active',
  FALSE,
  'Notice how estrangement changes the narrator.',
  1,
  42,
  120,
  'Seed progress note for MVP session tracking.',
  'Deterministic MVP seed session.',
  JSON_OBJECT('seedKey', 'mvp-db-schema'),
  TRUE
)
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  is_pinned = VALUES(is_pinned),
  reading_goal = VALUES(reading_goal),
  start_page = VALUES(start_page),
  current_page = VALUES(current_page),
  target_page = VALUES(target_page),
  progress_note = VALUES(progress_note),
  summary = VALUES(summary),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO session_windows (
  id,
  session_id,
  user_id,
  window_type,
  title,
  position,
  status,
  context_snapshot,
  is_test_data
)
VALUES
  (1, 1, 1, 'question', 'Reflection Question', 1, 'open', JSON_OBJECT('seedKey', 'question-window'), TRUE),
  (2, 1, 1, 'debate', 'Persona Debate', 2, 'open', JSON_OBJECT('seedKey', 'debate-window'), TRUE)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  position = VALUES(position),
  status = VALUES(status),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO personas (
  id,
  name,
  display_name,
  description,
  system_prompt,
  tone,
  role_key,
  source_session_id,
  is_active,
  is_test_data
)
VALUES
  (1, 'careful-critic', 'Careful Critic', 'Challenges vague reflections with textual evidence.', 'Respond as a careful literary critic. Ask for evidence and clarify claims.', 'critical', 'skeptic', NULL, TRUE, TRUE),
  (2, 'empathetic-reader', 'Empathetic Reader', 'Explores emotional and personal reading responses.', 'Respond as an empathetic reader. Connect themes to lived experience.', 'warm', 'empathy_reader', NULL, TRUE, TRUE)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  description = VALUES(description),
  system_prompt = VALUES(system_prompt),
  role_key = VALUES(role_key),
  source_session_id = VALUES(source_session_id),
  is_active = VALUES(is_active),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO questions (
  id,
  session_id,
  window_id,
  user_id,
  question_text,
  question_type,
  status,
  ai_model,
  prompt_snapshot,
  context_snapshot,
  is_test_data
)
VALUES (
  1,
  1,
  1,
  1,
  'What tension in the book feels most important to your reflection?',
  'reflection',
  'active',
  'seed',
  JSON_OBJECT('seedKey', 'question-prompt'),
  JSON_OBJECT('bookId', 1, 'sessionId', 1),
  TRUE
)
ON DUPLICATE KEY UPDATE
  question_text = VALUES(question_text),
  status = VALUES(status),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO messages (
  id,
  session_id,
  window_id,
  user_id,
  role,
  content,
  message_order,
  ai_model,
  persona_id,
  question_id,
  context_snapshot,
  token_usage,
  streaming_status,
  is_test_data
)
VALUES
  (1, 1, 1, 1, 'user', 'The tension between belonging and estrangement stands out.', 1, NULL, NULL, 1, JSON_OBJECT('source', 'seed-user'), NULL, 'complete', TRUE),
  (2, 1, 1, 1, 'assistant', 'What scene best shows that tension for you?', 2, 'seed', NULL, 1, JSON_OBJECT('source', 'seed-ai'), JSON_OBJECT('promptTokens', 12, 'completionTokens', 9), 'complete', TRUE),
  (3, 1, 2, 1, 'user', 'How would a critic challenge my reading?', 1, NULL, NULL, NULL, JSON_OBJECT('source', 'seed-debate-user'), NULL, 'complete', TRUE),
  (4, 1, 2, 1, 'assistant', 'I would ask which passages support estrangement rather than simple isolation.', 2, 'seed', 1, NULL, JSON_OBJECT('source', 'seed-persona'), JSON_OBJECT('promptTokens', 16, 'completionTokens', 13), 'complete', TRUE)
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  message_order = VALUES(message_order),
  token_usage = VALUES(token_usage),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO session_highlights (
  id,
  session_id,
  book_id,
  user_id,
  page_number,
  location_label,
  quote_text,
  note,
  highlight_order,
  is_test_data
)
VALUES (
  1,
  1,
  1,
  1,
  42,
  'Chapter 1',
  'The tension between belonging and estrangement stands out.',
  'Seed highlight tied to the opening reflection.',
  1,
  TRUE
)
ON DUPLICATE KEY UPDATE
  page_number = VALUES(page_number),
  location_label = VALUES(location_label),
  quote_text = VALUES(quote_text),
  note = VALUES(note),
  highlight_order = VALUES(highlight_order),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO metrics (
  id,
  user_id,
  book_id,
  session_id,
  window_id,
  question_id,
  persona_id,
  metric_name,
  metric_scope,
  metric_period_start,
  metric_period_end,
  metric_value,
  metric_unit,
  metric_details,
  source_ref,
  generated_by,
  is_test_data
)
VALUES (
  1,
  1,
  1,
  1,
  NULL,
  NULL,
  NULL,
  'message_count',
  'session',
  CURRENT_DATE,
  CURRENT_DATE,
  4,
  'messages',
  JSON_OBJECT('source', 'seed', 'windowCount', 2),
  'seed:mvp-db-schema',
  'seed',
  TRUE
)
ON DUPLICATE KEY UPDATE
  metric_value = VALUES(metric_value),
  metric_details = VALUES(metric_details),
  is_test_data = VALUES(is_test_data);
