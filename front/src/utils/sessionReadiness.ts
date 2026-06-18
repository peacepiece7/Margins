import type { SessionFlowState, SessionReadinessSummary } from '../types/view-models/sessionFlow';

type Translate = (text: string, values?: Record<string, string | number>) => string;

const identityTranslate: Translate = (text, values = {}) => Object.entries(values).reduce(
  (result, [key, value]) => result.replaceAll(`{{${key}}}`, String(value)),
  text,
);

export function buildSessionReadiness(state: SessionFlowState, t: Translate = identityTranslate): SessionReadinessSummary {
  const progressSet = state.currentPage !== undefined && state.targetPage !== undefined;
  const questionCount = state.stats?.questionCount || 0;
  const answeredQuestionCount = state.stats?.answeredQuestionCount || 0;
  const personaReplyCount = state.stats?.personaResponseCount || 0;
  const completed = state.session?.status === 'completed';

  const items = [
    {
      id: 'progress',
      label: t('Progress'),
      value: progressSet ? `${state.progressPercent}%` : t('Not set'),
      complete: progressSet,
    },
    {
      id: 'questions',
      label: t('Questions'),
      value: t('{{count}} prompts', { count: questionCount }),
      complete: questionCount > 0,
    },
    {
      id: 'answers',
      label: t('Answers'),
      value: `${answeredQuestionCount}/${questionCount || 0}`,
      complete: answeredQuestionCount > 0,
    },
    {
      id: 'quotes',
      label: t('Quotes'),
      value: `${state.highlights.length}`,
      complete: state.highlights.length > 0,
    },
    {
      id: 'personas',
      label: t('Personas'),
      value: t('{{count}} replies', { count: personaReplyCount }),
      complete: personaReplyCount > 0,
    },
    {
      id: 'closeout',
      label: t('Closeout'),
      value: completed ? t('Completed') : t('Open'),
      complete: completed,
    },
  ];
  const completedCount = items.filter((item) => item.complete).length;

  return {
    completedCount,
    totalCount: items.length,
    percent: Math.round((completedCount / items.length) * 100),
    items,
  };
}
