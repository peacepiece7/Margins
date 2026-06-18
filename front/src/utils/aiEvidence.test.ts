import { describe, expect, it } from 'vitest';
import { buildAiEvidenceItems } from './aiEvidence';

describe('buildAiEvidenceItems', () => {
  it('extracts question, persona, and recent message references', () => {
    const items = buildAiEvidenceItems(JSON.stringify({
      references: {
        question: { id: 9, label: 'Question #9', text: 'What changed?' },
        persona: { id: 4, label: 'Skeptic', text: 'Challenge weak claims.' },
        highlights: [
          { id: 12, label: 'Quote p. 42', text: 'A saved quote' },
        ],
        messages: [
          { id: 100, label: 'user', text: 'My current answer' },
        ],
      },
    }));

    expect(items).toEqual([
      { id: 'question-9', label: 'Question #9', text: 'What changed?' },
      { id: 'persona-4', label: 'Skeptic', text: 'Challenge weak claims.' },
      { id: 'highlight-0-12', label: 'Quote p. 42', text: 'A saved quote' },
      { id: 'message-0-100', label: 'user', text: 'My current answer' },
    ]);
  });

  it('ignores missing or malformed snapshots', () => {
    expect(buildAiEvidenceItems()).toEqual([]);
    expect(buildAiEvidenceItems('not-json')).toEqual([]);
    expect(buildAiEvidenceItems(JSON.stringify({ references: { question: null } }))).toEqual([]);
  });
});
