import { FormEvent, useEffect, useRef, useState } from 'react';
import { useSessionFlow } from '../../hooks/useSessionFlow';
import { useI18n } from '../../i18n';
import type { PersonaDraft } from '../../types/models/persona';
import type { Question } from '../../types/models/session';
import type { ComposerMode } from '../../types/view-models/sessionFlow';
import { buildAiEvidenceItems } from '../../utils/aiEvidence';
import { markdownFilename } from '../../utils/exportFilename';
import { inputLimits, isNonBlankWithinMaxLength, isWithinMaxLength } from '../../utils/inputLimits';
import { isOptionalPageNumberDraft, parseOptionalPageNumber } from '../../utils/pageNumber';
import { personaRoleLabel } from '../../utils/personaRoleLabel';
import { buildSessionBrief } from '../../utils/sessionBrief';
import { buildSessionReadiness } from '../../utils/sessionReadiness';
import { testAttr } from '../../utils/testAttrs';

export function SessionWorkbench() {
  const flow = useSessionFlow();
  const { t } = useI18n();
  const [message, setMessage] = useState('');
  const [debate, setDebate] = useState('');
  const [personaName, setPersonaName] = useState('');
  const [personaTone, setPersonaTone] = useState('');
  const [personaDescription, setPersonaDescription] = useState('');
  const [personaInstructions, setPersonaInstructions] = useState('');
  const [closeoutSummary, setCloseoutSummary] = useState('');
  const [editingSessionTitle, setEditingSessionTitle] = useState(false);
  const [sessionTitleDraft, setSessionTitleDraft] = useState('');
  const [sessionTagDraft, setSessionTagDraft] = useState('');
  const [insightType, setInsightType] = useState('takeaway');
  const [insightTitle, setInsightTitle] = useState('');
  const [insightContent, setInsightContent] = useState('');
  const [insightEvidence, setInsightEvidence] = useState('');
  const [newWindowTitle, setNewWindowTitle] = useState('');
  const [editingWindowTitle, setEditingWindowTitle] = useState(false);
  const [windowTitleDraft, setWindowTitleDraft] = useState('');
  const [readingGoal, setReadingGoal] = useState('');
  const [startPage, setStartPage] = useState('');
  const [currentPage, setCurrentPage] = useState('');
  const [targetPage, setTargetPage] = useState('');
  const [progressNote, setProgressNote] = useState('');
  const [highlightPage, setHighlightPage] = useState('');
  const [highlightLocation, setHighlightLocation] = useState('');
  const [highlightQuote, setHighlightQuote] = useState('');
  const [highlightNote, setHighlightNote] = useState('');
  const [editingHighlightId, setEditingHighlightId] = useState<number | undefined>();
  const [editHighlightPage, setEditHighlightPage] = useState('');
  const [editHighlightLocation, setEditHighlightLocation] = useState('');
  const [editHighlightQuote, setEditHighlightQuote] = useState('');
  const [editHighlightNote, setEditHighlightNote] = useState('');
  const [libraryQuery, setLibraryQuery] = useState('');
  const [libraryStatus, setLibraryStatus] = useState('all');
  const [memorySearchQuery, setMemorySearchQuery] = useState('');
  const [savedBookQuery, setSavedBookQuery] = useState('');
  const [sessionSearchQuery, setSessionSearchQuery] = useState('');
  const [questionFilter, setQuestionFilter] = useState('all');
  const [customQuestionText, setCustomQuestionText] = useState('');
  const [questionDrafts, setQuestionDrafts] = useState<Question[]>([]);
  const [personaDrafts, setPersonaDrafts] = useState<PersonaDraft[]>([]);
  const [editingMessageId, setEditingMessageId] = useState<number | undefined>();
  const [editMessageContent, setEditMessageContent] = useState('');
  const [composerMode, setComposerMode] = useState<ComposerMode>('message');
  const readingGoalInputRef = useRef<HTMLInputElement>(null);
  const generateQuestionsButtonRef = useRef<HTMLButtonElement>(null);
  const messageInputRef = useRef<HTMLInputElement>(null);
  const selectedQuestionIdRef = useRef<number | undefined>(undefined);
  const highlightQuoteInputRef = useRef<HTMLInputElement>(null);
  const debateInputRef = useRef<HTMLInputElement>(null);
  const closeoutSummaryRef = useRef<HTMLTextAreaElement>(null);
  const questionPanelRef = useRef<HTMLElement>(null);
  const reviewReadinessRef = useRef<HTMLElement>(null);
  const activeMessages = flow.state.window
    ? flow.state.messages.filter((item) => item.windowId === flow.state.window?.windowId)
    : flow.state.messages;
  const activeMessagesWithStream = flow.state.streamingMessage
    && (!flow.state.window || flow.state.streamingMessage.windowId === flow.state.window.windowId)
    ? [...activeMessages, flow.state.streamingMessage]
    : activeMessages;
  const normalizedSessionSearch = sessionSearchQuery.trim().toLowerCase();
  const filteredActiveMessages = activeMessagesWithStream.filter((item) => {
    const searchableText = `${item.role} ${item.personaDisplayName || ''} ${item.content}`.toLowerCase();

    return !normalizedSessionSearch || searchableText.includes(normalizedSessionSearch);
  });
  function messageDisplayLabel(item: typeof activeMessagesWithStream[number]) {
    if (item.personaDisplayName) {
      return item.personaDisplayName;
    }
    if (item.personaId) {
      return t('Persona {{id}}', { id: item.personaId });
    }
    if (item.role === 'assistant') {
      return t('Book answer');
    }
    if (item.role === 'user' && item.questionId !== undefined) {
      return t('Reader answer');
    }
    if (item.role === 'user') {
      return t('Reader question');
    }
    return item.role;
  }
  const filteredHighlights = flow.state.highlights.filter((highlight) => {
    const searchableText = `${highlight.quoteText} ${highlight.note || ''} ${highlight.locationLabel || ''} ${highlight.pageNumber ?? ''}`.toLowerCase();

    return !normalizedSessionSearch || searchableText.includes(normalizedSessionSearch);
  });
  const activeQuestionWindow = flow.state.window?.windowType === 'question'
    ? flow.state.window
    : flow.state.windows.find((window) => window.windowType === 'question');
  const reflectionQuestions = activeQuestionWindow
    ? flow.state.questions.filter((question) => question.windowId === activeQuestionWindow.windowId)
    : flow.state.questions;
  const answeredQuestionIds = new Set(
    flow.state.messages
      .filter((item) => item.role === 'user' && item.questionId !== undefined)
      .map((item) => item.questionId as number),
  );
  const answeredReflectionCount = reflectionQuestions.filter((question) => answeredQuestionIds.has(question.questionId)).length;
  const filteredReflectionQuestions = reflectionQuestions.filter((question) => {
    const answered = answeredQuestionIds.has(question.questionId);
    return questionFilter === 'all'
      || (questionFilter === 'answered' && answered)
      || (questionFilter === 'unanswered' && !answered);
  });
  const selectedQuestion = flow.state.questions.find((question) => question.questionId === flow.state.selectedQuestionId);
  const selectedPersona = flow.state.personas.find((persona) => persona.personaId === flow.state.selectedPersonaId);
  const answeredQuestions = flow.state.questions
    .map((question) => ({
      question,
      answer: flow.state.messages.find((item) => item.role === 'user' && item.questionId === question.questionId),
    }))
    .filter((item) => item.answer);
  const personaResponses = flow.state.messages.filter((item) => item.role === 'assistant' && item.personaId);
  const libraryStats = flow.state.sessionSummaries.reduce(
    (stats, summary) => ({
      total: stats.total + 1,
      completed: stats.completed + (summary.status === 'completed' ? 1 : 0),
      active: stats.active + (summary.status === 'completed' ? 0 : 1),
      highlights: stats.highlights + (summary.highlightCount || 0),
      answers: stats.answers + (summary.answeredQuestionCount || 0),
      messages: stats.messages + summary.messageCount,
    }),
    { total: 0, completed: 0, active: 0, highlights: 0, answers: 0, messages: 0 },
  );
  const readerStats = flow.state.readerStats;
  const normalizedLibraryQuery = libraryQuery.trim().toLowerCase();
  const filteredSessionSummaries = flow.state.sessionSummaries.filter((summary) => {
    const matchesStatus =
      libraryStatus === 'all' ||
      (libraryStatus === 'completed' && summary.status === 'completed') ||
      (libraryStatus === 'active' && summary.status !== 'completed');
    const searchableText = `${summary.bookTitle} ${summary.bookAuthor || ''} ${summary.title} ${(summary.tags || []).map((tag) => tag.label).join(' ')}`.toLowerCase();

    return matchesStatus && (!normalizedLibraryQuery || searchableText.includes(normalizedLibraryQuery));
  });
  const normalizedSavedBookQuery = savedBookQuery.trim().toLowerCase();
  const filteredSavedBooks = flow.state.savedBooks.filter((book) => {
    const searchableText = `${book.title} ${book.author || ''}`.toLowerCase();

    return !normalizedSavedBookQuery || searchableText.includes(normalizedSavedBookQuery);
  });
  const sessionReadiness = buildSessionReadiness(flow.state, t);
  const sessionBrief = buildSessionBrief(flow.state, t);
  const progressPageDraftsValid = [startPage, currentPage, targetPage].every(isOptionalPageNumberDraft);
  const highlightPageDraftValid = isOptionalPageNumberDraft(highlightPage);
  const editHighlightPageDraftValid = isOptionalPageNumberDraft(editHighlightPage);
  const sessionTitleDraftValid = isNonBlankWithinMaxLength(sessionTitleDraft, inputLimits.readingSessionTitleEdit);
  const sessionTagDraftValid = isNonBlankWithinMaxLength(sessionTagDraft, inputLimits.sessionTag);
  const newWindowTitleValid = isNonBlankWithinMaxLength(newWindowTitle, inputLimits.sessionWindowTitle);
  const windowTitleDraftValid = isNonBlankWithinMaxLength(windowTitleDraft, inputLimits.sessionWindowTitleEdit);
  const highlightDraftValid = isNonBlankWithinMaxLength(highlightQuote, inputLimits.highlightQuote)
    && isWithinMaxLength(highlightLocation, inputLimits.highlightLocation)
    && isWithinMaxLength(highlightNote, inputLimits.highlightNote);
  const editHighlightDraftValid = isNonBlankWithinMaxLength(editHighlightQuote, inputLimits.highlightQuote)
    && isWithinMaxLength(editHighlightLocation, inputLimits.highlightLocation)
    && isWithinMaxLength(editHighlightNote, inputLimits.highlightNote);
  const personaDraftValid = isNonBlankWithinMaxLength(personaName, inputLimits.personaDisplayName)
    && isWithinMaxLength(personaTone, inputLimits.personaTone)
    && personaInstructions.trim().length > 0;
  const insightDraftValid = isNonBlankWithinMaxLength(insightContent, Number.MAX_SAFE_INTEGER)
    && isWithinMaxLength(insightType, inputLimits.insightType)
    && isWithinMaxLength(insightTitle, inputLimits.insightTitle);

  useEffect(() => {
    if (!flow.state.hydrated && !flow.state.loading) {
      void flow.loadLatest();
    }
  }, [flow.state.hydrated, flow.state.loading]);

  useEffect(() => {
    setCloseoutSummary(flow.state.sessionSummary || '');
  }, [flow.state.session?.sessionId, flow.state.sessionSummary]);

  useEffect(() => {
    setSessionTitleDraft(flow.state.session?.title || '');
    setEditingSessionTitle(false);
    setSessionSearchQuery('');
    setEditingMessageId(undefined);
    setEditMessageContent('');
  }, [flow.state.session?.sessionId, flow.state.session?.title]);

  useEffect(() => {
    setWindowTitleDraft(flow.state.window?.title || '');
    setEditingWindowTitle(false);
    setEditingMessageId(undefined);
    setEditMessageContent('');
  }, [flow.state.window?.windowId, flow.state.window?.title]);

  useEffect(() => {
    selectedQuestionIdRef.current = flow.state.selectedQuestionId;
  }, [flow.state.selectedQuestionId]);

  useEffect(() => {
    setReadingGoal(flow.state.readingGoal || '');
    setStartPage(flow.state.startPage === undefined ? '' : String(flow.state.startPage));
    setCurrentPage(flow.state.currentPage === undefined ? '' : String(flow.state.currentPage));
    setTargetPage(flow.state.targetPage === undefined ? '' : String(flow.state.targetPage));
    setProgressNote(flow.state.progressNote || '');
  }, [
    flow.state.session?.sessionId,
    flow.state.readingGoal,
    flow.state.startPage,
    flow.state.currentPage,
    flow.state.targetPage,
    flow.state.progressNote,
  ]);

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    void flow.search();
  }

  function submitMemorySearch(event: FormEvent) {
    event.preventDefault();
    if (!memorySearchQuery.trim()) {
      flow.clearReadingMemorySearch();
      return;
    }

    void flow.searchReadingMemory(memorySearchQuery.trim());
  }

  function submitMessage(event: FormEvent) {
    event.preventDefault();
    if (!message.trim()) {
      return;
    }

    void flow.send(message.trim(), selectedQuestionIdRef.current).then((saved) => {
      if (saved) {
        setMessage('');
      }
    });
  }

  function startEditMessage(messageId: number, content: string) {
    setEditingMessageId(messageId);
    setEditMessageContent(content);
  }

  function cancelEditMessage() {
    setEditingMessageId(undefined);
    setEditMessageContent('');
  }

  function submitEditMessage(event: FormEvent) {
    event.preventDefault();
    if (!editingMessageId || !editMessageContent.trim()) {
      return;
    }

    void flow.updateMessage(editingMessageId, editMessageContent.trim()).then((saved) => {
      if (saved) {
        cancelEditMessage();
      }
    });
  }

  function submitDebate(event: FormEvent) {
    event.preventDefault();
    if (!debate.trim()) {
      return;
    }

    void flow.debate(debate.trim()).then((saved) => {
      if (saved) {
        setDebate('');
      }
    });
  }

  function submitDebateAll() {
    if (!debate.trim()) {
      return;
    }

    void flow.debateAll(debate.trim()).then((saved) => {
      if (saved) {
        setDebate('');
      }
    });
  }

  function retryRoomPreparation() {
    if (reflectionQuestions.length === 0) {
      void flow.generateQuestions();
    }
    if (flow.state.personas.length === 0) {
      generatePersonaDrafts();
    }
  }

  function submitPersona(event: FormEvent) {
    event.preventDefault();
    if (!personaDraftValid) {
      return;
    }

    void flow.createPersona({
      displayName: personaName.trim(),
      tone: personaTone.trim() || undefined,
      description: personaDescription.trim() || undefined,
      systemPrompt: personaInstructions.trim(),
    }).then((saved) => {
      if (saved) {
        setPersonaName('');
        setPersonaTone('');
        setPersonaDescription('');
        setPersonaInstructions('');
      }
    });
  }

  function generatePersonaDrafts() {
    void flow.generatePersonaDrafts().then((drafts) => {
      setPersonaDrafts(drafts);
    });
  }

  function savePersonaDraft(draft: PersonaDraft) {
    void flow.createPersona({
      displayName: draft.displayName,
      tone: draft.tone,
      description: draft.description,
      roleKey: draft.roleKey,
      systemPrompt: draft.systemPrompt,
    }).then((saved) => {
      if (saved) {
        setPersonaDrafts((current) => current.filter((item) => item.displayName !== draft.displayName || item.systemPrompt !== draft.systemPrompt));
      }
    });
  }

  function submitWindow(event: FormEvent) {
    event.preventDefault();
    if (!newWindowTitleValid) {
      return;
    }

    void flow.addWindow(newWindowTitle.trim()).then((saved) => {
      if (saved) {
        setNewWindowTitle('');
      }
    });
  }

  function submitCustomQuestion(event: FormEvent) {
    event.preventDefault();
    if (!customQuestionText.trim()) {
      return;
    }

    void flow.createQuestion(customQuestionText.trim()).then((saved) => {
      if (saved) {
        setCustomQuestionText('');
      }
    });
  }

  function suggestQuestionDrafts() {
    void flow.suggestQuestions().then((drafts) => {
      setQuestionDrafts(drafts);
    });
  }

  function saveQuestionDraft(question: Question) {
    void flow.createQuestion(question.questionText).then((saved) => {
      if (saved) {
        setQuestionDrafts((current) => current.filter((item) => item.questionText !== question.questionText));
      }
    });
  }

  function submitWindowTitle(event: FormEvent) {
    event.preventDefault();
    if (!windowTitleDraftValid) {
      return;
    }

    void flow.updateWindowTitle(windowTitleDraft.trim()).then((saved) => {
      if (saved) {
        setEditingWindowTitle(false);
      }
    });
  }

  function submitComplete(event: FormEvent) {
    event.preventDefault();
    if (!closeoutSummary.trim()) {
      return;
    }

    void flow.complete(closeoutSummary.trim());
  }

  function focusElement(element: HTMLElement | null) {
    element?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    window.setTimeout(() => element?.focus(), 0);
  }

  function followNextAction(actionId: string, targetWindowId?: number, targetQuestionId?: number) {
    if (targetWindowId && flow.state.window?.windowId !== targetWindowId) {
      flow.selectWindow(targetWindowId);
    }
    if (targetQuestionId) {
      flow.selectQuestion(targetQuestionId);
    }

    window.setTimeout(() => {
      if (actionId === 'set_progress') {
        focusElement(readingGoalInputRef.current);
      } else if (actionId === 'generate_questions') {
        focusElement(generateQuestionsButtonRef.current);
      } else if (actionId === 'answer_open_question') {
        setComposerMode('message');
        focusElement(messageInputRef.current);
      } else if (actionId === 'save_highlight') {
        focusElement(highlightQuoteInputRef.current);
      } else if (actionId === 'ask_persona') {
        setComposerMode('persona');
        focusElement(debateInputRef.current);
      } else if (actionId === 'complete_session') {
        focusElement(closeoutSummaryRef.current);
      }
    }, 0);
  }

  function jumpToSessionArea(areaId: string) {
    if (areaId === 'questions') {
      focusElement(questionPanelRef.current);
    } else if (areaId === 'progress') {
      focusElement(readingGoalInputRef.current);
    } else if (areaId === 'quotes') {
      focusElement(highlightQuoteInputRef.current);
    } else if (areaId === 'messages') {
      setComposerMode('message');
      focusElement(messageInputRef.current);
    } else if (areaId === 'review') {
      focusElement(reviewReadinessRef.current);
    }
  }

  function submitSessionTitle(event: FormEvent) {
    event.preventDefault();
    if (!sessionTitleDraftValid) {
      return;
    }

    void flow.updateSessionTitle(sessionTitleDraft.trim()).then((saved) => {
      if (saved) {
        setEditingSessionTitle(false);
      }
    });
  }

  function submitSessionTag(event: FormEvent) {
    event.preventDefault();
    if (!sessionTagDraftValid) {
      return;
    }

    void flow.createSessionTag(sessionTagDraft.trim()).then((saved) => {
      if (saved) {
        setSessionTagDraft('');
      }
    });
  }

  function submitSessionInsight(event: FormEvent) {
    event.preventDefault();
    if (!insightDraftValid) {
      return;
    }

    void flow.createSessionInsight({
      insightType: insightType.trim() || 'takeaway',
      title: insightTitle.trim() || undefined,
      content: insightContent.trim(),
      evidence: insightEvidence.trim() || undefined,
    }).then((saved) => {
      if (saved) {
        setInsightType('takeaway');
        setInsightTitle('');
        setInsightContent('');
        setInsightEvidence('');
      }
    });
  }

  function pageValue(value: string) {
    return parseOptionalPageNumber(value);
  }

  function submitProgress(event: FormEvent) {
    event.preventDefault();
    if (!progressPageDraftsValid) {
      return;
    }

    void flow.updateProgress({
      readingGoal: readingGoal.trim() || undefined,
      startPage: pageValue(startPage),
      currentPage: pageValue(currentPage),
      targetPage: pageValue(targetPage),
      progressNote: progressNote.trim() || undefined,
    });
  }

  function submitHighlight(event: FormEvent) {
    event.preventDefault();
    if (!highlightPageDraftValid || !highlightDraftValid) {
      return;
    }

    void flow.addHighlight({
      pageNumber: pageValue(highlightPage),
      locationLabel: highlightLocation.trim() || undefined,
      quoteText: highlightQuote.trim(),
      note: highlightNote.trim() || undefined,
    }).then((saved) => {
      if (saved) {
        setHighlightPage('');
        setHighlightLocation('');
        setHighlightQuote('');
        setHighlightNote('');
      }
    });
  }

  function startEditHighlight(highlightId: number) {
    const highlight = flow.state.highlights.find((item) => item.highlightId === highlightId);
    if (!highlight) {
      return;
    }

    setEditingHighlightId(highlight.highlightId);
    setEditHighlightPage(highlight.pageNumber === undefined ? '' : String(highlight.pageNumber));
    setEditHighlightLocation(highlight.locationLabel || '');
    setEditHighlightQuote(highlight.quoteText);
    setEditHighlightNote(highlight.note || '');
  }

  function cancelEditHighlight() {
    setEditingHighlightId(undefined);
    setEditHighlightPage('');
    setEditHighlightLocation('');
    setEditHighlightQuote('');
    setEditHighlightNote('');
  }

  function submitEditHighlight(event: FormEvent) {
    event.preventDefault();
    if (!editingHighlightId || !editHighlightPageDraftValid || !editHighlightDraftValid) {
      return;
    }

    void flow.updateHighlight(editingHighlightId, {
      pageNumber: pageValue(editHighlightPage),
      locationLabel: editHighlightLocation.trim() || undefined,
      quoteText: editHighlightQuote.trim(),
      note: editHighlightNote.trim() || undefined,
    }).then((saved) => {
      if (saved) {
        cancelEditHighlight();
      }
    });
  }

  function markdownDownload(filename: string, lines: string[]) {
    const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  function reviewFilename() {
    return markdownFilename(flow.state.selectedBook?.title, 'review');
  }

  function transcriptFilename() {
    const title = flow.state.session?.title || flow.state.selectedBook?.title || 'session';
    return markdownFilename(title, 'transcript');
  }

  function exportTranscript() {
    const messagesByWindow = flow.state.windows.map((window) => ({
      window,
      messages: flow.state.messages.filter((item) => item.windowId === window.windowId),
    }));
    const lines = [
      `# ${flow.state.session?.title || flow.state.selectedBook?.title || 'Reading session'} transcript`,
      '',
      `Book: ${flow.state.selectedBook?.title || 'Unknown book'}`,
      flow.state.selectedBook?.author ? `Author: ${flow.state.selectedBook.author}` : undefined,
      `Status: ${flow.state.session?.status || 'unknown'}`,
      flow.state.currentPage !== undefined && flow.state.targetPage !== undefined
        ? `Progress: page ${flow.state.currentPage}/${flow.state.targetPage}${flow.state.progressPercent !== undefined ? ` (${flow.state.progressPercent}%)` : ''}`
        : undefined,
      flow.state.readingGoal ? `Goal: ${flow.state.readingGoal}` : undefined,
      flow.state.tags.length > 0 ? `Tags: ${flow.state.tags.map((tag) => tag.label).join(', ')}` : undefined,
      '',
      '## Highlights',
      ...(
        flow.state.highlights.length > 0
          ? flow.state.highlights.flatMap((highlight) => [
              `- ${highlight.pageNumber !== undefined ? `p. ${highlight.pageNumber}: ` : ''}${highlight.quoteText}`,
              highlight.locationLabel ? `  - Location: ${highlight.locationLabel}` : undefined,
              highlight.note ? `  - Note: ${highlight.note}` : undefined,
            ]).filter((line): line is string => Boolean(line))
          : ['No highlights recorded.']
      ),
      '',
      '## Questions',
      ...(
        flow.state.questions.length > 0
          ? flow.state.questions.map((question) => `- ${question.questionText}`)
          : ['No questions generated.']
      ),
      '',
      '## Messages',
      ...(
        messagesByWindow.length > 0
          ? messagesByWindow.flatMap(({ window, messages }) => [
              `### ${window.title}`,
              ...(
                messages.length > 0
                  ? messages.map((item) => `- ${messageDisplayLabel(item)}: ${item.content}`)
                  : ['- No messages in this window.']
              ),
              '',
            ])
          : ['No windows recorded.']
      ),
    ].filter((line): line is string => line !== undefined);

    markdownDownload(transcriptFilename(), lines);
  }

  function exportReview() {
    const lines = [
      `# ${flow.state.selectedBook?.title || 'Reading session'} review`,
      '',
      `Status: ${flow.state.session?.status || 'unknown'}`,
      flow.state.currentPage !== undefined && flow.state.targetPage !== undefined
        ? `Progress: page ${flow.state.currentPage}/${flow.state.targetPage}${flow.state.progressPercent !== undefined ? ` (${flow.state.progressPercent}%)` : ''}`
        : undefined,
      flow.state.tags.length > 0 ? `Tags: ${flow.state.tags.map((tag) => tag.label).join(', ')}` : undefined,
      '',
      '## Closeout',
      flow.state.sessionSummary || 'No closeout summary recorded.',
      '',
      '## Reading Goal',
      flow.state.readingGoal || 'No reading goal recorded.',
      '',
      '## Insights',
      ...(
        flow.state.insights.length > 0
          ? flow.state.insights.flatMap((insight) => [
              `- ${insight.title || insight.insightType}: ${insight.content}`,
              insight.evidence ? `  - Evidence: ${insight.evidence}` : undefined,
            ]).filter((line): line is string => Boolean(line))
          : ['No insights recorded.']
      ),
      '',
      '## Highlights',
      ...(
        flow.state.highlights.length > 0
          ? flow.state.highlights.flatMap((highlight) => [
              `- ${highlight.pageNumber !== undefined ? `p. ${highlight.pageNumber}: ` : ''}${highlight.quoteText}`,
              highlight.note ? `  - Note: ${highlight.note}` : undefined,
            ]).filter((line): line is string => Boolean(line))
          : ['No highlights recorded.']
      ),
      '',
      '## Answered Questions',
      ...(
        answeredQuestions.length > 0
          ? answeredQuestions.flatMap(({ question, answer }) => [
              `- ${question.questionText}`,
              `  - ${answer?.content || ''}`,
            ])
          : ['No answered questions recorded.']
      ),
      '',
      '## Persona Responses',
      ...(
        personaResponses.length > 0
          ? personaResponses.map((response) => `- ${response.personaDisplayName || 'Persona'}: ${response.content}`)
          : ['No persona responses recorded.']
      ),
      '',
    ].filter((line): line is string => line !== undefined);

    markdownDownload(reviewFilename(), lines);
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-6xl flex-col gap-6 px-5 py-6">
      <header className="flex items-center justify-between border-b border-stone-300 pb-4">
        <div>
          <h1 className="text-2xl font-semibold">Margins</h1>
          <p className="text-sm text-stone-600">{t('Reading session workbench')}</p>
        </div>
        <div className="text-right text-sm text-stone-600">
          {flow.state.session ? t('Session #{{id}}', { id: flow.state.session.sessionId }) : t('Single-user mode')}
        </div>
      </header>

      <nav className="grid gap-2 rounded border border-stone-300 bg-white p-3 sm:grid-cols-4" aria-label={t('Reading workflow')} {...testAttr('reading-workflow')}>
        {[
          { label: t('Add book'), value: flow.state.selectedBook ? flow.state.selectedBook.title : t('Search') },
          { label: t('Questions'), value: t('{{count}} prompts', { count: reflectionQuestions.length }) },
          { label: t('Personas'), value: t('{{count}} voices', { count: flow.state.personas.length }) },
          { label: t('Capture'), value: t('{{count}} quotes', { count: flow.state.highlights.length }) },
        ].map((item) => (
          <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2" key={item.label}>
            <div className="text-xs font-semibold uppercase text-stone-500">{item.label}</div>
            <div className="mt-1 truncate text-sm font-medium text-stone-900">{item.value}</div>
          </div>
        ))}
      </nav>

      <section className="grid gap-5 lg:grid-cols-[360px_1fr]">
        <aside className="flex flex-col gap-4">
          <form className="flex gap-2" onSubmit={submitSearch} {...testAttr('book-search-form')}>
            <input
              className="min-w-0 flex-1 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
              onChange={(event) => flow.setQuery(event.target.value)}
              placeholder={t('Search a book')}
              value={flow.state.query}
              {...testAttr('book-search-input')}
            />
            <button
              className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
              disabled={flow.state.loading || !flow.state.query.trim()}
              type="submit"
              {...testAttr('book-search-submit')}
            >
              {t('Search')}
            </button>
          </form>

          {flow.state.error && (
            <div className="grid gap-2 rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-800 sm:grid-cols-[minmax(0,1fr)_auto]" {...testAttr('error-message')}>
              <div>{flow.state.error}</div>
              <button
                className="rounded border border-red-300 bg-white px-3 py-1 text-xs font-medium text-red-800 disabled:opacity-50"
                disabled={flow.state.loading}
                onClick={() => void flow.loadLatest()}
                type="button"
                {...testAttr('error-retry')}
              >
                {t('Retry')}
              </button>
            </div>
          )}

          <details className="rounded border border-stone-300 bg-white p-3" {...testAttr('library-history-details')}>
            <summary className="cursor-pointer text-sm font-semibold text-stone-900">{t('Library and history')}</summary>
            <div className="mt-3 flex flex-col gap-4">
          {(flow.state.sessionSummaries.length > 0 || readerStats) && (
            <section className="grid grid-cols-2 gap-2 rounded border border-stone-300 bg-white p-3" {...testAttr('library-dashboard')}>
              <div className="col-span-2 flex items-center justify-between">
                <div className="text-sm font-semibold">{t('Library dashboard')}</div>
                <div className="text-xs text-stone-500" {...testAttr('reader-session-count')}>
                  {t('{{count}} sessions', { count: readerStats?.sessionCount ?? libraryStats.total })}
                </div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Completed')}</div>
                <div className="text-sm font-semibold" {...testAttr('reader-completed-count')}>{readerStats?.completedSessionCount ?? libraryStats.completed}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Active')}</div>
                <div className="text-sm font-semibold" {...testAttr('reader-active-count')}>{readerStats?.activeSessionCount ?? libraryStats.active}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Quotes')}</div>
                <div className="text-sm font-semibold" {...testAttr('reader-highlight-count')}>{readerStats?.highlightCount ?? libraryStats.highlights}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Answers')}</div>
                <div className="text-sm font-semibold" {...testAttr('reader-answer-count')}>{readerStats?.answeredQuestionCount ?? libraryStats.answers}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Books')}</div>
                <div className="text-sm font-semibold" {...testAttr('reader-book-count')}>{readerStats?.distinctBookCount ?? flow.state.savedBooks.length}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Avg progress')}</div>
                <div className="text-sm font-semibold" {...testAttr('reader-average-progress')}>
                  {readerStats?.averageProgressPercent != null ? `${readerStats.averageProgressPercent}%` : '-'}
                </div>
              </div>
            </section>
          )}

          {flow.state.sessionSummaries.length > 0 && (
            <section className="flex flex-col gap-2 rounded border border-stone-300 bg-white p-3" {...testAttr('memory-search')}>
              <div>
                <div className="text-sm font-semibold">{t('Reading memory')}</div>
                <div className="text-xs text-stone-500">
                  {t('{{count}} matches', { count: flow.state.memorySearchResults.length })}
                </div>
              </div>
              <form className="flex gap-2" onSubmit={submitMemorySearch} {...testAttr('memory-search-form')}>
                <input
                  className="min-w-0 flex-1 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                  onChange={(event) => setMemorySearchQuery(event.target.value)}
                  placeholder={t('Search notes, debate, quotes')}
                  value={memorySearchQuery}
                  {...testAttr('memory-search-input')}
                />
                <button
                  className="rounded border border-stone-900 px-3 py-2 text-xs font-medium disabled:opacity-50"
                  disabled={flow.state.loading || !memorySearchQuery.trim()}
                  type="submit"
                  {...testAttr('memory-search-submit')}
                >
                  {t('Find')}
                </button>
              </form>
              {flow.state.memorySearchResults.length > 0 && (
                <div className="flex flex-col gap-2" {...testAttr('memory-search-results')}>
                  {flow.state.memorySearchResults.map((result) => (
                    <button
                      className="rounded border border-stone-200 bg-stone-50 p-2 text-left hover:border-stone-700"
                      key={`${result.resultType}-${result.sourceId}`}
                      onClick={() => void flow.loadSession(result.sessionId)}
                      type="button"
                      {...testAttr('memory-search-result')}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="truncate text-sm font-medium">{result.bookTitle}</div>
                        <span className="shrink-0 rounded border border-stone-300 bg-white px-1.5 py-0.5 text-[11px] uppercase text-stone-600">
                          {result.resultType}
                        </span>
                      </div>
                      <div className="truncate text-xs text-stone-500">{result.sessionTitle}</div>
                      <div className="mt-1 line-clamp-2 text-xs leading-5 text-stone-700">{result.snippet}</div>
                    </button>
                  ))}
                </div>
              )}
            </section>
          )}

          {flow.state.savedBooks.length > 0 && (
            <section className="flex flex-col gap-2 rounded border border-stone-300 bg-white p-3" {...testAttr('saved-book-library')}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm font-semibold">{t('Saved books')}</div>
                  <div className="text-xs text-stone-500">
                    {t('{{visible}}/{{total}} available', { visible: filteredSavedBooks.length, total: flow.state.savedBooks.length })}
                  </div>
                </div>
              </div>
              <input
                className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                onChange={(event) => setSavedBookQuery(event.target.value)}
                placeholder={t('Filter saved books')}
                value={savedBookQuery}
                {...testAttr('saved-book-search')}
              />
              <div className="flex flex-col gap-2">
                {filteredSavedBooks.map((book) => (
                  <article className="rounded border border-stone-200 bg-stone-50 p-2" key={book.bookId} {...testAttr('saved-book-item')}>
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <div className="truncate text-sm font-medium">{book.title}</div>
                        <div className="truncate text-xs text-stone-500">{book.author || t('Unknown author')}</div>
                      </div>
                      <button
                        className="shrink-0 rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600 disabled:opacity-50"
                        disabled={flow.state.loading}
                        onClick={() => void flow.startSessionFromBook(book)}
                        type="button"
                        {...testAttr('saved-book-start-session')}
                      >
                        {t('Start')}
                      </button>
                    </div>
                  </article>
                ))}
                {filteredSavedBooks.length === 0 && (
                  <div className="rounded border border-stone-200 bg-stone-50 px-3 py-4 text-sm text-stone-500" {...testAttr('saved-book-empty')}>
                    {t('No saved books match the current filter.')}
                  </div>
                )}
              </div>
            </section>
          )}

          {flow.state.sessionSummaries.length > 0 && (
            <section className="flex flex-col gap-2 rounded border border-stone-300 bg-white p-3" {...testAttr('session-library')}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm font-semibold">{t('Reading sessions')}</div>
                  <div className="text-xs text-stone-500">
                    {t('{{visible}}/{{total}} saved', { visible: filteredSessionSummaries.length, total: flow.state.sessionSummaries.length })}
                  </div>
                </div>
              </div>
              <div className="grid gap-2 sm:grid-cols-[1fr_130px]" {...testAttr('session-library-filters')}>
                <input
                  className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                  onChange={(event) => setLibraryQuery(event.target.value)}
                  placeholder={t('Filter sessions')}
                  value={libraryQuery}
                  {...testAttr('session-library-search')}
                />
                <select
                  className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                  onChange={(event) => setLibraryStatus(event.target.value)}
                  value={libraryStatus}
                  {...testAttr('session-library-status')}
                >
                  <option value="all">{t('All')}</option>
                  <option value="active">{t('Active')}</option>
                  <option value="completed">{t('Completed')}</option>
                </select>
              </div>
              <div className="flex flex-col gap-2">
                {filteredSessionSummaries.map((summary) => {
                  const active = flow.state.session?.sessionId === summary.sessionId;

                  return (
                    <article
                      className={`rounded border p-2 text-left ${
                        active
                          ? 'border-stone-900 bg-stone-900 text-white'
                          : 'border-stone-200 bg-stone-50 text-stone-700 hover:border-stone-700'
                      }`}
                      key={summary.sessionId}
                      {...testAttr('session-library-item')}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <button
                          className="min-w-0 flex-1 text-left"
                          onClick={() => void flow.loadSession(summary.sessionId)}
                          type="button"
                          {...testAttr('session-select')}
                        >
                          <div className="text-sm font-medium">{summary.bookTitle}</div>
                          <div className={`truncate text-xs ${active ? 'text-stone-100' : 'text-stone-600'}`}>{summary.title}</div>
                          <div className={`text-xs ${active ? 'text-stone-200' : 'text-stone-500'}`}>
                            {summary.progressPercent !== undefined ? `${summary.progressPercent}% - ` : ''}
                            {summary.messageCount} messages - {summary.status}
                          </div>
                          {summary.pinned && (
                            <div className={`mt-1 inline-flex rounded border px-1.5 py-0.5 text-[11px] font-medium ${
                              active ? 'border-stone-500 text-stone-100' : 'border-stone-300 bg-white text-stone-700'
                            }`} {...testAttr('session-pin-badge')}>
                              {t('Pinned')}
                            </div>
                          )}
                          {summary.tags && summary.tags.length > 0 && (
                            <div className="mt-1 flex flex-wrap gap-1" {...testAttr('session-library-tags')}>
                              {summary.tags.map((tag) => (
                                <span
                                  className={`rounded border px-1.5 py-0.5 text-[11px] ${
                                    active ? 'border-stone-500 text-stone-100' : 'border-stone-300 bg-white text-stone-600'
                                  }`}
                                  key={tag.tagId}
                                >
                                  {tag.label}
                                </span>
                              ))}
                            </div>
                          )}
                        </button>
                        <div className="flex shrink-0 flex-col gap-1">
                          <button
                            className={`rounded border px-2 py-1 text-xs font-medium disabled:opacity-50 ${
                              active
                                ? 'border-stone-500 bg-stone-800 text-stone-100'
                                : 'border-stone-300 bg-white text-stone-600'
                            }`}
                            disabled={flow.state.loading}
                            onClick={() => void flow.updateSessionPinned(summary.sessionId, !summary.pinned)}
                            type="button"
                            {...testAttr('session-pin-submit')}
                          >
                            {summary.pinned ? 'Unpin' : 'Pin'}
                          </button>
                          <button
                            className={`rounded border px-2 py-1 text-xs font-medium disabled:opacity-50 ${
                              active
                                ? 'border-stone-500 bg-stone-800 text-stone-100'
                                : 'border-stone-300 bg-white text-stone-600'
                            }`}
                            disabled={flow.state.loading}
                            onClick={() => void flow.archiveSession(summary.sessionId)}
                            type="button"
                            {...testAttr('session-archive-submit')}
                          >
                            Archive
                          </button>
                        </div>
                      </div>
                    </article>
                  );
                })}
                {filteredSessionSummaries.length === 0 && (
                  <div className="rounded border border-stone-200 bg-stone-50 px-3 py-4 text-sm text-stone-500" {...testAttr('session-library-empty')}>
                    No sessions match the current filters.
                  </div>
                )}
              </div>
            </section>
          )}
            </div>
          </details>

          <div className="flex flex-col gap-2" {...testAttr('candidate-list')}>
            {flow.state.candidates.map((candidate) => (
              <button
                className="rounded border border-stone-300 bg-white p-3 text-left hover:border-stone-800"
                key={candidate.candidateId}
                onClick={() => void flow.selectCandidate(candidate)}
                type="button"
                {...testAttr('candidate-select')}
              >
                <div className="font-medium">{candidate.title}</div>
                <div className="text-sm text-stone-600">{candidate.author}</div>
                {candidate.reason && <div className="mt-2 text-xs text-stone-500">{candidate.reason}</div>}
              </button>
            ))}
          </div>

          {flow.state.session && (
            <section
              className="flex flex-col gap-3 rounded border border-stone-300 bg-white p-3 focus:outline-none focus:ring-2 focus:ring-stone-400"
              ref={questionPanelRef}
              tabIndex={-1}
              {...testAttr('question-panel')}
            >
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-sm font-semibold">{t('Reflection questions')}</div>
                  <div className="text-xs text-stone-500">
                    {activeQuestionWindow?.title || t('No reflection window')} - {t('{{answered}}/{{total}} answered', { answered: answeredReflectionCount, total: reflectionQuestions.length })}
                  </div>
                </div>
                <div className="flex flex-wrap justify-end gap-2">
                  <button
                    className="rounded border border-stone-300 px-3 py-2 text-xs font-medium disabled:opacity-50"
                    disabled={flow.state.loading || !activeQuestionWindow}
                    onClick={suggestQuestionDrafts}
                    type="button"
                    {...testAttr('suggest-questions')}
                  >
                    {t('Draft')}
                  </button>
                  <button
                    className="rounded border border-stone-900 px-3 py-2 text-xs font-medium disabled:opacity-50"
                    disabled={flow.state.loading || !activeQuestionWindow}
                    onClick={() => void flow.generateQuestions()}
                    ref={generateQuestionsButtonRef}
                    type="button"
                    {...testAttr('generate-questions')}
                  >
                    {t('Generate')}
                  </button>
                </div>
              </div>

              <select
                className="rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                onChange={(event) => setQuestionFilter(event.target.value)}
                value={questionFilter}
                {...testAttr('question-filter')}
              >
                <option value="all">{t('All questions')}</option>
                <option value="unanswered">{t('Unanswered')}</option>
                <option value="answered">{t('Answered')}</option>
              </select>

              <form className="grid gap-2 sm:grid-cols-[1fr_auto]" onSubmit={submitCustomQuestion} {...testAttr('question-create-form')}>
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  onChange={(event) => setCustomQuestionText(event.target.value)}
                  placeholder={t('Add your own question')}
                  value={customQuestionText}
                  {...testAttr('question-create-input')}
                />
                <button
                  className="rounded border border-stone-900 px-3 py-2 text-xs font-medium disabled:opacity-50"
                  disabled={flow.state.loading || !activeQuestionWindow || !customQuestionText.trim()}
                  type="submit"
                  {...testAttr('question-create-submit')}
                >
                  {t('Add')}
                </button>
              </form>

              {questionDrafts.length > 0 && (
                <div className="grid gap-2" {...testAttr('question-drafts')}>
                  {questionDrafts.map((question) => (
                    <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2" key={question.questionText}>
                      <div className="text-sm leading-5 text-stone-800">{question.questionText}</div>
                      <div className="mt-2 flex items-center justify-between gap-3">
                        <span className="text-xs text-stone-500">{question.questionType || t('Question')}</span>
                        <button
                          className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium disabled:opacity-50"
                          disabled={flow.state.loading}
                          onClick={() => saveQuestionDraft(question)}
                          type="button"
                          {...testAttr('question-draft-save')}
                        >
                          {t('Save')}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="flex flex-col gap-2">
                {filteredReflectionQuestions.map((question) => {
                  const active = flow.state.selectedQuestionId === question.questionId;
                  const answered = answeredQuestionIds.has(question.questionId);

                  return (
                    <div
                      className={`rounded border ${
                        active
                          ? 'border-stone-900 bg-stone-900 text-white'
                          : 'border-stone-200 bg-stone-50 text-stone-700'
                      }`}
                      key={question.questionId}
                      {...testAttr('question-item')}
                    >
                      <button
                        className={`grid w-full gap-2 p-2 text-left text-sm leading-5 focus:outline-none ${
                          active ? 'text-white' : 'text-stone-700 hover:bg-white'
                        }`}
                        onClick={() => {
                          selectedQuestionIdRef.current = question.questionId;
                          flow.selectQuestion(question.questionId);
                        }}
                        type="button"
                        {...testAttr('question-select')}
                      >
                        <span className="flex items-start justify-between gap-2">
                          <span className="min-w-0 flex-1">{question.questionText}</span>
                          <span className={`shrink-0 rounded px-2 py-0.5 text-[11px] font-medium ${active ? 'bg-white text-stone-900' : 'bg-white text-stone-600'}`} {...testAttr('question-answer-status')}>
                            {answered ? t('Answered') : t('Open')}
                          </span>
                        </span>
                        <span className={`text-xs ${active ? 'text-stone-200' : 'text-stone-500'}`}>
                          {t('Select prompt')}
                        </span>
                      </button>
                      {!answered && (
                        <div className="border-t border-stone-200 px-2 py-1 text-right">
                          <button
                            className={`rounded border px-2 py-1 text-xs font-medium disabled:opacity-50 ${active ? 'border-white bg-stone-800 text-white' : 'border-stone-300 bg-white text-stone-600'}`}
                            disabled={flow.state.loading}
                            onClick={() => void flow.deleteQuestion(question.questionId)}
                            type="button"
                            {...testAttr('question-delete-submit')}
                          >
                            {t('Delete')}
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
                {filteredReflectionQuestions.length === 0 && (
                  <div className="rounded border border-stone-200 bg-stone-50 px-3 py-4 text-sm text-stone-500" {...testAttr('question-empty')}>
                    {t('No questions match this filter.')}
                  </div>
                )}
              </div>
            </section>
          )}
        </aside>

        <section className="flex min-h-[640px] flex-col rounded border border-stone-300 bg-white">
          <div className="border-b border-stone-200 px-4 py-3" {...testAttr('session-summary')}>
            <div className="text-sm text-stone-500">{t('Current book')}</div>
            <div className="text-lg font-semibold">
              {flow.state.selectedBook ? flow.state.selectedBook.title : t('No book selected')}
            </div>
            {flow.state.session && (
              <div className="mt-3" {...testAttr('session-title-panel')}>
                {editingSessionTitle ? (
                  <form className="flex flex-wrap gap-2" onSubmit={submitSessionTitle} {...testAttr('session-title-form')}>
                    <input
                      className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                      maxLength={inputLimits.readingSessionTitleEdit}
                      onChange={(event) => setSessionTitleDraft(event.target.value)}
                      value={sessionTitleDraft}
                      {...testAttr('session-title-input')}
                    />
                    <button
                      className="rounded border border-stone-900 px-3 py-2 text-xs font-medium disabled:opacity-50"
                      disabled={flow.state.loading || !sessionTitleDraftValid}
                      type="submit"
                      {...testAttr('session-title-save')}
                    >
                      {t('Save')}
                    </button>
                    <button
                      className="rounded border border-stone-300 px-3 py-2 text-xs font-medium text-stone-600"
                      onClick={() => {
                        setSessionTitleDraft(flow.state.session?.title || '');
                        setEditingSessionTitle(false);
                      }}
                      type="button"
                      {...testAttr('session-title-cancel')}
                    >
                      {t('Cancel')}
                    </button>
                  </form>
                ) : (
                  <div className="grid gap-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <div className="text-sm font-medium text-stone-700">{flow.state.session.title}</div>
                      <button
                        className="rounded border border-stone-300 px-2 py-1 text-xs font-medium text-stone-600"
                        onClick={() => setEditingSessionTitle(true)}
                        type="button"
                        {...testAttr('session-title-edit')}
                      >
                        {t('Edit title')}
                      </button>
                      <button
                        className="rounded border border-stone-900 px-2 py-1 text-xs font-medium text-stone-700 disabled:opacity-50"
                        disabled={flow.state.loading}
                        onClick={exportTranscript}
                        type="button"
                        {...testAttr('session-transcript-export-submit')}
                      >
                        {t('Export transcript')}
                      </button>
                    </div>
                    <div className="flex flex-wrap items-center gap-2" {...testAttr('session-tag-list')}>
                      {flow.state.tags.map((tag) => (
                        <span className="inline-flex items-center gap-1 rounded border border-stone-300 bg-stone-50 px-2 py-1 text-xs text-stone-700" key={tag.tagId}>
                          {tag.label}
                          <button
                            className="font-semibold text-stone-500 disabled:opacity-50"
                            disabled={flow.state.loading}
                            onClick={() => void flow.deleteSessionTag(tag.tagId)}
                            type="button"
                            {...testAttr('session-tag-delete')}
                          >
                            x
                          </button>
                        </span>
                      ))}
                    </div>
                    <form className="flex max-w-md gap-2" onSubmit={submitSessionTag} {...testAttr('session-tag-form')}>
                      <input
                        className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                        maxLength={inputLimits.sessionTag}
                        onChange={(event) => setSessionTagDraft(event.target.value)}
                        placeholder={t('Add tag')}
                        value={sessionTagDraft}
                        {...testAttr('session-tag-input')}
                      />
                      <button
                        className="rounded border border-stone-900 px-3 py-2 text-xs font-medium disabled:opacity-50"
                        disabled={flow.state.loading || !sessionTagDraftValid}
                        type="submit"
                        {...testAttr('session-tag-submit')}
                      >
                        {t('Add')}
                      </button>
                    </form>
                  </div>
                )}
              </div>
            )}
            {flow.state.window && (
              <div className="mt-1 text-sm text-stone-600">
                Window #{flow.state.window.windowId} - {flow.state.window.status}
              </div>
            )}
          </div>

          {flow.state.session && (
            <section className="grid gap-3 border-b border-stone-200 bg-stone-50 px-4 py-4" {...testAttr('reading-room-board')}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="text-xs font-semibold uppercase text-stone-500">{t('Reading room')}</div>
                  <div className="mt-1 text-lg font-semibold text-stone-900" {...testAttr('reading-room-title')}>
                    {flow.state.selectedBook ? flow.state.selectedBook.title : t('No book selected')}
                  </div>
                </div>
                {(reflectionQuestions.length === 0 || flow.state.personas.length === 0) && (
                  <button
                    className="rounded border border-stone-900 bg-white px-3 py-2 text-xs font-medium disabled:opacity-50"
                    disabled={flow.state.loading}
                    onClick={retryRoomPreparation}
                    type="button"
                    {...testAttr('reading-room-prepare-retry')}
                  >
                    {t('Prepare room')}
                  </button>
                )}
              </div>

              <div className="grid gap-3 xl:grid-cols-4">
                <section className="min-w-0 rounded border border-stone-200 bg-white p-3" {...testAttr('reading-room-questions')}>
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-xs font-semibold uppercase text-stone-500">{t('Questions')}</div>
                      <div className="mt-1 text-sm font-medium text-stone-900">
                        {t('{{answered}}/{{total}} answered', { answered: answeredReflectionCount, total: reflectionQuestions.length })}
                      </div>
                    </div>
                    <button
                      className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600"
                      onClick={() => jumpToSessionArea('questions')}
                      type="button"
                      {...testAttr('reading-room-questions-jump')}
                    >
                      {t('Open')}
                    </button>
                  </div>
                  <div className="mt-3 grid gap-2">
                    {reflectionQuestions.slice(0, 3).map((question) => (
                      <button
                        className="rounded border border-stone-200 bg-stone-50 px-3 py-2 text-left text-xs leading-5 text-stone-700 hover:border-stone-500"
                        key={question.questionId}
                        onClick={() => {
                          if (question.windowId) {
                            flow.selectWindow(question.windowId);
                          }
                          selectedQuestionIdRef.current = question.questionId;
                          flow.selectQuestion(question.questionId);
                          setComposerMode('message');
                          focusElement(messageInputRef.current);
                        }}
                        type="button"
                        {...testAttr('reading-room-question-item')}
                      >
                        {question.questionText}
                      </button>
                    ))}
                    {reflectionQuestions.length === 0 && (
                      <div className="rounded border border-stone-200 bg-stone-50 px-3 py-4 text-sm text-stone-500" {...testAttr('reading-room-questions-empty')}>
                        {t('No questions yet.')}
                      </div>
                    )}
                  </div>
                </section>

                <section className="min-w-0 rounded border border-stone-200 bg-white p-3" {...testAttr('reading-room-personas')}>
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-xs font-semibold uppercase text-stone-500">{t('Persona cast')}</div>
                      <div className="mt-1 text-sm font-medium text-stone-900">
                        {t('{{count}} voices', { count: flow.state.personas.length })}
                      </div>
                    </div>
                    <button
                      className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600"
                      onClick={() => {
                        setComposerMode('persona');
                        focusElement(debateInputRef.current);
                      }}
                      type="button"
                      {...testAttr('reading-room-personas-jump')}
                    >
                      {t('Debate')}
                    </button>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {flow.state.personas.slice(0, 4).map((persona) => (
                      <button
                        className={`rounded border px-2 py-1 text-left text-xs font-medium ${
                          selectedPersona?.personaId === persona.personaId
                            ? 'border-stone-900 bg-stone-900 text-white'
                            : 'border-stone-300 bg-stone-50 text-stone-700'
                        }`}
                        key={persona.personaId}
                        onClick={() => flow.selectPersona(persona.personaId)}
                        type="button"
                        {...testAttr('reading-room-persona-item')}
                      >
                        <span className="block">{persona.displayName}</span>
                        <span className="mt-1 block text-[11px] uppercase opacity-70" {...testAttr('persona-role-label')}>
                          {t(personaRoleLabel(persona.roleKey))}
                        </span>
                      </button>
                    ))}
                    {flow.state.personas.length === 0 && (
                      <div className="rounded border border-stone-200 bg-stone-50 px-3 py-4 text-sm text-stone-500" {...testAttr('reading-room-personas-empty')}>
                        {t('No personas yet.')}
                      </div>
                    )}
                  </div>
                </section>

                <section className="min-w-0 rounded border border-stone-200 bg-white p-3" {...testAttr('reading-room-capture')}>
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-xs font-semibold uppercase text-stone-500">{t('Capture')}</div>
                      <div className="mt-1 text-sm font-medium text-stone-900">
                        {t('{{count}} quotes', { count: flow.state.highlights.length })}
                      </div>
                    </div>
                    <button
                      className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600"
                      onClick={() => jumpToSessionArea('quotes')}
                      type="button"
                      {...testAttr('reading-room-capture-jump')}
                    >
                      {t('Save')}
                    </button>
                  </div>
                  <div className="mt-3 rounded border border-stone-200 bg-stone-50 px-3 py-2 text-xs leading-5 text-stone-700" {...testAttr('reading-room-latest-capture')}>
                    {flow.state.highlights[0]
                      ? flow.state.highlights[0].quoteText
                      : t('Page, location, quote, note')}
                  </div>
                  {flow.state.currentPage === undefined && (
                    <button
                      className="mt-2 w-full rounded border border-amber-300 bg-amber-50 px-3 py-2 text-left text-xs leading-5 text-amber-900 hover:border-amber-500"
                      onClick={() => jumpToSessionArea('progress')}
                      type="button"
                      {...testAttr('reading-boundary-warning')}
                    >
                      {t('Set current page to keep AI from reading ahead.')}
                    </button>
                  )}
                </section>

                <section className="min-w-0 rounded border border-stone-200 bg-white p-3" {...testAttr('reading-room-discussion')}>
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-xs font-semibold uppercase text-stone-500">{t('Discussion')}</div>
                      <div className="mt-1 text-sm font-medium text-stone-900">
                        {t('{{count}} replies', { count: personaResponses.length })}
                      </div>
                    </div>
                    <button
                      className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600"
                      onClick={() => jumpToSessionArea('messages')}
                      type="button"
                      {...testAttr('reading-room-discussion-jump')}
                    >
                      {t('Open')}
                    </button>
                  </div>
                  <div className="mt-3 rounded border border-stone-200 bg-stone-50 px-3 py-2 text-xs leading-5 text-stone-700" {...testAttr('reading-room-latest-discussion')}>
                    {personaResponses[0]
                      ? `${personaResponses[0].personaDisplayName || t('Persona response')}: ${personaResponses[0].content}`
                      : t('No persona responses yet.')}
                  </div>
                </section>
              </div>
            </section>
          )}

          {flow.state.windows.length > 0 && (
            <section className="grid gap-3 border-b border-stone-200 px-4 py-3">
              <div className="flex flex-wrap gap-2" {...testAttr('window-tabs')}>
                {flow.state.windows.map((window) => {
                  const active = flow.state.window?.windowId === window.windowId;

                  return (
                    <button
                      className={`rounded border px-3 py-2 text-sm font-medium ${
                        active
                          ? 'border-stone-900 bg-stone-900 text-white'
                          : 'border-stone-300 bg-white text-stone-700 hover:border-stone-700'
                      }`}
                      key={window.windowId}
                      onClick={() => flow.selectWindow(window.windowId)}
                      type="button"
                      {...testAttr(`window-tab-${window.windowType}`)}
                    >
                      {window.title}
                    </button>
                  );
                })}
              </div>
              {flow.state.session && (
                <div className="grid gap-2">
                  {flow.state.window && (
                    <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2" {...testAttr('window-title-panel')}>
                      {editingWindowTitle ? (
                        <form className="flex flex-wrap gap-2" onSubmit={submitWindowTitle} {...testAttr('window-title-form')}>
                          <input
                            className="min-w-0 flex-1 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                            maxLength={inputLimits.sessionWindowTitleEdit}
                            onChange={(event) => setWindowTitleDraft(event.target.value)}
                            value={windowTitleDraft}
                            {...testAttr('window-title-edit-input')}
                          />
                          <button
                            className="rounded border border-stone-900 bg-white px-3 py-2 text-xs font-medium disabled:opacity-50"
                            disabled={flow.state.loading || !windowTitleDraftValid}
                            type="submit"
                            {...testAttr('window-title-save')}
                          >
                            {t('Save')}
                          </button>
                          <button
                            className="rounded border border-stone-300 bg-white px-3 py-2 text-xs font-medium text-stone-600"
                            onClick={() => {
                              setWindowTitleDraft(flow.state.window?.title || '');
                              setEditingWindowTitle(false);
                            }}
                            type="button"
                            {...testAttr('window-title-cancel')}
                          >
                            {t('Cancel')}
                          </button>
                        </form>
                      ) : (
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <div>
                            <div className="text-xs font-medium uppercase text-stone-500">{t('Active window')}</div>
                            <div className="text-sm font-medium" {...testAttr('window-title-current')}>{flow.state.window.title}</div>
                          </div>
                          <div className="flex flex-wrap gap-2">
                            <button
                              className="rounded border border-stone-300 bg-white px-3 py-2 text-xs font-medium text-stone-600 disabled:opacity-50"
                              disabled={flow.state.loading}
                              onClick={() => setEditingWindowTitle(true)}
                              type="button"
                              {...testAttr('window-title-edit')}
                            >
                              {t('Edit window')}
                            </button>
                            <button
                              className="rounded border border-stone-300 bg-white px-3 py-2 text-xs font-medium text-stone-600 disabled:opacity-50"
                              disabled={flow.state.loading || flow.state.windows.length <= 1}
                              onClick={() => void flow.archiveWindow()}
                              type="button"
                              {...testAttr('window-archive-submit')}
                            >
                              {t('Archive window')}
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                  <form className="grid gap-2 sm:grid-cols-[1fr_auto]" onSubmit={submitWindow} {...testAttr('window-create-form')}>
                    <input
                      className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                      maxLength={inputLimits.sessionWindowTitle}
                      onChange={(event) => setNewWindowTitle(event.target.value)}
                      placeholder={t('New reflection window')}
                      value={newWindowTitle}
                      {...testAttr('window-title-input')}
                    />
                    <button
                      className="rounded border border-stone-900 px-3 py-2 text-sm font-medium disabled:opacity-50"
                      disabled={flow.state.loading || !newWindowTitleValid}
                      type="submit"
                      {...testAttr('window-create-submit')}
                    >
                      Add window
                    </button>
                  </form>
                </div>
              )}
            </section>
          )}

          {flow.state.session && (
            <nav className="border-b border-stone-200 bg-white px-4 py-3" aria-label={t('Session areas')} {...testAttr('session-jump-nav')}>
              <div className="grid grid-cols-3 gap-1 sm:grid-cols-5">
                {[
                  { id: 'questions', label: t('Questions') },
                  { id: 'progress', label: t('Progress') },
                  { id: 'quotes', label: t('Quotes') },
                  { id: 'messages', label: t('Messages') },
                  { id: 'review', label: t('Review') },
                ].map((area) => (
                  <button
                    className="min-h-9 rounded border border-stone-300 bg-white px-2 py-2 text-xs font-medium text-stone-700 hover:border-stone-700 focus:border-stone-900 focus:outline-none"
                    key={area.id}
                    onClick={() => jumpToSessionArea(area.id)}
                    type="button"
                    {...testAttr('session-jump-item')}
                  >
                    {area.label}
                  </button>
                ))}
              </div>
            </nav>
          )}

          {flow.state.stats && (
            <div className="grid gap-2 border-b border-stone-200 px-4 py-3 sm:grid-cols-4" {...testAttr('session-stats')}>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Questions')}</div>
                <div className="text-sm font-semibold">
                  {t('{{answered}}/{{total}} answered', { answered: flow.state.stats.answeredQuestionCount, total: flow.state.stats.questionCount })}
                </div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Messages')}</div>
                <div className="text-sm font-semibold">{flow.state.stats.messageCount}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Debate')}</div>
                <div className="text-sm font-semibold">{flow.state.stats.personaResponseCount} {t('Persona responses')}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2">
                <div className="text-xs text-stone-500">{t('Windows')}</div>
                <div className="text-sm font-semibold">{flow.state.stats.windowCount}</div>
              </div>
              <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2 sm:col-span-4" {...testAttr('session-progress-percent')}>
                <div className="flex items-center justify-between text-xs text-stone-500">
                  <span>{t('Progress')}</span>
                  <span>{flow.state.progressPercent !== undefined ? `${flow.state.progressPercent}%` : t('Not set')}</span>
                </div>
                <div className="mt-2 h-2 overflow-hidden rounded bg-stone-200">
                  <div
                    className="h-full rounded bg-stone-900"
                    style={{ width: `${flow.state.progressPercent || 0}%` }}
                  />
                </div>
              </div>
            </div>
          )}

          {flow.state.nextActions.length > 0 && (
            <section className="border-b border-stone-200 px-4 py-3" {...testAttr('next-actions')}>
              <div className="mb-2 text-xs font-semibold uppercase text-stone-500">{t('Next actions')}</div>
              <div className="grid gap-2 md:grid-cols-2">
                {flow.state.nextActions.map((action) => (
                  <button
                    className="rounded border border-stone-200 bg-white px-3 py-2 text-left hover:border-stone-400 focus:border-stone-900 focus:outline-none"
                    key={action.actionId}
                    aria-label={t(action.label)}
                    onClick={() => followNextAction(action.actionId, action.targetWindowId, action.targetQuestionId)}
                    type="button"
                    data-action-id={action.actionId}
                    {...testAttr('next-action-item')}
                  >
                    <div className="text-sm font-semibold text-stone-900">{t(action.label)}</div>
                    <div className="mt-1 text-xs leading-5 text-stone-600" aria-hidden="true">{t(action.detail)}</div>
                  </button>
                ))}
              </div>
            </section>
          )}

          {flow.state.session && (
            <section
              className="border-b border-stone-200 px-4 py-3 focus:outline-none focus:ring-2 focus:ring-stone-400"
              ref={reviewReadinessRef}
              tabIndex={-1}
              {...testAttr('review-readiness')}
            >
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <div className="text-xs font-semibold uppercase text-stone-500">{t('Review readiness')}</div>
                  <div className="mt-1 text-sm font-semibold text-stone-900" {...testAttr('review-readiness-score')}>
                    {t('{{ready}}/{{total}} ready', { ready: sessionReadiness.completedCount, total: sessionReadiness.totalCount })}
                  </div>
                </div>
                <div className="min-w-32 flex-1 sm:max-w-64">
                  <div className="h-2 overflow-hidden rounded bg-stone-200" aria-hidden="true">
                    <div
                      className="h-full rounded bg-stone-900"
                      style={{ width: `${sessionReadiness.percent}%` }}
                    />
                  </div>
                </div>
              </div>
              <div className="grid gap-2 sm:grid-cols-3 xl:grid-cols-6">
                {sessionReadiness.items.map((item) => (
                  <div
                    className={`rounded border px-3 py-2 ${item.complete ? 'border-stone-900 bg-white' : 'border-stone-200 bg-stone-50'}`}
                    key={item.id}
                    {...testAttr('review-readiness-item')}
                  >
                    <div className="text-xs font-medium text-stone-500">{item.label}</div>
                    <div className="mt-1 text-sm font-semibold text-stone-900">{item.value}</div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {flow.state.session && (
            <section className="border-b border-stone-200 bg-white px-4 py-3" {...testAttr('session-brief')}>
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <div className="text-xs font-semibold uppercase text-stone-500">{t('Session brief')}</div>
                  <div className="mt-1 text-sm font-semibold text-stone-900" {...testAttr('session-brief-headline')}>
                    {sessionBrief.headline}
                  </div>
                </div>
                {flow.state.session.status === 'completed' && (
                  <div className="rounded border border-stone-300 px-2 py-1 text-xs font-medium text-stone-700">
                    {t('Review locked')}
                  </div>
                )}
              </div>
              <div className="grid gap-2 md:grid-cols-2">
                {sessionBrief.items.map((item) => (
                  <div
                    className="min-w-0 rounded border border-stone-200 bg-stone-50 px-3 py-2"
                    key={item.id}
                    {...testAttr('session-brief-item')}
                  >
                    <div className="text-xs font-medium text-stone-500">{item.label}</div>
                    <div className="mt-1 break-words text-sm font-semibold leading-5 text-stone-900" title={item.value}>
                      {item.value}
                    </div>
                    {item.detail && (
                      <div className="mt-1 break-words text-xs leading-5 text-stone-600" title={item.detail}>
                        {item.detail}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {flow.state.session && (
            <section className="grid gap-2 border-b border-stone-200 px-4 py-3" {...testAttr('session-search-panel')}>
              <div className="grid gap-2 md:grid-cols-[1fr_auto]">
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  onChange={(event) => setSessionSearchQuery(event.target.value)}
                  placeholder={t('Search messages and quotes')}
                  value={sessionSearchQuery}
                  {...testAttr('session-search-input')}
                />
                {sessionSearchQuery.trim() && (
                  <button
                    className="rounded border border-stone-300 bg-white px-3 py-2 text-xs font-medium text-stone-600"
                    onClick={() => setSessionSearchQuery('')}
                    type="button"
                    {...testAttr('session-search-clear')}
                  >
                    {t('Clear')}
                  </button>
                )}
              </div>
              <div className="text-xs text-stone-500" {...testAttr('session-search-count')}>
                {normalizedSessionSearch
                  ? t('{{visibleMessages}}/{{totalMessages}} messages - {{visibleQuotes}}/{{totalQuotes}} quotes', {
                    visibleMessages: filteredActiveMessages.length,
                    totalMessages: activeMessages.length,
                    visibleQuotes: filteredHighlights.length,
                    totalQuotes: flow.state.highlights.length,
                  })
                  : t('{{messages}} messages - {{quotes}} quotes', { messages: activeMessages.length, quotes: flow.state.highlights.length })}
              </div>
            </section>
          )}

          {flow.state.session && (
            <form className="grid gap-3 border-b border-stone-200 px-4 py-3" onSubmit={submitProgress} {...testAttr('session-progress-form')}>
              <div className="grid gap-3 lg:grid-cols-[1fr_360px]">
                <div className="grid gap-2">
                  <input
                    className="rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                    maxLength={inputLimits.readingGoal}
                    onChange={(event) => setReadingGoal(event.target.value)}
                    placeholder={t('Reading goal for this session')}
                    ref={readingGoalInputRef}
                    value={readingGoal}
                    {...testAttr('reading-goal-input')}
                  />
                  <textarea
                    className="min-h-16 resize-y rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                    maxLength={inputLimits.progressNote}
                    onChange={(event) => setProgressNote(event.target.value)}
                    placeholder={t('Progress note')}
                    value={progressNote}
                    {...testAttr('progress-note-input')}
                  />
                </div>
                <div className="grid gap-2 sm:grid-cols-[1fr_1fr_1fr_auto] lg:grid-cols-[1fr_1fr_1fr]">
                  <input
                    className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                    min="0"
                    onChange={(event) => setStartPage(event.target.value)}
                    placeholder={t('StartPage')}
                    step="1"
                    type="number"
                    value={startPage}
                    {...testAttr('start-page-input')}
                  />
                  <input
                    className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                    min="0"
                    onChange={(event) => setCurrentPage(event.target.value)}
                    placeholder={t('Current')}
                    step="1"
                    type="number"
                    value={currentPage}
                    {...testAttr('current-page-input')}
                  />
                  <input
                    className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                    min="0"
                    onChange={(event) => setTargetPage(event.target.value)}
                    placeholder={t('Target')}
                    step="1"
                    type="number"
                    value={targetPage}
                    {...testAttr('target-page-input')}
                  />
                  <button
                    className="rounded border border-stone-900 px-3 py-2 text-sm font-medium disabled:opacity-50 sm:col-span-1 lg:col-span-3"
                    disabled={flow.state.loading || !progressPageDraftsValid}
                    type="submit"
                    {...testAttr('progress-save-submit')}
                  >
                    {t('Save progress')}
                  </button>
                </div>
              </div>
            </form>
          )}

          {flow.state.session && (
            <section className="grid gap-3 border-b border-stone-200 px-4 py-3" {...testAttr('highlight-panel')}>
              <form className="grid gap-2 lg:grid-cols-[90px_140px_1fr_auto]" onSubmit={submitHighlight} {...testAttr('highlight-form')}>
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  min="0"
                  onChange={(event) => setHighlightPage(event.target.value)}
                  placeholder={t('Page')}
                  step="1"
                  type="number"
                  value={highlightPage}
                  {...testAttr('highlight-page-input')}
                />
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  maxLength={inputLimits.highlightLocation}
                  onChange={(event) => setHighlightLocation(event.target.value)}
                  placeholder={t('Location')}
                  value={highlightLocation}
                  {...testAttr('highlight-location-input')}
                />
                  <input
                    className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                    maxLength={inputLimits.highlightQuote}
                    onChange={(event) => setHighlightQuote(event.target.value)}
                    placeholder={t('Quote or passage')}
                    ref={highlightQuoteInputRef}
                    value={highlightQuote}
                    {...testAttr('highlight-quote-input')}
                  />
                <button
                  className="rounded border border-stone-900 px-3 py-2 text-sm font-medium disabled:opacity-50"
                  disabled={flow.state.loading || !highlightPageDraftValid || !highlightDraftValid}
                  type="submit"
                  {...testAttr('highlight-save-submit')}
                >
                  Save quote
                </button>
                <textarea
                  className="min-h-14 resize-y rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700 lg:col-span-4"
                  maxLength={inputLimits.highlightNote}
                  onChange={(event) => setHighlightNote(event.target.value)}
                  placeholder={t('Why this passage matters')}
                  value={highlightNote}
                  {...testAttr('highlight-note-input')}
                />
              </form>

              {flow.state.highlights.length > 0 && (
                <div className="grid gap-2" {...testAttr('highlight-list')}>
                  {filteredHighlights.map((highlight) => (
                    <article className="rounded border border-stone-200 bg-stone-50 px-3 py-2" key={highlight.highlightId} {...testAttr('highlight-item')}>
                      {editingHighlightId === highlight.highlightId ? (
                        <form className="grid gap-2" onSubmit={submitEditHighlight} {...testAttr('highlight-edit-form')}>
                          <div className="grid gap-2 lg:grid-cols-[90px_140px_1fr]">
                            <input
                              className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                              min="0"
                              onChange={(event) => setEditHighlightPage(event.target.value)}
                              placeholder={t('Page')}
                              step="1"
                              type="number"
                              value={editHighlightPage}
                              {...testAttr('highlight-edit-page-input')}
                            />
                            <input
                              className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                              maxLength={inputLimits.highlightLocation}
                              onChange={(event) => setEditHighlightLocation(event.target.value)}
                              placeholder={t('Location')}
                              value={editHighlightLocation}
                              {...testAttr('highlight-edit-location-input')}
                            />
                            <input
                              className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                              maxLength={inputLimits.highlightQuote}
                              onChange={(event) => setEditHighlightQuote(event.target.value)}
                              placeholder={t('Quote or passage')}
                              value={editHighlightQuote}
                              {...testAttr('highlight-edit-quote-input')}
                            />
                          </div>
                          <textarea
                            className="min-h-14 resize-y rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                            maxLength={inputLimits.highlightNote}
                            onChange={(event) => setEditHighlightNote(event.target.value)}
                            placeholder={t('Why this passage matters')}
                            value={editHighlightNote}
                            {...testAttr('highlight-edit-note-input')}
                          />
                          <div className="flex flex-wrap justify-end gap-2">
                            <button
                              className="rounded border border-stone-300 bg-white px-3 py-2 text-xs font-medium text-stone-600 disabled:opacity-50"
                              disabled={flow.state.loading}
                              onClick={cancelEditHighlight}
                              type="button"
                              {...testAttr('highlight-edit-cancel')}
                            >
                              Cancel
                            </button>
                            <button
                              className="rounded border border-stone-900 bg-white px-3 py-2 text-xs font-medium disabled:opacity-50"
                              disabled={flow.state.loading || !editHighlightPageDraftValid || !editHighlightDraftValid}
                              type="submit"
                              {...testAttr('highlight-edit-submit')}
                            >
                              Save
                            </button>
                          </div>
                        </form>
                      ) : (
                        <>
                          <div className="flex items-start justify-between gap-3">
                            <div className="text-xs font-medium uppercase text-stone-500">
                              {highlight.pageNumber !== undefined ? `p. ${highlight.pageNumber}` : 'passage'}
                              {highlight.locationLabel ? ` - ${highlight.locationLabel}` : ''}
                            </div>
                            <div className="flex flex-wrap gap-2">
                              <button
                                className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600 disabled:opacity-50"
                                disabled={flow.state.loading}
                                onClick={() => startEditHighlight(highlight.highlightId)}
                                type="button"
                                {...testAttr('highlight-edit-open')}
                              >
                                Edit
                              </button>
                              <button
                                className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600 disabled:opacity-50"
                                disabled={flow.state.loading}
                                onClick={() => void flow.deleteHighlight(highlight.highlightId)}
                                type="button"
                                {...testAttr('highlight-delete-submit')}
                              >
                                Delete
                              </button>
                            </div>
                          </div>
                          <div className="mt-1 text-sm leading-6">{highlight.quoteText}</div>
                          {highlight.note && <div className="mt-1 text-xs leading-5 text-stone-600">{highlight.note}</div>}
                        </>
                      )}
                    </article>
                  ))}
                  {filteredHighlights.length === 0 && (
                    <div className="rounded border border-stone-200 bg-stone-50 px-3 py-4 text-sm text-stone-500" {...testAttr('highlight-search-empty')}>
                      No quotes match the current search.
                    </div>
                  )}
                </div>
              )}
            </section>
          )}

          {flow.state.session && (
            <form className="grid gap-3 border-b border-stone-200 px-4 py-3 md:grid-cols-[1fr_auto]" onSubmit={submitComplete} {...testAttr('session-closeout')}>
              <textarea
                className="min-h-20 resize-y rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700 read-only:bg-stone-50 read-only:text-stone-700"
                onChange={(event) => setCloseoutSummary(event.target.value)}
                placeholder={t('Summarize what this reading session resolved')}
                readOnly={flow.state.session.status === 'completed'}
                ref={closeoutSummaryRef}
                value={closeoutSummary}
                {...testAttr('session-summary-input')}
              />
              <div className="flex flex-col justify-between gap-2">
                <div className="text-xs font-medium uppercase text-stone-500">
                  {flow.state.session.status}
                </div>
                <button
                  className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                  disabled={flow.state.loading || flow.state.session.status === 'completed' || !closeoutSummary.trim()}
                  type="submit"
                  {...testAttr('session-complete-submit')}
                >
                  {flow.state.session.status === 'completed' ? t('Completed') : t('Complete')}
                </button>
              </div>
            </form>
          )}

          {flow.state.session?.status === 'completed' && (
            <section className="grid gap-3 border-b border-stone-200 bg-stone-50 px-4 py-4" {...testAttr('session-review')}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="text-sm font-semibold">{t('Session review')}</div>
                  <div className="text-xs text-stone-500">
                    {t('{{answers}} answers - {{quotes}} quotes - {{personaReplies}} persona replies', {
                      answers: flow.state.stats?.answeredQuestionCount || 0,
                      quotes: flow.state.highlights.length,
                      personaReplies: flow.state.stats?.personaResponseCount || 0,
                    })}
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <div className="rounded border border-stone-300 bg-white px-3 py-2 text-xs text-stone-600" {...testAttr('review-progress')}>
                    {flow.state.currentPage !== undefined && flow.state.targetPage !== undefined
                      ? `${t('Page {{current}}/{{target}}', { current: flow.state.currentPage, target: flow.state.targetPage })}${flow.state.progressPercent !== undefined ? ` - ${flow.state.progressPercent}%` : ''}`
                      : t('Progress not recorded')}
                  </div>
                  <button
                    className="rounded border border-stone-900 bg-white px-3 py-2 text-xs font-medium disabled:opacity-50"
                    disabled={flow.state.loading}
                    onClick={() => void flow.createMetricSnapshot()}
                    type="button"
                    {...testAttr('metric-snapshot-submit')}
                  >
                    {t('Save metric')}
                  </button>
                  <button
                    className="rounded border border-stone-900 bg-white px-3 py-2 text-xs font-medium disabled:opacity-50"
                    disabled={flow.state.loading}
                    onClick={exportReview}
                    type="button"
                    {...testAttr('review-export-submit')}
                  >
                    {t('Export MD')}
                  </button>
                </div>
              </div>

              <section className="grid gap-2 md:grid-cols-2" {...testAttr('review-overview')}>
                {flow.state.sessionSummary && (
                  <div className="rounded border border-stone-200 bg-white px-3 py-2 md:col-span-2" {...testAttr('review-summary')}>
                    <div className="text-xs font-medium uppercase text-stone-500">{t('Closeout')}</div>
                    <div className="mt-1 text-sm leading-6">{flow.state.sessionSummary}</div>
                  </div>
                )}

                {flow.state.readingGoal && (
                  <div className="rounded border border-stone-200 bg-white px-3 py-2" {...testAttr('review-goal')}>
                    <div className="text-xs font-medium uppercase text-stone-500">{t('Reading goal')}</div>
                    <div className="mt-1 text-sm leading-6">{flow.state.readingGoal}</div>
                  </div>
                )}

                {flow.state.tags.length > 0 && (
                  <div className="rounded border border-stone-200 bg-white px-3 py-2" {...testAttr('review-tags')}>
                    <div className="text-xs font-medium uppercase text-stone-500">{t('Tags')}</div>
                    <div className="mt-2 flex flex-wrap gap-1">
                      {flow.state.tags.map((tag) => (
                        <span className="rounded border border-stone-300 bg-stone-50 px-2 py-1 text-xs text-stone-700" key={tag.tagId}>
                          {tag.label}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </section>

              <section className="grid gap-2 rounded border border-stone-200 bg-white p-3" {...testAttr('review-insight-panel')}>
                <div>
                  <div className="text-xs font-semibold uppercase text-stone-500">{t('Review insights')}</div>
                  <div className="mt-1 text-sm font-medium text-stone-900">{t('Save the conclusions worth carrying forward')}</div>
                </div>
                <form className="grid gap-2" onSubmit={submitSessionInsight} {...testAttr('review-insight-form')}>
                  <div className="grid gap-2 sm:grid-cols-[130px_1fr]">
                    <select
                      className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                      onChange={(event) => setInsightType(event.target.value)}
                      value={insightType}
                      {...testAttr('review-insight-type')}
                    >
                      <option value="takeaway">{t('Takeaway')}</option>
                      <option value="theme">{t('Theme')}</option>
                      <option value="question">{t('Question')}</option>
                      <option value="debate">{t('Debate')}</option>
                    </select>
                    <input
                      className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                      maxLength={inputLimits.insightTitle}
                      onChange={(event) => setInsightTitle(event.target.value)}
                      placeholder={t('Insight title')}
                      value={insightTitle}
                      {...testAttr('review-insight-title')}
                    />
                  </div>
                  <textarea
                    className="min-h-20 resize-y rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                    onChange={(event) => setInsightContent(event.target.value)}
                    placeholder={t('What should be remembered from this reading or debate?')}
                    value={insightContent}
                    {...testAttr('review-insight-content')}
                  />
                  <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
                    <input
                      className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                      onChange={(event) => setInsightEvidence(event.target.value)}
                      placeholder={t('Evidence or passage')}
                      value={insightEvidence}
                      {...testAttr('review-insight-evidence')}
                    />
                    <button
                      className="rounded border border-stone-900 px-3 py-2 text-xs font-medium disabled:opacity-50"
                      disabled={flow.state.loading || !insightDraftValid}
                      type="submit"
                      {...testAttr('review-insight-submit')}
                    >
                      {t('Save insight')}
                    </button>
                  </div>
                </form>

                {flow.state.insights.length > 0 && (
                  <div className="grid gap-2 md:grid-cols-2" {...testAttr('review-insights')}>
                    {flow.state.insights.map((insight) => (
                      <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2" key={insight.insightId}>
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <div className="text-xs font-medium uppercase text-stone-500">{insight.insightType}</div>
                            {insight.title && <div className="mt-1 text-sm font-medium leading-6">{insight.title}</div>}
                          </div>
                          <button
                            className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600 disabled:opacity-50"
                            disabled={flow.state.loading}
                            onClick={() => void flow.deleteSessionInsight(insight.insightId)}
                            type="button"
                            {...testAttr('review-insight-delete')}
                          >
                            {t('Delete')}
                          </button>
                        </div>
                        <div className="mt-1 text-sm leading-6">{insight.content}</div>
                        {insight.evidence && <div className="mt-1 text-xs leading-5 text-stone-600">{insight.evidence}</div>}
                      </div>
                    ))}
                  </div>
                )}
              </section>

              {flow.state.lastMetricSnapshot && (
                <div className="rounded border border-stone-200 bg-white px-3 py-2" {...testAttr('metric-snapshot-result')}>
                  <div className="text-xs font-medium uppercase text-stone-500">
                    Metric #{flow.state.lastMetricSnapshot.metricId}
                  </div>
                  <div className="mt-1 text-sm leading-6">
                    {flow.state.lastMetricSnapshot.metricName} saved
                    {flow.state.lastMetricSnapshot.metricValue !== undefined && flow.state.lastMetricSnapshot.metricValue !== null
                      ? ` - ${flow.state.lastMetricSnapshot.metricValue}${flow.state.lastMetricSnapshot.metricUnit ? `%` : ''}`
                      : ''}
                  </div>
                  <div className="mt-1 text-xs leading-5 text-stone-600">
                    {flow.state.lastMetricSnapshot.messageCount} messages - {flow.state.lastMetricSnapshot.answeredQuestionCount}/{flow.state.lastMetricSnapshot.questionCount} answers - {flow.state.lastMetricSnapshot.highlightCount} quotes
                  </div>
                </div>
              )}

              <section className="grid gap-3 lg:grid-cols-3" {...testAttr('review-evidence-grid')}>
                {flow.state.highlights.length > 0 && (
                  <div className="grid content-start gap-2" {...testAttr('review-highlights')}>
                    <div className="text-xs font-semibold uppercase text-stone-500">{t('Saved quotes')}</div>
                    {flow.state.highlights.slice(0, 3).map((highlight) => (
                      <div className="rounded border border-stone-200 bg-white px-3 py-2" key={highlight.highlightId}>
                        <div className="text-xs font-medium uppercase text-stone-500">
                          {highlight.pageNumber !== undefined ? `p. ${highlight.pageNumber}` : 'passage'}
                          {highlight.locationLabel ? ` - ${highlight.locationLabel}` : ''}
                        </div>
                        <div className="mt-1 text-sm leading-6">{highlight.quoteText}</div>
                      </div>
                    ))}
                  </div>
                )}

                {answeredQuestions.length > 0 && (
                  <div className="grid content-start gap-2" {...testAttr('review-answers')}>
                    <div className="text-xs font-semibold uppercase text-stone-500">{t('Answered prompts')}</div>
                    {answeredQuestions.slice(0, 3).map(({ question, answer }) => (
                      <div className="rounded border border-stone-200 bg-white px-3 py-2" key={question.questionId}>
                        <div className="text-xs font-medium uppercase text-stone-500">{t('Answered question')}</div>
                        <div className="mt-1 text-sm font-medium leading-6">{question.questionText}</div>
                        <div className="mt-1 text-sm leading-6 text-stone-700">{answer?.content}</div>
                      </div>
                    ))}
                  </div>
                )}

                {personaResponses.length > 0 && (
                  <div className="grid content-start gap-2" {...testAttr('review-personas')}>
                    <div className="text-xs font-semibold uppercase text-stone-500">{t('Persona responses')}</div>
                    {personaResponses.slice(0, 2).map((response) => (
                      <div className="rounded border border-stone-200 bg-white px-3 py-2" key={response.id}>
                        <div className="text-xs font-medium uppercase text-stone-500">{response.personaDisplayName || t('Persona response')}</div>
                        <div className="mt-1 text-sm leading-6">{response.content}</div>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </section>
          )}

          <div className="flex-1 space-y-3 overflow-auto px-4 py-4" {...testAttr('message-list')}>
            {flow.state.loading && !flow.state.hydrated && (
              <div className="py-16 text-center text-sm text-stone-500">{t('Loading saved reading session...')}</div>
            )}
            {!flow.state.loading && activeMessagesWithStream.length === 0 && (
              <div className="py-16 text-center text-sm text-stone-500">{t('Create a session from a candidate, then write inside the window.')}</div>
            )}
            {!flow.state.loading && activeMessagesWithStream.length > 0 && filteredActiveMessages.length === 0 && (
              <div className="py-16 text-center text-sm text-stone-500" {...testAttr('message-search-empty')}>
                {t('No messages match the current search.')}
              </div>
            )}
            {filteredActiveMessages.map((item) => {
              const evidenceItems = buildAiEvidenceItems(item.contextSnapshot);

              return (
              <div className="rounded border border-stone-200 bg-stone-50 p-3" key={item.id} {...testAttr('message-item')}>
                {editingMessageId === item.persistedMessageId ? (
                  <form className="grid gap-2" onSubmit={submitEditMessage} {...testAttr('message-edit-form')}>
                    <textarea
                      className="min-h-20 resize-y rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                      onChange={(event) => setEditMessageContent(event.target.value)}
                      value={editMessageContent}
                      {...testAttr('message-edit-input')}
                    />
                    <div className="flex flex-wrap justify-end gap-2">
                      <button
                        className="rounded border border-stone-300 bg-white px-3 py-2 text-xs font-medium text-stone-600 disabled:opacity-50"
                        disabled={flow.state.loading}
                        onClick={cancelEditMessage}
                        type="button"
                        {...testAttr('message-edit-cancel')}
                      >
                        {t('Cancel')}
                      </button>
                      <button
                        className="rounded border border-stone-900 bg-white px-3 py-2 text-xs font-medium disabled:opacity-50"
                        disabled={flow.state.loading || !editMessageContent.trim()}
                        type="submit"
                        {...testAttr('message-edit-submit')}
                      >
                        {t('Save')}
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <div className="flex items-start justify-between gap-3">
                      <div className="text-xs font-medium uppercase tracking-wide text-stone-500">
                        {messageDisplayLabel(item)}
                        {item.persistedMessageId ? ` - #${item.persistedMessageId}` : ''}
                        {!item.persistedMessageId && item.role === 'assistant' ? ' - streaming' : ''}
                      </div>
                      {item.role === 'user' && item.persistedMessageId && (
                        <div className="flex flex-wrap gap-2">
                          <button
                            className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600 disabled:opacity-50"
                            disabled={flow.state.loading}
                            onClick={() => startEditMessage(item.persistedMessageId as number, item.content)}
                            type="button"
                            {...testAttr('message-edit-open')}
                          >
                            {t('Edit')}
                          </button>
                          <button
                            className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium text-stone-600 disabled:opacity-50"
                            disabled={flow.state.loading}
                            onClick={() => void flow.deleteMessage(item.persistedMessageId as number)}
                            type="button"
                            {...testAttr('message-delete-submit')}
                          >
                            {t('Delete')}
                          </button>
                        </div>
                      )}
                    </div>
                    <div className="mt-2 text-sm leading-6" {...(!item.persistedMessageId && item.role === 'assistant' ? testAttr('streaming-message') : {})}>
                      {item.content || t('Streaming response...')}
                    </div>
                    {evidenceItems.length > 0 && (
                      <div className="mt-3 grid gap-2 border-t border-stone-200 pt-3" {...testAttr('message-evidence')}>
                        <div className="text-xs font-semibold uppercase text-stone-500">{t('Used context')}</div>
                        <div className="flex flex-wrap gap-2">
                          {evidenceItems.map((evidence) => (
                            <span
                              className="max-w-full rounded border border-stone-300 bg-white px-2 py-1 text-xs leading-5 text-stone-700"
                              key={evidence.id}
                              title={evidence.text}
                              {...testAttr('message-evidence-item')}
                            >
                              <span className="font-medium">{evidence.label}</span>
                              <span className="ml-1 text-stone-500">{evidence.text}</span>
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </>
                )}
              </div>
              );
            })}
          </div>

          <div className="grid gap-3 border-t border-stone-200 bg-stone-50 p-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)]" {...testAttr('session-composers')}>
            <div className="grid grid-cols-2 gap-1 rounded border border-stone-300 bg-white p-1 xl:hidden" role="tablist" aria-label={t('Composer mode')} {...testAttr('composer-mode-tabs')}>
              {[
                { id: 'message' as ComposerMode, label: t('Ask book') },
                { id: 'persona' as ComposerMode, label: t('Debate personas') },
              ].map((mode) => (
                <button
                  aria-controls={`${mode.id}-composer-panel`}
                  aria-selected={composerMode === mode.id}
                  className={`min-h-9 rounded px-3 py-2 text-sm font-medium ${
                    composerMode === mode.id
                      ? 'bg-stone-900 text-white'
                      : 'text-stone-700 hover:bg-stone-100'
                  }`}
                  id={`${mode.id}-composer-tab`}
                  key={mode.id}
                  onClick={() => setComposerMode(mode.id)}
                  role="tab"
                  type="button"
                  {...testAttr('composer-mode-tab')}
                >
                  {mode.label}
                </button>
              ))}
            </div>
            <section
              aria-labelledby="message-composer-tab"
              className={`${composerMode === 'message' ? 'grid' : 'hidden'} content-start gap-2 rounded border border-stone-200 bg-white p-3 xl:grid`}
              id="message-composer-panel"
              role="tabpanel"
              {...testAttr('message-composer')}
            >
              <div {...testAttr('ask-book-composer')}>
                <div className="text-xs font-semibold uppercase text-stone-500">{t('Ask book')}</div>
                <div className="mt-1 text-sm font-medium text-stone-900">
                  {selectedQuestion ? t('Answer the selected prompt') : t('Ask about the current reading')}
                </div>
              </div>
              <form className="flex items-start gap-2" onSubmit={submitMessage} {...testAttr('message-form')}>
                <input
                  className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  onChange={(event) => setMessage(event.target.value)}
                  placeholder={selectedQuestion ? t('Answer selected question') : t('Ask book')}
                  ref={messageInputRef}
                  value={message}
                  {...testAttr('message-input')}
                />
                <button
                  className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                  disabled={!flow.state.window || flow.state.loading || !message.trim()}
                  type="submit"
                  {...testAttr('message-submit')}
                >
                  {t('Send')}
                </button>
              </form>
            </section>

            <section
              aria-labelledby="persona-composer-tab"
              className={`${composerMode === 'persona' ? 'grid' : 'hidden'} content-start gap-3 rounded border border-stone-200 bg-white p-3 xl:grid`}
              id="persona-composer-panel"
              role="tabpanel"
              {...testAttr('persona-composer')}
            >
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-xs font-semibold uppercase text-stone-500">{t('Debate personas')}</div>
                  <div className="mt-1 text-sm font-medium text-stone-900">
                    {t('Challenge the current interpretation')}
                  </div>
                </div>
                <button
                  className="rounded border border-stone-300 px-3 py-2 text-xs font-medium disabled:opacity-50"
                  disabled={flow.state.loading || !flow.state.session}
                  onClick={generatePersonaDrafts}
                  type="button"
                  {...testAttr('persona-drafts-generate')}
                >
                  {t('Draft')}
                </button>
              </div>
              <form className="grid gap-2 md:grid-cols-2" onSubmit={submitPersona} {...testAttr('persona-create-form')}>
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  maxLength={inputLimits.personaDisplayName}
                  onChange={(event) => setPersonaName(event.target.value)}
                  placeholder={t('Persona name')}
                  value={personaName}
                  {...testAttr('persona-create-name-input')}
                />
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  maxLength={inputLimits.personaTone}
                  onChange={(event) => setPersonaTone(event.target.value)}
                  placeholder={t('Tone')}
                  value={personaTone}
                  {...testAttr('persona-create-tone-input')}
                />
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700 md:col-span-2"
                  onChange={(event) => setPersonaDescription(event.target.value)}
                  placeholder={t('What this voice watches for')}
                  value={personaDescription}
                  {...testAttr('persona-create-description-input')}
                />
                <textarea
                  className="min-h-16 resize-y rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700 md:col-span-2"
                  onChange={(event) => setPersonaInstructions(event.target.value)}
                  placeholder={t('Instructions for how this persona should respond')}
                  value={personaInstructions}
                  {...testAttr('persona-create-instructions-input')}
                />
                <button
                  className="rounded border border-stone-900 px-3 py-2 text-sm font-medium disabled:opacity-50 md:col-span-2"
                  disabled={flow.state.loading || !personaDraftValid}
                  type="submit"
                  {...testAttr('persona-create-submit')}
                >
                  {t('Add persona')}
                </button>
              </form>

              {personaDrafts.length > 0 && (
                <div className="grid gap-2" {...testAttr('persona-drafts')}>
                  {personaDrafts.map((draft) => (
                    <div className="rounded border border-stone-200 bg-stone-50 px-3 py-2" key={`${draft.displayName}-${draft.systemPrompt}`}>
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="text-sm font-medium text-stone-900">{draft.displayName}</div>
                          <div className="mt-1 flex flex-wrap gap-1 text-xs text-stone-500">
                            <span className="rounded border border-stone-300 bg-white px-1.5 py-0.5" {...testAttr('persona-draft-role')}>
                              {t(personaRoleLabel(draft.roleKey))}
                            </span>
                            {draft.tone && <span className="rounded border border-stone-300 bg-white px-1.5 py-0.5">{draft.tone}</span>}
                          </div>
                        </div>
                        <button
                          className="rounded border border-stone-300 bg-white px-2 py-1 text-xs font-medium disabled:opacity-50"
                          disabled={flow.state.loading}
                          onClick={() => savePersonaDraft(draft)}
                          type="button"
                          {...testAttr('persona-draft-save')}
                        >
                          {t('Save')}
                        </button>
                      </div>
                      {draft.description && <div className="mt-2 text-sm leading-5 text-stone-700">{draft.description}</div>}
                      {draft.reason && <div className="mt-1 text-xs leading-5 text-stone-500">{draft.reason}</div>}
                    </div>
                  ))}
                </div>
              )}

              <form className="grid gap-2 md:grid-cols-[180px_minmax(0,1fr)]" onSubmit={submitDebate} {...testAttr('debate-form')}>
                <select
                  className="min-w-0 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
                  disabled={flow.state.personas.length === 0 || flow.state.loading}
                  onChange={(event) => flow.selectPersona(Number(event.target.value))}
                  value={selectedPersona?.personaId || ''}
                  {...testAttr('persona-select')}
                >
                  {flow.state.personas.map((persona) => (
                    <option key={persona.personaId} value={persona.personaId}>
                      {persona.displayName} - {t(personaRoleLabel(persona.roleKey))}
                    </option>
                  ))}
                </select>
                <input
                  className="min-w-0 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                  onChange={(event) => setDebate(event.target.value)}
                  placeholder={selectedPersona ? t('Debate with {{name}}', { name: selectedPersona.displayName }) : t('Persona debate')}
                  ref={debateInputRef}
                  value={debate}
                  {...testAttr('debate-input')}
                />
                <div className="flex flex-wrap justify-end gap-2 md:col-span-2">
                  <button
                    className="rounded border border-stone-900 px-3 py-2 text-sm font-medium disabled:opacity-50"
                    disabled={!flow.state.window || !selectedPersona || flow.state.loading || !debate.trim()}
                    type="submit"
                    {...testAttr('debate-submit')}
                  >
                    {t('Debate')}
                  </button>
                  <button
                    className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                    disabled={!flow.state.window || flow.state.personas.length === 0 || flow.state.loading || !debate.trim()}
                    onClick={submitDebateAll}
                    type="button"
                    {...testAttr('debate-all-submit')}
                  >
                    {t('Debate all')}
                  </button>
                </div>
              </form>
            </section>
          </div>
        </section>
      </section>
    </main>
  );
}
