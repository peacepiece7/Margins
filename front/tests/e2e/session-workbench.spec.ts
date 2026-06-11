import { expect, test } from '@playwright/test';

test.beforeEach(async ({ request }) => {
  const response = await request.post('http://localhost:8080/api/test/reset');
  expect(response.ok()).toBeTruthy();
});

test('creates a session and shows message and persona responses', async ({ page }) => {
  await page.goto('/');

  await page.getByTestId('book-search-input').fill('Dune');
  await page.getByTestId('book-search-submit').click();
  await expect(page.getByTestId('candidate-list')).toContainText('Dune');

  await page.getByTestId('candidate-select').first().click();
  await expect(page.getByTestId('session-summary')).toContainText('Dune');
  await expect(page.getByTestId('session-summary')).toContainText('Window #');

  await page.getByTestId('message-input').fill('What does the opening suggest?');
  await page.getByRole('button', { name: 'Send' }).click();
  await expect(page.getByTestId('message-list')).toContainText('What does the opening suggest?');
  await expect(page.getByTestId('message-list')).toContainText('Placeholder AI response');

  await page.getByTestId('debate-input').fill('Challenge this reading');
  await page.getByRole('button', { name: 'Debate' }).click();
  await expect(page.getByTestId('message-list')).toContainText('Challenge this reading');
  await expect(page.getByTestId('message-list')).toContainText('Persona 1');
  await expect(page.getByTestId('message-list')).toContainText('Placeholder persona response');
});
