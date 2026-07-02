import { afterEach, describe, expect, it, vi } from 'vitest';
import { marginsRepository } from './marginsRepository';

function streamResponse(chunks: string[]) {
  const encoder = new TextEncoder();
  const body = new ReadableStream({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk));
      }
      controller.close();
    },
  });

  return new Response(body, {
    status: 200,
    headers: { 'Content-Type': 'text/event-stream' },
  });
}

function apiResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('marginsRepository JSON errors', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('throws the backend message from non-2xx ApiResponse failures', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(apiResponse(400, {
        success: false,
        message: 'username: must not be blank',
      })),
    );

    await expect(marginsRepository.login('', 'reader')).rejects.toThrow('username: must not be blank');
  });

  it('throws the backend message from success=false ApiResponse bodies', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(apiResponse(200, {
        success: false,
        message: 'domain rule failed',
      })),
    );

    await expect(marginsRepository.books()).rejects.toThrow('domain rule failed');
  });

  it('falls back to status text when an error response is not JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('not json', { status: 503 })),
    );

    await expect(marginsRepository.books()).rejects.toThrow('Request failed: 503');
  });
});

describe('marginsRepository.streamMessage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('forwards message deltas and resolves with the done payload', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamResponse([
        'event: message.delta\ndata: {"delta":"Hello"}\n\n',
        'event: message.delta\ndata: {"delta":" reader"}\n\n',
        'event: message.done\ndata: {"messageId":7,"windowId":3,"role":"assistant","content":"Hello reader","streamingReady":true,"aiModel":"placeholder"}\n\n',
      ])),
    );
    const deltas: string[] = [];

    const result = await marginsRepository.streamMessage(3, 'Prompt?', 11, (delta) => deltas.push(delta));

    expect(deltas).toEqual(['Hello', ' reader']);
    expect(result).toEqual({
      messageId: 7,
      windowId: 3,
      role: 'assistant',
      content: 'Hello reader',
      streamingReady: true,
      aiModel: 'placeholder',
    });
    expect(fetch).toHaveBeenCalledWith('/api/session-windows/3/messages/stream', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        Accept: 'text/event-stream',
        'Content-Type': 'application/json',
      }),
      body: JSON.stringify({ content: 'Prompt?', questionId: 11 }),
    }));
  });

  it('parses CRLF-delimited SSE events', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamResponse([
        'event: message.delta\r\ndata: {"delta":"CRLF"}\r\n\r\n',
        'event: message.done\r\ndata: {"messageId":8,"windowId":3,"role":"assistant","content":"CRLF","streamingReady":true,"aiModel":"placeholder"}\r\n\r\n',
      ])),
    );
    const deltas: string[] = [];

    const result = await marginsRepository.streamMessage(3, 'Prompt?', undefined, (delta) => deltas.push(delta));

    expect(deltas).toEqual(['CRLF']);
    expect(result.content).toBe('CRLF');
  });

  it('throws the backend message when the stream emits message.error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamResponse([
        'event: message.delta\ndata: {"delta":"partial"}\n\n',
        'event: message.error\ndata: {"message":"OpenAI stream failed"}\n\n',
      ])),
    );
    const deltas: string[] = [];

    await expect(marginsRepository.streamMessage(3, 'Prompt?', undefined, (delta) => deltas.push(delta)))
      .rejects
      .toThrow('OpenAI stream failed');
    expect(deltas).toEqual(['partial']);
  });

  it('throws the backend message when the streaming request fails before opening', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(apiResponse(400, {
        success: false,
        message: 'content: must not be blank',
      })),
    );

    await expect(marginsRepository.streamMessage(3, '', undefined, () => undefined))
      .rejects
      .toThrow('content: must not be blank');
  });
});

describe('marginsRepository.debateAll', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('can send selected persona ids with the shared debate content', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(apiResponse(200, {
        success: true,
        data: {
          messages: [],
        },
      })),
    );

    await marginsRepository.debateAll(8, 'Compare every voice', [4, 5]);

    expect(fetch).toHaveBeenCalledWith('/api/session-windows/8/debate/all', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ content: 'Compare every voice', personaIds: [4, 5] }),
    }));
  });
});

describe('marginsRepository.createSession', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('keeps generated reading-session titles within the backend limit', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(apiResponse(200, {
        success: true,
        data: {
          sessionId: 1,
          bookId: 2,
          title: 'created',
          status: 'active',
        },
      })),
    );

    await marginsRepository.createSession({
      bookId: 2,
      title: 'a'.repeat(255),
      author: 'Author',
    });

    const body = JSON.parse(String(vi.mocked(fetch).mock.calls[0][1]?.body)) as { title: string };
    expect(body.title).toHaveLength(255);
    expect(body.title.endsWith(' reflection')).toBe(true);
  });
});

describe('marginsRepository.saveBook', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('sends provider metadata from external candidates', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(apiResponse(200, {
        success: true,
        data: {
          bookId: 9,
          title: 'The Martian',
          author: 'Andy Weir',
          isbn: '9780553418026',
          source: 'google',
        },
      })),
    );

    await marginsRepository.saveBook({
      candidateId: 'google:9780553418026',
      isbn: '9780553418026',
      isbn10: '0553418025',
      isbn13: '9780553418026',
      title: 'The Martian',
      subtitle: 'A Novel',
      author: 'Andy Weir',
      authors: ['Andy Weir'],
      publisher: 'Crown',
      publishedDate: '2014-02-11',
      publishedYear: 2014,
      description: 'Mars survival story',
      thumbnail: 'https://books.example/martian.jpg',
      language: 'en',
      pageCount: 384,
    });

    expect(fetch).toHaveBeenCalledWith('/api/books', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        candidateId: 'google:9780553418026',
        isbn: '9780553418026',
        isbn10: '0553418025',
        isbn13: '9780553418026',
        title: 'The Martian',
        subtitle: 'A Novel',
        author: 'Andy Weir',
        authors: ['Andy Weir'],
        publisher: 'Crown',
        publishedDate: '2014-02-11',
        publishedYear: 2014,
        description: 'Mars survival story',
        thumbnail: 'https://books.example/martian.jpg',
        language: 'en',
        pageCount: 384,
      }),
    }));
  });
});
