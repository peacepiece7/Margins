export interface CreateReadingSessionResponse {
  sessionId: number;
  bookId: number;
  title: string;
  status: string;
}

export interface CreateSessionWindowResponse {
  windowId: number;
  sessionId: number;
  windowType: string;
  title: string;
  status: string;
}

export interface AiMessageResponse {
  messageId: number;
  windowId: number;
  personaId?: number;
  role: string;
  content: string;
  streamingReady: boolean;
  aiModel: string;
}
