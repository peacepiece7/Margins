import { expect, test, type Page } from '@playwright/test';

test.setTimeout(60000);

const backendUrl = process.env.MARGINS_BACKEND_URL || 'http://localhost:8080';
const e2eUsername = process.env.MARGINS_E2E_USERNAME || process.env.MARGINS_SINGLE_USER_USERNAME || 'peacepiece';
const e2eDisplayName = process.env.MARGINS_E2E_DISPLAY_NAME || process.env.MARGINS_SINGLE_USER_DISPLAY_NAME || e2eUsername;
const e2ePassword = process.env.MARGINS_E2E_PASSWORD || process.env.MARGINS_SINGLE_USER_PASSWORD || 'reader';

async function login(page: Page) {
  await expect(page.getByTestId('login-form')).toBeVisible();
  await expect(page.getByTestId('login-username-input')).toHaveValue('');
  await expect(page.getByTestId('login-password-input')).toHaveValue('');
  await page.getByTestId('login-username-input').fill(e2eUsername);
  await page.getByTestId('login-password-input').fill(e2ePassword);
  await page.getByTestId('login-submit').click();
}

test.beforeEach(async ({ request }) => {
  const response = await request.post(`${backendUrl}/api/test/reset`);
  expect(response.ok()).toBeTruthy();
});

test('follows the owner replan page flow from book registration to reflection and debate', async ({ page }) => {
  let deleteDialogCount = 0;
  page.on('dialog', async (dialog) => {
    expect(dialog.type()).toBe('confirm');
    expect(dialog.message()).toBe('삭제하시겠습니까?');
    deleteDialogCount += 1;
    await dialog.accept();
  });

  await page.goto('/');
  await login(page);
  await expect(page.getByTestId('auth-session-bar')).toContainText(e2eDisplayName);
  await expect(page.getByTestId('reading-portal')).toBeVisible();

  await page.getByTestId('book-search-input').fill('Dune');
  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith('/api/books/search-candidates') && response.request().method() === 'POST'),
    page.getByTestId('book-search-submit').click(),
  ]);
  await expect(page.getByTestId('book-candidate-list')).toContainText(/dune|듄/i);
  await expect(page.getByTestId('book-candidate-id').first()).toContainText('고유번호');
  const savedCandidateTitle = await page.getByTestId('book-candidate-title').first().innerText();

  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith('/api/books') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('book-candidate-save').first().click(),
  ]);
  await expect(page.getByTestId('book-list-page')).toContainText(savedCandidateTitle);

  await page.getByTestId('saved-book-detail-link').filter({ hasText: savedCandidateTitle }).click();
  await expect(page.getByTestId('book-detail-page')).toContainText('등록 책 상세');
  await page.getByTestId('book-edit-title-input').fill('Dune: Edited');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/books/') && response.request().method() === 'PATCH' && response.status() === 200),
    page.getByTestId('book-edit-submit').click(),
  ]);
  await expect(page.getByTestId('portal-sidebar')).toContainText('Dune: Edited');

  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().endsWith('/questions/generate') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('book-generate-questions').click(),
  ]);
  await expect(page.getByTestId('book-question-delete').first()).toBeVisible();
  const deletedQuestionText = await page.getByTestId('book-question-row').last().getByTestId('book-question-link').innerText();
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/questions/') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('book-question-delete').last().click(),
  ]);
  expect(deleteDialogCount).toBe(1);
  await expect(page.getByTestId('book-question-panel')).not.toContainText(deletedQuestionText);
  if ((await page.getByTestId('book-question-row').count()) === 0) {
    await Promise.all([
      page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().endsWith('/questions/generate') && response.request().method() === 'POST' && response.status() === 200),
      page.getByTestId('book-generate-questions').click(),
    ]);
    await expect(page.getByTestId('book-question-row').first()).toBeVisible();
  }
  const selectedQuestionText = await page.getByTestId('book-question-link').first().innerText();
  await page.getByTestId('book-question-link').first().click();
  await expect(page.getByTestId('review-page')).toBeVisible();
  await expect(page.getByTestId('question-answer-form')).toContainText(selectedQuestionText);
  await page.getByTestId('question-answer-back').click();
  await expect(page.getByTestId('book-detail-page')).toBeVisible();
  await page.getByTestId('book-question-link').first().click();
  await expect(page.getByTestId('question-answer-form')).toContainText(selectedQuestionText);
  const selectedAnswerText = 'This selected question answer should stay visible.';
  await page.getByTestId('question-answer-input').fill(selectedAnswerText);
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().includes('/messages/stream') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('question-answer-submit').click(),
  ]);
  await expect(page.getByTestId('question-answer-history')).toContainText('내 답변');
  await expect(page.getByTestId('question-answer-history')).toContainText(selectedAnswerText);
  await page.getByTestId('question-answer-back').click();
  await page.getByTestId('book-question-link').first().click();
  await expect(page.getByTestId('question-answer-history')).toContainText(selectedAnswerText);

  await page.getByTestId('portal-nav-book-detail').click();

  await page.getByTestId('book-start-review').click();
  await expect(page.getByTestId('review-page')).toBeVisible();
  const reflectionEditorBox = await page.getByTestId('reflection-content-input').boundingBox();
  expect(reflectionEditorBox?.height).toBeGreaterThanOrEqual(300);
  await page.getByTestId('reflection-content-input').locator('.ck-content[contenteditable="true"]').fill('The opening ritual makes power feel intimate and dangerous.');
  await page.getByTestId('reflection-evidence-input').fill('Gom Jabbar scene');
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/reading-sessions/') && response.url().includes('/insights') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('reflection-submit').click(),
  ]);
  await expect(page.getByTestId('reflection-list')).toContainText('The opening ritual makes power feel intimate and dangerous.');

  await page.getByTestId('portal-nav-book-detail').click();
  await page.getByTestId('debate-topic-input').fill('How does ritual shape political authority?');
  await page.getByTestId('debate-enter-submit').click();
  await expect(page.getByTestId('debate-page')).toBeVisible();
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().endsWith('/debate') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('debate-session-submit').click(),
  ]);
  await expect(page.getByTestId('debate-message-list')).toContainText('How does ritual shape political authority?');
  await expect(page.getByTestId('debate-message-list')).toContainText('전사 아르단', { timeout: 20000 });
  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/session-windows/') && response.url().endsWith('/debate/all') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('debate-all-submit').click(),
  ]);
  await expect(page.getByTestId('debate-message-list')).toContainText('마법사 리라', { timeout: 20000 });
  await expect(page.getByTestId('debate-message-list')).toContainText('성직자 세렌', { timeout: 20000 });

  await page.reload();
  await expect(page.getByTestId('auth-session-bar')).toContainText(e2eDisplayName);
  await expect(page.getByTestId('portal-sidebar')).toContainText('Dune: Edited');
  await page.getByTestId('portal-nav-debate').click();
  await expect(page.getByTestId('debate-message-list')).toContainText('How does ritual shape political authority?');
  await expect(page.getByTestId('debate-message-list')).toContainText('전사 아르단');
  await expect(page.getByTestId('debate-message-list')).toContainText('마법사 리라');
  await expect(page.getByTestId('debate-message-list')).toContainText('성직자 세렌');
});

test('supports manual registration and saved-book deletion from the page shell', async ({ page }) => {
  let deleteDialogCount = 0;
  page.on('dialog', async (dialog) => {
    expect(dialog.type()).toBe('confirm');
    expect(dialog.message()).toBe('삭제하시겠습니까?');
    deleteDialogCount += 1;
    await dialog.accept();
  });

  await page.goto('/');
  await login(page);
  await expect(page.getByTestId('auth-session-bar')).toContainText(e2eDisplayName);

  await page.getByTestId('manual-book-title-input').fill('Manual Margins Book');
  await page.getByTestId('manual-book-author-input').fill('Reader Author');
  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith('/api/books') && response.request().method() === 'POST' && response.status() === 200),
    page.getByTestId('manual-book-submit').click(),
  ]);
  await expect(page.getByTestId('book-list-page')).toContainText('Manual Margins Book');

  await Promise.all([
    page.waitForResponse((response) => response.url().includes('/api/books/') && response.request().method() === 'DELETE' && response.status() === 200),
    page.getByTestId('saved-book-delete').filter({ hasText: '삭제' }).first().click(),
  ]);
  expect(deleteDialogCount).toBe(1);
  await expect(page.getByTestId('book-list-page')).not.toContainText('Manual Margins Book');
});
