import type { SessionFlowState, SessionReadinessSummary } from '../types/view-models/sessionFlow';

export function buildSessionReadiness(state: SessionFlowState): SessionReadinessSummary {
  const progressSet = state.currentPage !== undefined && state.targetPage !== undefined;
  const questionCount = state.stats?.questionCount || 0;
  const answeredQuestionCount = state.stats?.answeredQuestionCount || 0;
  const personaReplyCount = state.stats?.personaResponseCount || 0;
  const completed = state.session?.status === 'completed';

  const items = [
    {
      id: 'progress',
      label: 'Progress',
      value: progressSet ? `${state.progressPercent}%` : 'Not set',
      complete: progressSet,
    },
    {
      id: 'questions',
      label: 'Questions',
      value: `${questionCount} prompts`,
      complete: questionCount > 0,
    },
    {
      id: 'answers',
      label: 'Answers',
      value: `${answeredQuestionCount}/${questionCount || 0}`,
      complete: answeredQuestionCount > 0,
    },
    {
      id: 'quotes',
      label: 'Quotes',
      value: `${state.highlights.length}`,
      complete: state.highlights.length > 0,
    },
    {
      id: 'personas',
      label: 'Personas',
      value: `${personaReplyCount} replies`,
      complete: personaReplyCount > 0,
    },
    {
      id: 'closeout',
      label: 'Closeout',
      value: completed ? 'Completed' : 'Open',
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
