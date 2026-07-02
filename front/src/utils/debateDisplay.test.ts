import { describe, expect, it } from 'vitest';
import { debateTopicFromWindowTitle, personaIcon } from './debateDisplay';

describe('debate display helpers', () => {
  it('parses current and legacy debate title prefixes', () => {
    expect(debateTopicFromWindowTitle('Debate: How does ritual shape authority?')).toBe('How does ritual shape authority?');
    expect(debateTopicFromWindowTitle('토론: 권력은 어떻게 만들어지는가?')).toBe('권력은 어떻게 만들어지는가?');
    expect(debateTopicFromWindowTitle('Open room')).toBe('Open room');
  });

  it('maps professional persona labels to stable icons', () => {
    expect(personaIcon({ displayName: '문학평론가', name: 'literary-critic' })).toBe('§');
    expect(personaIcon({ displayName: '철학자', name: 'philosopher' })).toBe('?');
    expect(personaIcon({ displayName: '심리학자', name: 'psychologist' })).toBe('Ψ');
    expect(personaIcon({ displayName: 'Skeptical Reader', name: 'skeptical-reader' })).toBe('!');
    expect(personaIcon({ displayName: 'Custom Reader', name: 'reader-custom' })).toBe('•');
  });
});
