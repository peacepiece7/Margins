import type { BookCandidate, SaveBookResponse } from '../models/book';
import type { Persona } from '../models/persona';
import type {
  CreateReadingSessionResponse,
  CreateSessionWindowResponse,
  Question,
  ReadingLibraryStatsResponse,
  ReadingSessionNextAction,
  MetricSnapshotResponse,
  ReadingSessionStats,
  ReadingSessionSummary,
  ReadingSessionReview,
  SessionSearchResult,
  SessionHighlight,
  SessionInsight,
  SessionTag,
  SessionWindowTimeline,
} from '../models/session';

export interface SessionDisplayMessage {
  id: string;
  sessionId: number;
  windowId: number;
  role: string;
  content: string;
  personaId?: number;
  personaDisplayName?: string;
  questionId?: number;
  persistedMessageId?: number;
}

export interface SessionReadinessItem {
  id: string;
  label: string;
  value: string;
  complete: boolean;
}

export interface SessionReadinessSummary {
  completedCount: number;
  totalCount: number;
  percent: number;
  items: SessionReadinessItem[];
}

export interface SessionBriefItem {
  id: string;
  label: string;
  value: string;
  detail?: string;
}

export interface SessionBriefSummary {
  headline: string;
  items: SessionBriefItem[];
}

export type ComposerMode = 'message' | 'persona';

export interface SessionFlowState {
  query: string;
  candidates: BookCandidate[];
  savedBooks: SaveBookResponse[];
  selectedBook?: SaveBookResponse;
  session?: CreateReadingSessionResponse;
  sessionSummary?: string;
  review?: ReadingSessionReview;
  readingGoal?: string;
  startPage?: number;
  currentPage?: number;
  targetPage?: number;
  progressPercent?: number;
  progressNote?: string;
  stats?: ReadingSessionStats;
  nextActions: ReadingSessionNextAction[];
  readerStats?: ReadingLibraryStatsResponse;
  lastMetricSnapshot?: MetricSnapshotResponse;
  sessionSummaries: ReadingSessionSummary[];
  memorySearchResults: SessionSearchResult[];
  window?: CreateSessionWindowResponse;
  windows: SessionWindowTimeline[];
  highlights: SessionHighlight[];
  tags: SessionTag[];
  insights: SessionInsight[];
  questions: Question[];
  selectedQuestionId?: number;
  personas: Persona[];
  selectedPersonaId?: number;
  messages: SessionDisplayMessage[];
  streamingMessage?: SessionDisplayMessage;
  loading: boolean;
  hydrated: boolean;
  error?: string;
}
