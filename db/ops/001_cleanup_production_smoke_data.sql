-- Soft-delete production smoke data left behind by interrupted mutable smoke tests.
-- This script intentionally matches only the operational smoke title prefix.

START TRANSACTION;

SET @smoke_title_pattern := _utf8mb4'Margins Smoke %' COLLATE utf8mb4_unicode_ci;
SET @cleanup_at := CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE IF EXISTS tmp_smoke_books;
CREATE TEMPORARY TABLE tmp_smoke_books (
  id BIGINT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_smoke_books (id)
SELECT id
FROM books
WHERE title LIKE @smoke_title_pattern;

DROP TEMPORARY TABLE IF EXISTS tmp_smoke_sessions;
CREATE TEMPORARY TABLE tmp_smoke_sessions (
  id BIGINT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_smoke_sessions (id)
SELECT id
FROM reading_sessions
WHERE book_id IN (SELECT id FROM tmp_smoke_books);

DROP TEMPORARY TABLE IF EXISTS tmp_smoke_windows;
CREATE TEMPORARY TABLE tmp_smoke_windows (
  id BIGINT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_smoke_windows (id)
SELECT id
FROM session_windows
WHERE session_id IN (SELECT id FROM tmp_smoke_sessions);

DROP TEMPORARY TABLE IF EXISTS tmp_smoke_questions;
CREATE TEMPORARY TABLE tmp_smoke_questions (
  id BIGINT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_smoke_questions (id)
SELECT id
FROM questions
WHERE session_id IN (SELECT id FROM tmp_smoke_sessions)
   OR window_id IN (SELECT id FROM tmp_smoke_windows);

SELECT 'books' AS target, COUNT(*) AS active_rows
FROM books
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_books)
UNION ALL
SELECT 'reading_sessions', COUNT(*)
FROM reading_sessions
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_sessions)
UNION ALL
SELECT 'session_windows', COUNT(*)
FROM session_windows
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_windows)
UNION ALL
SELECT 'questions', COUNT(*)
FROM questions
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_questions);

UPDATE messages
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND (
    session_id IN (SELECT id FROM tmp_smoke_sessions)
    OR window_id IN (SELECT id FROM tmp_smoke_windows)
    OR question_id IN (SELECT id FROM tmp_smoke_questions)
  );

UPDATE questions
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_questions);

UPDATE session_highlights
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND (
    book_id IN (SELECT id FROM tmp_smoke_books)
    OR session_id IN (SELECT id FROM tmp_smoke_sessions)
  );

UPDATE session_tags
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND session_id IN (SELECT id FROM tmp_smoke_sessions);

UPDATE session_insights
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND session_id IN (SELECT id FROM tmp_smoke_sessions);

UPDATE session_windows
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_windows);

UPDATE reading_sessions
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_sessions);

UPDATE books
SET deleted_at = COALESCE(deleted_at, @cleanup_at)
WHERE deleted_at IS NULL
  AND id IN (SELECT id FROM tmp_smoke_books);

DELETE FROM metrics
WHERE book_id IN (SELECT id FROM tmp_smoke_books)
   OR session_id IN (SELECT id FROM tmp_smoke_sessions)
   OR window_id IN (SELECT id FROM tmp_smoke_windows)
   OR question_id IN (SELECT id FROM tmp_smoke_questions);

COMMIT;
