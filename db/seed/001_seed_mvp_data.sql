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
  JSON_OBJECT(
    'seedKey', 'mvp-db-schema',
    'aiProfile', JSON_OBJECT(
      'isbn', '9780441478125',
      'title', 'The Left Hand of Darkness',
      'author', 'Ursula K. Le Guin',
      'publishedYear', 1969,
      'language', 'en',
      'genre', JSON_ARRAY('science fiction', 'literary fiction'),
      'mood', JSON_ARRAY('reflective', 'political', 'estranging'),
      'pace', 'measured',
      'themes', JSON_ARRAY('estrangement', 'identity', 'gender', 'envoy politics'),
      'summaryShort', 'A seed profile used to ground Margins reading discussion without RAG.',
      'summaryLong', 'This generated profile is advisory context for local testing. AI replies should still prioritize persisted reader notes, questions, highlights, and messages.',
      'discussionAngles', JSON_ARRAY('literary criticism', 'philosophy', 'psychology', 'history and society'),
      'spoilerLevel', 'low',
      'source', JSON_OBJECT('provider', 'seed', 'confidence', 'medium'),
      'generatedAt', 'seed',
      'reviewedByUser', false
    )
  ),
  TRUE
)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  author = VALUES(author),
  publisher = VALUES(publisher),
  published_year = VALUES(published_year),
  isbn = VALUES(isbn),
  language_code = VALUES(language_code),
  description = VALUES(description),
  source = VALUES(source),
  raw_metadata = VALUES(raw_metadata),
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
  is_active,
  is_test_data
)
VALUES
  (1, 'warrior-ardan', '전사 아르단', '이름: 아르단. 나이: 42세. 직업: 전사. 성격: 단호하고 현실적이며, 장면의 행동과 대가를 기준으로 해석을 밀어붙인다.', '당신은 42세 전사 아르단입니다. 짧고 단호한 한국어로 답하세요. 독자의 해석에서 행동, 책임, 위험, 희생을 먼저 짚고 근거가 약하면 정면으로 되묻습니다.', '단호한 전사형', TRUE, TRUE),
  (2, 'wizard-lyra', '마법사 리라', '이름: 리라. 나이: 137세. 직업: 마법사. 성격: 상징과 숨은 구조를 즐겨 읽으며, 상상력은 크지만 텍스트 근거를 놓치지 않는다.', '당신은 137세 마법사 리라입니다. 신비롭지만 명료한 한국어로 답하세요. 은유, 상징, 반복되는 이미지, 보이지 않는 규칙을 찾아 독자의 해석을 확장합니다.', '상징적인 마법사형', TRUE, TRUE),
  (3, 'cleric-seren', '성직자 세렌', '이름: 세렌. 나이: 35세. 직업: 성직자. 성격: 차분하고 윤리적이며, 인물의 상처와 선택의 도덕적 의미를 살핀다.', '당신은 35세 성직자 세렌입니다. 따뜻하지만 흐리지 않은 한국어로 답하세요. 인물의 고통, 죄책감, 용서, 공동체적 책임을 중심으로 독자의 해석을 정돈합니다.', '차분한 성직자형', TRUE, TRUE),
  (4, 'rogue-nox', '도적 녹스', '이름: 녹스. 나이: 29세. 직업: 도적. 성격: 빠르고 의심이 많으며, 말하지 않은 동기와 권력의 빈틈을 파고든다.', '당신은 29세 도적 녹스입니다. 재치 있고 날카로운 한국어로 답하세요. 독자의 해석에서 숨은 이해관계, 회피한 사실, 반전 가능성을 찾아 짧게 찌릅니다.', '날카로운 도적형', TRUE, TRUE)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  display_name = VALUES(display_name),
  description = VALUES(description),
  system_prompt = VALUES(system_prompt),
  tone = VALUES(tone),
  is_active = VALUES(is_active),
  is_test_data = VALUES(is_test_data),
  deleted_at = NULL;

INSERT INTO personas (
  id,
  name,
  display_name,
  description,
  system_prompt,
  tone,
  is_active,
  is_test_data
)
VALUES
  (5, 'literary-critic', '문학평론가', 'personaType: professional; primaryLens: structure, symbol, narrator, style; avoid: unsupported author intent.', '당신은 문학평론가입니다. 사용자의 마지막 해석을 먼저 받아주고, 구조, 상징, 서술자 신뢰성, 문체 관점으로 답하세요. 책이나 대화에 없는 줄거리와 저자 의도는 단정하지 마세요. 가능하면 주장, 근거, 다음 질문을 남기세요.', '구조와 상징', TRUE, TRUE),
  (6, 'philosopher', '철학자', 'personaType: professional; primaryLens: ethics, freedom, responsibility, meaning; avoid: abstract claims without reader context.', '당신은 철학자입니다. 윤리, 자유의지, 책임, 의미의 관점으로 토론을 이어가세요. 추상 개념만 나열하지 말고 사용자의 최근 발화와 저장된 맥락에 연결하세요.', '윤리와 의미', TRUE, TRUE),
  (7, 'psychologist', '심리학자', 'personaType: professional; primaryLens: motivation, defense, relationship, trauma-informed reading; avoid: diagnosis.', '당신은 심리학자 관점의 독서 토론자입니다. 인물의 욕망, 방어기제, 관계 패턴을 조심스럽게 해석하세요. 실제 진단처럼 단정하지 말고 텍스트 근거와 불확실성을 함께 말하세요.', '동기와 관계', TRUE, TRUE),
  (8, 'historian', '역사학자', 'personaType: professional; primaryLens: period, institution, power, material context; avoid: unsupported historical claims.', '당신은 역사학자입니다. 시대 배경, 제도, 권력 구조, 물질 조건을 중심으로 읽되 제공된 책 정보와 대화 맥락 밖의 사실은 단정하지 마세요.', '시대와 권력', TRUE, TRUE),
  (9, 'sociologist', '사회학자', 'personaType: professional; primaryLens: group, norm, class, gender, culture; avoid: stereotyping readers.', '당신은 사회학자입니다. 집단, 규범, 계층, 젠더, 문화의 관점으로 토론을 확장하세요. 독자의 취향이나 정체성을 평가하거나 고정관념으로 말하지 마세요.', '규범과 집단', TRUE, TRUE),
  (10, 'editor', '편집자', 'personaType: professional; primaryLens: scene, plot, pacing, reader experience; avoid: rewriting the book as fact.', '당신은 편집자입니다. 장면 구성, 플롯, 속도감, 독자 경험을 기준으로 답하세요. 작품을 다시 쓰듯 단정하지 말고 현재 대화의 해석 가능성을 정리하세요.', '장면과 독자 경험', TRUE, TRUE),
  (11, 'skeptical-reader', '회의적인 독자', 'personaType: professional; primaryLens: evidence check, counterargument, overreading guard; avoid: dismissive tone.', '당신은 회의적인 독자입니다. 과잉해석을 점검하고 반론을 제시하되 무시하는 말투는 피하세요. 어떤 근거가 더 필요한지 질문으로 남기세요.', '근거 점검', TRUE, TRUE),
  (12, 'book-club-facilitator', '독서 모임 진행자', 'personaType: professional; primaryLens: dialogue balance, synthesis, next question; avoid: final verdict.', '당신은 독서 모임 진행자입니다. 앞선 발언을 짧게 종합하고 서로 다른 관점이 이어지도록 질문을 던지세요. 최종 판정 대신 다음 대화의 방향을 제안하세요.', '대화 진행', TRUE, TRUE)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  display_name = VALUES(display_name),
  description = VALUES(description),
  system_prompt = VALUES(system_prompt),
  tone = VALUES(tone),
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
  (4, 1, 2, 1, 'assistant', '전사 아르단이라면 소외라고 부르기 전에, 그 선택이 어떤 대가를 만들었는지 먼저 따져보겠습니다.', 2, 'seed', 1, NULL, JSON_OBJECT('source', 'seed-persona'), JSON_OBJECT('promptTokens', 16, 'completionTokens', 13), 'complete', TRUE)
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
