import type { ApiResponse } from '../types/models/api';
import type {
  BookCandidate,
  BookCandidateSearchResponse,
  SaveBookResponse,
} from '../types/models/book';
import type {
  AiMessageResponse,
  CreateReadingSessionResponse,
  CreateSessionWindowResponse,
} from '../types/models/session';

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  const result = (await response.json()) as ApiResponse<T>;
  if (!result.success) {
    throw new Error(result.message || 'Request failed');
  }

  return result.data;
}

export const marginsRepository = {
  searchCandidates(query: string): Promise<BookCandidateSearchResponse> {
    return postJson('/api/books/search-candidates', { query });
  },

  saveBook(candidate: BookCandidate): Promise<SaveBookResponse> {
    return postJson('/api/books', {
      candidateId: candidate.candidateId,
      title: candidate.title,
      author: candidate.author,
    });
  },

  createSession(book: SaveBookResponse): Promise<CreateReadingSessionResponse> {
    return postJson('/api/reading-sessions', {
      bookId: book.bookId,
      title: `${book.title} reflection`,
    });
  },

  createWindow(session: CreateReadingSessionResponse): Promise<CreateSessionWindowResponse> {
    return postJson('/api/session-windows', {
      sessionId: session.sessionId,
      windowType: 'question',
      title: 'Reflection Window',
    });
  },

  sendMessage(windowId: number, content: string): Promise<AiMessageResponse> {
    return postJson(`/api/session-windows/${windowId}/messages`, { content });
  },

  debate(windowId: number, content: string): Promise<AiMessageResponse> {
    return postJson(`/api/session-windows/${windowId}/debate`, {
      personaId: 1,
      content,
    });
  },
};
