import type { Persona } from '../types/models/persona';

const personaIconRules = [
  { icon: '§', keywords: ['critic', '평론'] },
  { icon: '?', keywords: ['philosopher', '철학'] },
  { icon: 'Ψ', keywords: ['psychologist', '심리'] },
  { icon: '#', keywords: ['historian', '역사'] },
  { icon: '◎', keywords: ['sociologist', '사회'] },
  { icon: '✎', keywords: ['editor', '편집'] },
  { icon: '!', keywords: ['skeptical', '회의'] },
  { icon: '◆', keywords: ['facilitator', '진행'] },
] as const;

export function debateTopicFromWindowTitle(title: string) {
  return title.replace(/^(Debate|토론): /, '');
}

export function personaIcon(persona?: Pick<Persona, 'displayName' | 'name'>) {
  const label = `${persona?.displayName || ''} ${persona?.name || ''}`.toLowerCase();
  const match = personaIconRules.find((rule) => rule.keywords.some((keyword) => label.includes(keyword)));

  return match?.icon || '•';
}
