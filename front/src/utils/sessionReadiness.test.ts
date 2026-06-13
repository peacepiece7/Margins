import { describe, expect, it } from 'vitest';
import type { SessionFlowState } from '../types/view-models/sessionFlow';
import { buildSessionReadiness } from './sessionReadiness';

function state(overrides: Partial<SessionFlowState> = {}): SessionFlowState {
  return {
    query: '',
    candidates: [],
    savedBooks: [],
    sessionSummaries: [],
    memorySearchResults: [],
    windows: [],
    highlights: [],
    tags: [],
    insights: [],
    questions: [],
    nextActions: [],
    personas: [],
    messages: [],
    loading: false,
    hydrated: true,
    ...overrides,
  };
}

describe('buildSessionReadiness', () => {
  it('treats a new session with null-normalized progress as not ready', () => {
    const summary = buildSessionReadiness(state({
      session: {
        sessionId: 1,
        bookId: 1,
        title: 'New session',
        status: 'active',
      },
      progressPercent: undefined,
      stats: {
        windowCount: 2,
        questionCount: 0,
        answeredQuestionCount: 0,
        messageCount: 0,
        personaResponseCount: 0,
        personaCount: 0,
      },
    }));

    expect(summary.completedCount).toBe(0);
    expect(summary.totalCount).toBe(6);
    expect(summary.items.find((item) => item.id === 'progress')).toMatchObject({
      value: 'Not set',
      complete: false,
    });
  });

  it('marks each review area ready from persisted timeline state', () => {
    const summary = buildSessionReadiness(state({
      session: {
        sessionId: 1,
        bookId: 1,
        title: 'Completed session',
        status: 'completed',
      },
      currentPage: 48,
      targetPage: 120,
      progressPercent: 40,
      highlights: [{
        highlightId: 1,
        sessionId: 1,
        bookId: 1,
        quoteText: 'A saved quote.',
        highlightOrder: 1,
      }],
      stats: {
        windowCount: 2,
        questionCount: 3,
        answeredQuestionCount: 1,
        messageCount: 4,
        personaResponseCount: 1,
        personaCount: 1,
      },
    }));

    expect(summary.completedCount).toBe(6);
    expect(summary.percent).toBe(100);
    expect(summary.items.map((item) => [item.id, item.complete])).toEqual([
      ['progress', true],
      ['questions', true],
      ['answers', true],
      ['quotes', true],
      ['personas', true],
      ['closeout', true],
    ]);
  });
});
