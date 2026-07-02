import { chromium } from 'playwright';

const args = process.argv.slice(2);

function readArg(names, fallback) {
  for (let index = 0; index < args.length; index += 1) {
    if (names.includes(args[index])) {
      return args[index + 1] ?? fallback;
    }
    for (const name of names) {
      if (args[index].startsWith(`${name}=`)) {
        return args[index].slice(name.length + 1);
      }
    }
  }
  return fallback;
}

function hasFlag(...names) {
  return args.some((arg) => names.includes(arg));
}

async function main() {
  if (!hasFlag('--allow-mutation', '-AllowMutation')) {
    throw new Error('Production flow smoke mutates data. Re-run with --allow-mutation to create and delete one smoke book.');
  }

  const url = readArg(['--url', '-Url'], process.env.MARGINS_PRODUCTION_URL || process.env.MARGINS_FRONT_URL || '');
  const username = readArg(['--username', '-Username'], process.env.MARGINS_E2E_USERNAME || process.env.MARGINS_SINGLE_USER_USERNAME || '');
  const password = readArg(['--password', '-Password'], process.env.MARGINS_E2E_PASSWORD || process.env.MARGINS_SINGLE_USER_PASSWORD || '');
  const displayName = readArg(['--display-name', '-DisplayName'], process.env.MARGINS_E2E_DISPLAY_NAME || process.env.MARGINS_SINGLE_USER_DISPLAY_NAME || username);
  if (!url) throw new Error('Missing --url or MARGINS_PRODUCTION_URL.');
  if (!username || !password) throw new Error('Missing production smoke username/password.');

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const consoleErrors = [];
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });
  page.on('dialog', async (dialog) => {
    if (dialog.type() !== 'confirm') {
      throw new Error(`Unexpected dialog type: ${dialog.type()}`);
    }
    await dialog.accept();
  });

  const smokeTitle = `Margins Smoke ${new Date().toISOString().replace(/[:.]/g, '-')}`;

  try {
    await page.goto(url, { waitUntil: 'networkidle' });
    await page.getByPlaceholder(/Username|사용자 이름/).fill(username);
    await page.getByPlaceholder(/Password|비밀번호/).fill(password);
    await page.getByRole('button', { name: 'Login' }).click();
    await page.getByText(displayName).waitFor({ state: 'visible', timeout: 15000 });
    if (!(await page.locator('body').innerText()).includes(displayName)) {
      throw new Error(`Login succeeded but auth bar did not include expected display name: ${displayName}`);
    }

    const manualForm = page.locator('form').filter({ hasText: 'Add a book manually' });
    await manualForm.getByPlaceholder('Book title, author, or reading theme').fill(smokeTitle);
    await manualForm.getByPlaceholder('Author', { exact: true }).fill('Production Smoke');
    await Promise.all([
      page.waitForResponse((response) => response.url().endsWith('/api/books') && response.request().method() === 'POST' && response.status() === 200),
      manualForm.getByRole('button', { name: 'Add manually' }).click(),
    ]);
    await page.getByText(smokeTitle).waitFor({ state: 'visible', timeout: 15000 });
    if (!(await page.locator('body').innerText()).includes(smokeTitle)) {
      throw new Error('Created smoke book did not appear in the saved-book list.');
    }

    const smokeRow = page.locator('article').filter({ hasText: smokeTitle });
    await Promise.all([
      page.waitForResponse((response) => response.url().includes('/api/books/') && response.request().method() === 'DELETE' && response.status() === 200),
      smokeRow.getByRole('button', { name: 'Delete' }).click(),
    ]);
    if ((await page.locator('body').innerText()).includes(smokeTitle)) {
      throw new Error('Smoke book still appears after deletion.');
    }
    if (consoleErrors.length > 0) {
      throw new Error(`Production flow console errors: ${consoleErrors.join('\n')}`);
    }

    console.log('# Production Flow Smoke');
    console.log('');
    console.log(`Checked: ${url}`);
    console.log(`Created and deleted: ${smokeTitle}`);
    console.log('PASS: production login, manual book creation, saved-book display, and delete flow work.');
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
