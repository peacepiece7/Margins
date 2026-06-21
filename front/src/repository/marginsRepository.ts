import type { ApiResponse } from '../types/models/api';
import type { LoginResponse } from '../types/models/auth';
import type {
  BookListResponse,
  BookCandidate,
  BookCandidateSearchResponse,
  SaveBookResponse,
} from '../types/models/book';
import type { PersonaListResponse } from '../types/models/persona';
import type {
  AiMessageResponse,
  CreateReadingSessionResponse,
  CreateSessionWindowResponse,
  Question,
  QuestionListResponse,
  MetricSnapshotResponse,
  ReadingLibraryStatsResponse,
  ReadingSessionListResponse,
  SessionSearchResponse,
  ReadingSessionTimelineResponse,
  SessionMessage,
} from '../types/models/session';
import { fitTextWithSuffix, inputLimits } from '../utils/inputLimits';

const AUTH_STORAGE_KEY = 'margins.auth';

function authHeaders(): Record<string, string> {
  if (typeof window === 'undefined') {
    return {};
  }

  const value = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!value) {
    return {};
  }

  try {
    const session = JSON.parse(value) as LoginResponse;
    return session.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {};
  } catch {
    return {};
  }
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  });

  return readApiResponse<T>(response);
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: authHeaders(),
  });

  return readApiResponse<T>(response);
}

async function deleteJson<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    method: 'DELETE',
    headers: authHeaders(),
  });

  return readApiResponse<T>(response);
}

async function patchJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  });

  return readApiResponse<T>(response);
}

async function readApiResponse<T>(response: Response): Promise<T> {
  let result: ApiResponse<T> | undefined;
  try {
    result = (await response.json()) as ApiResponse<T>;
  } catch {
    result = undefined;
  }

  if (!response.ok) {
    throw new Error(result?.message || `Request failed: ${response.status}`);
  }

  if (!result?.success) {
    throw new Error(result?.message || 'Request failed');
  }

  return result.data;
}

interface StreamEvent {
  event: string;
  data: unknown;
}

function parseStreamBlock(block: string): StreamEvent | undefined {
  const lines = block.split(/\r?\n/);
  const event = lines.find((line) => line.startsWith('event:'))?.slice('event:'.length).trim();
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length).trim())
    .join('\n');

  if (!event || !data) {
    return undefined;
  }

  return { event, data: JSON.parse(data) as unknown };
}

async function postStream<T>(
  path: string,
  body: unknown,
  onEvent: (event: StreamEvent) => void,
): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { Accept: 'text/event-stream', 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    await readApiResponse<T>(response);
  }
  if (!response.body) {
    throw new Error('Streaming response was empty');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let finalData: T | undefined;

  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
    const blocks = buffer.split(/\r?\n\r?\n/);
    buffer = blocks.pop() || '';

    for (const block of blocks) {
      const event = parseStreamBlock(block);
      if (!event) {
        continue;
      }
      onEvent(event);
      if (event.event === 'message.error') {
        const data = event.data as { message?: string };
        throw new Error(data.message || 'Streaming response failed');
      }
      if (event.event === 'message.done') {
        finalData = event.data as T;
      }
    }

    if (done) {
      break;
    }
  }

  if (buffer.trim()) {
    const event = parseStreamBlock(buffer);
    if (event) {
      onEvent(event);
      if (event.event === 'message.error') {
        const data = event.data as { message?: string };
        throw new Error(data.message || 'Streaming response failed');
      }
      if (event.event === 'message.done') {
        finalData = event.data as T;
      }
    }
  }

  if (!finalData) {
    throw new Error('Streaming response did not finish');
  }

  return finalData;
}

export const marginsRepository = {
  login(username: string, password: string): Promise<LoginResponse> {
    return postJson('/api/auth/login', { username, password });
  },

  searchCandidates(query: string): Promise<BookCandidateSearchResponse> {
    return postJson('/api/books/search-candidates', { query });
  },

  latestTimeline(): Promise<ReadingSessionTimelineResponse | null> {
    return getJson('/api/reading-sessions/latest');
  },

  sessionTimeline(sessionId: number): Promise<ReadingSessionTimelineResponse | null> {
    return getJson(`/api/reading-sessions/${sessionId}`);
  },

  sessions(): Promise<ReadingSessionListResponse> {
    return getJson('/api/reading-sessions');
  },

  readingStats(): Promise<ReadingLibraryStatsResponse> {
    return getJson('/api/reading-sessions/stats');
  },

  searchReadingMemory(query: string): Promise<SessionSearchResponse> {
    return getJson(`/api/reading-sessions/search?query=${encodeURIComponent(query)}`);
  },

  books(): Promise<BookListResponse> {
    return getJson('/api/books');
  },

  archiveSession(sessionId: number): Promise<ReadingSessionListResponse> {
    return deleteJson(`/api/reading-sessions/${sessionId}`);
  },

  updateSessionPinned(sessionId: number, pinned: boolean): Promise<ReadingSessionListResponse> {
    return patchJson(`/api/reading-sessions/${sessionId}/pin`, { pinned });
  },

  completeSession(sessionId: number, summary: string): Promise<ReadingSessionTimelineResponse> {
    return postJson(`/api/reading-sessions/${sessionId}/complete`, { summary });
  },

  createMetricSnapshot(sessionId: number): Promise<MetricSnapshotResponse> {
    return postJson(`/api/reading-sessions/${sessionId}/metrics/snapshot`, {});
  },

  updateSessionTitle(sessionId: number, title: string): Promise<ReadingSessionTimelineResponse> {
    return patchJson(`/api/reading-sessions/${sessionId}/title`, { title });
  },

  updateSessionProgress(
    sessionId: number,
    progress: {
      readingGoal?: string;
      startPage?: number;
      currentPage?: number;
      targetPage?: number;
      progressNote?: string;
    },
  ): Promise<ReadingSessionTimelineResponse> {
    return patchJson(`/api/reading-sessions/${sessionId}/progress`, progress);
  },

  createHighlight(
    sessionId: number,
    highlight: {
      pageNumber?: number;
      locationLabel?: string;
      quoteText: string;
      note?: string;
    },
  ): Promise<ReadingSessionTimelineResponse> {
    return postJson(`/api/reading-sessions/${sessionId}/highlights`, highlight);
  },

  deleteHighlight(sessionId: number, highlightId: number): Promise<ReadingSessionTimelineResponse> {
    return deleteJson(`/api/reading-sessions/${sessionId}/highlights/${highlightId}`);
  },

  createSessionTag(sessionId: number, label: string): Promise<ReadingSessionTimelineResponse> {
    return postJson(`/api/reading-sessions/${sessionId}/tags`, { label });
  },

  deleteSessionTag(sessionId: number, tagId: number): Promise<ReadingSessionTimelineResponse> {
    return deleteJson(`/api/reading-sessions/${sessionId}/tags/${tagId}`);
  },

  createSessionInsight(
    sessionId: number,
    insight: {
      insightType?: string;
      title?: string;
      content: string;
      evidence?: string;
    },
  ): Promise<ReadingSessionTimelineResponse> {
    return postJson(`/api/reading-sessions/${sessionId}/insights`, insight);
  },

  deleteSessionInsight(sessionId: number, insightId: number): Promise<ReadingSessionTimelineResponse> {
    return deleteJson(`/api/reading-sessions/${sessionId}/insights/${insightId}`);
  },

  updateHighlight(
    sessionId: number,
    highlightId: number,
    highlight: {
      pageNumber?: number;
      locationLabel?: string;
      quoteText: string;
      note?: string;
    },
  ): Promise<ReadingSessionTimelineResponse> {
    return patchJson(`/api/reading-sessions/${sessionId}/highlights/${highlightId}`, highlight);
  },

  personas(): Promise<PersonaListResponse> {
    return getJson('/api/personas');
  },

  createPersona(persona: {
    displayName: string;
    description?: string;
    systemPrompt: string;
    tone?: string;
  }): Promise<PersonaListResponse> {
    return postJson('/api/personas', persona);
  },

  saveBook(candidate: BookCandidate): Promise<SaveBookResponse> {
    return postJson('/api/books', {
      candidateId: candidate.candidateId,
      title: candidate.title,
      author: candidate.author,
      publishedYear: candidate.publishedYear,
    });
  },

  saveManualBook(title: string, author: string): Promise<SaveBookResponse> {
    return postJson('/api/books', {
      candidateId: `manual-${Date.now()}`,
      title,
      author,
    });
  },

  updateBook(bookId: number, title: string, author: string): Promise<SaveBookResponse> {
    return patchJson(`/api/books/${bookId}`, { title, author });
  },

  deleteBook(bookId: number): Promise<BookListResponse> {
    return deleteJson(`/api/books/${bookId}`);
  },

  createSession(book: SaveBookResponse): Promise<CreateReadingSessionResponse> {
    return postJson('/api/reading-sessions', {
      bookId: book.bookId,
      title: fitTextWithSuffix(book.title, ' reflection', inputLimits.readingSessionTitle),
    });
  },

  createWindow(
    session: CreateReadingSessionResponse,
    windowType = 'question',
    title = 'Reflection Window',
  ): Promise<CreateSessionWindowResponse> {
    return postJson('/api/session-windows', {
      sessionId: session.sessionId,
      windowType,
      title,
    });
  },

  updateWindowTitle(windowId: number, title: string): Promise<CreateSessionWindowResponse> {
    return patchJson(`/api/session-windows/${windowId}/title`, { title });
  },

  archiveWindow(windowId: number): Promise<CreateSessionWindowResponse> {
    return deleteJson(`/api/session-windows/${windowId}`);
  },

  generateQuestions(windowId: number, count = 3, focus?: string): Promise<QuestionListResponse> {
    return postJson(`/api/session-windows/${windowId}/questions/generate`, { count, focus });
  },

  createQuestion(windowId: number, questionText: string): Promise<QuestionListResponse> {
    return postJson(`/api/session-windows/${windowId}/questions`, { questionText });
  },

  deleteQuestion(questionId: number): Promise<Question> {
    return deleteJson(`/api/questions/${questionId}`);
  },

  sendMessage(windowId: number, content: string, questionId?: number): Promise<AiMessageResponse> {
    return postJson(`/api/session-windows/${windowId}/messages`, { content, questionId });
  },

  streamMessage(
    windowId: number,
    content: string,
    questionId: number | undefined,
    onDelta: (delta: string) => void,
  ): Promise<AiMessageResponse> {
    return postStream<AiMessageResponse>(
      `/api/session-windows/${windowId}/messages/stream`,
      { content, questionId },
      (event) => {
        if (event.event === 'message.delta') {
          const data = event.data as { delta?: string };
          onDelta(data.delta || '');
        }
      },
    );
  },

  updateMessage(messageId: number, content: string): Promise<SessionMessage> {
    return patchJson(`/api/messages/${messageId}`, { content });
  },

  deleteMessage(messageId: number): Promise<SessionMessage> {
    return deleteJson(`/api/messages/${messageId}`);
  },

  debate(windowId: number, personaId: number, content: string): Promise<AiMessageResponse> {
    return postJson(`/api/session-windows/${windowId}/debate`, {
      personaId,
      content,
    });
  },

  debateAll(windowId: number, content: string): Promise<{ messages: AiMessageResponse[] }> {
    return postJson(`/api/session-windows/${windowId}/debate/all`, {
      content,
    });
  },
};
