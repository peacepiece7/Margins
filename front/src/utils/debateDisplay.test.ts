import { describe, expect, it } from 'vitest';
import { debateTopicFromWindowTitle, personaIcon } from './debateDisplay';

describe('debate display helpers', () => {
  it('parses current and legacy debate title prefixes', () => {
    expect(debateTopicFromWindowTitle('Debate: How does ritual shape authority?')).toBe('How does ritual shape authority?');
    expect(debateTopicFromWindowTitle('토론: 권력은 어떻게 만들어지는가?')).toBe('권력은 어떻게 만들어지는가?');
    expect(debateTopicFromWindowTitle('Open room')).toBe('Open room');
  });

  it('maps professional and seeded persona labels to stable icons', () => {
    expect(personaIcon({ displayName: 'Warrior Ardan', name: 'warrior' })).toBe('⚔️');
    expect(personaIcon({ displayName: '전사 아르단', name: 'seed-warrior' })).toBe('⚔️');
    expect(personaIcon({ displayName: 'Wizard Lyra', name: 'wizard' })).toBe('🪄');
    expect(personaIcon({ displayName: '성직자 세렌', name: 'seed-cleric' })).toBe('✚');
    expect(personaIcon({ displayName: 'Skeptical Reader', name: 'skeptic' })).toBe('👤');
  });
});
