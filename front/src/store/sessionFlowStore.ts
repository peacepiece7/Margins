import { useState } from 'react';
import { marginsRepository } from '../repository/marginsRepository';
import type { BookCandidate } from '../types/models/book';
import type { SessionFlowState } from '../types/view-models/sessionFlow';

const initialState: SessionFlowState = {
  query: '',
  candidates: [],
  messages: [],
  loading: false,
};

export function useSessionFlowStore() {
  const [state, setState] = useState<SessionFlowState>(initialState);

  async function run(action: () => Promise<Partial<SessionFlowState>>) {
    setState((current) => ({ ...current, loading: true, error: undefined }));
    try {
      const patch = await action();
      setState((current) => ({ ...current, ...patch, loading: false }));
    } catch (error) {
      setState((current) => ({
        ...current,
        loading: false,
        error: error instanceof Error ? error.message : 'Unknown error',
      }));
    }
  }

  return {
    state,
    setQuery(query: string) {
      setState((current) => ({ ...current, query }));
    },
    search() {
      return run(async () => {
        const result = await marginsRepository.searchCandidates(state.query);
        return { candidates: result.candidates };
      });
    },
    selectCandidate(candidate: BookCandidate) {
      return run(async () => {
        const selectedBook = await marginsRepository.saveBook(candidate);
        const session = await marginsRepository.createSession(selectedBook);
        const window = await marginsRepository.createWindow(session);
        return { selectedBook, session, window, messages: [] };
      });
    },
    send(content: string) {
      if (!state.window) {
        return Promise.resolve();
      }
      const windowId = state.window.windowId;
      setState((current) => ({ ...current, loading: true, error: undefined }));
      return marginsRepository
        .sendMessage(windowId, content)
        .then((response) => {
          setState((current) => ({
            ...current,
            loading: false,
            messages: [
              ...current.messages,
              { id: `local-user-${Date.now()}`, role: 'user', content },
              {
                id: `assistant-${response.messageId}`,
                role: response.role,
                content: response.content,
                persistedMessageId: response.messageId,
              },
            ],
          }));
        })
        .catch((error) => {
          setState((current) => ({
            ...current,
            loading: false,
            error: error instanceof Error ? error.message : 'Unknown error',
          }));
        });
    },
    debate(content: string) {
      if (!state.window) {
        return Promise.resolve();
      }
      const windowId = state.window.windowId;
      setState((current) => ({ ...current, loading: true, error: undefined }));
      return marginsRepository
        .debate(windowId, content)
        .then((response) => {
          setState((current) => ({
            ...current,
            loading: false,
            messages: [
              ...current.messages,
              { id: `local-debate-${Date.now()}`, role: 'user', content },
              {
                id: `persona-${response.messageId}`,
                role: response.role,
                content: response.content,
                personaId: response.personaId,
                persistedMessageId: response.messageId,
              },
            ],
          }));
        })
        .catch((error) => {
          setState((current) => ({
            ...current,
            loading: false,
            error: error instanceof Error ? error.message : 'Unknown error',
          }));
        });
    },
  };
}
