export interface CreateReadingSessionResponse {
  sessionId: number;
  bookId: number;
  title: string;
  status: string;
}

export interface CreateSessionWindowResponse {
  windowId: number;
  sessionId: number;
  windowType: string;
  title: string;
  status: string;
}

export interface AiMessageResponse {
  messageId: number;
  windowId: number;
  personaId?: number;
  role: string;
  content: string;
  streamingReady: boolean;
  aiModel: string;
}

export interface AiMessageListResponse {
  messages: AiMessageResponse[];
}

export interface SessionWindowTimeline {
  windowId: number;
  sessionId: number;
  windowType: string;
  title: string;
  position: number;
  status: string;
}

export interface SessionMessage {
  messageId: number;
  sessionId: number;
  windowId: number;
  parentMessageId?: number;
  role: string;
  content: string;
  messageOrder: number;
  aiModel?: string;
  personaId?: number;
  questionId?: number;
  streamingStatus: string;
}

export interface Question {
  questionId: number;
  sessionId: number;
  windowId: number;
  questionText: string;
  questionType: string;
  status: string;
  aiModel?: string;
}

export interface QuestionListResponse {
  questions: Question[];
}

export interface SessionHighlight {
  highlightId: number;
  sessionId: number;
  bookId: number;
  pageNumber?: number;
  locationLabel?: string;
  quoteText: string;
  note?: string;
  highlightOrder: number;
}

export interface SessionTag {
  tagId: number;
  sessionId: number;
  label: string;
}

export interface SessionInsight {
  insightId: number;
  sessionId: number;
  insightType: string;
  title?: string;
  content: string;
  evidence?: string;
  insightOrder: number;
}

export interface ReadingSessionTimelineResponse {
  sessionId: number;
  bookId: number;
  bookTitle: string;
  bookAuthor?: string | null;
  title: string;
  status: string;
  pinned: boolean;
  readingGoal?: string | null;
  startPage?: number | null;
  currentPage?: number | null;
  targetPage?: number | null;
  progressPercent?: number | null;
  progressNote?: string | null;
  summary?: string | null;
  stats: ReadingSessionStats;
  nextActions: ReadingSessionNextAction[];
  windows: SessionWindowTimeline[];
  highlights: SessionHighlight[];
  tags: SessionTag[];
  insights: SessionInsight[];
  questions: Question[];
  messages: SessionMessage[];
}

export interface ReadingSessionNextAction {
  actionId: string;
  label: string;
  detail: string;
  targetWindowId?: number;
  targetQuestionId?: number;
}

export interface ReadingSessionStats {
  windowCount: number;
  questionCount: number;
  answeredQuestionCount: number;
  messageCount: number;
  personaResponseCount: number;
  personaCount: number;
}

export interface ReadingSessionSummary {
  sessionId: number;
  bookId: number;
  bookTitle: string;
  bookAuthor?: string | null;
  title: string;
  status: string;
  pinned: boolean;
  readingGoal?: string | null;
  startPage?: number | null;
  currentPage?: number | null;
  targetPage?: number | null;
  progressPercent?: number | null;
  summary?: string | null;
  windowCount: number;
  questionCount: number;
  answeredQuestionCount: number;
  highlightCount: number;
  messageCount: number;
  tags: SessionTag[];
}

export interface ReadingSessionListResponse {
  sessions: ReadingSessionSummary[];
}

export interface ReadingLibraryStatsResponse {
  sessionCount: number;
  activeSessionCount: number;
  completedSessionCount: number;
  distinctBookCount: number;
  answeredQuestionCount: number;
  highlightCount: number;
  messageCount: number;
  averageProgressPercent?: number | null;
}

export interface SessionSearchResult {
  sessionId: number;
  sourceId: number;
  resultType: string;
  bookTitle: string;
  sessionTitle: string;
  snippet: string;
}

export interface SessionSearchResponse {
  query: string;
  results: SessionSearchResult[];
}

export interface MetricSnapshotResponse {
  metricId: number;
  sessionId: number;
  metricName: string;
  metricValue?: number | null;
  metricUnit?: string | null;
  windowCount: number;
  questionCount: number;
  answeredQuestionCount: number;
  highlightCount: number;
  messageCount: number;
  personaCount: number;
  pagesReadEstimate?: number | null;
}
