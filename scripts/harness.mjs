import { mkdir, mkdtemp, readdir, readFile, rm, stat, writeFile } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';

const command = process.argv[2] || 'help';
const args = process.argv.slice(3);

function hasFlag(...names) {
  return args.some((arg) => names.includes(arg));
}

async function run(cmd, cmdArgs = [], options = {}) {
  await new Promise((resolveRun, rejectRun) => {
    const child = spawn(cmd, cmdArgs, {
      cwd: options.cwd || process.cwd(),
      env: { ...process.env, ...options.env },
      stdio: 'inherit',
      shell: false,
    });
    child.on('error', rejectRun);
    child.on('exit', (code) => {
      if (code === 0) resolveRun();
      else rejectRun(new Error(`${cmd} ${cmdArgs.join(' ')} failed with exit code ${code}`));
    });
  });
}

async function capture(cmd, cmdArgs = [], options = {}) {
  return await new Promise((resolveRun) => {
    const child = spawn(cmd, cmdArgs, {
      cwd: options.cwd || process.cwd(),
      env: { ...process.env, ...options.env },
      stdio: ['pipe', 'pipe', 'pipe'],
      shell: false,
    });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => {
      stdout += chunk;
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk;
    });
    child.stdin.end();
    child.on('error', (error) => resolveRun({ ok: false, stdout, stderr, error }));
    child.on('exit', (code) => resolveRun({ ok: code === 0, code, stdout, stderr }));
  });
}

function assertContains(name, text, needles) {
  const missing = needles.filter((needle) => !text.includes(needle));
  if (missing.length > 0) {
    throw new Error(`${name} missing required text: ${missing.join(', ')}`);
  }
}

async function exists(path) {
  try {
    await stat(path);
    return true;
  } catch {
    return false;
  }
}

async function auditCiWorkflow() {
  const text = await readFile('.github/workflows/ci.yml', 'utf8');
  assertContains('ci workflow', text, [
    'Node harness quality audit',
    'npm run quality:full -- -SkipBackend -SkipFrontendBuild',
    'Build release artifact',
    'npm run deploy:build -- --skip-tests',
    'Verify release artifact',
    'npm run deploy:verify',
    'Upload release artifact',
    'margins-release-${{ matrix.os }}',
  ]);
  for (const forbidden of ['deploy-raspberry-pi.ps1', ' -SshPreflight', ' -LiveDeploySmoke', 'MARGINS_DEPLOY_SSH_KEY']) {
    if (text.includes(forbidden)) {
      throw new Error(`CI workflow contains forbidden live deploy text: ${forbidden}`);
    }
  }
  console.log('# CI Workflow Audit');
  console.log('');
  console.log('PASS: CI workflow exposes Node script audit and avoids live Raspberry Pi deploy commands.');
}

async function auditDocs() {
  const readme = await readFile('README.md', 'utf8');
  const infraSdd = await readFile('docs/infra/sdd.md', 'utf8');
  const infraBdd = await readFile('docs/infra/bdd.md', 'utf8');
  assertContains('README', readme, ['scripts/deploy.mjs', 'npm run deploy:dry-run', 'npm run audit:scripts']);
  assertContains('infra sdd', infraSdd, ['scripts/deploy.mjs', 'PowerShell is not required on macOS']);
  assertContains('infra bdd', infraBdd, ['PowerShell is not required on macOS']);
  for (const domain of ['project', 'front', 'back', 'db', 'infra']) {
    if (!(await exists(`docs/${domain}/sdd.md`))) throw new Error(`docs/${domain}/sdd.md is missing`);
    if (!(await exists(`docs/${domain}/bdd.md`))) throw new Error(`docs/${domain}/bdd.md is missing`);
  }
  console.log('# Node Harness Docs Audit');
  console.log('');
  console.log('PASS: Node-first script contract is documented.');
}

async function auditDbContract() {
  const schemaFiles = (await readdir('db/schema')).filter((file) => file.endsWith('.sql')).sort();
  const schemaText = (await Promise.all(schemaFiles.map((file) => readFile(`db/schema/${file}`, 'utf8')))).join('\n');
  assertContains('db schema', schemaText, [
    'users',
    'books',
    'reading_sessions',
    'session_windows',
    'messages',
    'personas',
    'questions',
    'metrics',
    'deleted_at',
    'is_test_data',
  ]);
  console.log('# DB Contract Audit');
  console.log('');
  console.log('PASS: MVP DB schema contract is present.');
}

async function auditArtifactSecretGuard() {
  const auditRoot = await mkdtemp(join(tmpdir(), 'margins-artifact-audit-'));
  const releaseRoot = join(auditRoot, 'release');
  const artifactPath = join(auditRoot, 'margins-release.zip');
  try {
    const files = new Map([
      ['back/margins-back.jar', 'jar placeholder'],
      ['front/dist/index.html', '<!doctype html><html></html>'],
      ['mysql-compose.yml', 'services: {}\n'],
      ['runtime/env.example', 'MARGINS_MYSQL_PASSWORD=change-me\nMARGINS_AUTH_JWT_SECRET=change-me\nMARGINS_SINGLE_USER_PASSWORD=change-me\nOPENAI_API_KEY=\n'],
      ['runtime/systemd/margins-back.service.example', '[Service]\n'],
      ['runtime/nginx/margins.conf.example', 'server {}\n'],
      ['manifest.txt', 'builtAt=audit\nsourceCommit=audit\nbackendJar=back/margins-back.jar\nfrontendDist=front/dist\n'],
      ['checksums.sha256', 'placeholder\n'],
      ['README.md', 'audit fixture\n'],
      ['runtime/.env', 'OPENAI_API_KEY=sk-audit\n'],
    ]);
    for (const [relativePath, contents] of files) {
      const filePath = join(releaseRoot, ...relativePath.split('/'));
      await mkdir(dirname(filePath), { recursive: true });
      await writeFile(filePath, contents, 'utf8');
    }
    await run('jar', ['--create', '--file', artifactPath, '-C', releaseRoot, '.']);
    const result = await capture('node', ['scripts/deploy.mjs', 'verify', '--artifact-path', artifactPath]);
    if (result.ok) {
      throw new Error('Artifact secret guard accepted a release containing runtime/.env');
    }
    const output = `${result.stdout}\n${result.stderr}`;
    if (!output.includes('Artifact contains forbidden secret-like file: runtime/.env')) {
      throw new Error('Artifact secret guard failed for an unexpected reason.');
    }
  } finally {
    await rm(auditRoot, { recursive: true, force: true });
  }
  console.log('# Artifact Secret Guard Audit');
  console.log('');
  console.log('PASS: Node artifact verifier rejects secret-like release contents.');
}

async function auditAcceptanceTraceability() {
  const readiness = await readFile('docs/project/development-readiness.md', 'utf8');
  assertContains('development readiness', readiness, [
    'MVP Requirement Map',
    'Book search and add',
    'Raspberry Pi deploy flow',
    'Node harness quality gate',
    'Node release verification',
  ]);
  console.log('# Acceptance Traceability Audit');
  console.log('');
  console.log('PASS: MVP readiness evidence remains connected to Node verification.');
}

async function quality() {
  await run('node', ['harness/scripts/audit-cross-platform-scripts.mjs']);
  await auditCiWorkflow();
  await auditDocs();
  await auditDbContract();
  await auditArtifactSecretGuard();
  await auditAcceptanceTraceability();
  await run('node', ['scripts/deploy.mjs', 'dry-run']);
  if (!hasFlag('--skip-backend', '-SkipBackend')) {
    await run('node', ['scripts/local.mjs', 'back-test']);
  }
  await run(process.platform === 'win32' ? 'npm.cmd' : 'npm', ['run', 'test:unit'], { cwd: 'front' });
  if (!hasFlag('--skip-frontend-build', '-SkipFrontendBuild')) {
    await run(process.platform === 'win32' ? 'npm.cmd' : 'npm', ['run', 'build'], { cwd: 'front' });
    await run(process.platform === 'win32' ? 'npm.cmd' : 'npm', ['run', 'verify:production-selectors'], { cwd: 'front' });
  }
  console.log('');
  console.log('PASS: Node harness quality gate completed without PowerShell.');
}

async function main() {
  if (command === 'help') {
    console.log('Commands: quality, audit-ci, audit-docs');
    return;
  }
  if (command === 'quality') return quality();
  if (command === 'audit-ci') return auditCiWorkflow();
  if (command === 'audit-docs') return auditDocs();
  throw new Error(`Unknown command: ${command}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
