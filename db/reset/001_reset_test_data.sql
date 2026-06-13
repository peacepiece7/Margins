SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM metrics WHERE is_test_data = TRUE;
DELETE FROM messages WHERE is_test_data = TRUE;
DELETE FROM session_insights WHERE is_test_data = TRUE;
DELETE FROM session_tags WHERE is_test_data = TRUE;
DELETE FROM session_highlights WHERE is_test_data = TRUE;
DELETE FROM questions WHERE is_test_data = TRUE;
DELETE FROM personas WHERE is_test_data = TRUE;
DELETE FROM session_windows WHERE is_test_data = TRUE;
DELETE FROM reading_sessions WHERE is_test_data = TRUE;
DELETE FROM book_candidates WHERE is_test_data = TRUE;
DELETE FROM books WHERE is_test_data = TRUE;
DELETE FROM users WHERE is_test_data = TRUE;

SET FOREIGN_KEY_CHECKS = 1;

SOURCE db/seed/001_seed_mvp_data.sql;
