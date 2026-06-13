import { chromium } from '@playwright/test';
import { createReadStream } from 'node:fs';
import { access, readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { extname, join, resolve } from 'node:path';

const distDir = resolve(process.env.MARGINS_DIST_DIR ?? join(process.cwd(), 'dist'));
const distIndex = join(distDir, 'index.html');

await access(distIndex);

const indexHtml = await readFile(distIndex, 'utf8');
const assetRefs = [...indexHtml.matchAll(/\b(?:src|href)="\/?(assets\/[^"]+)"/g)].map((match) => match[1]);
if (assetRefs.length === 0) {
  throw new Error('Production build index.html does not reference bundled assets.');
}

for (const assetRef of assetRefs) {
  await access(join(distDir, assetRef));
}

const contentTypes = new Map([
  ['.css', 'text/css'],
  ['.html', 'text/html'],
  ['.js', 'text/javascript'],
]);

const server = createServer((request, response) => {
  const requestPath = request.url === '/' ? '/index.html' : new URL(request.url ?? '/', 'http://localhost').pathname;
  const relativePath = requestPath.replace(/^\/+/, '');
  const filePath = resolve(distDir, relativePath);

  if (!filePath.startsWith(distDir)) {
    response.writeHead(403);
    response.end('Forbidden');
    return;
  }

  response.setHeader('Content-Type', contentTypes.get(extname(filePath)) ?? 'application/octet-stream');
  createReadStream(filePath)
    .on('error', () => {
      response.writeHead(404);
      response.end('Not found');
    })
    .pipe(response);
});

await new Promise((resolveListen) => {
  server.listen(0, '127.0.0.1', resolveListen);
});

const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  const address = server.address();
  const pageUrl = `http://127.0.0.1:${address.port}/`;
  await page.goto(pageUrl);
  await page.waitForLoadState('networkidle');

  const rootChildCount = await page.locator('#root > *').count();
  if (rootChildCount === 0) {
    throw new Error('Production build did not render the React application root.');
  }

  const selectorCount = await page.locator('[data-testid]').count();
  if (selectorCount > 0) {
    throw new Error(`Production build rendered ${selectorCount} data-testid attributes.`);
  }

  console.log('Production build rendered app root and no data-testid attributes.');
} finally {
  await browser.close();
  await new Promise((resolveClose) => server.close(resolveClose));
}
