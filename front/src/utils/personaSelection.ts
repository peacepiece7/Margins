import type { Persona } from '../types/models/persona';

export function selectAvailablePersonaId(personas: Persona[], currentPersonaId?: number) {
  if (currentPersonaId && personas.some((persona) => persona.personaId === currentPersonaId)) {
    return currentPersonaId;
  }

  return personas[0]?.personaId;
}
