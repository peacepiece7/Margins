import { beforeEach, describe, expect, it, vi } from 'vitest';
import { marginsRepository } from '../repository/marginsRepository';
import type { SaveBookResponse } from '../types/models/book';
import type {
  CreateReadingSessionResponse,
  CreateSessionWindowResponse,
  ReadingSessionTimelineResponse,
} from '../types/models/session';
import { createDefaultSessionPatch } from './sessionFlowStore';

vi.mock('../repository/marginsRepository', () => ({
  marginsRepository: {
    createSession: vi.fn(),
    createWindow: vi.fn(),
    generateQuestions: vi.fn(),
    generatePersonas: vi.fn(),
    createPersona: vi.fn(),
    sessionTimeline: vi.fn(),
    sessions: vi.fn(),
    readingStats: vi.fn(),
  },
}));

describe('createDefaultSessionPatch', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  function resetProvisioningMocks() {
    vi.mocked(marginsRepository.generateQuestions).mockResolvedValue({ questions: [] });
    vi.mocked(marginsRepository.generatePersonas).mockResolvedValue({ personas: [] });
    vi.mocked(marginsRepository.createPersona).mockResolvedValue({ personas: [] });
  }

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

  it('loads the created session when a default window create fails', async () => {
    resetProvisioningMocks();
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
    vi.mocked(marginsRepository.createWindow)
      .mockResolvedValueOnce(questionWindow)
      .mockRejectedValueOnce(new Error('Debate window failed'));
    vi.mocked(marginsRepository.sessionTimeline).mockResolvedValue(timelineFor());
    vi.mocked(marginsRepository.sessions).mockResolvedValue({ sessions: [] });
    vi.mocked(marginsRepository.readingStats).mockResolvedValue(readerStats());

    const patch = await createDefaultSessionPatch(book, []);

    expect(marginsRepository.sessionTimeline).toHaveBeenCalledWith(11);
    expect(patch.session?.sessionId).toBe(11);
    expect(patch.selectedBook?.bookId).toBe(7);
    expect(patch.window?.windowId).toBe(21);
    expect(patch.windows).toHaveLength(1);
    expect(patch.error).toBe('Session started, but default windows could not all be created: Debate window failed');
  });

  it('loads the created session when library refresh fails after creation', async () => {
    resetProvisioningMocks();
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
    vi.mocked(marginsRepository.createWindow)
      .mockResolvedValueOnce(questionWindow)
      .mockResolvedValueOnce({
        windowId: 22,
        sessionId: 11,
        windowType: 'debate',
        title: 'Persona Debate',
        status: 'open',
      });
    vi.mocked(marginsRepository.sessionTimeline).mockResolvedValue(timelineFor());
    vi.mocked(marginsRepository.sessions).mockRejectedValue(new Error('Library unavailable'));
    vi.mocked(marginsRepository.readingStats).mockResolvedValue(readerStats());

    const patch = await createDefaultSessionPatch(book, []);

    expect(patch.session?.sessionId).toBe(11);
    expect(patch.window?.windowId).toBe(21);
    expect(patch.windows).toHaveLength(1);
    expect(patch.error).toBe('Session started, but library summaries could not be refreshed: Library unavailable');
  });

  it('prepares generated questions and personas for a new book session', async () => {
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
    vi.mocked(marginsRepository.createWindow)
      .mockResolvedValueOnce(questionWindow)
      .mockResolvedValueOnce({
        windowId: 22,
        sessionId: 11,
        windowType: 'debate',
        title: 'Persona Debate',
        status: 'open',
      });
    vi.mocked(marginsRepository.generateQuestions).mockResolvedValue({ questions: [] });
    vi.mocked(marginsRepository.generatePersonas).mockResolvedValue({
      personas: [
        { displayName: 'Historian', tone: 'critical', systemPrompt: 'Read historically.' },
        { displayName: 'Stylist', tone: 'formal', systemPrompt: 'Read for style.' },
        { displayName: 'Skeptic', tone: 'skeptical', systemPrompt: 'Challenge claims.' },
      ],
    });
    vi.mocked(marginsRepository.createPersona)
      .mockResolvedValueOnce({ personas: [{ personaId: 100, name: 'historian', displayName: 'Historian' }] })
      .mockResolvedValueOnce({ personas: [{ personaId: 101, name: 'stylist', displayName: 'Stylist' }] })
      .mockResolvedValueOnce({ personas: [{ personaId: 102, name: 'skeptic', displayName: 'Skeptic' }] });
    vi.mocked(marginsRepository.sessionTimeline).mockResolvedValue(timelineFor());
    vi.mocked(marginsRepository.sessions).mockResolvedValue({ sessions: [] });
    vi.mocked(marginsRepository.readingStats).mockResolvedValue(readerStats());

    const patch = await createDefaultSessionPatch(book, []);

    expect(marginsRepository.generateQuestions).toHaveBeenCalledWith(21, 3, 'Dune');
    expect(marginsRepository.generatePersonas).toHaveBeenCalledWith({
      count: 3,
      bookTitle: 'Dune',
      context: 'Dune by Frank Herbert',
    });
    expect(marginsRepository.createPersona).toHaveBeenCalledTimes(3);
    expect(marginsRepository.createPersona).toHaveBeenNthCalledWith(1, {
      displayName: 'Historian',
      description: undefined,
      tone: 'critical',
      systemPrompt: 'Read historically.',
      sessionId: 11,
    });
    expect(patch.personas).toEqual([{ personaId: 102, name: 'skeptic', displayName: 'Skeptic' }]);
    expect(patch.selectedPersonaId).toBe(102);
    expect(patch.error).toBeUndefined();
  });

  it('opens the created session when AI preparation fails', async () => {
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
    vi.mocked(marginsRepository.createWindow)
      .mockResolvedValueOnce(questionWindow)
      .mockResolvedValueOnce({
        windowId: 22,
        sessionId: 11,
        windowType: 'debate',
        title: 'Persona Debate',
        status: 'open',
      });
    vi.mocked(marginsRepository.generateQuestions).mockRejectedValue(new Error('OpenAI unavailable'));
    vi.mocked(marginsRepository.generatePersonas).mockResolvedValue({ personas: [] });
    vi.mocked(marginsRepository.createPersona).mockResolvedValue({ personas: [] });
    vi.mocked(marginsRepository.sessionTimeline).mockResolvedValue(timelineFor());
    vi.mocked(marginsRepository.sessions).mockResolvedValue({ sessions: [] });
    vi.mocked(marginsRepository.readingStats).mockResolvedValue(readerStats());

    const patch = await createDefaultSessionPatch(book, []);

    expect(patch.session?.sessionId).toBe(11);
    expect(patch.window?.windowId).toBe(21);
    expect(patch.error).toBe('Session started, but AI prompts or personas could not all be prepared: OpenAI unavailable');
  });
});
