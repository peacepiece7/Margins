import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';
import { readFileSync } from 'node:fs';

test.setTimeout(60000);

const backendUrl = process.env.MARGINS_BACKEND_URL || 'http://localhost:8080';
const mobileJumpViewport = { width: 375, height: 900 };

test.beforeEach(async ({ request }) => {
  const response = await request.post(`${backendUrl}/api/test/reset`);
  expect(response.ok()).toBeTruthy();
});

async function loginAndCreateDuneSession(page: Page) {
  await page.goto('/');
  await page.getByTestId('login-submit').click();
  await expect(page.getByTestId('auth-session-bar')).toContainText('Test Reader');
  await page.getByTestId('book-search-input').fill('Dune');
  await page.getByTestId('book-search-submit').click();
  await expect(page.getByTestId('candidate-list')).toContainText('Dune');
  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith('/api/reading-sessions') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('candidate-select').first().click(),
  ]);
  await expect(page.getByTestId('session-summary')).toContainText('Dune');
}

test('stops automatic initial-load retries and lets the reader retry manually', async ({ page }) => {
  let latestRequests = 0;
  let failLatest = true;

  await page.route('**/api/reading-sessions/latest', async (route) => {
    latestRequests += 1;
    if (failLatest) {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'temporary timeline outage' }),
      });
      return;
    }

    await route.continue();
  });

  await page.goto('/');
  await page.getByTestId('login-submit').click();
  await expect(page.getByTestId('error-message')).toContainText('temporary timeline outage');
  await page.waitForTimeout(500);
  expect(latestRequests).toBe(1);

  failLatest = false;
  await page.getByTestId('error-retry').click();
  await expect(page.getByTestId('error-message')).toBeHidden();
  expect(latestRequests).toBe(2);
});

test('falls back to latest session when stored selected session cannot be restored', async ({ page }) => {
  let staleSessionRequests = 0;

  await page.route('**/api/reading-sessions/999999', async (route) => {
    staleSessionRequests += 1;
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'session not found' }),
    });
  });

  await page.goto('/');
  await page.getByTestId('login-submit').click();
  await expect(page.getByTestId('auth-session-bar')).toContainText('Test Reader');
  await page.evaluate(() => window.localStorage.setItem('margins.selectedSessionId', '999999'));
  await page.reload();

  await expect(page.getByTestId('error-message')).toBeHidden();
  await expect(page.getByTestId('session-summary')).toBeVisible();
  expect(staleSessionRequests).toBe(1);
  const storedSessionId = await page.evaluate(() => window.localStorage.getItem('margins.selectedSessionId'));
  expect(storedSessionId).not.toBe('999999');
});

test('keeps reader drafts when save requests fail', async ({ page }) => {
  await loginAndCreateDuneSession(page);

  let failMessageSave = true;
  await page.route('**/api/session-windows/*/messages/stream', async (route) => {
    if (failMessageSave) {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'message save outage' }),
      });
      return;
    }

    await route.continue();
  });

  await page.getByTestId('message-input').fill('Draft answer that should not disappear.');
  await page.getByTestId('message-submit').click();
  await expect(page.getByTestId('error-message')).toContainText('message save outage');
  await expect(page.getByTestId('message-input')).toHaveValue('Draft answer that should not disappear.');

  failMessageSave = false;
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/messages/stream') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('message-submit').click(),
  ]);
  await expect(page.getByTestId('message-input')).toHaveValue('');
  await expect(page.getByTestId('message-list')).toContainText('Draft answer that should not disappear.');

  let failHighlightSave = true;
  await page.route('**/api/reading-sessions/*/highlights', async (route) => {
    if (route.request().method() === 'POST' && failHighlightSave) {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'highlight save outage' }),
      });
      return;
    }

    await route.continue();
  });

  await page.getByTestId('highlight-page-input').fill('12');
  await page.getByTestId('highlight-location-input').fill('Draft margin');
  await page.getByTestId('highlight-quote-input').fill('A fragile quote draft.');
  await page.getByTestId('highlight-note-input').fill('Do not clear this note until save succeeds.');
  await page.getByTestId('highlight-save-submit').click();
  await expect(page.getByTestId('error-message')).toContainText('highlight save outage');
  await expect(page.getByTestId('highlight-page-input')).toHaveValue('12');
  await expect(page.getByTestId('highlight-location-input')).toHaveValue('Draft margin');
  await expect(page.getByTestId('highlight-quote-input')).toHaveValue('A fragile quote draft.');
  await expect(page.getByTestId('highlight-note-input')).toHaveValue('Do not clear this note until save succeeds.');

  failHighlightSave = false;
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/highlights') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('highlight-save-submit').click(),
  ]);
  await expect(page.getByTestId('highlight-quote-input')).toHaveValue('');
  await expect(page.getByTestId('highlight-list')).toContainText('A fragile quote draft.');

  let failQuestionSave = true;
  await page.route('**/api/session-windows/*/questions', async (route) => {
    if (route.request().method() === 'POST' && failQuestionSave) {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'question save outage' }),
      });
      return;
    }

    await route.continue();
  });

  await page.getByTestId('question-create-input').fill('Draft question should stay visible?');
  await page.getByTestId('question-create-submit').click();
  await expect(page.getByTestId('error-message')).toContainText('question save outage');
  await expect(page.getByTestId('question-create-input')).toHaveValue('Draft question should stay visible?');

  failQuestionSave = false;
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/questions') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('question-create-submit').click(),
  ]);
  await expect(page.getByTestId('question-create-input')).toHaveValue('');
  await expect(page.getByTestId('question-panel')).toContainText('Draft question should stay visible?');

  let failTagSave = true;
  await page.route('**/api/reading-sessions/*/tags', async (route) => {
    if (route.request().method() === 'POST' && failTagSave) {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'tag save outage' }),
      });
      return;
    }

    await route.continue();
  });

  await page.getByTestId('session-tag-input').fill('draft-tag');
  await page.getByTestId('session-tag-submit').click();
  await expect(page.getByTestId('error-message')).toContainText('tag save outage');
  await expect(page.getByTestId('session-tag-input')).toHaveValue('draft-tag');

  failTagSave = false;
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/tags') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('session-tag-submit').click(),
  ]);
  await expect(page.getByTestId('session-tag-input')).toHaveValue('');
  await expect(page.getByTestId('session-tag-list')).toContainText('draft-tag');

  let failPersonaSave = true;
  await page.route('**/api/personas', async (route) => {
    if (route.request().method() === 'POST' && failPersonaSave) {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'persona save outage' }),
      });
      return;
    }

    await route.continue();
  });

  await page.getByTestId('persona-create-name-input').fill('Draft Persona');
  await page.getByTestId('persona-create-tone-input').fill('careful');
  await page.getByTestId('persona-create-description-input').fill('Keeps track of missing evidence.');
  await page.getByTestId('persona-create-instructions-input').fill('Ask for evidence before accepting a reading.');
  await page.getByTestId('persona-create-submit').click();
  await expect(page.getByTestId('error-message')).toContainText('persona save outage');
  await expect(page.getByTestId('persona-create-name-input')).toHaveValue('Draft Persona');
  await expect(page.getByTestId('persona-create-tone-input')).toHaveValue('careful');
  await expect(page.getByTestId('persona-create-description-input')).toHaveValue('Keeps track of missing evidence.');
  await expect(page.getByTestId('persona-create-instructions-input')).toHaveValue('Ask for evidence before accepting a reading.');

  failPersonaSave = false;
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/personas') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('persona-create-submit').click(),
  ]);
  await expect(page.getByTestId('persona-create-name-input')).toHaveValue('');
  await expect(page.getByTestId('persona-select')).toContainText('Draft Persona');
});

test('preserves selected reflection question after timeline refresh', async ({ page }) => {
  await loginAndCreateDuneSession(page);
  await page.getByTestId('generate-questions').click();
  await expect(page.getByTestId('question-panel')).toContainText('Which passage would you use as evidence');

  await page.getByTestId('question-select').nth(1).click();
  await expect(page.getByTestId('session-brief')).toContainText('Which passage would you use as evidence');

  await page.getByTestId('message-input').fill('The second prompt should remain selected after save.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/messages/stream') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('message-submit').click(),
  ]);

  await expect(page.getByTestId('message-list')).toContainText('The second prompt should remain selected after save.');
  await expect(page.getByTestId('session-brief')).toContainText('Which passage would you use as evidence');
  await expect(page.getByTestId('question-select').nth(1)).toContainText('Answered');
});

test('creates a session and shows question, message, and persona responses', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.getByTestId('login-form')).toBeVisible();
  await page.getByTestId('login-submit').click();
  await expect(page.getByTestId('auth-session-bar')).toContainText('Test Reader');
  const authHeaders = await page.evaluate(() => {
    const auth = JSON.parse(localStorage.getItem('margins.auth') || '{}') as { accessToken?: string };

    return { Authorization: `Bearer ${auth.accessToken}` };
  });

  await page.getByTestId('book-search-input').fill('Dune');
  await page.getByTestId('book-search-submit').click();
  await expect(page.getByTestId('candidate-list')).toContainText('Dune');

  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith('/api/reading-sessions') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('candidate-select').first().click(),
  ]);
  await expect(page.getByTestId('session-summary')).toContainText('Dune');
  await expect(page.getByTestId('session-summary')).toContainText('Window #');
  await expect(page.getByTestId('window-tabs')).toContainText('Reflection Window');
  await expect(page.getByTestId('window-tabs')).toContainText('Persona Debate');
  await expect(page.getByTestId('review-readiness-score')).toContainText('1/6 ready');
  await expect(page.getByTestId('session-brief-headline')).toContainText('Active session brief');
  await expect(page.getByTestId('session-brief')).toContainText('Progress not set');
  await expect(page.getByTestId('session-brief')).toContainText('0 quotes');
  await expect(page.getByTestId('message-composer')).toContainText('Ask book');
  await expect(page.getByTestId('message-composer')).toContainText('Answer the selected prompt');
  await expect(page.getByTestId('persona-composer')).toContainText('Debate personas');
  await expect(page.getByTestId('persona-composer')).toContainText('Challenge the current interpretation');
  await expect(page.getByTestId('session-jump-nav')).toContainText('Questions');
  await page.getByTestId('session-jump-item').filter({ hasText: 'Questions' }).click();
  await expect(page.getByTestId('question-panel')).toBeFocused();
  await page.getByTestId('session-jump-item').filter({ hasText: 'Progress' }).click();
  await expect(page.getByTestId('reading-goal-input')).toBeFocused();
  await page.getByTestId('session-jump-item').filter({ hasText: 'Quotes' }).click();
  await expect(page.getByTestId('highlight-quote-input')).toBeFocused();
  await page.getByTestId('session-jump-item').filter({ hasText: 'Messages' }).click();
  await expect(page.getByTestId('message-input')).toBeFocused();
  await page.getByTestId('session-jump-item').filter({ hasText: 'Review' }).click();
  await expect(page.getByTestId('review-readiness')).toBeFocused();
  await page.setViewportSize(mobileJumpViewport);
  await expect(page.getByTestId('session-jump-nav')).toBeVisible();
  const mobileJumpBoxes = await page.getByTestId('session-jump-item').evaluateAll((items) =>
    items.map((item) => {
      const box = item.getBoundingClientRect();
      return { top: Math.round(box.top), width: Math.round(box.width) };
    }),
  );
  expect(mobileJumpBoxes).toHaveLength(5);
  expect(mobileJumpBoxes[0].top).toBe(mobileJumpBoxes[1].top);
  expect(mobileJumpBoxes[1].top).toBe(mobileJumpBoxes[2].top);
  expect(mobileJumpBoxes[3].top).toBeGreaterThan(mobileJumpBoxes[0].top);
  expect(mobileJumpBoxes[0].width).toBeGreaterThan(70);
  await expect(page.getByTestId('composer-mode-tabs')).toBeVisible();
  await expect(page.getByTestId('composer-mode-tab').filter({ hasText: 'Ask book' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByTestId('message-submit')).toBeDisabled();
  await expect(page.getByTestId('message-composer')).toBeVisible();
  await expect(page.getByTestId('persona-composer')).toBeHidden();
  await page.getByTestId('composer-mode-tab').filter({ hasText: 'Persona' }).click();
  await expect(page.getByTestId('composer-mode-tab').filter({ hasText: 'Persona' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByTestId('persona-composer')).toBeVisible();
  await expect(page.getByTestId('message-composer')).toBeHidden();
  await expect(page.getByTestId('debate-submit')).toBeDisabled();
  await page.getByTestId('composer-mode-tab').filter({ hasText: 'Ask book' }).click();
  await expect(page.getByTestId('message-composer')).toBeVisible();
  await page.setViewportSize({ width: 1280, height: 900 });
  await expect(page.getByTestId('composer-mode-tabs')).toBeHidden();
  await expect(page.getByTestId('persona-composer')).toBeVisible();
  await expect(page.getByTestId('next-actions')).toContainText('Set reading progress');
  await expect(page.getByTestId('next-actions')).toContainText('Generate reflection questions');
  await expect(page.getByTestId('next-actions')).toContainText('Save a quote');
  await page.getByTestId('next-action-item').filter({ hasText: 'Set reading progress' }).click();
  await expect(page.getByTestId('reading-goal-input')).toBeFocused();
  await page.getByTestId('next-action-item').filter({ hasText: 'Generate reflection questions' }).click();
  await expect(page.getByTestId('generate-questions')).toBeFocused();
  await page.getByTestId('session-title-edit').click();
  await page.getByTestId('session-title-input').fill('Dune opening ritual notes');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/title') && response.request().method() === 'PATCH' && response.status() === 200),
    page.getByTestId('session-title-save').click(),
  ]);
  await expect(page.getByTestId('session-title-panel')).toContainText('Dune opening ritual notes');
  await expect(page.getByTestId('session-library')).toContainText('Dune opening ritual notes');
  await page.getByTestId('session-tag-input').fill('politics');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/tags') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('session-tag-submit').click(),
  ]);
  await expect(page.getByTestId('session-tag-list')).toContainText('politics');
  await expect(page.getByTestId('session-library-item').filter({ hasText: 'Dune opening ritual notes' })).toContainText('politics');
  await page.getByTestId('session-library-search').fill('politics');
  await expect(page.getByTestId('session-library')).toContainText('Dune opening ritual notes');
  await page.getByTestId('session-library-search').fill('');
  await page.getByTestId('session-tag-input').fill('temporary');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/tags') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('session-tag-submit').click(),
  ]);
  await expect(page.getByTestId('session-tag-list')).toContainText('temporary');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/tags') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('session-tag-delete').last().click(),
  ]);
  await expect(page.getByTestId('session-tag-list')).not.toContainText('temporary');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/pin') && response.request().method() === 'PATCH' && response.status() === 200),
    page.getByTestId('session-library-item').filter({ hasText: 'Dune opening ritual notes' }).getByTestId('session-pin-submit').click(),
  ]);
  await expect(page.getByTestId('session-library-item').filter({ hasText: 'Dune opening ritual notes' })).toContainText('Pinned');

  const savedBooksBeforeDuplicate = await request.get(`${backendUrl}/api/books`, { headers: authHeaders });
  expect(savedBooksBeforeDuplicate.ok()).toBeTruthy();
  const savedBooksBeforeDuplicateJson = await savedBooksBeforeDuplicate.json();
  const savedDune = savedBooksBeforeDuplicateJson.data.books.find((book: { bookId: number; title: string }) => book.title === 'Dune');
  expect(savedDune).toBeTruthy();
  const duplicateBookResponse = await request.post(`${backendUrl}/api/books`, {
    headers: authHeaders,
    data: {
      candidateId: 'duplicate-dune',
      title: ' dune ',
      author: ' ai candidate ',
    },
  });
  expect(duplicateBookResponse.ok()).toBeTruthy();
  const duplicateBookJson = await duplicateBookResponse.json();
  expect(duplicateBookJson.data.bookId).toBe(savedDune.bookId);
  const savedBooksAfterDuplicate = await request.get(`${backendUrl}/api/books`, { headers: authHeaders });
  const savedBooksAfterDuplicateJson = await savedBooksAfterDuplicate.json();
  expect(savedBooksAfterDuplicateJson.data.books).toHaveLength(savedBooksBeforeDuplicateJson.data.books.length);

  await page.getByTestId('generate-questions').click();
  await expect(page.getByTestId('question-panel')).toContainText('What detail from Dune');
  await expect(page.getByTestId('question-panel')).toContainText('0/3 answered');
  await expect(page.getByTestId('review-readiness-score')).toContainText('1/6 ready');
  await page.getByTestId('question-create-input').fill('What private question should guide this reading?');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/questions') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('question-create-submit').click(),
  ]);
  await expect(page.getByTestId('question-panel')).toContainText('What private question should guide this reading?');
  await expect(page.getByTestId('question-panel')).toContainText('0/4 answered');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/questions/') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('question-item').filter({ hasText: 'What private question should guide this reading?' }).getByTestId('question-delete-submit').click(),
  ]);
  await expect(page.getByTestId('question-panel')).not.toContainText('What private question should guide this reading?');
  await expect(page.getByTestId('question-panel')).toContainText('0/3 answered');
  const questionDeleteTimelineResponse = await request.get(`${backendUrl}/api/reading-sessions/latest`, { headers: authHeaders });
  expect(questionDeleteTimelineResponse.ok()).toBeTruthy();
  const questionDeleteTimeline = await questionDeleteTimelineResponse.json();
  expect(questionDeleteTimeline.data.questions).not.toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        questionText: 'What private question should guide this reading?',
      }),
    ]),
  );
  await expect(page.getByTestId('question-answer-status').first()).toContainText('Open');
  await expect(page.getByTestId('question-select').first()).toContainText('Select prompt');
  await page.getByTestId('question-filter').selectOption('answered');
  await expect(page.getByTestId('question-empty')).toContainText('No questions match this filter.');
  await page.getByTestId('question-filter').selectOption('unanswered');
  await expect(page.getByTestId('question-panel')).toContainText('What detail from Dune');
  await page.getByTestId('question-filter').selectOption('all');
  await page.getByTestId('question-select').first().click();
  await expect(page.getByTestId('session-brief')).toContainText('What detail from Dune');

  await page.getByTestId('current-page-input').fill('48.5');
  await expect(page.getByTestId('progress-save-submit')).toBeDisabled();
  await page.getByTestId('current-page-input').fill('');
  await page.getByTestId('reading-goal-input').fill('Track how power and prophecy shape the opening.');
  await page.getByTestId('start-page-input').fill('1');
  await page.getByTestId('current-page-input').fill('48');
  await page.getByTestId('target-page-input').fill('120');
  await page.getByTestId('progress-note-input').fill('The opening frames power through ritual and threat.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/progress') && response.status() === 200),
    page.getByTestId('progress-save-submit').click(),
  ]);

  const progressResponse = await request.get(`${backendUrl}/api/reading-sessions/latest`, { headers: authHeaders });
  expect(progressResponse.ok()).toBeTruthy();
  const progressTimeline = await progressResponse.json();
  expect(progressTimeline.data.readingGoal).toBe('Track how power and prophecy shape the opening.');
  expect(progressTimeline.data.startPage).toBe(1);
  expect(progressTimeline.data.currentPage).toBe(48);
  expect(progressTimeline.data.targetPage).toBe(120);
  expect(progressTimeline.data.progressPercent).toBe(40);
  expect(progressTimeline.data.title).toBe('Dune opening ritual notes');
  expect(progressTimeline.data.tags).toEqual(
    expect.arrayContaining([
      expect.objectContaining({ label: 'politics' }),
    ]),
  );
  expect(progressTimeline.data.progressNote).toBe('The opening frames power through ritual and threat.');
  await expect(page.getByTestId('session-progress-percent')).toContainText('40%');
  await expect(page.getByTestId('session-brief')).toContainText('Page 48 of 120 (40%)');
  await expect(page.getByTestId('session-brief')).toContainText('The opening frames power through ritual and threat.');
  await expect(page.getByTestId('review-readiness-score')).toContainText('2/6 ready');
  await expect(page.getByTestId('reader-average-progress')).toContainText('38%');

  await page.getByTestId('highlight-page-input').fill('-1');
  await page.getByTestId('highlight-quote-input').fill('Invalid page draft should not submit.');
  await expect(page.getByTestId('highlight-save-submit')).toBeDisabled();
  await page.getByTestId('highlight-page-input').fill('');
  await page.getByTestId('highlight-quote-input').fill('');
  await page.getByTestId('highlight-page-input').fill('48');
  await page.getByTestId('highlight-location-input').fill('Opening ritual');
  await page.getByTestId('highlight-quote-input').fill('A beginning is the time for taking the most delicate care.');
  await page.getByTestId('highlight-note-input').fill('This line anchors the session question about power.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/highlights') && response.status() === 200),
    page.getByTestId('highlight-save-submit').click(),
  ]);
  await expect(page.getByTestId('highlight-list')).toContainText('A beginning is the time for taking the most delicate care.');
  await expect(page.getByTestId('highlight-list')).toContainText('This line anchors the session question about power.');
  await expect(page.getByTestId('session-brief')).toContainText('1 quote');
  await expect(page.getByTestId('review-readiness-score')).toContainText('3/6 ready');

  await page.getByTestId('highlight-item').filter({ hasText: 'A beginning is the time for taking the most delicate care.' }).getByTestId('highlight-edit-open').click();
  await page.getByTestId('highlight-edit-page-input').fill('50.5');
  await expect(page.getByTestId('highlight-edit-submit')).toBeDisabled();
  await page.getByTestId('highlight-edit-page-input').fill('50');
  await page.getByTestId('highlight-edit-location-input').fill('Edited opening ritual');
  await page.getByTestId('highlight-edit-note-input').fill('Edited evidence note for power and prophecy.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/highlights') && response.request().method() === 'PATCH' && response.status() === 200),
    page.getByTestId('highlight-edit-submit').click(),
  ]);
  await expect(page.getByTestId('highlight-list')).toContainText('Edited opening ritual');
  await expect(page.getByTestId('highlight-list')).toContainText('Edited evidence note for power and prophecy.');
  await expect(page.getByTestId('session-brief')).toContainText('Edited opening ritual');

  await page.getByTestId('highlight-page-input').fill('49');
  await page.getByTestId('highlight-location-input').fill('Scratch note');
  await page.getByTestId('highlight-quote-input').fill('Temporary quote to remove.');
  await page.getByTestId('highlight-note-input').fill('This should not remain in the session.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/highlights') && response.status() === 200),
    page.getByTestId('highlight-save-submit').click(),
  ]);
  await expect(page.getByTestId('highlight-list')).toContainText('Temporary quote to remove.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/highlights') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('highlight-item').filter({ hasText: 'Temporary quote to remove.' }).getByTestId('highlight-delete-submit').click(),
  ]);
  await expect(page.getByTestId('highlight-list')).not.toContainText('Temporary quote to remove.');

  const highlightResponse = await request.get(`${backendUrl}/api/reading-sessions/latest`, { headers: authHeaders });
  expect(highlightResponse.ok()).toBeTruthy();
  const highlightTimeline = await highlightResponse.json();
  expect(highlightTimeline.data.highlights).toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        pageNumber: 50,
        locationLabel: 'Edited opening ritual',
        quoteText: 'A beginning is the time for taking the most delicate care.',
        note: 'Edited evidence note for power and prophecy.',
      }),
    ]),
  );
  expect(highlightTimeline.data.highlights).not.toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        quoteText: 'Temporary quote to remove.',
      }),
    ]),
  );

  await page.getByTestId('message-input').fill('What does the opening suggest?');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/messages/stream') && response.request().method() === 'POST' && response.status() === 200),
    page.getByRole('button', { name: 'Send' }).click(),
  ]);
  await expect(page.getByTestId('message-list')).toContainText('What does the opening suggest?');
  await expect(page.getByTestId('message-list')).toContainText('Placeholder AI response');
  await expect(page.getByTestId('question-panel')).toContainText('1/3 answered');
  await expect(page.getByTestId('session-brief')).toContainText('1 answer - 0 persona replies');
  await expect(page.getByTestId('review-readiness-score')).toContainText('4/6 ready');
  await page.getByTestId('message-item').filter({ hasText: 'What does the opening suggest?' }).getByTestId('message-edit-open').click();
  await page.getByTestId('message-edit-input').fill('What does the opening ritual suggest?');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/messages/') && response.request().method() === 'PATCH' && response.status() === 200),
    page.getByTestId('message-edit-submit').click(),
  ]);
  await expect(page.getByTestId('message-list')).toContainText('What does the opening ritual suggest?');
  await expect(page.getByTestId('message-list')).not.toContainText('What does the opening suggest?');
  await page.getByTestId('message-input').fill('Temporary message to remove.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/messages/stream') && response.request().method() === 'POST' && response.status() === 200),
    page.getByRole('button', { name: 'Send' }).click(),
  ]);
  await expect(page.getByTestId('message-list')).toContainText('Temporary message to remove.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/messages/') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('message-item').filter({ hasText: 'Temporary message to remove.' }).getByTestId('message-delete-submit').click(),
  ]);
  await expect(page.getByTestId('message-list')).not.toContainText('Temporary message to remove.');
  await expect(page.getByTestId('session-stats')).toContainText('1/3 answered');
  await page.getByTestId('session-search-input').fill('delicate care');
  await expect(page.getByTestId('session-search-count')).toContainText('0/2 messages - 1/1 quotes');
  await expect(page.getByTestId('highlight-list')).toContainText('A beginning is the time for taking the most delicate care.');
  await expect(page.getByTestId('message-search-empty')).toContainText('No messages match the current search.');
  await page.getByTestId('session-search-input').fill('ritual suggest');
  await expect(page.getByTestId('session-search-count')).toContainText('1/2 messages - 0/1 quotes');
  await expect(page.getByTestId('message-list')).toContainText('What does the opening ritual suggest?');
  await expect(page.getByTestId('highlight-search-empty')).toContainText('No quotes match the current search.');
  await page.getByTestId('session-search-clear').click();
  await expect(page.getByTestId('session-search-count')).toContainText('2 messages - 1 quotes');
  await page.getByTestId('question-filter').selectOption('answered');
  await expect(page.getByTestId('question-answer-status').first()).toContainText('Answered');
  await expect(page.getByTestId('question-panel')).toContainText('What detail from Dune');
  await page.getByTestId('question-filter').selectOption('all');
  await expect(page.getByTestId('session-stats')).toContainText('1/3 answered');

  const timelineResponse = await request.get(`${backendUrl}/api/reading-sessions/latest`, { headers: authHeaders });
  expect(timelineResponse.ok()).toBeTruthy();
  const timeline = await timelineResponse.json();
  const answeredMessage = timeline.data.messages.find((message: { content: string }) => message.content === 'What does the opening ritual suggest?');
  expect(answeredMessage.questionId).toBeTruthy();
  expect(timeline.data.messages).not.toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        content: 'Temporary message to remove.',
      }),
    ]),
  );

  await page.getByTestId('window-tab-debate').click();
  await page.getByTestId('persona-create-name-input').fill('Skeptical Historian');
  await page.getByTestId('persona-create-tone-input').fill('skeptical');
  await page.getByTestId('persona-create-description-input').fill('Checks claims against historical context.');
  await page.getByTestId('persona-create-instructions-input').fill('Respond as a skeptical historian who asks for historical context and evidence.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/personas') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('persona-create-submit').click(),
  ]);
  await expect(page.getByTestId('persona-select')).toContainText('Skeptical Historian');
  await page.getByTestId('window-tab-question').click();
  await page.setViewportSize(mobileJumpViewport);
  await expect(page.getByTestId('message-composer')).toBeVisible();
  await expect(page.getByTestId('persona-composer')).toBeHidden();
  await page.getByTestId('next-action-item').filter({ hasText: 'Ask a persona' }).click();
  await expect(page.getByTestId('window-title-panel')).toContainText('Persona Debate');
  await expect(page.getByTestId('composer-mode-tab').filter({ hasText: 'Persona' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByTestId('persona-composer')).toBeVisible();
  await expect(page.getByTestId('message-composer')).toBeHidden();
  await expect(page.getByTestId('debate-input')).toBeFocused();
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.getByTestId('persona-select').selectOption({ label: 'Skeptical Historian' });
  await page.getByTestId('debate-input').fill('Challenge this reading');
  await page.getByTestId('debate-submit').click();
  await expect(page.getByTestId('message-list')).toContainText('Challenge this reading');
  await expect(page.getByTestId('message-list')).toContainText('Skeptical Historian');
  await expect(page.getByTestId('message-list')).toContainText('Placeholder persona response');
  await expect(page.getByTestId('session-stats')).toContainText('1 persona replies');
  await expect(page.getByTestId('session-brief')).toContainText('1 answer - 1 persona reply');
  await expect(page.getByTestId('review-readiness-score')).toContainText('5/6 ready');
  await expect(page.getByTestId('session-stats')).toContainText('4');
  await page.getByTestId('debate-input').fill('Compare the political and emotional stakes');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/debate/all') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('debate-all-submit').click(),
  ]);
  await expect(page.getByTestId('message-list')).toContainText('Compare the political and emotional stakes');
  await expect(page.getByTestId('message-list')).toContainText('Skeptical Historian');
  await expect(page.getByTestId('session-stats')).toContainText('4 persona replies');
  await expect(page.getByTestId('session-brief')).toContainText('1 answer - 4 persona replies');
  await page.getByTestId('window-title-input').fill('Ecology notes');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('window-create-submit').click(),
  ]);
  await expect(page.getByTestId('window-tabs')).toContainText('Ecology notes');
  await expect(page.getByTestId('session-stats')).toContainText('3');
  await page.getByRole('button', { name: 'Ecology notes' }).click();
  await page.getByTestId('window-title-edit').click();
  await page.getByTestId('window-title-edit-input').fill('Ecology thread');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/title') && response.request().method() === 'PATCH' && response.status() === 200),
    page.getByTestId('window-title-save').click(),
  ]);
  await expect(page.getByTestId('window-title-panel')).toContainText('Ecology thread');
  await expect(page.getByTestId('window-tabs')).toContainText('Ecology thread');
  await page.getByTestId('generate-questions').click();
  await expect(page.getByTestId('question-panel')).toContainText('Ecology thread - 0/3 answered');
  await expect(page.getByTestId('question-panel')).toContainText('What detail from Dune - Ecology thread');
  await page.getByTestId('question-select').first().click();
  await page.getByTestId('message-input').fill('Track the ecology thread separately.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/messages/stream') && response.request().method() === 'POST' && response.status() === 200),
    page.getByRole('button', { name: 'Send' }).click(),
  ]);
  await expect(page.getByTestId('message-list')).toContainText('Track the ecology thread separately.');
  await expect(page.getByTestId('message-list')).toContainText('Placeholder AI response');
  await expect(page.getByTestId('question-panel')).toContainText('Ecology thread - 1/3 answered');
  await expect(page.getByTestId('session-stats')).toContainText('2/6 answered');
  await expect(page.getByTestId('session-stats')).toContainText('6');
  await expect(page.getByTestId('next-actions')).toContainText('Answer an open question');
  await page.getByTestId('window-title-input').fill('Scratch window');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('window-create-submit').click(),
  ]);
  await expect(page.getByTestId('window-tabs')).toContainText('Scratch window');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('window-archive-submit').click(),
  ]);
  await expect(page.getByTestId('window-tabs')).not.toContainText('Scratch window');
  await expect(page.getByTestId('session-stats')).toContainText('3');
  const archivedWindowTimelineResponse = await request.get(`${backendUrl}/api/reading-sessions/latest`, { headers: authHeaders });
  expect(archivedWindowTimelineResponse.ok()).toBeTruthy();
  const archivedWindowTimeline = await archivedWindowTimelineResponse.json();
  expect(archivedWindowTimeline.data.windows).not.toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        title: 'Scratch window',
      }),
    ]),
  );
  await expect(page.getByTestId('saved-book-library')).toContainText('Dune');
  await page.getByTestId('saved-book-search').fill('left hand');
  await expect(page.getByTestId('saved-book-library')).toContainText('The Left Hand of Darkness');
  await expect(page.getByTestId('saved-book-library')).not.toContainText('Dune');
  await page.getByTestId('saved-book-search').fill('missing saved book');
  await expect(page.getByTestId('saved-book-empty')).toContainText('No saved books match the current filter.');
  await page.getByTestId('saved-book-search').fill('');
  await page.getByTestId('saved-book-item').filter({ hasText: 'Dune' }).getByTestId('saved-book-start-session').click();
  await expect(page.getByTestId('session-summary')).toContainText('Dune');
  await expect(page.getByTestId('session-library')).toContainText('0 messages - active');

  await page.getByTestId('book-search-input').fill('Foundation');
  await page.getByTestId('book-search-submit').click();
  await expect(page.getByTestId('candidate-list')).toContainText('Foundation');
  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith('/api/reading-sessions') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('candidate-select').first().click(),
  ]);
  await expect(page.getByTestId('session-summary')).toContainText('Foundation');
  await expect(page.getByTestId('session-library')).toContainText('Foundation');
  await expect(page.getByTestId('session-library')).toContainText('Dune');
  const pinnedSessionListResponse = await request.get(`${backendUrl}/api/reading-sessions`, { headers: authHeaders });
  expect(pinnedSessionListResponse.ok()).toBeTruthy();
  const pinnedSessionList = await pinnedSessionListResponse.json();
  expect(pinnedSessionList.data.sessions[0]).toEqual(expect.objectContaining({
    bookTitle: 'Dune',
    pinned: true,
  }));

  await page.getByTestId('session-library-item').filter({ hasText: 'Dune' }).filter({ hasText: '10 messages' }).getByTestId('session-select').click();
  await expect(page.getByTestId('session-summary')).toContainText('Dune');
  await page.getByRole('button', { name: 'Reflection Window' }).click();
  await expect(page.getByTestId('message-list')).toContainText('What does the opening ritual suggest?');
  await page.getByRole('button', { name: 'Ecology thread' }).click();
  await expect(page.getByTestId('message-list')).toContainText('Track the ecology thread separately.');
  await expect(page.getByTestId('question-panel')).toContainText('Ecology thread - 1/3 answered');
  await expect(page.getByTestId('session-stats')).toContainText('2/6 answered');
  const transcriptDownload = await Promise.all([
    page.waitForEvent('download'),
    page.getByTestId('session-transcript-export-submit').click(),
  ]).then(([sessionDownload]) => sessionDownload);
  expect(transcriptDownload.suggestedFilename()).toBe('dune-opening-ritual-notes-transcript.md');
  const transcriptPath = await transcriptDownload.path();
  expect(transcriptPath).toBeTruthy();
  const transcriptMarkdown = readFileSync(transcriptPath as string, 'utf-8');
  expect(transcriptMarkdown).toContain('# Dune opening ritual notes transcript');
  expect(transcriptMarkdown).toContain('Book: Dune');
  expect(transcriptMarkdown).toContain('Progress: page 48/120 (40%)');
  expect(transcriptMarkdown).toContain('## Highlights');
  expect(transcriptMarkdown).toContain('A beginning is the time for taking the most delicate care.');
  expect(transcriptMarkdown).toContain('## Questions');
  expect(transcriptMarkdown).toContain('What detail from Dune');
  expect(transcriptMarkdown).toContain('### Reflection Window');
  expect(transcriptMarkdown).toContain('What does the opening ritual suggest?');
  expect(transcriptMarkdown).toContain('Placeholder AI response');
  expect(transcriptMarkdown).toContain('### Ecology thread');
  expect(transcriptMarkdown).toContain('What detail from Dune - Ecology thread');
  expect(transcriptMarkdown).toContain('Track the ecology thread separately.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('session-library-item').filter({ hasText: 'Foundation' }).getByTestId('session-archive-submit').click(),
  ]);
  await expect(page.getByTestId('session-library')).not.toContainText('Foundation');
  const archivedSessionListResponse = await request.get(`${backendUrl}/api/reading-sessions`, { headers: authHeaders });
  expect(archivedSessionListResponse.ok()).toBeTruthy();
  const archivedSessionList = await archivedSessionListResponse.json();
  expect(archivedSessionList.data.sessions).not.toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        bookTitle: 'Foundation',
      }),
    ]),
  );

  const closeoutSummary = 'Dune closeout: institutions and prophecy feel unresolved.';
  await page.getByTestId('session-summary-input').fill(closeoutSummary);
  await page.getByTestId('session-complete-submit').click();
  await expect(page.getByTestId('session-closeout')).toContainText('completed');
  await expect(page.getByTestId('review-readiness-score')).toContainText('6/6 ready');
  await expect(page.getByTestId('session-library')).toContainText('completed');
  await expect(page.getByTestId('library-dashboard')).toContainText('3 sessions');
  await expect(page.getByTestId('library-dashboard')).toContainText('Completed1');
  await expect(page.getByTestId('library-dashboard')).toContainText('Active2');
  await expect(page.getByTestId('library-dashboard')).toContainText('Quotes2');
  await expect(page.getByTestId('library-dashboard')).toContainText('Answers3');
  await expect(page.getByTestId('reader-book-count')).toContainText('2');
  await expect(page.getByTestId('reader-average-progress')).toContainText('38%');
  const readerStatsResponse = await request.get(`${backendUrl}/api/reading-sessions/stats`, { headers: authHeaders });
  expect(readerStatsResponse.ok()).toBeTruthy();
  const readerStats = await readerStatsResponse.json();
  expect(readerStats.data).toEqual(
    expect.objectContaining({
      sessionCount: 3,
      activeSessionCount: 2,
      completedSessionCount: 1,
      distinctBookCount: 2,
      answeredQuestionCount: 3,
      highlightCount: 2,
      messageCount: 14,
      averageProgressPercent: 38,
    }),
  );
  await expect(page.getByTestId('session-library')).toContainText('40%');
  await page.getByTestId('session-library-status').selectOption('completed');
  await expect(page.getByTestId('session-library')).toContainText('Dune');
  await expect(page.getByTestId('session-library')).not.toContainText('Foundation');
  await page.getByTestId('session-library-status').selectOption('all');
  await page.getByTestId('session-library-search').fill('ritual notes');
  await expect(page.getByTestId('session-library')).toContainText('Dune opening ritual notes');
  await page.getByTestId('session-library-search').fill('');
  await page.getByTestId('session-library-search').fill('Foundation');
  await expect(page.getByTestId('session-library-empty')).toContainText('No sessions match the current filters.');
  await expect(page.getByTestId('session-library')).not.toContainText('Dune');
  await page.getByTestId('session-library-search').fill('');
  await expect(page.getByTestId('session-review')).toContainText('Session review');
  await expect(page.getByTestId('session-review')).toContainText(closeoutSummary);
  await expect(page.getByTestId('review-overview')).toContainText(closeoutSummary);
  await expect(page.getByTestId('review-tags')).toContainText('politics');
  await expect(page.getByTestId('review-insight-panel')).toContainText('Review insights');
  await page.getByTestId('review-insight-type').selectOption('theme');
  await page.getByTestId('review-insight-title').fill('Ritual before politics');
  await page.getByTestId('review-insight-content').fill('The opening turns power into a ceremony before explaining the empire.');
  await page.getByTestId('review-insight-evidence').fill('Gom Jabbar scene');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/insights') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('review-insight-submit').click(),
  ]);
  await expect(page.getByTestId('review-insights')).toContainText('Ritual before politics');
  await expect(page.getByTestId('review-insights')).toContainText('The opening turns power into a ceremony before explaining the empire.');
  await page.getByTestId('memory-search-input').fill('ceremony');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/search') && response.status() === 200),
    page.getByTestId('memory-search-submit').click(),
  ]);
  await expect(page.getByTestId('memory-search-results')).toContainText('insight');
  await expect(page.getByTestId('memory-search-results')).toContainText('ceremony');
  await page.getByTestId('memory-search-result').first().click();
  await expect(page.getByTestId('session-title-panel')).toContainText('Dune opening ritual notes');
  await page.getByTestId('review-insight-title').fill('Temporary insight');
  await page.getByTestId('review-insight-content').fill('Remove this trial note.');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/insights') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('review-insight-submit').click(),
  ]);
  await expect(page.getByTestId('review-insights')).toContainText('Temporary insight');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/insights') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('review-insight-delete').last().click(),
  ]);
  await expect(page.getByTestId('review-insights')).not.toContainText('Temporary insight');
  await expect(page.getByTestId('review-highlights')).toContainText('A beginning is the time for taking the most delicate care.');
  await expect(page.getByTestId('review-answers')).toContainText('What does the opening ritual suggest?');
  await expect(page.getByTestId('review-personas')).toContainText('Skeptical Historian');
  await expect(page.getByTestId('review-evidence-grid')).toContainText('Saved quotes');
  await expect(page.getByTestId('review-evidence-grid')).toContainText('Answered prompts');
  await expect(page.getByTestId('review-evidence-grid')).toContainText('Persona responses');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/metrics/snapshot') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('metric-snapshot-submit').click(),
  ]);
  await expect(page.getByTestId('metric-snapshot-result')).toContainText('Metric #');
  await expect(page.getByTestId('metric-snapshot-result')).toContainText('session_snapshot saved - 40%');
  await expect(page.getByTestId('metric-snapshot-result')).toContainText('10 messages - 2/6 answers - 1 quotes');

  const download = await Promise.all([
    page.waitForEvent('download'),
    page.getByTestId('review-export-submit').click(),
  ]).then(([reviewDownload]) => reviewDownload);
  expect(download.suggestedFilename()).toBe('dune-review.md');
  const reviewPath = await download.path();
  expect(reviewPath).toBeTruthy();
  const reviewMarkdown = readFileSync(reviewPath as string, 'utf-8');
  expect(reviewMarkdown).toContain('# Dune review');
  expect(reviewMarkdown).toContain('Progress: page 48/120 (40%)');
  expect(reviewMarkdown).toContain('Tags: politics');
  expect(reviewMarkdown).toContain('Ritual before politics: The opening turns power into a ceremony before explaining the empire.');
  expect(reviewMarkdown).toContain('Evidence: Gom Jabbar scene');
  expect(reviewMarkdown).toContain(closeoutSummary);
  expect(reviewMarkdown).toContain('A beginning is the time for taking the most delicate care.');
  expect(reviewMarkdown).toContain('Skeptical Historian');

  const completedTimelineResponse = await request.get(`${backendUrl}/api/reading-sessions/latest`, { headers: authHeaders });
  expect(completedTimelineResponse.ok()).toBeTruthy();
  const completedTimeline = await completedTimelineResponse.json();
  expect(completedTimeline.data.status).toBe('completed');
  expect(completedTimeline.data.summary).toBe(closeoutSummary);
  expect(completedTimeline.data.tags).toEqual(
    expect.arrayContaining([
      expect.objectContaining({ label: 'politics' }),
    ]),
  );
  expect(completedTimeline.data.insights).toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        title: 'Ritual before politics',
        content: 'The opening turns power into a ceremony before explaining the empire.',
      }),
    ]),
  );

  await page.reload();
  await expect(page.getByTestId('auth-session-bar')).toContainText('Test Reader');
  await expect(page.getByTestId('session-summary')).toContainText('Dune');
  await expect(page.getByTestId('reading-goal-input')).toHaveValue('Track how power and prophecy shape the opening.');
  await expect(page.getByTestId('current-page-input')).toHaveValue('48');
  await expect(page.getByTestId('progress-note-input')).toHaveValue('The opening frames power through ritual and threat.');
  await expect(page.getByTestId('highlight-list')).toContainText('A beginning is the time for taking the most delicate care.');
  await expect(page.getByTestId('highlight-list')).toContainText('Edited evidence note for power and prophecy.');
  await expect(page.getByTestId('session-summary-input')).toHaveValue(closeoutSummary);
  await expect(page.getByTestId('session-summary-input')).toHaveAttribute('readonly', '');
  await expect(page.getByTestId('session-closeout')).toContainText('completed');
  await expect(page.getByTestId('session-complete-submit')).toContainText('Completed');
  await expect(page.getByTestId('session-review')).toContainText(closeoutSummary);
  await expect(page.getByTestId('review-tags')).toContainText('politics');
  await expect(page.getByTestId('review-insights')).toContainText('Ritual before politics');
  await expect(page.getByTestId('review-insights')).toContainText('Gom Jabbar scene');
  await expect(page.getByTestId('review-progress')).toContainText('Page 48/120');
  await expect(page.getByTestId('review-progress')).toContainText('40%');
  await expect(page.getByTestId('message-list')).toContainText('What does the opening ritual suggest?');
  await expect(page.getByTestId('message-list')).toContainText('Placeholder AI response');
  await page.getByTestId('window-tab-debate').click();
  await expect(page.getByTestId('message-list')).toContainText('Challenge this reading');
  await expect(page.getByTestId('message-list')).toContainText('Placeholder persona response');

  await page.getByTestId('logout-submit').click();
  await expect(page.getByTestId('login-form')).toBeVisible();
  const clearedStorage = await page.evaluate(() => ({
    auth: window.localStorage.getItem('margins.auth'),
    selectedSessionId: window.localStorage.getItem('margins.selectedSessionId'),
  }));
  expect(clearedStorage).toEqual({ auth: null, selectedSessionId: null });
});
