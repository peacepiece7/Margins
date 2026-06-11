import { FormEvent, useState } from 'react';
import { useSessionFlow } from '../../hooks/useSessionFlow';
import { testAttr } from '../../utils/testAttrs';

export function SessionWorkbench() {
  const flow = useSessionFlow();
  const [message, setMessage] = useState('');
  const [debate, setDebate] = useState('');

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    void flow.search();
  }

  function submitMessage(event: FormEvent) {
    event.preventDefault();
    if (!message.trim()) {
      return;
    }
    void flow.send(message.trim());
    setMessage('');
  }

  function submitDebate(event: FormEvent) {
    event.preventDefault();
    if (!debate.trim()) {
      return;
    }
    void flow.debate(debate.trim());
    setDebate('');
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-6xl flex-col gap-6 px-5 py-6">
      <header className="flex items-center justify-between border-b border-stone-300 pb-4">
        <div>
          <h1 className="text-2xl font-semibold">Margins</h1>
          <p className="text-sm text-stone-600">Reading session workbench</p>
        </div>
        <div className="text-right text-sm text-stone-600">
          {flow.state.session ? `Session #${flow.state.session.sessionId}` : 'Single-user mode'}
        </div>
      </header>

      <section className="grid gap-5 lg:grid-cols-[360px_1fr]">
        <aside className="flex flex-col gap-4">
          <form className="flex gap-2" onSubmit={submitSearch} {...testAttr('book-search-form')}>
            <input
              className="min-w-0 flex-1 rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
              value={flow.state.query}
              onChange={(event) => flow.setQuery(event.target.value)}
              placeholder="Search a book"
              {...testAttr('book-search-input')}
            />
            <button
              className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
              disabled={flow.state.loading || !flow.state.query.trim()}
              type="submit"
              {...testAttr('book-search-submit')}
            >
              Search
            </button>
          </form>

          {flow.state.error && (
            <div className="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-800" {...testAttr('error-message')}>
              {flow.state.error}
            </div>
          )}

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
        </aside>

        <section className="flex min-h-[640px] flex-col rounded border border-stone-300 bg-white">
          <div className="border-b border-stone-200 px-4 py-3" {...testAttr('session-summary')}>
            <div className="text-sm text-stone-500">Current book</div>
            <div className="text-lg font-semibold">
              {flow.state.selectedBook ? flow.state.selectedBook.title : 'No book selected'}
            </div>
            {flow.state.window && (
              <div className="mt-1 text-sm text-stone-600">
                Window #{flow.state.window.windowId} · {flow.state.window.status}
              </div>
            )}
          </div>

          <div className="flex-1 space-y-3 overflow-auto px-4 py-4" {...testAttr('message-list')}>
            {flow.state.messages.length === 0 && (
              <div className="py-16 text-center text-sm text-stone-500">Create a session from a candidate, then write inside the window.</div>
            )}
            {flow.state.messages.map((item) => (
              <div className="rounded border border-stone-200 bg-stone-50 p-3" key={item.messageId} {...testAttr('message-item')}>
                <div className="text-xs font-medium uppercase tracking-wide text-stone-500">
                  {item.personaId ? `Persona ${item.personaId}` : item.role} · #{item.messageId}
                </div>
                <div className="mt-2 text-sm leading-6">{item.content}</div>
              </div>
            ))}
          </div>

          <div className="grid gap-3 border-t border-stone-200 p-4 md:grid-cols-2">
            <form className="flex gap-2" onSubmit={submitMessage} {...testAttr('message-form')}>
              <input
                className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                placeholder="Window message"
                {...testAttr('message-input')}
              />
              <button className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50" disabled={!flow.state.window || flow.state.loading} type="submit">
                Send
              </button>
            </form>
            <form className="flex gap-2" onSubmit={submitDebate} {...testAttr('debate-form')}>
              <input
                className="min-w-0 flex-1 rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
                value={debate}
                onChange={(event) => setDebate(event.target.value)}
                placeholder="Persona debate"
                {...testAttr('debate-input')}
              />
              <button className="rounded border border-stone-900 px-3 py-2 text-sm font-medium disabled:opacity-50" disabled={!flow.state.window || flow.state.loading} type="submit">
                Debate
              </button>
            </form>
          </div>
        </section>
      </section>
    </main>
  );
}
