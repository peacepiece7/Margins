import { useState } from 'react';
import { marginsRepository } from '../repository/marginsRepository';
import type { BookCandidate, SaveBookResponse } from '../types/models/book';
import type { Persona } from '../types/models/persona';
import type { ReadingSessionTimelineResponse, SaveReadingSessionReviewRequest, SessionWindowTimeline } from '../types/models/session';
import type { SessionFlowState } from '../types/view-models/sessionFlow';
import { selectAvailablePersonaId } from '../utils/personaSelection';

const initialState: SessionFlowState = {
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
  streamingMessage: undefined,
  loading: false,
  hydrated: false,
};

const SELECTED_SESSION_STORAGE_KEY = 'margins.selectedSessionId';

function readStoredSessionId() {
  if (typeof window === 'undefined') {
    return undefined;
  }

  const value = window.localStorage.getItem(SELECTED_SESSION_STORAGE_KEY);
  const parsed = value ? Number(value) : Number.NaN;
  return Number.isFinite(parsed) ? parsed : undefined;
}

function writeStoredSessionId(sessionId: number) {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(SELECTED_SESSION_STORAGE_KEY, String(sessionId));
  }
}

function clearStoredSessionId() {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(SELECTED_SESSION_STORAGE_KEY);
  }
}

async function libraryPatch(): Promise<Partial<SessionFlowState>> {
  const [sessionResult, statsResult] = await Promise.all([
    marginsRepository.sessions(),
    marginsRepository.readingStats(),
  ]);

  return {
    sessionSummaries: sessionResult.sessions,
    readerStats: statsResult,
  };
}

function defaultWindowWarning(error: unknown) {
  const detail = error instanceof Error ? error.message : 'Unknown error';
  return `Session started, but default windows could not all be created: ${detail}`;
}

function libraryRefreshWarning(error: unknown) {
  const detail = error instanceof Error ? error.message : 'Unknown error';
  return `Session started, but library summaries could not be refreshed: ${detail}`;
}

function appendWarning(existing: string | undefined, next: string) {
  return existing ? `${existing} ${next}` : next;
}

function emptyTimelinePatch(): Partial<SessionFlowState> {
  return {
    hydrated: true,
    selectedBook: undefined,
    session: undefined,
    sessionSummary: undefined,
    review: undefined,
    lastMetricSnapshot: undefined,
    memorySearchResults: [],
    readingGoal: undefined,
    startPage: undefined,
    currentPage: undefined,
    targetPage: undefined,
    progressPercent: undefined,
    progressNote: undefined,
    stats: undefined,
    window: undefined,
    windows: [],
    highlights: [],
    tags: [],
    insights: [],
    questions: [],
    nextActions: [],
    selectedQuestionId: undefined,
    messages: [],
    streamingMessage: undefined,
  };
}

function debateWindowId(windows: SessionWindowTimeline[]) {
  return windows.find((window) => window.windowType === 'debate')?.windowId;
}

function messageWindowId(windows: SessionWindowTimeline[]) {
  return windows.find((window) => window.windowType === 'question')?.windowId || windows[0]?.windowId;
}

function questionFocus(bookTitle?: string, windowTitle?: string) {
  if (!bookTitle) {
    return windowTitle;
  }
  if (!windowTitle || windowTitle === 'Reflection Window') {
    return bookTitle;
  }

  return `${bookTitle} - ${windowTitle}`;
}

async function restoreStoredOrLatestTimeline(storedSessionId?: number) {
  if (!storedSessionId) {
    return marginsRepository.latestTimeline();
  }

  try {
    const storedTimeline = await marginsRepository.sessionTimeline(storedSessionId);
    if (storedTimeline) {
      return storedTimeline;
    }
  } catch {
    clearStoredSessionId();
  }

  const fallbackTimeline = await marginsRepository.latestTimeline();
  if (fallbackTimeline) {
    writeStoredSessionId(fallbackTimeline.sessionId);
  }

  return fallbackTimeline;
}

function patchFromTimeline(
  timeline: ReadingSessionTimelineResponse | null,
  personas: Persona[],
  currentWindowId?: number,
  currentQuestionId?: number,
): Partial<SessionFlowState> {
  if (!timeline) {
    return emptyTimelinePatch();
  }

  const selectedWindow = timeline.windows.find((window) => window.windowId === currentWindowId)
    || timeline.windows.find((window) => window.windowType === 'question')
    || timeline.windows[0];
  const preservedQuestionId = timeline.questions.find((question) =>
    question.questionId === currentQuestionId && question.windowId === selectedWindow?.windowId
  )?.questionId;
  const fallbackQuestionId = timeline.questions.find((question) => question.windowId === selectedWindow?.windowId)?.questionId;
  const personaById = new Map(personas.map((persona) => [persona.personaId, persona.displayName]));

  return {
    hydrated: true,
    selectedBook: {
      bookId: timeline.bookId,
      title: timeline.bookTitle,
      author: timeline.bookAuthor || '',
    },
    session: {
      sessionId: timeline.sessionId,
      bookId: timeline.bookId,
      title: timeline.title,
      status: timeline.status,
    },
    sessionSummary: timeline.summary ?? undefined,
    review: timeline.review ?? undefined,
    lastMetricSnapshot: undefined,
    readingGoal: timeline.readingGoal ?? undefined,
    startPage: timeline.startPage ?? undefined,
    currentPage: timeline.currentPage ?? undefined,
    targetPage: timeline.targetPage ?? undefined,
    progressPercent: timeline.progressPercent ?? undefined,
    progressNote: timeline.progressNote ?? undefined,
    stats: timeline.stats,
    nextActions: timeline.nextActions || [],
    window: selectedWindow
      ? {
          windowId: selectedWindow.windowId,
          sessionId: selectedWindow.sessionId,
          windowType: selectedWindow.windowType,
          title: selectedWindow.title,
          status: selectedWindow.status,
        }
      : undefined,
    windows: timeline.windows,
    highlights: timeline.highlights || [],
    tags: timeline.tags || [],
    insights: timeline.insights || [],
    questions: timeline.questions,
    selectedQuestionId: preservedQuestionId || fallbackQuestionId,
    messages: timeline.messages.map((message) => ({
      id: `persisted-${message.messageId}`,
      sessionId: message.sessionId,
      windowId: message.windowId,
      role: message.role,
      content: message.content,
      personaId: message.personaId,
      personaDisplayName: message.personaId ? personaById.get(message.personaId) : undefined,
      questionId: message.questionId,
      persistedMessageId: message.messageId,
    })),
    streamingMessage: undefined,
  };
}

export function useSessionFlowStore() {
  const [state, setState] = useState<SessionFlowState>(initialState);

  async function createSessionPatch(selectedBook: SaveBookResponse): Promise<Partial<SessionFlowState>> {
    return createDefaultSessionPatch(selectedBook, state.personas);
  }

  async function run(action: () => Promise<Partial<SessionFlowState>>, options: { markHydratedOnError?: boolean } = {}) {
    setState((current) => ({ ...current, loading: true, error: undefined }));
    try {
      const patch = await action();
      setState((current) => ({ ...current, ...patch, loading: false }));
      return true;
    } catch (error) {
      setState((current) => ({
        ...current,
        loading: false,
        hydrated: options.markHydratedOnError ? true : current.hydrated,
        error: error instanceof Error ? error.message : 'Unknown error',
      }));
      return false;
    }
  }

  return {
    state,
    setQuery(query: string) {
      setState((current) => ({ ...current, query }));
    },
    loadLatest() {
      return run(async () => {
        const storedSessionId = readStoredSessionId();
        const [timeline, personaResult, sessionResult, statsResult] = await Promise.all([
          restoreStoredOrLatestTimeline(storedSessionId),
          marginsRepository.personas(),
          marginsRepository.sessions(),
          marginsRepository.readingStats(),
        ]);
        const bookResult = await marginsRepository.books();
        const selectedPersonaId = selectAvailablePersonaId(personaResult.personas, state.selectedPersonaId);

        return {
          ...patchFromTimeline(timeline, personaResult.personas, state.window?.windowId, state.selectedQuestionId),
          sessionSummaries: sessionResult.sessions,
          readerStats: statsResult,
          savedBooks: bookResult.books,
          personas: personaResult.personas,
          selectedPersonaId,
        };
      }, { markHydratedOnError: true });
    },
    loadSession(sessionId: number) {
      return run(async () => {
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);
        writeStoredSessionId(sessionId);

        return {
          ...patchFromTimeline(timeline, state.personas, undefined),
          ...library,
        };
      });
    },
    archiveSession(sessionId: number) {
      return run(async () => {
        const sessionResult = await marginsRepository.archiveSession(sessionId);
        const statsResult = await marginsRepository.readingStats();
        if (state.session?.sessionId !== sessionId) {
          return { sessionSummaries: sessionResult.sessions, readerStats: statsResult };
        }

        clearStoredSessionId();
        const timeline = await marginsRepository.latestTimeline();
        const nextPatch = patchFromTimeline(timeline, state.personas, undefined);
        if (timeline) {
          writeStoredSessionId(timeline.sessionId);
        }

        return {
          ...nextPatch,
          sessionSummaries: sessionResult.sessions,
          readerStats: statsResult,
        };
      });
    },
    updateSessionPinned(sessionId: number, pinned: boolean) {
      return run(async () => {
        const sessionResult = await marginsRepository.updateSessionPinned(sessionId, pinned);

        return {
          sessionSummaries: sessionResult.sessions,
        };
      });
    },
    selectWindow(windowId: number) {
      setState((current) => {
        const selectedWindow = current.windows.find((window) => window.windowId === windowId);
        if (!selectedWindow) {
          return current;
        }
        const selectedQuestionId = current.questions.find((question) => question.windowId === selectedWindow.windowId)?.questionId;

        return {
          ...current,
          window: {
            windowId: selectedWindow.windowId,
            sessionId: selectedWindow.sessionId,
            windowType: selectedWindow.windowType,
            title: selectedWindow.title,
            status: selectedWindow.status,
          },
          selectedQuestionId,
        };
      });
    },
    selectPersona(personaId: number) {
      setState((current) => ({ ...current, selectedPersonaId: personaId }));
    },
    createPersona(persona: {
      displayName: string;
      description?: string;
      systemPrompt: string;
      tone?: string;
    }) {
      return run(async () => {
        const result = await marginsRepository.createPersona(persona);
        const selectedPersonaId = selectAvailablePersonaId(result.personas, result.personas[result.personas.length - 1]?.personaId);

        return {
          personas: result.personas,
          selectedPersonaId,
        };
      });
    },
    selectQuestion(questionId: number) {
      setState((current) => ({ ...current, selectedQuestionId: questionId }));
    },
    search() {
      return run(async () => {
        const result = await marginsRepository.searchCandidates(state.query);
        return { candidates: result.candidates };
      });
    },
    searchReadingMemory(query: string) {
      return run(async () => {
        const result = await marginsRepository.searchReadingMemory(query);
        return { memorySearchResults: result.results };
      });
    },
    clearReadingMemorySearch() {
      setState((current) => ({ ...current, memorySearchResults: [] }));
    },
    selectCandidate(candidate: BookCandidate) {
      return run(async () => {
        const selectedBook = await marginsRepository.saveBook(candidate);
        const sessionPatch = await createSessionPatch(selectedBook);
        const bookResult = await marginsRepository.books();
        return {
          ...sessionPatch,
          savedBooks: bookResult.books,
        };
      });
    },
    startSessionFromBook(book: SaveBookResponse) {
      return run(async () => createSessionPatch(book));
    },
    addWindow(title: string) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const window = await marginsRepository.createWindow(state.session!, 'question', title);
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);

        return {
          ...patchFromTimeline(timeline, state.personas, window.windowId),
          ...library,
        };
      });
    },
    updateWindowTitle(title: string) {
      if (!state.session || !state.window) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      const windowId = state.window.windowId;
      return run(async () => {
        await marginsRepository.updateWindowTitle(windowId, title);
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);

        return {
          ...patchFromTimeline(timeline, state.personas, windowId),
          ...library,
        };
      });
    },
    archiveWindow() {
      if (!state.session || !state.window || state.windows.length <= 1) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      const windowId = state.window.windowId;
      return run(async () => {
        await marginsRepository.archiveWindow(windowId);
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);

        return {
          ...patchFromTimeline(timeline, state.personas, undefined),
          ...library,
        };
      });
    },
    generateQuestions() {
      const windowId = state.window?.windowType === 'question'
        ? state.window.windowId
        : messageWindowId(state.windows);
      if (!windowId) {
        return Promise.resolve(false);
      }

      return run(async () => {
        await marginsRepository.generateQuestions(windowId, 3, questionFocus(state.selectedBook?.title, state.window?.title));
        const timeline = state.session
          ? await marginsRepository.sessionTimeline(state.session.sessionId)
          : await marginsRepository.latestTimeline();
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, windowId),
          ...library,
        };
      });
    },
    createQuestion(questionText: string) {
      const windowId = state.window?.windowType === 'question'
        ? state.window.windowId
        : messageWindowId(state.windows);
      if (!state.session || !windowId) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const result = await marginsRepository.createQuestion(windowId, questionText);
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);
        const selectedQuestionId = result.questions[result.questions.length - 1]?.questionId;

        return {
          ...patchFromTimeline(timeline, state.personas, windowId),
          ...library,
          selectedQuestionId,
        };
      });
    },
    deleteQuestion(questionId: number) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        await marginsRepository.deleteQuestion(questionId);
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);

        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    send(content: string) {
      if (!state.window) {
        return Promise.resolve(false);
      }
      const windowId = state.window.windowType === 'debate'
        ? messageWindowId(state.windows) || state.window.windowId
        : state.window.windowId;
      const sessionId = state.session?.sessionId || 0;
      setState((current) => ({
        ...current,
        loading: true,
        error: undefined,
        streamingMessage: {
          id: `streaming-${windowId}`,
          sessionId,
          windowId,
          role: 'assistant',
          content: '',
        },
      }));
      return marginsRepository
        .streamMessage(windowId, content, state.selectedQuestionId, (delta) => {
          setState((current) => {
            if (!current.streamingMessage || current.streamingMessage.windowId !== windowId) {
              return current;
            }

            return {
              ...current,
              streamingMessage: {
                ...current.streamingMessage,
                content: `${current.streamingMessage.content}${delta}`,
              },
            };
          });
        })
        .then(async () => {
          const timeline = state.session
            ? await marginsRepository.sessionTimeline(state.session.sessionId)
            : await marginsRepository.latestTimeline();
          const library = await libraryPatch();
          setState((current) => ({
            ...current,
            ...patchFromTimeline(timeline, current.personas, windowId, current.selectedQuestionId),
            ...library,
            loading: false,
            streamingMessage: undefined,
          }));
          return true;
        })
        .catch((error) => {
          setState((current) => ({
            ...current,
            loading: false,
            streamingMessage: undefined,
            error: error instanceof Error ? error.message : 'Unknown error',
          }));
          return false;
        });
    },
    updateMessage(messageId: number, content: string) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        await marginsRepository.updateMessage(messageId, content);
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);

        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    deleteMessage(messageId: number) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        await marginsRepository.deleteMessage(messageId);
        const [timeline, library] = await Promise.all([
          marginsRepository.sessionTimeline(sessionId),
          libraryPatch(),
        ]);

        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    debate(content: string) {
      const personaId = selectAvailablePersonaId(state.personas, state.selectedPersonaId);
      if (!state.window || !personaId) {
        return Promise.resolve(false);
      }
      const windowId = state.window.windowType === 'debate'
        ? state.window.windowId
        : debateWindowId(state.windows) || state.window.windowId;
      setState((current) => ({ ...current, loading: true, error: undefined }));
      return marginsRepository
        .debate(windowId, personaId, content)
        .then(async () => {
          const timeline = state.session
            ? await marginsRepository.sessionTimeline(state.session.sessionId)
            : await marginsRepository.latestTimeline();
          const library = await libraryPatch();
          setState((current) => ({
            ...current,
            ...patchFromTimeline(timeline, current.personas, windowId, current.selectedQuestionId),
            ...library,
            loading: false,
          }));
          return true;
        })
        .catch((error) => {
          setState((current) => ({
            ...current,
            loading: false,
            error: error instanceof Error ? error.message : 'Unknown error',
          }));
          return false;
        });
    },
    debateAll(content: string) {
      if (!state.window) {
        return Promise.resolve(false);
      }
      const windowId = state.window.windowType === 'debate'
        ? state.window.windowId
        : debateWindowId(state.windows) || state.window.windowId;
      setState((current) => ({ ...current, loading: true, error: undefined }));
      return marginsRepository
        .debateAll(windowId, content)
        .then(async () => {
          const timeline = state.session
            ? await marginsRepository.sessionTimeline(state.session.sessionId)
            : await marginsRepository.latestTimeline();
          const library = await libraryPatch();
          setState((current) => ({
            ...current,
            ...patchFromTimeline(timeline, current.personas, windowId, current.selectedQuestionId),
            ...library,
            loading: false,
          }));
          return true;
        })
        .catch((error) => {
          setState((current) => ({
            ...current,
            loading: false,
            error: error instanceof Error ? error.message : 'Unknown error',
          }));
          return false;
        });
    },
    complete(summary: string) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.completeSession(sessionId, summary);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    createMetricSnapshot() {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const snapshot = await marginsRepository.createMetricSnapshot(sessionId);

        return {
          lastMetricSnapshot: snapshot,
        };
      });
    },
    updateProgress(progress: {
      readingGoal?: string;
      startPage?: number;
      currentPage?: number;
      targetPage?: number;
      progressNote?: string;
    }) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.updateSessionProgress(sessionId, progress);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    saveReview(review: SaveReadingSessionReviewRequest) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.saveReview(sessionId, review);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    updateSessionTitle(title: string) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.updateSessionTitle(sessionId, title);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    addHighlight(highlight: {
      pageNumber?: number;
      locationLabel?: string;
      quoteText: string;
      note?: string;
    }) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.createHighlight(sessionId, highlight);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    deleteHighlight(highlightId: number) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.deleteHighlight(sessionId, highlightId);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    createSessionTag(label: string) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.createSessionTag(sessionId, label);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    deleteSessionTag(tagId: number) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.deleteSessionTag(sessionId, tagId);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    createSessionInsight(insight: {
      insightType?: string;
      title?: string;
      content: string;
      evidence?: string;
    }) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.createSessionInsight(sessionId, insight);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    deleteSessionInsight(insightId: number) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.deleteSessionInsight(sessionId, insightId);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
    updateHighlight(
      highlightId: number,
      highlight: {
        pageNumber?: number;
        locationLabel?: string;
        quoteText: string;
        note?: string;
      },
    ) {
      if (!state.session) {
        return Promise.resolve(false);
      }

      const sessionId = state.session.sessionId;
      return run(async () => {
        const timeline = await marginsRepository.updateHighlight(sessionId, highlightId, highlight);
        const library = await libraryPatch();
        return {
          ...patchFromTimeline(timeline, state.personas, state.window?.windowId, state.selectedQuestionId),
          ...library,
        };
      });
    },
  };
}

export async function createDefaultSessionPatch(
  selectedBook: SaveBookResponse,
  personas: Persona[],
): Promise<Partial<SessionFlowState>> {
  const session = await marginsRepository.createSession(selectedBook);
  let preferredWindowId: number | undefined;
  let warning: string | undefined;

  try {
    const questionWindow = await marginsRepository.createWindow(session, 'question', 'Reflection Window');
    preferredWindowId = questionWindow.windowId;
    await marginsRepository.createWindow(session, 'debate', 'Persona Debate');
  } catch (error) {
    warning = defaultWindowWarning(error);
  }

  const timeline = await marginsRepository.sessionTimeline(session.sessionId);
  let library: Partial<SessionFlowState> = {};
  try {
    library = await libraryPatch();
  } catch (error) {
    warning = appendWarning(warning, libraryRefreshWarning(error));
  }
  writeStoredSessionId(session.sessionId);

  return {
    selectedBook,
    session,
    ...library,
    ...patchFromTimeline(timeline, personas, preferredWindowId),
    error: warning,
  };
}
