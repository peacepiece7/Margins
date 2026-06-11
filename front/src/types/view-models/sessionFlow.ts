import type { BookCandidate, SaveBookResponse } from '../models/book';
import type {
  AiMessageResponse,
  CreateReadingSessionResponse,
  CreateSessionWindowResponse,
} from '../models/session';

export interface SessionFlowState {
  query: string;
  candidates: BookCandidate[];
  selectedBook?: SaveBookResponse;
  session?: CreateReadingSessionResponse;
  window?: CreateSessionWindowResponse;
  messages: AiMessageResponse[];
  loading: boolean;
  error?: string;
}
