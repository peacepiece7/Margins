import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { basename, join } from 'node:path';
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

function sha256Text(text) {
  return createHash('sha256').update(text).digest('hex');
}

function assetPathsFromHtml(html) {
  return {
    css: [...html.matchAll(/href="(\/assets\/[^"]+\.css)"/g)].map((match) => match[1]),
    js: [...html.matchAll(/src="(\/assets\/[^"]+\.js)"/g)].map((match) => match[1]),
  };
}

async function fetchText(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    throw new Error(`${url} returned HTTP ${response.status}`);
  }
  return await response.text();
}

async function assertAssetMatches(baseUrl, assetPath) {
  const localPath = join('dist', assetPath.slice(1));
  const [localText, remoteText] = await Promise.all([
    readFile(localPath, 'utf8'),
    fetchText(new URL(assetPath, baseUrl).toString()),
  ]);
  const localHash = sha256Text(localText);
  const remoteHash = sha256Text(remoteText);
  if (localHash !== remoteHash) {
    throw new Error(`${assetPath} hash mismatch: local=${localHash} remote=${remoteHash}`);
  }
  return `${basename(assetPath)}=${remoteHash}`;
}

async function main() {
  const url = readArg(['--url', '-Url'], process.env.MARGINS_PRODUCTION_URL || process.env.MARGINS_FRONT_URL || '');
  if (!url) {
    throw new Error('Missing --url or MARGINS_PRODUCTION_URL.');
  }
  const baseUrl = new URL(url);
  const html = await fetchText(baseUrl.toString());
  const localHtml = await readFile('dist/index.html', 'utf8');
  const remoteAssets = assetPathsFromHtml(html);
  const localAssets = assetPathsFromHtml(localHtml);
  const expectedAssets = [...localAssets.css, ...localAssets.js].sort();
  const actualAssets = [...remoteAssets.css, ...remoteAssets.js].sort();
  if (expectedAssets.join('\n') !== actualAssets.join('\n')) {
    throw new Error(`Production HTML asset refs do not match local dist.\nExpected:\n${expectedAssets.join('\n')}\nActual:\n${actualAssets.join('\n')}`);
  }
  const assetHashes = [];
  for (const assetPath of expectedAssets) {
    assetHashes.push(await assertAssetMatches(baseUrl, assetPath));
  }

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const consoleErrors = [];
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });

  try {
    baseUrl.searchParams.set('smoke', String(Date.now()));
    await page.goto(baseUrl.toString(), { waitUntil: 'networkidle' });
    const result = await page.evaluate(() => {
      const h1 = document.querySelector('h1');
      return {
        text: document.body.innerText,
        overlay: Boolean(document.querySelector('.vite-error-overlay, #webpack-dev-server-client-overlay, [data-nextjs-dialog]')),
        lang: document.documentElement.lang,
        bodyFont: getComputedStyle(document.body).fontFamily,
        h1Font: getComputedStyle(h1 || document.body).fontFamily,
      };
    });
    if (result.overlay) {
      throw new Error('Production UI rendered a framework error overlay.');
    }
    if (!result.text.includes('Margins') || !result.text.includes('Login') || !result.text.includes('Sign in to continue your private reading archive.')) {
      throw new Error(`Production UI missing expected login shell text: ${JSON.stringify(result.text)}`);
    }
    if (!result.bodyFont.includes('Inter') || !result.h1Font.includes('Newsreader')) {
      throw new Error(`Production UI fonts are not the expected editorial stack: body=${result.bodyFont} h1=${result.h1Font}`);
    }
    if (consoleErrors.length > 0) {
      throw new Error(`Production UI console errors: ${consoleErrors.join('\n')}`);
    }

    console.log('# Production UI Smoke');
    console.log('');
    console.log(`Checked: ${baseUrl.origin}`);
    console.log(`Assets: ${assetHashes.join(', ')}`);
    console.log(`Language: ${result.lang}`);
    console.log('PASS: production UI matches the current built frontend assets and renders the login shell.');
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
