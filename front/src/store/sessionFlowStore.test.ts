import { beforeEach, describe, expect, it, vi } from 'vitest';
import { marginsRepository } from '../repository/marginsRepository';
import type { SaveBookResponse } from '../types/models/book';
import type {
  CreateReadingSessionResponse,
  CreateSessionWindowResponse,
  ReadingSessionTimelineResponse,
} from '../types/models/session';
import { createDebateWindowPatch, createDefaultSessionPatch } from './sessionFlowStore';

vi.mock('../repository/marginsRepository', () => ({
  marginsRepository: {
    createSession: vi.fn(),
    createWindow: vi.fn(),
    sessionTimeline: vi.fn(),
    sessions: vi.fn(),
    readingStats: vi.fn(),
  },
}));

describe('createDefaultSessionPatch', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  function timelineFor(sessionId = 11): ReadingSessionTimelineResponse {
    return {
      sessionId,
      bookId: 7,
      bookTitle: 'Dune',
      bookAuthor: 'Frank Herbert',
      title: 'Dune reflection',
      status: 'active',
      pinned: false,
      stats: {
        windowCount: 1,
        questionCount: 0,
        answeredQuestionCount: 0,
        messageCount: 0,
        personaResponseCount: 0,
        personaCount: 0,
      },
      nextActions: [],
      windows: [{
        windowId: 21,
        sessionId,
        windowType: 'question',
        title: 'Reflection Window',
        position: 1,
        status: 'open',
      }],
      highlights: [],
      tags: [],
      insights: [],
      questions: [],
      messages: [],
    };
  }

  function readerStats() {
    return {
      sessionCount: 1,
      activeSessionCount: 1,
      completedSessionCount: 0,
      distinctBookCount: 1,
      answeredQuestionCount: 0,
      highlightCount: 0,
      messageCount: 0,
    };
  }

  it('loads the created session when the default reflection window create fails', async () => {
    const book: SaveBookResponse = {
      bookId: 7,
      title: 'Dune',
      author: 'Frank Herbert',
    };
    const session: CreateReadingSessionResponse = {
      sessionId: 11,
      bookId: 7,
      title: 'Dune reflection',
      status: 'active',
    };
    vi.mocked(marginsRepository.createSession).mockResolvedValue(session);
    vi.mocked(marginsRepository.createWindow).mockRejectedValueOnce(new Error('Reflection window failed'));
    vi.mocked(marginsRepository.sessionTimeline).mockResolvedValue(timelineFor());
    vi.mocked(marginsRepository.sessions).mockResolvedValue({ sessions: [] });
    vi.mocked(marginsRepository.readingStats).mockResolvedValue(readerStats());

    const patch = await createDefaultSessionPatch(book, []);

    expect(marginsRepository.sessionTimeline).toHaveBeenCalledWith(11);
    expect(patch.session?.sessionId).toBe(11);
    expect(patch.selectedBook?.bookId).toBe(7);
    expect(patch.window?.windowId).toBe(21);
    expect(patch.windows).toHaveLength(1);
    expect(patch.error).toBe('Session started, but the default window could not be created: Reflection window failed');
  });

  it('loads the created session when library refresh fails after creation', async () => {
    const book: SaveBookResponse = {
      bookId: 7,
      title: 'Dune',
      author: 'Frank Herbert',
    };
    const session: CreateReadingSessionResponse = {
      sessionId: 11,
      bookId: 7,
      title: 'Dune reflection',
      status: 'active',
    };
    const questionWindow: CreateSessionWindowResponse = {
      windowId: 21,
      sessionId: 11,
      windowType: 'question',
      title: 'Reflection Window',
      status: 'open',
    };

    vi.mocked(marginsRepository.createSession).mockResolvedValue(session);
    vi.mocked(marginsRepository.createWindow).mockResolvedValueOnce(questionWindow);
    vi.mocked(marginsRepository.sessionTimeline).mockResolvedValue(timelineFor());
    vi.mocked(marginsRepository.sessions).mockRejectedValue(new Error('Library unavailable'));
    vi.mocked(marginsRepository.readingStats).mockResolvedValue(readerStats());

    const patch = await createDefaultSessionPatch(book, []);

    expect(patch.session?.sessionId).toBe(11);
    expect(patch.window?.windowId).toBe(21);
    expect(patch.windows).toHaveLength(1);
    expect(patch.error).toBe('Session started, but library summaries could not be refreshed: Library unavailable');
    expect(marginsRepository.createWindow).toHaveBeenCalledTimes(1);
  });

  it('creates a topic-specific debate window and selects it', async () => {
    const session: CreateReadingSessionResponse = {
      sessionId: 11,
      bookId: 7,
      title: 'Dune reflection',
      status: 'active',
    };
    const debateWindow: CreateSessionWindowResponse = {
      windowId: 31,
      sessionId: 11,
      windowType: 'debate',
      title: 'Debate: How does ritual shape political authority?',
      status: 'open',
    };
    const timeline = timelineFor();
    timeline.stats.windowCount = 2;
    timeline.windows = [
      ...timeline.windows,
      {
        windowId: 31,
        sessionId: 11,
        windowType: 'debate',
        title: 'Debate: How does ritual shape political authority?',
        position: 2,
        status: 'open',
      },
    ];

    vi.mocked(marginsRepository.createWindow).mockResolvedValue(debateWindow);
    vi.mocked(marginsRepository.sessionTimeline).mockResolvedValue(timeline);
    vi.mocked(marginsRepository.sessions).mockResolvedValue({ sessions: [] });
    vi.mocked(marginsRepository.readingStats).mockResolvedValue(readerStats());

    const patch = await createDebateWindowPatch(session, [], 'How does ritual shape political authority?');

    expect(marginsRepository.createWindow).toHaveBeenCalledWith(
      session,
      'debate',
      'Debate: How does ritual shape political authority?',
    );
    expect(patch.window?.windowId).toBe(31);
    expect(patch.windows).toHaveLength(2);
  });
});
