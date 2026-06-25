import type { Persona } from '../types/models/persona';
import type { SessionDisplayMessage } from '../types/view-models/sessionFlow';

export function selectAvailablePersonaId(personas: Persona[], currentPersonaId?: number) {
  if (currentPersonaId && personas.some((persona) => persona.personaId === currentPersonaId)) {
    return currentPersonaId;
  }

  return personas[0]?.personaId;
}

export function selectNextDebatePersona(personas: Persona[], messages: SessionDisplayMessage[]) {
  if (!personas.length) {
    return undefined;
  }

  const personaIds = new Set(personas.map((persona) => persona.personaId));
  const responseCounts = new Map<number, number>();

  messages.forEach((message) => {
    if (message.role === 'assistant' && message.personaId && personaIds.has(message.personaId)) {
      responseCounts.set(message.personaId, (responseCounts.get(message.personaId) || 0) + 1);
    }
  });

  return [...personas].sort((left, right) => {
    const replyDifference = (responseCounts.get(left.personaId) || 0) - (responseCounts.get(right.personaId) || 0);
    return replyDifference || left.personaId - right.personaId;
  })[0];
}
