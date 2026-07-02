import type { Persona } from '../types/models/persona';

const personaIconRules = [
  { icon: '⚔️', keywords: ['warrior', '전사'] },
  { icon: '🪄', keywords: ['wizard', '마법사'] },
  { icon: '✚', keywords: ['cleric', '성직자'] },
  { icon: '🗡️', keywords: ['rogue', '도적'] },
] as const;

export function debateTopicFromWindowTitle(title: string) {
  return title.replace(/^(Debate|토론): /, '');
}

export function personaIcon(persona?: Pick<Persona, 'displayName' | 'name'>) {
  const label = `${persona?.displayName || ''} ${persona?.name || ''}`.toLowerCase();
  const match = personaIconRules.find((rule) => rule.keywords.some((keyword) => label.includes(keyword)));

  return match?.icon || '👤';
}
