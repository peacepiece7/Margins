import type { SessionBriefSummary, SessionFlowState } from '../types/view-models/sessionFlow';

function countLabel(count: number, singular: string, plural = `${singular}s`) {
  return `${count} ${count === 1 ? singular : plural}`;
}

function progressValue(state: SessionFlowState) {
  if (state.currentPage !== undefined && state.targetPage !== undefined) {
    const percent = state.progressPercent !== undefined ? ` (${state.progressPercent}%)` : '';
    return `Page ${state.currentPage} of ${state.targetPage}${percent}`;
  }

  if (state.progressPercent !== undefined) {
    return `${state.progressPercent}% logged`;
  }

  return 'Progress not set';
}

function focusValue(state: SessionFlowState) {
  const selectedQuestion = state.questions.find((question) => question.questionId === state.selectedQuestionId);
  if (selectedQuestion) {
    return selectedQuestion.questionText;
  }

  if (state.readingGoal) {
    return state.readingGoal;
  }

  return state.session?.title || state.selectedBook?.title || 'No active focus';
}

export function buildSessionBrief(state: SessionFlowState): SessionBriefSummary {
  const answeredQuestionCount = state.stats?.answeredQuestionCount || 0;
  const personaReplyCount = state.stats?.personaResponseCount || 0;
  const latestHighlight = state.highlights[state.highlights.length - 1];
  const nextAction = state.nextActions[0];

  return {
    headline: state.session?.status === 'completed' ? 'Completed session brief' : 'Active session brief',
    items: [
      {
        id: 'focus',
        label: 'Focus',
        value: focusValue(state),
        detail: state.window?.title,
      },
      {
        id: 'progress',
        label: 'Progress',
        value: progressValue(state),
        detail: state.progressNote,
      },
      {
        id: 'evidence',
        label: 'Evidence',
        value: countLabel(state.highlights.length, 'quote'),
        detail: latestHighlight?.locationLabel || latestHighlight?.quoteText,
      },
      {
        id: 'discussion',
        label: 'Discussion',
        value: `${countLabel(answeredQuestionCount, 'answer')} - ${countLabel(personaReplyCount, 'persona reply', 'persona replies')}`,
        detail: nextAction ? nextAction.label : undefined,
      },
    ],
  };
}
