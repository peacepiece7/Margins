import type { BookCandidate, SaveBookResponse } from '../models/book';
import type {
  CreateReadingSessionResponse,
  CreateSessionWindowResponse,
} from '../models/session';

export interface SessionDisplayMessage {
  id: string;
  role: string;
  content: string;
  personaId?: number;
  persistedMessageId?: number;
}

export interface SessionFlowState {
  query: string;
  candidates: BookCandidate[];
  selectedBook?: SaveBookResponse;
  session?: CreateReadingSessionResponse;
  window?: CreateSessionWindowResponse;
  messages: SessionDisplayMessage[];
  loading: boolean;
  error?: string;
}
