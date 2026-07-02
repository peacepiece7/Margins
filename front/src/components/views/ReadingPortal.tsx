import { FormEvent, useEffect, useState } from 'react';
import { LoadingSpinner } from '../atoms/LoadingSpinner';
import { Skeleton } from '../atoms/Skeleton';
import { MarkdownContent } from '../molecules/MarkdownContent';
import { ReflectionMarkdownEditor } from '../molecules/ReflectionMarkdownEditor';
import { useI18n } from '../../i18n';
import { useSpeechRecognition } from '../../hooks/useSpeechRecognition';
import { useSessionFlow } from '../../hooks/useSessionFlow';
import type { BookCandidate, SaveBookResponse } from '../../types/models/book';
import type { Persona } from '../../types/models/persona';
import type { SessionDisplayMessage } from '../../types/view-models/sessionFlow';
import type { MarginsPage } from '../../types/view-models/sessionFlow';
import { confirmDelete } from '../../utils/deleteConfirmation';
import { debateTopicFromWindowTitle, personaIcon } from '../../utils/debateDisplay';
import { markdownToPlainText } from '../../utils/markdown';
import { selectNextDebatePersona } from '../../utils/personaSelection';
import { testAttr } from '../../utils/testAttrs';

function messageName(message: SessionDisplayMessage, userLabel: string) {
  if (message.role === 'user') {
    return userLabel;
  }
  return message.personaDisplayName || 'AI';
}

function BookCandidateSkeleton() {
  return (
    <article className="grid gap-3 rounded border border-stone-300 bg-white p-4" {...testAttr('book-candidate-skeleton')}>
      <div className="grid gap-2">
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="mt-1 h-3 w-2/3" />
        <Skeleton className="mt-2 h-4 w-full" />
        <Skeleton className="h-4 w-4/5" />
      </div>
      <Skeleton className="h-10 w-full" />
    </article>
  );
}

function QuestionRowSkeleton() {
  return (
    <div className="grid gap-2 rounded border border-stone-200 bg-white p-3 md:grid-cols-[minmax(0,1fr)_auto] md:items-center" {...testAttr('book-question-skeleton')}>
      <div className="grid gap-2">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
      </div>
      <Skeleton className="h-9 w-20" />
    </div>
  );
}

function DebateReplySkeleton() {
  return (
    <article className="flex justify-start gap-3" {...testAttr('debate-reply-skeleton')}>
      <Skeleton className="h-10 w-10 shrink-0 rounded-full bg-stone-300" />
      <div className="grid max-w-[78%] items-start gap-1">
        <Skeleton className="h-3 w-20 bg-stone-300" />
        <div className="grid gap-2 rounded-2xl rounded-bl-sm bg-white px-4 py-3 shadow-sm">
          <Skeleton className="h-4 w-56 max-w-full" />
          <Skeleton className="h-4 w-40 max-w-full" />
        </div>
      </div>
    </article>
  );
}

interface SpeechDraftControlProps {
  disabled?: boolean;
  label: string;
  onChange: (value: string) => void;
  value: string;
}

function SpeechDraftControl({ disabled = false, label, onChange, value }: SpeechDraftControlProps) {
  const { locale, t } = useI18n();
  const speech = useSpeechRecognition({
    language: locale === 'ko' ? 'ko-KR' : 'en-US',
    messages: {
      permissionDenied: t('speechPermissionDenied'),
      retry: t('speechRetry'),
      startFailed: t('speechStartFailed'),
      unsupported: t('speechUnsupported'),
    },
    onTranscript: onChange,
    value,
  });
  const blocked = disabled || !speech.supported;

  return (
    <div className="grid gap-1">
      <button
        aria-label={speech.listening ? t('speechListeningStop') : t('speechStart')}
        aria-pressed={speech.listening}
        className={`inline-flex min-h-9 items-center justify-center gap-2 rounded border px-3 py-2 text-sm font-medium ${
          speech.listening
            ? 'border-red-700 bg-red-50 text-red-800'
            : 'border-stone-300 bg-white text-stone-700 hover:border-stone-950'
        } disabled:cursor-not-allowed disabled:opacity-50`}
        disabled={blocked}
        onClick={speech.toggle}
        type="button"
        {...testAttr(`${label}-speech-toggle`)}
      >
        <span aria-hidden="true" className={`h-2 w-2 rounded-full ${speech.listening ? 'bg-red-600' : 'bg-stone-400'}`} />
        {speech.listening ? t('speechStop') : t('speechInput')}
      </button>
      {!speech.supported && (
        <div className="text-xs text-stone-500" {...testAttr(`${label}-speech-unsupported`)}>
          {t('speechUnsupported')}
        </div>
      )}
      {speech.error && (
        <div className="text-xs text-red-700" role="status" {...testAttr(`${label}-speech-error`)}>
          {speech.error}
        </div>
      )}
    </div>
  );
}

export function ReadingPortal() {
  const { t } = useI18n();
  const flow = useSessionFlow();
  const [page, setPage] = useState<MarginsPage>('book-search');
  const [selectedBookId, setSelectedBookId] = useState<number | undefined>();
  const [manualTitle, setManualTitle] = useState('');
  const [manualAuthor, setManualAuthor] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [editAuthor, setEditAuthor] = useState('');
  const [reflectionContent, setReflectionContent] = useState('');
  const [reflectionEvidence, setReflectionEvidence] = useState('');
  const [answerDraft, setAnswerDraft] = useState('');
  const [debateTopic, setDebateTopic] = useState('');
  const [debateDraft, setDebateDraft] = useState('');
  const [selectedDebatePersonaIds, setSelectedDebatePersonaIds] = useState<number[]>([]);
  const [pendingQuestionGeneration, setPendingQuestionGeneration] = useState(false);
  const [questionGenerationPending, setQuestionGenerationPending] = useState(false);
  const [bookSearchPending, setBookSearchPending] = useState(false);

  const selectedBook = flow.state.savedBooks.find((book) => book.bookId === selectedBookId)
    || flow.state.selectedBook;
  const debateWindows = flow.state.windows.filter((window) => window.windowType === 'debate');
  const fallbackDebateWindow = debateWindows[debateWindows.length - 1];
  const activeSessionMatchesSelectedBook = Boolean(
    selectedBook && flow.state.session && flow.state.session.bookId === selectedBook.bookId,
  );
  const currentSession = activeSessionMatchesSelectedBook ? flow.state.session : undefined;
  const currentStats = activeSessionMatchesSelectedBook ? flow.state.stats : undefined;
  const currentQuestions = activeSessionMatchesSelectedBook ? flow.state.questions : [];
  const questionWindow = activeSessionMatchesSelectedBook
    ? flow.state.windows.find((window) => window.windowType === 'question') || flow.state.windows[0]
    : undefined;
  const selectedDebatePersonas = flow.state.personas.filter((persona) => selectedDebatePersonaIds.includes(persona.personaId));
  const activeMessages = flow.state.window
    ? flow.state.messages.filter((message) => message.windowId === flow.state.window?.windowId)
    : flow.state.messages;
  const answeredQuestionIds = new Set(
    flow.state.messages
      .filter((message) => message.role === 'user' && message.questionId !== undefined)
      .map((message) => message.questionId as number),
  );
  const selectedQuestion = currentQuestions.find((question) => question.questionId === flow.state.selectedQuestionId);
  const selectedQuestionMessages = selectedQuestion
    ? flow.state.messages.filter((message) => (
      message.windowId === selectedQuestion.windowId
      && message.questionId === selectedQuestion.questionId
    ))
    : [];
  const showQuestionSkeleton = pendingQuestionGeneration || questionGenerationPending;
  const showDebateReplySkeleton = flow.state.loading && page === 'debate' && flow.state.window?.windowType === 'debate';
  const reflectionDraftText = markdownToPlainText(reflectionContent);
  const pageLabels: Record<MarginsPage, string> = {
    'book-search': t('pageBookSearch'),
    'book-list': t('pageBookList'),
    'book-detail': t('pageBookDetail'),
    review: t('pageReview'),
    debate: t('pageDebate'),
  };

  useEffect(() => {
    if (!flow.state.hydrated && !flow.state.loading) {
      void flow.loadLatest();
    }
  }, [flow.state.hydrated, flow.state.loading]);

  useEffect(() => {
    if (flow.state.selectedBook) {
      setSelectedBookId(flow.state.selectedBook.bookId);
      setEditTitle(flow.state.selectedBook.title);
      setEditAuthor(flow.state.selectedBook.author || '');
    }
  }, [flow.state.selectedBook?.bookId, flow.state.selectedBook?.title, flow.state.selectedBook?.author]);

  useEffect(() => {
    if (page === 'debate' && fallbackDebateWindow && flow.state.window?.windowType !== 'debate') {
      flow.selectWindow(fallbackDebateWindow.windowId);
    }
  }, [page, fallbackDebateWindow?.windowId, flow.state.window?.windowType]);

  useEffect(() => {
    if (!flow.state.personas.length) {
      return;
    }

    setSelectedDebatePersonaIds((current) => {
      const activeIds = new Set(flow.state.personas.map((persona) => persona.personaId));
      const preserved = current.filter((personaId) => activeIds.has(personaId));
      return preserved.length ? preserved : flow.state.personas.slice(0, 3).map((persona) => persona.personaId);
    });
  }, [flow.state.personas]);

  useEffect(() => {
    if (!pendingQuestionGeneration || !selectedBook || flow.state.session?.bookId !== selectedBook.bookId || !questionWindow) {
      return;
    }

    setPendingQuestionGeneration(false);
    if (flow.state.window?.windowId !== questionWindow.windowId) {
      flow.selectWindow(questionWindow.windowId);
    }
    setQuestionGenerationPending(true);
    void flow.generateQuestions().finally(() => setQuestionGenerationPending(false));
  }, [
    pendingQuestionGeneration,
    selectedBook?.bookId,
    flow.state.session?.bookId,
    flow.state.window?.windowId,
    questionWindow?.windowId,
  ]);

  function goToBook(book: SaveBookResponse, nextPage: MarginsPage = 'book-detail') {
    setSelectedBookId(book.bookId);
    setEditTitle(book.title);
    setEditAuthor(book.author || '');
    setPage(nextPage);
  }

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    if (!flow.state.query.trim()) {
      return;
    }

    setBookSearchPending(true);
    void flow.search().finally(() => setBookSearchPending(false));
  }

  function saveCandidate(candidate: BookCandidate) {
    void flow.saveCandidateBook(candidate).then((saved) => {
      if (saved) {
        setPage('book-list');
      }
    });
  }

  function submitManualBook(event: FormEvent) {
    event.preventDefault();
    if (!manualTitle.trim() || !manualAuthor.trim()) {
      return;
    }

    void flow.saveManualBook(manualTitle.trim(), manualAuthor.trim()).then((saved) => {
      if (saved) {
        setManualTitle('');
        setManualAuthor('');
        setPage('book-list');
      }
    });
  }

  function submitBookEdit(event: FormEvent) {
    event.preventDefault();
    if (!selectedBook || !editTitle.trim() || !editAuthor.trim()) {
      return;
    }

    void flow.updateBook(selectedBook.bookId, editTitle.trim(), editAuthor.trim());
  }

  function generateQuestionsForSelectedBook() {
    if (!selectedBook) {
      return;
    }

    if (flow.state.session?.bookId === selectedBook.bookId && questionWindow) {
      if (flow.state.window?.windowId !== questionWindow.windowId) {
        flow.selectWindow(questionWindow.windowId);
      }
      setQuestionGenerationPending(true);
      void flow.generateQuestions().finally(() => setQuestionGenerationPending(false));
      return;
    }

    setPendingQuestionGeneration(true);
    void flow.startSessionFromBook(selectedBook);
  }

  function deleteSelectedBook() {
    if (!selectedBook || !confirmDelete(t('deleteConfirm'))) {
      return;
    }

    void flow.deleteBook(selectedBook.bookId).then((deleted) => {
      if (deleted) {
        setSelectedBookId(undefined);
        setPage('book-list');
      }
    });
  }

  function startReflection() {
    if (!selectedBook) {
      return;
    }

    const start = flow.state.session?.bookId === selectedBook.bookId
      ? Promise.resolve(true)
      : flow.startSessionFromBook(selectedBook);
    void start.then((started) => {
      if (started) {
        setPage('review');
      }
    });
  }

  function submitReflection(event: FormEvent) {
    event.preventDefault();
    if (!reflectionDraftText) {
      return;
    }

    void flow.createSessionInsight({
      insightType: 'reflection',
      title: selectedBook ? `${selectedBook.title} ${t('review')}` : t('review'),
      content: reflectionContent.trim(),
      evidence: reflectionEvidence.trim() || undefined,
    }).then((saved) => {
      if (saved) {
        setReflectionContent('');
        setReflectionEvidence('');
      }
    });
  }

  function submitAnswer(event: FormEvent) {
    event.preventDefault();
    if (!answerDraft.trim()) {
      return;
    }

    void flow.send(answerDraft.trim()).then((saved) => {
      if (saved) {
        setAnswerDraft('');
      }
    });
  }

  function deleteQuestion(questionId: number) {
    if (!confirmDelete(t('deleteConfirm'))) {
      return;
    }

    void flow.deleteQuestion(questionId);
  }

  function enterDebate() {
    if (!selectedBook || !debateTopic.trim()) {
      return;
    }

    const topic = debateTopic.trim();
    void flow.startDebateSession(selectedBook, topic).then((started) => {
      if (started) {
        setDebateDraft(topic);
        setPage('debate');
      }
    });
  }

  function submitDebate(event: FormEvent) {
    event.preventDefault();
    requestNextPersonaReply();
  }

  function requestAllPersonaReplies() {
    const prompt = debateDraft.trim() || debateTopic.trim();
    if (!prompt || !selectedDebatePersonas.length) {
      return;
    }

    const personaIds = selectedDebatePersonas.map((persona) => persona.personaId);
    void flow.debateWithPersonas(personaIds, prompt).then((saved) => {
      if (saved && debateDraft.trim()) {
        setDebateDraft('');
      }
    });
  }

  function toggleDebatePersona(personaId: number) {
    setSelectedDebatePersonaIds((current) => (
      current.includes(personaId)
        ? current.filter((selectedId) => selectedId !== personaId)
        : [...current, personaId]
    ));
  }

  function requestPersonaReply(personaId: number) {
    const prompt = debateDraft.trim() || debateTopic.trim();
    if (!prompt) {
      return;
    }

    void flow.debateWithPersona(personaId, prompt).then((saved) => {
      if (saved && debateDraft.trim()) {
        setDebateDraft('');
      }
    });
  }

  function requestNextPersonaReply() {
    const prompt = debateDraft.trim() || debateTopic.trim();
    if (!prompt || !selectedDebatePersonas.length) {
      return;
    }

    const nextPersona = selectNextDebatePersona(selectedDebatePersonas, activeMessages);
    if (nextPersona) {
      requestPersonaReply(nextPersona.personaId);
    }
  }

  return (
    <main className="min-h-screen text-stone-950" {...testAttr('reading-portal')}>
      <header className="border-b border-stone-300/80 bg-stone-50/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-col gap-5 px-5 py-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="font-display text-5xl font-semibold tracking-normal">Margins</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-stone-600">{t('appTagline')}</p>
          </div>
          <nav className="grid gap-2 sm:grid-cols-5" aria-label={t('pageNavigation')} {...testAttr('portal-page-nav')}>
            {(Object.keys(pageLabels) as MarginsPage[]).map((pageId) => (
              <button
                className={`rounded border px-3 py-2 text-sm font-medium ${
                  page === pageId
                    ? 'border-stone-950 bg-stone-950 text-white'
                    : 'border-stone-300 bg-white/85 text-stone-700 hover:border-stone-950'
                }`}
                key={pageId}
                onClick={() => setPage(pageId)}
                type="button"
                {...testAttr(`portal-nav-${pageId}`)}
              >
                {pageLabels[pageId]}
              </button>
            ))}
          </nav>
        </div>
      </header>

      <section className="mx-auto grid max-w-7xl gap-5 px-5 py-6 lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="grid content-start gap-3 rounded border border-stone-300 bg-stone-50/95 p-4 shadow-[0_18px_50px_rgba(23,23,23,0.06)]" {...testAttr('portal-sidebar')}>
          <div>
            <div className="text-xs font-semibold uppercase text-stone-500">{t('currentBook')}</div>
            <div className="mt-1 text-lg font-semibold">{selectedBook?.title || t('noBookSelected')}</div>
            {selectedBook?.author && <div className="text-sm text-stone-600">{selectedBook.author}</div>}
          </div>
          {currentSession && (
            <div className="rounded bg-stone-100 p-3 text-sm leading-6">
              <div className="font-medium">{currentSession.title}</div>
              <div className="text-stone-600">
                {t('questionCount')} {currentStats?.answeredQuestionCount || 0}/{currentStats?.questionCount || 0} · {t('messageCount')} {currentStats?.messageCount || 0}
              </div>
            </div>
          )}
          {flow.state.error && (
            <div className="rounded border border-red-300 bg-red-50 p-3 text-sm text-red-800" {...testAttr('portal-error')}>
              {flow.state.error}
            </div>
          )}
        </aside>

        <section className="min-w-0">
          {page === 'book-search' && (
            <section className="grid gap-5" {...testAttr('book-search-page')}>
              <div className="rounded border border-stone-300 bg-stone-50/95 p-5 shadow-[0_18px_50px_rgba(23,23,23,0.06)]">
                <h2 className="font-display text-3xl font-semibold tracking-normal">{t('searchHeading')}</h2>
                <form className="mt-4 flex gap-2" onSubmit={submitSearch} {...testAttr('book-search-form')}>
                  <input
                    className="min-w-0 flex-1 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-900"
                    onChange={(event) => flow.setQuery(event.target.value)}
                    placeholder={t('searchPlaceholder')}
                    value={flow.state.query}
                    {...testAttr('book-search-input')}
                  />
                  <button
                    className="inline-flex items-center justify-center gap-2 rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
                    disabled={bookSearchPending || flow.state.loading || !flow.state.query.trim()}
                    type="submit"
                    {...testAttr('book-search-submit')}
                  >
                    {bookSearchPending && <LoadingSpinner />}
                    {bookSearchPending ? t('loadingSearch') : t('search')}
                  </button>
                </form>
              </div>

              <div className="grid gap-3 md:grid-cols-3" {...testAttr('book-candidate-list')}>
                {bookSearchPending && [0, 1, 2].map((item) => <BookCandidateSkeleton key={item} />)}
                {flow.state.candidates.map((candidate) => (
                  <article className="grid gap-3 rounded border border-stone-300 bg-stone-50/95 p-4 shadow-[0_12px_36px_rgba(23,23,23,0.05)]" key={candidate.candidateId} {...testAttr('book-candidate-card')}>
                    <div className="grid gap-3 sm:grid-cols-[72px_minmax(0,1fr)]">
                      {candidate.thumbnail ? (
                        <img
                          alt=""
                          className="h-28 w-[72px] rounded border border-stone-200 bg-white object-cover"
                          src={candidate.thumbnail}
                          {...testAttr('book-candidate-cover')}
                        />
                      ) : (
                        <div className="h-28 w-[72px] rounded border border-stone-200 bg-stone-100" aria-hidden="true" />
                      )}
                      <div>
                        <div className="text-lg font-semibold" {...testAttr('book-candidate-title')}>{candidate.title}</div>
                        {candidate.subtitle && <div className="text-sm text-stone-500">{candidate.subtitle}</div>}
                        <div className="text-sm text-stone-600">{candidate.author}</div>
                        <div className="mt-2 text-xs font-medium text-stone-500" {...testAttr('book-candidate-id')}>
                          {t('bookId')} {candidate.candidateId}
                        </div>
                        <div className="mt-1 flex flex-wrap gap-x-2 gap-y-1 text-xs font-medium text-stone-500">
                          {candidate.publisher && <span>{candidate.publisher}</span>}
                          {candidate.publishedYear && <span>{candidate.publishedYear}</span>}
                          {candidate.language && <span>{candidate.language.toUpperCase()}</span>}
                        </div>
                        {candidate.isbn && <div className="mt-1 text-xs font-medium text-stone-500">ISBN {candidate.isbn}</div>}
                        {candidate.reason && <p className="mt-2 text-sm leading-6 text-stone-600">{candidate.reason}</p>}
                      </div>
                    </div>
                    <button
                      className="rounded border border-stone-950 px-3 py-2 text-sm font-medium"
                      disabled={flow.state.loading}
                      onClick={() => saveCandidate(candidate)}
                      type="button"
                      {...testAttr('book-candidate-save')}
                    >
                      {t('register')}
                    </button>
                  </article>
                ))}
              </div>

              <form className="grid gap-3 rounded border border-stone-300 bg-stone-50/95 p-5 shadow-[0_18px_50px_rgba(23,23,23,0.06)]" onSubmit={submitManualBook} {...testAttr('manual-book-form')}>
                <div>
                  <h3 className="font-semibold">{t('candidateEmptyTitle')}</h3>
                  <p className="text-sm text-stone-600">{t('candidateEmptyDescription')}</p>
                </div>
                <div className="grid gap-2 md:grid-cols-[1fr_1fr_auto]">
                  <input
                    className="rounded border border-stone-300 bg-white px-3 py-2 text-sm"
                    onChange={(event) => setManualTitle(event.target.value)}
                    placeholder={t('searchPlaceholder')}
                    value={manualTitle}
                    {...testAttr('manual-book-title-input')}
                  />
                  <input
                    className="rounded border border-stone-300 bg-white px-3 py-2 text-sm"
                    onChange={(event) => setManualAuthor(event.target.value)}
                    placeholder={t('bookAuthorPlaceholder')}
                    value={manualAuthor}
                    {...testAttr('manual-book-author-input')}
                  />
                  <button
                    className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
                    disabled={flow.state.loading || !manualTitle.trim() || !manualAuthor.trim()}
                    type="submit"
                    {...testAttr('manual-book-submit')}
                  >
                    {t('manualAdd')}
                  </button>
                </div>
              </form>
            </section>
          )}

          {page === 'book-list' && (
            <section className="grid gap-3" {...testAttr('book-list-page')}>
              <h2 className="font-display text-3xl font-semibold tracking-normal">{t('bookList')}</h2>
              {flow.state.savedBooks.map((book) => (
                <article className="flex flex-col gap-3 rounded border border-stone-300 bg-stone-50/95 p-4 shadow-[0_12px_36px_rgba(23,23,23,0.05)] md:flex-row md:items-center md:justify-between" key={book.bookId} {...testAttr('saved-book-row')}>
                  <button className="grid min-w-0 gap-3 text-left sm:grid-cols-[56px_minmax(0,1fr)]" onClick={() => goToBook(book)} type="button" {...testAttr('saved-book-detail-link')}>
                    {book.coverImageUrl ? (
                      <img
                        alt=""
                        className="h-20 w-14 rounded border border-stone-200 bg-white object-cover"
                        src={book.coverImageUrl}
                        {...testAttr('saved-book-cover')}
                      />
                    ) : (
                      <div className="h-20 w-14 rounded border border-stone-200 bg-stone-100" aria-hidden="true" />
                    )}
                    <div className="min-w-0">
                      <div className="text-lg font-semibold">{book.title}</div>
                      {book.subtitle && <div className="text-sm text-stone-500">{book.subtitle}</div>}
                      <div className="text-sm text-stone-600">{book.author}</div>
                      <div className="mt-1 flex flex-wrap gap-x-2 gap-y-1 text-xs text-stone-500">
                        {book.publisher && <span>{book.publisher}</span>}
                        {book.publishedYear && <span>{book.publishedYear}</span>}
                        {book.source && <span>{book.source}</span>}
                        {book.isbn && <span>ISBN {book.isbn}</span>}
                      </div>
                    </div>
                  </button>
                  <div className="flex gap-2">
                    <button className="rounded border border-stone-300 px-3 py-2 text-sm" onClick={() => goToBook(book)} type="button" {...testAttr('saved-book-edit-open')}>
                      {t('edit')}
                    </button>
                    <button
                      className="rounded border border-red-300 px-3 py-2 text-sm text-red-700"
                      disabled={flow.state.loading}
                      onClick={() => {
                        if (confirmDelete(t('deleteConfirm'))) {
                          void flow.deleteBook(book.bookId);
                        }
                      }}
                      type="button"
                      {...testAttr('saved-book-delete')}
                    >
                      {t('delete')}
                    </button>
                  </div>
                </article>
              ))}
              {!flow.state.savedBooks.length && <div className="rounded border border-stone-300 bg-stone-50/95 p-8 text-center text-sm text-stone-500">{t('noSavedBooks')}</div>}
            </section>
          )}

          {page === 'book-detail' && (
            <section className="grid gap-5" {...testAttr('book-detail-page')}>
              <div className="rounded border border-stone-300 bg-stone-50/95 p-5 shadow-[0_18px_50px_rgba(23,23,23,0.06)]">
                <h2 className="font-display text-3xl font-semibold tracking-normal">{t('bookDetail')}</h2>
                {selectedBook ? (
                  <form className="mt-4 grid gap-3" onSubmit={submitBookEdit} {...testAttr('book-edit-form')}>
                    <input className="rounded border border-stone-300 bg-white px-3 py-2 text-sm" onChange={(event) => setEditTitle(event.target.value)} value={editTitle} {...testAttr('book-edit-title-input')} />
                    <input className="rounded border border-stone-300 bg-white px-3 py-2 text-sm" onChange={(event) => setEditAuthor(event.target.value)} value={editAuthor} {...testAttr('book-edit-author-input')} />
                    <div className="flex flex-wrap gap-2">
                      <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={flow.state.loading || !editTitle.trim() || !editAuthor.trim()} type="submit" {...testAttr('book-edit-submit')}>
                        {t('saveEdit')}
                      </button>
                      <button className="rounded border border-red-300 px-4 py-2 text-sm text-red-700" disabled={flow.state.loading} onClick={deleteSelectedBook} type="button" {...testAttr('book-detail-delete')}>
                        {t('delete')}
                      </button>
                      <button className="rounded border border-stone-900 px-4 py-2 text-sm font-medium" disabled={flow.state.loading} onClick={startReflection} type="button" {...testAttr('book-start-review')}>
                        {t('startReview')}
                      </button>
                    </div>
                  </form>
                ) : (
                  <div className="mt-4 text-sm text-stone-500">{t('bookDetailEmpty')}</div>
                )}
              </div>

              <div className="rounded border border-stone-300 bg-white p-5" {...testAttr('book-question-panel')}>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="font-semibold">{t('questionPanelTitle')}</h3>
                    <p className="text-sm text-stone-600">{t('questionPanelDescription')}</p>
                  </div>
                  <button className="rounded border border-stone-950 px-3 py-2 text-sm font-medium disabled:opacity-50" disabled={!selectedBook || flow.state.loading || pendingQuestionGeneration || questionGenerationPending} onClick={generateQuestionsForSelectedBook} type="button" {...testAttr('book-generate-questions')}>
                    {t('questionGenerate')}
                  </button>
                </div>
                <div className="mt-4 grid gap-2">
                  {showQuestionSkeleton && [0, 1, 2].map((item) => <QuestionRowSkeleton key={item} />)}
                  {currentQuestions.map((question) => {
                    const answered = answeredQuestionIds.has(question.questionId);

                    return (
                      <div
                        className={`grid gap-2 rounded border p-3 md:grid-cols-[minmax(0,1fr)_auto] md:items-center ${flow.state.selectedQuestionId === question.questionId ? 'border-stone-950 bg-stone-100' : 'border-stone-200 bg-white'}`}
                        key={question.questionId}
                        {...testAttr('book-question-row')}
                      >
                        <button
                          className="text-left text-sm leading-6"
                          onClick={() => {
                            if (flow.state.window?.windowId !== question.windowId) {
                              flow.selectWindow(question.windowId);
                            }
                            flow.selectQuestion(question.questionId);
                            setPage('review');
                          }}
                          type="button"
                          {...testAttr('book-question-link')}
                        >
                          {question.questionText}
                        </button>
                        {answered ? (
                          <span className="rounded bg-stone-100 px-3 py-2 text-sm text-stone-500" {...testAttr('book-question-answered')}>
                            {t('questionAnswered')}
                          </span>
                        ) : (
                          <button
                            className="rounded border border-red-300 px-3 py-2 text-sm text-red-700 disabled:opacity-50"
                            disabled={flow.state.loading}
                            onClick={() => deleteQuestion(question.questionId)}
                            type="button"
                            {...testAttr('book-question-delete')}
                          >
                            {t('delete')}
                          </button>
                        )}
                      </div>
                    );
                  })}
                  {!showQuestionSkeleton && !currentQuestions.length && <div className="rounded bg-stone-100 p-4 text-sm text-stone-500">{t('questionEmpty')}</div>}
                </div>
              </div>

              <div className="rounded border border-stone-300 bg-white p-5" {...testAttr('book-debate-entry')}>
                <h3 className="font-semibold">{t('debateEntryTitle')}</h3>
                <p className="mt-1 text-sm text-stone-600">{t('debateEntryDescription')}</p>
                <div className="mt-3 grid gap-2" {...testAttr('debate-participant-picker')}>
                  <div className="text-sm font-medium">{t('debateParticipantPicker')}</div>
                  <div className="flex flex-wrap gap-2">
                    {flow.state.personas.map((persona) => {
                      const selected = selectedDebatePersonaIds.includes(persona.personaId);
                      return (
                        <button
                          className={`grid max-w-56 gap-1 rounded border px-3 py-2 text-left text-sm ${selected ? 'border-stone-950 bg-stone-950 text-white' : 'border-stone-300 bg-white text-stone-700'}`}
                          key={persona.personaId}
                          onClick={() => toggleDebatePersona(persona.personaId)}
                          title={persona.description}
                          type="button"
                          {...testAttr('debate-participant-toggle')}
                        >
                          <span className="font-medium"><span aria-hidden="true">{personaIcon(persona)}</span> {persona.displayName}</span>
                          {persona.tone && <span className={`text-xs ${selected ? 'text-stone-200' : 'text-stone-500'}`}>{persona.tone}</span>}
                          {persona.description && (
                            <span className={`line-clamp-2 text-xs leading-5 ${selected ? 'text-stone-200' : 'text-stone-500'}`}>
                              {persona.description}
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
                <div className="mt-3 flex gap-2">
                  <input className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setDebateTopic(event.target.value)} placeholder={t('debateTopicPlaceholder')} value={debateTopic} {...testAttr('debate-topic-input')} />
                  <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={!selectedBook || !debateTopic.trim() || !selectedDebatePersonaIds.length || flow.state.loading} onClick={enterDebate} type="button" {...testAttr('debate-enter-submit')}>
                    {t('debateEnter')}
                  </button>
                </div>
              </div>
            </section>
          )}

          {page === 'review' && (
            <section className="grid gap-6" {...testAttr('review-page')}>
              <form className="grid min-h-[calc(100vh-220px)] gap-4 rounded border border-stone-300 bg-white p-5" onSubmit={submitReflection} {...testAttr('reflection-form')}>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <h2 className="text-xl font-semibold">{t('reviewPageTitle')}</h2>
                  <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={!flow.state.session || flow.state.loading || !reflectionDraftText} type="submit" {...testAttr('reflection-submit')}>
                    {t('saveRecord')}
                  </button>
                </div>
                <div className="grid min-h-[62vh] gap-3 rounded border border-stone-300 bg-stone-100 p-4" {...testAttr('reflection-editor-shell')}>
                  <input className="min-w-0 rounded border border-stone-200 bg-white px-4 py-3 text-sm outline-none focus:border-stone-500" onChange={(event) => setReflectionEvidence(event.target.value)} placeholder={t('reviewEvidencePlaceholder')} value={reflectionEvidence} {...testAttr('reflection-evidence-input')} />
                  <div className="reflection-markdown-shell min-h-[54vh] rounded border border-stone-200 bg-white shadow-sm" {...testAttr('reflection-content-input')}>
                    <ReflectionMarkdownEditor onChange={setReflectionContent} placeholder={t('reviewPlaceholder')} textareaTestId="reflection-content-textarea" value={reflectionContent} />
                  </div>
                  <div className="flex justify-end">
                    <SpeechDraftControl disabled={flow.state.loading} label="reflection-content" onChange={setReflectionContent} value={reflectionContent} />
                  </div>
                </div>
              </form>

              <form className="grid gap-3 rounded border border-stone-300 bg-white p-5" onSubmit={submitAnswer} {...testAttr('question-answer-form')}>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="font-semibold">{t('questionAnswerTitle')}</h3>
                  <button
                    className="rounded border border-stone-300 px-3 py-2 text-sm"
                    onClick={() => setPage('book-detail')}
                    type="button"
                    {...testAttr('question-answer-back')}
                  >
                    {t('questionAnswerBack')}
                  </button>
                </div>
                <div className="rounded bg-stone-100 p-3 text-sm">{selectedQuestion?.questionText || t('questionFallback')}</div>
                <textarea className="min-h-24 rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setAnswerDraft(event.target.value)} placeholder={t('questionAnswerPlaceholder')} value={answerDraft} {...testAttr('question-answer-input')} />
                <SpeechDraftControl disabled={flow.state.loading} label="question-answer" onChange={setAnswerDraft} value={answerDraft} />
                <button className="rounded border border-stone-950 px-4 py-2 text-sm font-medium disabled:opacity-50" disabled={!flow.state.window || !answerDraft.trim() || flow.state.loading} type="submit" {...testAttr('question-answer-submit')}>
                  {t('questionAnswerSubmit')}
                </button>
                <div className="grid gap-2 border-t border-stone-200 pt-3" {...testAttr('question-answer-history')}>
                  <div className="text-sm font-medium">{t('questionAnswerHistory')}</div>
                  {selectedQuestionMessages.map((message) => (
                    <article
                      className={`rounded px-3 py-2 text-sm leading-6 ${message.role === 'user' ? 'bg-stone-950 text-white' : 'bg-stone-100 text-stone-800'}`}
                      key={message.id}
                      {...testAttr('question-answer-history-message')}
                    >
                      <div className={`text-xs font-semibold ${message.role === 'user' ? 'text-stone-200' : 'text-stone-500'}`}>
                        {message.role === 'user' ? t('userAnswer') : messageName(message, t('userMessageName'))}
                      </div>
                      <div className="mt-1 whitespace-pre-wrap">{message.content}</div>
                    </article>
                  ))}
                  {!selectedQuestionMessages.length && (
                    <div className="rounded bg-stone-100 px-3 py-2 text-sm text-stone-500">{t('questionAnswerEmpty')}</div>
                  )}
                </div>
              </form>

              <div className="grid gap-2 rounded border border-stone-300 bg-white p-5" {...testAttr('reflection-list')}>
                <h3 className="font-semibold">{t('reviewListTitle')}</h3>
                {flow.state.insights.map((insight) => (
                  <article className="rounded border border-stone-200 p-3" key={insight.insightId}>
                    <div className="text-xs font-semibold uppercase text-stone-500">{insight.insightType}</div>
                    <MarkdownContent className="mt-1 text-sm leading-6" value={insight.content} />
                    {insight.evidence && <div className="mt-1 text-xs text-stone-600">{insight.evidence}</div>}
                  </article>
                ))}
              </div>
            </section>
          )}

          {page === 'debate' && (
            <section className="grid gap-5 lg:grid-cols-[260px_minmax(0,1fr)]" {...testAttr('debate-page')}>
              <div className="grid gap-2 rounded border border-stone-300 bg-white p-5" {...testAttr('debate-window-list')}>
                <h3 className="font-semibold">{t('debateRooms')}</h3>
                {debateWindows.map((window) => (
                  <button
                    className={`rounded border px-3 py-3 text-left text-sm ${
                      flow.state.window?.windowId === window.windowId
                        ? 'border-stone-950 bg-stone-100'
                        : 'border-stone-200 hover:border-stone-400'
                    }`}
                    key={window.windowId}
                    onClick={() => {
                      flow.selectWindow(window.windowId);
                      setDebateTopic(debateTopicFromWindowTitle(window.title));
                    }}
                    type="button"
                    {...testAttr('debate-window-tab')}
                  >
                    <span className="block font-medium">{window.title}</span>
                    <span className="mt-1 block text-xs text-stone-500">Window #{window.windowId}</span>
                  </button>
                ))}
                {!debateWindows.length && <div className="text-sm text-stone-500">{t('debateNoRooms')}</div>}
              </div>

              <div className="grid min-h-[620px] grid-rows-[auto_auto_1fr_auto] overflow-hidden rounded border border-stone-300 bg-white" {...testAttr('debate-chat-panel')}>
                <div className="border-b border-stone-200 px-5 py-4">
                  <h2 className="text-xl font-semibold">{t('debate')}</h2>
                  <p className="mt-1 text-sm text-stone-600">
                    {flow.state.window?.windowType === 'debate'
                      ? flow.state.window.title
                      : t('debateRoomFallback')}
                  </p>
                </div>

                <div className="flex gap-2 overflow-x-auto border-b border-stone-200 px-5 py-3" {...testAttr('debate-speaker-strip')}>
                  {selectedDebatePersonas.map((persona) => (
                    <button
                      className="grid min-w-24 justify-items-center gap-1 rounded border border-stone-200 bg-stone-50 px-3 py-2 text-xs hover:border-stone-950 disabled:opacity-50"
                      disabled={flow.state.window?.windowType !== 'debate' || flow.state.loading || !(debateDraft.trim() || debateTopic.trim())}
                      key={persona.personaId}
                      onClick={() => requestPersonaReply(persona.personaId)}
                      type="button"
                      {...testAttr('debate-speaker-reply')}
                    >
                      <span className="grid h-9 w-9 place-items-center rounded-full bg-stone-900 text-base text-white" aria-hidden="true">{personaIcon(persona)}</span>
                      <span className="font-medium">{persona.displayName}</span>
                      <span className="text-stone-500">{t('debateSpeakerReply')}</span>
                    </button>
                  ))}
                </div>

                <div className="grid content-start gap-4 overflow-y-auto bg-stone-100 px-5 py-5" {...testAttr('debate-message-list')}>
                  {activeMessages.map((message) => {
                    const isUser = message.role === 'user';
                    const persona = flow.state.personas.find((item) => item.personaId === message.personaId);
                    return (
                      <article className={`flex gap-3 ${isUser ? 'justify-end' : 'justify-start'}`} key={message.id}>
                        {!isUser && (
                          <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-stone-950 text-sm font-semibold text-white" aria-hidden="true">
                            {message.personaId ? personaIcon(persona) : 'AI'}
                          </div>
                        )}
                        <div className={`max-w-[78%] ${isUser ? 'items-end' : 'items-start'} grid gap-1`}>
                          <div className={`text-xs font-medium ${isUser ? 'text-right text-stone-500' : 'text-stone-600'}`}>{messageName(message, t('userMessageName'))}</div>
                          <div className={`rounded-2xl px-4 py-3 text-sm leading-6 shadow-sm ${isUser ? 'rounded-br-sm bg-stone-950 text-white' : 'rounded-bl-sm bg-white text-stone-900'}`}>
                            {message.content}
                          </div>
                        </div>
                      </article>
                    );
                  })}
                  {showDebateReplySkeleton && <DebateReplySkeleton />}
                  {!showDebateReplySkeleton && !activeMessages.length && <div className="self-center rounded bg-white px-4 py-3 text-center text-sm text-stone-500">{t('debateEmpty')}</div>}
                </div>

                <form className="grid gap-3 border-t border-stone-200 bg-white p-4" onSubmit={submitDebate} {...testAttr('debate-session-form')}>
                  <textarea className="min-h-20 rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setDebateDraft(event.target.value)} placeholder={t('debatePlaceholder')} value={debateDraft} {...testAttr('debate-session-message-input')} />
                  <SpeechDraftControl disabled={flow.state.loading || flow.state.window?.windowType !== 'debate'} label="debate-session-message" onChange={setDebateDraft} value={debateDraft} />
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="text-xs text-stone-500">{t('debateParticipantCount')} {selectedDebatePersonas.length}</div>
                    <div className="flex flex-wrap gap-2">
                      <button className="rounded border border-stone-950 px-4 py-2 text-sm font-medium disabled:opacity-50" disabled={flow.state.window?.windowType !== 'debate' || !selectedDebatePersonas.length || flow.state.loading || !(debateDraft.trim() || debateTopic.trim())} onClick={requestAllPersonaReplies} type="button" {...testAttr('debate-all-submit')}>
                        {t('debateAllReply')}
                      </button>
                      <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={flow.state.window?.windowType !== 'debate' || !selectedDebatePersonas.length || flow.state.loading || !(debateDraft.trim() || debateTopic.trim())} type="submit" {...testAttr('debate-session-submit')}>
                        {t('debateSpeakerReply')}
                      </button>
                    </div>
                  </div>
                </form>
              </div>
            </section>
          )}
        </section>
      </section>
    </main>
  );
}
