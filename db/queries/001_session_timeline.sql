SELECT
  rs.id AS session_id,
  rs.title AS session_title,
  sw.id AS window_id,
  sw.window_type,
  sw.title AS window_title,
  m.id AS message_id,
  m.message_order,
  m.role,
  p.display_name AS persona_display_name,
  q.id AS question_id,
  m.content,
  m.created_at
FROM reading_sessions rs
JOIN session_windows sw ON sw.session_id = rs.id
LEFT JOIN messages m ON m.window_id = sw.id AND m.deleted_at IS NULL
LEFT JOIN personas p ON p.id = m.persona_id
LEFT JOIN questions q ON q.id = m.question_id
WHERE rs.id = ? AND rs.deleted_at IS NULL AND sw.deleted_at IS NULL
ORDER BY sw.position, m.message_order, m.id;
