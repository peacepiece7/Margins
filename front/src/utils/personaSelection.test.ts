import { describe, expect, it } from 'vitest';
import type { Persona } from '../types/models/persona';
import { selectAvailablePersonaId } from './personaSelection';

const personas: Persona[] = [
  {
    personaId: 10,
    name: 'skeptic',
    displayName: 'Skeptic',
  },
  {
    personaId: 20,
    name: 'historian',
    displayName: 'Historian',
  },
];

describe('selectAvailablePersonaId', () => {
  it('keeps the current persona when it is still available', () => {
    expect(selectAvailablePersonaId(personas, 20)).toBe(20);
  });

  it('falls back to the first available persona when the current persona disappeared', () => {
    expect(selectAvailablePersonaId(personas, 99)).toBe(10);
  });

  it('returns undefined when no personas are available', () => {
    expect(selectAvailablePersonaId([], 99)).toBeUndefined();
  });
});
