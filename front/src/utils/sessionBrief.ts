import type { SessionBriefSummary, SessionFlowState } from '../types/view-models/sessionFlow';

type Translate = (text: string, values?: Record<string, string | number>) => string;

const identityTranslate: Translate = (text, values = {}) => Object.entries(values).reduce(
  (result, [key, value]) => result.replaceAll(`{{${key}}}`, String(value)),
  text,
);

function countLabel(count: number, singular: string, plural: string, t: Translate) {
  return count === 1
    ? t('{{count}} {{label}}', { count, label: singular })
    : t('{{count}} {{label}}', { count, label: plural });
}

function progressValue(state: SessionFlowState, t: Translate) {
  if (state.currentPage !== undefined && state.targetPage !== undefined) {
    const percent = state.progressPercent !== undefined ? ` (${state.progressPercent}%)` : '';
    return `${t('Page {{current}} of {{target}}', { current: state.currentPage, target: state.targetPage })}${percent}`;
  }

  if (state.progressPercent !== undefined) {
    return t('{{percent}}% logged', { percent: state.progressPercent });
  }

  return t('Progress not set');
}

function focusValue(state: SessionFlowState, t: Translate) {
  const selectedQuestion = state.questions.find((question) => question.questionId === state.selectedQuestionId);
  if (selectedQuestion) {
    return selectedQuestion.questionText;
  }

  if (state.readingGoal) {
    return state.readingGoal;
  }

  return state.session?.title || state.selectedBook?.title || t('No active focus');
}

export function buildSessionBrief(state: SessionFlowState, t: Translate = identityTranslate): SessionBriefSummary {
  const answeredQuestionCount = state.stats?.answeredQuestionCount || 0;
  const personaReplyCount = state.stats?.personaResponseCount || 0;
  const latestHighlight = state.highlights[state.highlights.length - 1];
  const nextAction = state.nextActions[0];

  return {
    headline: state.session?.status === 'completed' ? t('Completed session brief') : t('Active session brief'),
    items: [
      {
        id: 'focus',
        label: t('Focus'),
        value: focusValue(state, t),
        detail: state.window?.title,
      },
      {
        id: 'progress',
        label: t('Progress'),
        value: progressValue(state, t),
        detail: state.progressNote,
      },
      {
        id: 'evidence',
        label: t('Evidence'),
        value: countLabel(state.highlights.length, t('quote'), t('quotes'), t),
        detail: latestHighlight?.locationLabel || latestHighlight?.quoteText,
      },
      {
        id: 'discussion',
        label: t('Discussion'),
        value: `${countLabel(answeredQuestionCount, t('answer'), t('answers'), t)} - ${countLabel(personaReplyCount, t('persona reply'), t('persona replies'), t)}`,
        detail: nextAction ? t(nextAction.label) : undefined,
      },
    ],
  };
}
