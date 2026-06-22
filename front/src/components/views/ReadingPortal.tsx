import { FormEvent, useEffect, useState } from 'react';
import { LoadingSpinner } from '../atoms/LoadingSpinner';
import { Skeleton } from '../atoms/Skeleton';
import { useSpeechRecognition } from '../../hooks/useSpeechRecognition';
import { useSessionFlow } from '../../hooks/useSessionFlow';
import type { BookCandidate, SaveBookResponse } from '../../types/models/book';
import type { Persona } from '../../types/models/persona';
import type { SessionDisplayMessage } from '../../types/view-models/sessionFlow';
import type { MarginsPage } from '../../types/view-models/sessionFlow';
import { confirmDelete } from '../../utils/deleteConfirmation';
import { testAttr } from '../../utils/testAttrs';

const pageLabels: Record<MarginsPage, string> = {
  'book-search': '책 검색/등록',
  'book-list': '등록 책 리스트',
  'book-detail': '등록 책 상세',
  review: '독후감',
  debate: '토론 세션',
};

function personaIcon(persona?: Persona) {
  const label = `${persona?.displayName || ''} ${persona?.name || ''}`;
  if (label.includes('전사') || label.includes('warrior')) {
    return '⚔️';
  }
  if (label.includes('마법사') || label.includes('wizard')) {
    return '🪄';
  }
  if (label.includes('성직자') || label.includes('cleric')) {
    return '✚';
  }
  if (label.includes('도적') || label.includes('rogue')) {
    return '🗡️';
  }
  return '👤';
}

function messageName(message: SessionDisplayMessage) {
  if (message.role === 'user') {
    return '나';
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
  const speech = useSpeechRecognition({ onTranscript: onChange, value });
  const blocked = disabled || !speech.supported;

  return (
    <div className="grid gap-1">
      <button
        aria-label={speech.listening ? '음성 입력 중지' : '음성 입력 시작'}
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
        {speech.listening ? '중지' : '음성 입력'}
      </button>
      {!speech.supported && (
        <div className="text-xs text-stone-500" {...testAttr(`${label}-speech-unsupported`)}>
          현재 브라우저는 음성 입력을 지원하지 않습니다.
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
  const questionWindow = flow.state.windows.find((window) => window.windowType === 'question') || flow.state.windows[0];
  const selectedDebatePersonas = flow.state.personas.filter((persona) => selectedDebatePersonaIds.includes(persona.personaId));
  const activeMessages = flow.state.window
    ? flow.state.messages.filter((message) => message.windowId === flow.state.window?.windowId)
    : flow.state.messages;
  const answeredQuestionIds = new Set(
    flow.state.messages
      .filter((message) => message.role === 'user' && message.questionId !== undefined)
      .map((message) => message.questionId as number),
  );
  const selectedQuestion = flow.state.questions.find((question) => question.questionId === flow.state.selectedQuestionId);
  const selectedQuestionMessages = selectedQuestion
    ? flow.state.messages.filter((message) => (
      message.windowId === selectedQuestion.windowId
      && message.questionId === selectedQuestion.questionId
    ))
    : [];
  const showQuestionSkeleton = pendingQuestionGeneration || questionGenerationPending;
  const showDebateReplySkeleton = flow.state.loading && page === 'debate' && flow.state.window?.windowType === 'debate';

  useEffect(() => {
    if (!flow.state.hydrated && !flow.state.loading) {
      void flow.loadLatest();
    }
  }, [flow.state.hydrated, flow.state.loading]);

  useEffect(() => {
    if (selectedBook) {
      setSelectedBookId(selectedBook.bookId);
      setEditTitle(selectedBook.title);
      setEditAuthor(selectedBook.author || '');
    }
  }, [selectedBook?.bookId, selectedBook?.title, selectedBook?.author]);

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
    if (!selectedBook || !confirmDelete()) {
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
    if (!reflectionContent.trim()) {
      return;
    }

    void flow.createSessionInsight({
      insightType: 'reflection',
      title: selectedBook ? `${selectedBook.title} 독후감` : '독후감',
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
    if (!confirmDelete()) {
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
    if (!debateDraft.trim() || !selectedDebatePersonaIds.length) {
      return;
    }

    const prompt = debateDraft.trim();
    void flow.debateWithPersonas(selectedDebatePersonaIds, prompt).then((saved) => {
      if (saved) {
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

    const responseCounts = new Map<number, number>();
    activeMessages.forEach((message) => {
      if (message.personaId) {
        responseCounts.set(message.personaId, (responseCounts.get(message.personaId) || 0) + 1);
      }
    });
    const nextPersona = [...selectedDebatePersonas].sort((left, right) => (
      (responseCounts.get(left.personaId) || 0) - (responseCounts.get(right.personaId) || 0)
    ))[0];
    requestPersonaReply(nextPersona.personaId);
  }

  return (
    <main className="min-h-screen bg-stone-100 text-stone-950" {...testAttr('reading-portal')}>
      <header className="border-b border-stone-300 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-5 py-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-3xl font-semibold">Margins</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-stone-600">
              도서 검색/등록부터 독후감, 질문 답변, AI 페르소나 토론까지 페이지 단위로 진행합니다.
            </p>
          </div>
          <nav className="grid gap-2 sm:grid-cols-5" aria-label="Primary pages" {...testAttr('portal-page-nav')}>
            {(Object.keys(pageLabels) as MarginsPage[]).map((pageId) => (
              <button
                className={`rounded border px-3 py-2 text-sm font-medium ${
                  page === pageId
                    ? 'border-stone-950 bg-stone-950 text-white'
                    : 'border-stone-300 bg-white text-stone-700 hover:border-stone-950'
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
        <aside className="grid content-start gap-3 rounded border border-stone-300 bg-white p-4" {...testAttr('portal-sidebar')}>
          <div>
            <div className="text-xs font-semibold uppercase text-stone-500">현재 책</div>
            <div className="mt-1 text-lg font-semibold">{selectedBook?.title || '선택된 책 없음'}</div>
            {selectedBook?.author && <div className="text-sm text-stone-600">{selectedBook.author}</div>}
          </div>
          {flow.state.session && (
            <div className="rounded bg-stone-100 p-3 text-sm leading-6">
              <div className="font-medium">{flow.state.session.title}</div>
              <div className="text-stone-600">
                질문 {flow.state.stats?.answeredQuestionCount || 0}/{flow.state.stats?.questionCount || 0} · 메시지 {flow.state.stats?.messageCount || 0}
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
              <div className="rounded border border-stone-300 bg-white p-5">
                <h2 className="text-xl font-semibold">책 검색 및 등록</h2>
                <form className="mt-4 flex gap-2" onSubmit={submitSearch} {...testAttr('book-search-form')}>
                  <input
                    className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-900"
                    onChange={(event) => flow.setQuery(event.target.value)}
                    placeholder="책 이름, 저자, 읽고 싶은 주제"
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
                    {bookSearchPending ? '검색 중' : '검색'}
                  </button>
                </form>
              </div>

              <div className="grid gap-3 md:grid-cols-3" {...testAttr('book-candidate-list')}>
                {bookSearchPending && [0, 1, 2].map((item) => <BookCandidateSkeleton key={item} />)}
                {flow.state.candidates.map((candidate) => (
                  <article className="grid gap-3 rounded border border-stone-300 bg-white p-4" key={candidate.candidateId} {...testAttr('book-candidate-card')}>
                    <div>
                      <div className="text-lg font-semibold">{candidate.title}</div>
                      <div className="text-sm text-stone-600">{candidate.author}</div>
                      <div className="mt-2 text-xs font-medium text-stone-500" {...testAttr('book-candidate-id')}>
                        고유번호 {candidate.candidateId}
                        {candidate.publishedYear ? ` · ${candidate.publishedYear}` : ''}
                      </div>
                      {candidate.isbn && <div className="mt-1 text-xs font-medium text-stone-500">ISBN {candidate.isbn}</div>}
                      {candidate.reason && <p className="mt-2 text-sm leading-6 text-stone-600">{candidate.reason}</p>}
                    </div>
                    <button
                      className="rounded border border-stone-950 px-3 py-2 text-sm font-medium"
                      disabled={flow.state.loading}
                      onClick={() => saveCandidate(candidate)}
                      type="button"
                      {...testAttr('book-candidate-save')}
                    >
                      등록
                    </button>
                  </article>
                ))}
              </div>

              <form className="grid gap-3 rounded border border-stone-300 bg-white p-5" onSubmit={submitManualBook} {...testAttr('manual-book-form')}>
                <div>
                  <h3 className="font-semibold">검색 결과가 없을 때 직접 등록</h3>
                  <p className="text-sm text-stone-600">책 이름과 저자를 입력해 내 책장에 추가합니다.</p>
                </div>
                <div className="grid gap-2 md:grid-cols-[1fr_1fr_auto]">
                  <input
                    className="rounded border border-stone-300 px-3 py-2 text-sm"
                    onChange={(event) => setManualTitle(event.target.value)}
                    placeholder="책 이름"
                    value={manualTitle}
                    {...testAttr('manual-book-title-input')}
                  />
                  <input
                    className="rounded border border-stone-300 px-3 py-2 text-sm"
                    onChange={(event) => setManualAuthor(event.target.value)}
                    placeholder="저자"
                    value={manualAuthor}
                    {...testAttr('manual-book-author-input')}
                  />
                  <button
                    className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
                    disabled={flow.state.loading || !manualTitle.trim() || !manualAuthor.trim()}
                    type="submit"
                    {...testAttr('manual-book-submit')}
                  >
                    직접 등록
                  </button>
                </div>
              </form>
            </section>
          )}

          {page === 'book-list' && (
            <section className="grid gap-3" {...testAttr('book-list-page')}>
              <h2 className="text-xl font-semibold">등록 책 리스트</h2>
              {flow.state.savedBooks.map((book) => (
                <article className="flex flex-col gap-3 rounded border border-stone-300 bg-white p-4 md:flex-row md:items-center md:justify-between" key={book.bookId} {...testAttr('saved-book-row')}>
                  <button className="text-left" onClick={() => goToBook(book)} type="button" {...testAttr('saved-book-detail-link')}>
                    <div className="text-lg font-semibold">{book.title}</div>
                    <div className="text-sm text-stone-600">{book.author}</div>
                  </button>
                  <div className="flex gap-2">
                    <button className="rounded border border-stone-300 px-3 py-2 text-sm" onClick={() => goToBook(book)} type="button" {...testAttr('saved-book-edit-open')}>
                      수정
                    </button>
                    <button
                      className="rounded border border-red-300 px-3 py-2 text-sm text-red-700"
                      disabled={flow.state.loading}
                      onClick={() => {
                        if (confirmDelete()) {
                          void flow.deleteBook(book.bookId);
                        }
                      }}
                      type="button"
                      {...testAttr('saved-book-delete')}
                    >
                      삭제
                    </button>
                  </div>
                </article>
              ))}
              {!flow.state.savedBooks.length && <div className="rounded border border-stone-300 bg-white p-8 text-center text-sm text-stone-500">등록된 책이 없습니다.</div>}
            </section>
          )}

          {page === 'book-detail' && (
            <section className="grid gap-5" {...testAttr('book-detail-page')}>
              <div className="rounded border border-stone-300 bg-white p-5">
                <h2 className="text-xl font-semibold">등록 책 상세</h2>
                {selectedBook ? (
                  <form className="mt-4 grid gap-3" onSubmit={submitBookEdit} {...testAttr('book-edit-form')}>
                    <input className="rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setEditTitle(event.target.value)} value={editTitle} {...testAttr('book-edit-title-input')} />
                    <input className="rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setEditAuthor(event.target.value)} value={editAuthor} {...testAttr('book-edit-author-input')} />
                    <div className="flex flex-wrap gap-2">
                      <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={flow.state.loading || !editTitle.trim() || !editAuthor.trim()} type="submit" {...testAttr('book-edit-submit')}>
                        수정 저장
                      </button>
                      <button className="rounded border border-red-300 px-4 py-2 text-sm text-red-700" disabled={flow.state.loading} onClick={deleteSelectedBook} type="button" {...testAttr('book-detail-delete')}>
                        삭제
                      </button>
                      <button className="rounded border border-stone-900 px-4 py-2 text-sm font-medium" disabled={flow.state.loading} onClick={startReflection} type="button" {...testAttr('book-start-review')}>
                        독후감 작성
                      </button>
                    </div>
                  </form>
                ) : (
                  <div className="mt-4 text-sm text-stone-500">목록에서 책을 선택하세요.</div>
                )}
              </div>

              <div className="rounded border border-stone-300 bg-white p-5" {...testAttr('book-question-panel')}>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="font-semibold">질문지 리스트</h3>
                    <p className="text-sm text-stone-600">OpenAI가 책과 저자를 바탕으로 만든 질문에 답변합니다.</p>
                  </div>
                  <button className="rounded border border-stone-950 px-3 py-2 text-sm font-medium disabled:opacity-50" disabled={!selectedBook || flow.state.loading || pendingQuestionGeneration || questionGenerationPending} onClick={generateQuestionsForSelectedBook} type="button" {...testAttr('book-generate-questions')}>
                    질문 생성
                  </button>
                </div>
                <div className="mt-4 grid gap-2">
                  {showQuestionSkeleton && [0, 1, 2].map((item) => <QuestionRowSkeleton key={item} />)}
                  {flow.state.questions.map((question) => {
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
                            답변 완료
                          </span>
                        ) : (
                          <button
                            className="rounded border border-red-300 px-3 py-2 text-sm text-red-700 disabled:opacity-50"
                            disabled={flow.state.loading}
                            onClick={() => deleteQuestion(question.questionId)}
                            type="button"
                            {...testAttr('book-question-delete')}
                          >
                            삭제
                          </button>
                        )}
                      </div>
                    );
                  })}
                  {!showQuestionSkeleton && !flow.state.questions.length && <div className="rounded bg-stone-100 p-4 text-sm text-stone-500">독후감 작성을 시작한 뒤 질문을 생성할 수 있습니다.</div>}
                </div>
              </div>

              <div className="rounded border border-stone-300 bg-white p-5" {...testAttr('book-debate-entry')}>
                <h3 className="font-semibold">토론하기</h3>
                <p className="mt-1 text-sm text-stone-600">토론 세션 진입 전 하나의 주제를 먼저 정합니다.</p>
                <div className="mt-3 grid gap-2" {...testAttr('debate-participant-picker')}>
                  <div className="text-sm font-medium">토론자 선택</div>
                  <div className="flex flex-wrap gap-2">
                    {flow.state.personas.map((persona) => {
                      const selected = selectedDebatePersonaIds.includes(persona.personaId);
                      return (
                        <button
                          className={`rounded border px-3 py-2 text-sm ${selected ? 'border-stone-950 bg-stone-950 text-white' : 'border-stone-300 bg-white text-stone-700'}`}
                          key={persona.personaId}
                          onClick={() => toggleDebatePersona(persona.personaId)}
                          type="button"
                          {...testAttr('debate-participant-toggle')}
                        >
                          <span aria-hidden="true">{personaIcon(persona)}</span> {persona.displayName}
                        </button>
                      );
                    })}
                  </div>
                </div>
                <div className="mt-3 flex gap-2">
                  <input className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setDebateTopic(event.target.value)} placeholder="토론 주제" value={debateTopic} {...testAttr('debate-topic-input')} />
                  <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={!selectedBook || !debateTopic.trim() || !selectedDebatePersonaIds.length || flow.state.loading} onClick={enterDebate} type="button" {...testAttr('debate-enter-submit')}>
                    토론 세션 진입
                  </button>
                </div>
              </div>
            </section>
          )}

          {page === 'review' && (
            <section className="grid gap-5" {...testAttr('review-page')}>
              <form className="grid gap-3 rounded border border-stone-300 bg-white p-5" onSubmit={submitReflection} {...testAttr('reflection-form')}>
                <h2 className="text-xl font-semibold">독후감 페이지</h2>
                <textarea className="min-h-36 rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setReflectionContent(event.target.value)} placeholder="독후감, 좋았던 점, 개인 의견을 기록하세요." value={reflectionContent} {...testAttr('reflection-content-input')} />
                <SpeechDraftControl disabled={flow.state.loading} label="reflection-content" onChange={setReflectionContent} value={reflectionContent} />
                <input className="rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setReflectionEvidence(event.target.value)} placeholder="관련 구절이나 근거" value={reflectionEvidence} {...testAttr('reflection-evidence-input')} />
                <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={!flow.state.session || flow.state.loading || !reflectionContent.trim()} type="submit" {...testAttr('reflection-submit')}>
                  기록 저장
                </button>
              </form>

              <form className="grid gap-3 rounded border border-stone-300 bg-white p-5" onSubmit={submitAnswer} {...testAttr('question-answer-form')}>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="font-semibold">선택 질문 답변</h3>
                  <button
                    className="rounded border border-stone-300 px-3 py-2 text-sm"
                    onClick={() => setPage('book-detail')}
                    type="button"
                    {...testAttr('question-answer-back')}
                  >
                    질문지로 돌아가기
                  </button>
                </div>
                <div className="rounded bg-stone-100 p-3 text-sm">{selectedQuestion?.questionText || '책 상세 페이지에서 질문을 선택하세요.'}</div>
                <textarea className="min-h-24 rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setAnswerDraft(event.target.value)} placeholder="질문에 대한 답변" value={answerDraft} {...testAttr('question-answer-input')} />
                <SpeechDraftControl disabled={flow.state.loading} label="question-answer" onChange={setAnswerDraft} value={answerDraft} />
                <button className="rounded border border-stone-950 px-4 py-2 text-sm font-medium disabled:opacity-50" disabled={!flow.state.window || !answerDraft.trim() || flow.state.loading} type="submit" {...testAttr('question-answer-submit')}>
                  답변 저장 및 AI 응답 받기
                </button>
                <div className="grid gap-2 border-t border-stone-200 pt-3" {...testAttr('question-answer-history')}>
                  <div className="text-sm font-medium">답변 기록</div>
                  {selectedQuestionMessages.map((message) => (
                    <article
                      className={`rounded px-3 py-2 text-sm leading-6 ${message.role === 'user' ? 'bg-stone-950 text-white' : 'bg-stone-100 text-stone-800'}`}
                      key={message.id}
                      {...testAttr('question-answer-history-message')}
                    >
                      <div className={`text-xs font-semibold ${message.role === 'user' ? 'text-stone-200' : 'text-stone-500'}`}>
                        {message.role === 'user' ? '내 답변' : messageName(message)}
                      </div>
                      <div className="mt-1 whitespace-pre-wrap">{message.content}</div>
                    </article>
                  ))}
                  {!selectedQuestionMessages.length && (
                    <div className="rounded bg-stone-100 px-3 py-2 text-sm text-stone-500">아직 저장된 답변이 없습니다.</div>
                  )}
                </div>
              </form>

              <div className="grid gap-2 rounded border border-stone-300 bg-white p-5" {...testAttr('reflection-list')}>
                <h3 className="font-semibold">저장된 기록</h3>
                {flow.state.insights.map((insight) => (
                  <article className="rounded border border-stone-200 p-3" key={insight.insightId}>
                    <div className="text-xs font-semibold uppercase text-stone-500">{insight.insightType}</div>
                    <div className="mt-1 text-sm leading-6">{insight.content}</div>
                    {insight.evidence && <div className="mt-1 text-xs text-stone-600">{insight.evidence}</div>}
                  </article>
                ))}
              </div>
            </section>
          )}

          {page === 'debate' && (
            <section className="grid gap-5 lg:grid-cols-[260px_minmax(0,1fr)]" {...testAttr('debate-page')}>
              <div className="grid gap-2 rounded border border-stone-300 bg-white p-5" {...testAttr('debate-window-list')}>
                <h3 className="font-semibold">토론방</h3>
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
                      setDebateTopic(window.title.replace(/^토론: /, ''));
                    }}
                    type="button"
                    {...testAttr('debate-window-tab')}
                  >
                    <span className="block font-medium">{window.title}</span>
                    <span className="mt-1 block text-xs text-stone-500">Window #{window.windowId}</span>
                  </button>
                ))}
                {!debateWindows.length && <div className="text-sm text-stone-500">아직 생성된 토론방이 없습니다.</div>}
              </div>

              <div className="grid min-h-[620px] grid-rows-[auto_auto_1fr_auto] overflow-hidden rounded border border-stone-300 bg-white" {...testAttr('debate-chat-panel')}>
                <div className="border-b border-stone-200 px-5 py-4">
                  <h2 className="text-xl font-semibold">토론 세션</h2>
                  <p className="mt-1 text-sm text-stone-600">
                    {flow.state.window?.windowType === 'debate'
                      ? flow.state.window.title
                      : '책 상세 페이지에서 토론 주제를 정하면 독립된 대화방이 생성됩니다.'}
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
                      <span className="text-stone-500">{persona.displayName} 답변 받기</span>
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
                          <div className={`text-xs font-medium ${isUser ? 'text-right text-stone-500' : 'text-stone-600'}`}>{messageName(message)}</div>
                          <div className={`rounded-2xl px-4 py-3 text-sm leading-6 shadow-sm ${isUser ? 'rounded-br-sm bg-stone-950 text-white' : 'rounded-bl-sm bg-white text-stone-900'}`}>
                            {message.content}
                          </div>
                        </div>
                      </article>
                    );
                  })}
                  {showDebateReplySkeleton && <DebateReplySkeleton />}
                  {!showDebateReplySkeleton && !activeMessages.length && <div className="self-center rounded bg-white px-4 py-3 text-center text-sm text-stone-500">아직 토론 메시지가 없습니다.</div>}
                </div>

                <form className="grid gap-3 border-t border-stone-200 bg-white p-4" onSubmit={submitDebate} {...testAttr('debate-session-form')}>
                  <textarea className="min-h-20 rounded border border-stone-300 px-3 py-2 text-sm" onChange={(event) => setDebateDraft(event.target.value)} placeholder="메신저처럼 보낼 말을 입력하세요." value={debateDraft} {...testAttr('debate-session-message-input')} />
                  <SpeechDraftControl disabled={flow.state.loading || flow.state.window?.windowType !== 'debate'} label="debate-session-message" onChange={setDebateDraft} value={debateDraft} />
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="text-xs text-stone-500">선택된 토론자 {selectedDebatePersonas.length}명</div>
                    <div className="flex flex-wrap gap-2">
                      <button className="rounded border border-stone-950 px-4 py-2 text-sm font-medium disabled:opacity-50" disabled={flow.state.window?.windowType !== 'debate' || !selectedDebatePersonas.length || flow.state.loading || !(debateDraft.trim() || debateTopic.trim())} onClick={requestNextPersonaReply} type="button" {...testAttr('debate-next-reply-submit')}>
                        다음 대답 받기
                      </button>
                      <button className="rounded bg-stone-950 px-4 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={flow.state.window?.windowType !== 'debate' || !selectedDebatePersonas.length || !debateDraft.trim() || flow.state.loading} type="submit" {...testAttr('debate-session-submit')}>
                        선택 토론자에게 보내기
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
