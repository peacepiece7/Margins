export interface AiEvidenceItem {
  id: string;
  label: string;
  text: string;
}

interface SnapshotReference {
  id?: number | null;
  label?: string;
  text?: string;
}

interface SnapshotShape {
    references?: {
      question?: SnapshotReference | null;
      persona?: SnapshotReference | null;
      messages?: SnapshotReference[];
      highlights?: SnapshotReference[];
    };
}

export function buildAiEvidenceItems(contextSnapshot?: string): AiEvidenceItem[] {
  if (!contextSnapshot) {
    return [];
  }

  try {
    const snapshot = JSON.parse(contextSnapshot) as SnapshotShape;
    const references = snapshot.references;
    if (!references) {
      return [];
    }

    return [
      referenceItem('question', references.question),
      referenceItem('persona', references.persona),
      ...(references.highlights || []).slice(0, 3).map((highlight, index) => referenceItem(`highlight-${index}`, highlight)),
      ...(references.messages || []).slice(0, 3).map((message, index) => referenceItem(`message-${index}`, message)),
    ].filter((item): item is AiEvidenceItem => Boolean(item));
  } catch {
    return [];
  }
}

function referenceItem(prefix: string, reference?: SnapshotReference | null): AiEvidenceItem | undefined {
  if (!reference || !reference.text) {
    return undefined;
  }

  return {
    id: `${prefix}-${reference.id ?? reference.label ?? reference.text}`,
    label: reference.label || prefix,
    text: reference.text,
  };
}
