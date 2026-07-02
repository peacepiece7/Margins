import { readdir, readFile, stat } from 'node:fs/promises';
import { join, relative } from 'node:path';

const repoRoot = process.cwd();
const scriptRoots = ['back/scripts', 'front/scripts', 'harness/scripts', 'infra/scripts', 'scripts'];
const failures = [];

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await walk(path)));
    } else {
      files.push(path);
    }
  }
  return files;
}

async function exists(path) {
  try {
    await stat(path);
    return true;
  } catch {
    return false;
  }
}

const files = [];
for (const root of scriptRoots) {
  if (await exists(root)) {
    files.push(...(await walk(root)));
  }
}

const projectScripts = files
  .map((path) => relative(repoRoot, path).replaceAll('\\', '/'))
  .filter((path) => /\.(ps1|sh|mjs)$/.test(path))
  .filter((path) => path !== 'harness/scripts/audit-cross-platform-scripts.mjs')
  .sort();

for (const path of projectScripts) {
  const text = await readFile(path, 'utf8');

  const forbiddenPatterns = [
    [/\bpowershell\s+-NoProfile\b/, 'hard-coded Windows PowerShell invocation'],
    [/Start-Process\s+-FilePath\s+["']powershell["']/, 'hard-coded Start-Process powershell executable'],
    [/WindowStyle\s+Hidden/, 'unconditional Windows-only WindowStyle Hidden'],
    [/\$gradleBat\b/, 'Windows-only Gradle launcher variable'],
    [/Install PowerShell 7[\s\S]*macOS/, 'macOS PowerShell installation guidance instead of Node-first commands'],
  ];

  for (const [pattern, description] of forbiddenPatterns) {
    if (pattern.test(text)) {
      failures.push(`${path}: contains ${description}`);
    }
  }
}

for (const path of projectScripts.filter((file) => file.endsWith('.sh'))) {
  const ps1Peer = path.replace(/\.sh$/, '.ps1');
  if (!(await exists(ps1Peer))) {
    failures.push(`${path}: missing Windows PowerShell peer ${ps1Peer}`);
  }
}

const packageJson = JSON.parse(await readFile('package.json', 'utf8'));
for (const [scriptName, scriptCommand] of Object.entries(packageJson.scripts ?? {})) {
  if (/\b(?:pwsh|powershell)\b|scripts\/pwsh\.mjs|\.ps1\b/i.test(scriptCommand)) {
    failures.push(`package.json: npm script ${scriptName} must use a Node entry point, not PowerShell: ${scriptCommand}`);
  }
}

const requiredScripts = [
  'local:doctor',
  'local:install',
  'local:db:up',
  'local:db:down',
  'local:dev',
  'local:quality',
  'back:test',
  'e2e:fullstack',
  'quality:full',
  'deploy:build',
  'deploy:verify',
  'deploy:dry-run',
  'deploy:upload-env',
  'deploy:pi',
  'deploy:apply-schema',
  'audit:scripts',
];
for (const scriptName of requiredScripts) {
  if (!packageJson.scripts?.[scriptName]) {
    failures.push(`package.json: missing npm script ${scriptName}`);
  }
}

const docsToCheck = ['README.md', 'docs/infra/sdd.md', 'docs/infra/bdd.md'];
for (const path of docsToCheck) {
  const text = await readFile(path, 'utf8');
  if (!text.includes('scripts/deploy.mjs')) {
    failures.push(`${path}: missing scripts/deploy.mjs Node deployment entry-point documentation`);
  }
  if (!text.includes('PowerShell is not required on macOS')) {
    failures.push(`${path}: missing no-PowerShell-on-macOS documentation`);
  }
}

if (failures.length > 0) {
  console.error('# Cross-Platform Script Audit');
  console.error('');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exitCode = 1;
} else {
  console.log('# Cross-Platform Script Audit');
  console.log('');
  console.log(`Checked ${projectScripts.length} project script files.`);
  console.log('PASS: shell script entry points are Node-first and do not require PowerShell on macOS.');
}
