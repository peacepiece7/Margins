export interface Persona {
  personaId: number;
  name: string;
  displayName: string;
  description?: string;
  tone?: string;
  roleKey?: string;
}

export interface PersonaListResponse {
  personas: Persona[];
}

export interface PersonaDraft {
  displayName: string;
  description?: string;
  tone?: string;
  roleKey?: string;
  systemPrompt: string;
  reason?: string;
}

export interface PersonaDraftListResponse {
  aiModel?: string;
  personas: PersonaDraft[];
}
