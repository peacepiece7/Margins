import { createHash, randomUUID } from 'node:crypto';
import { createReadStream } from 'node:fs';
import {
  access,
  cp,
  mkdir,
  readFile,
  readdir,
  rm,
  stat,
  writeFile,
} from 'node:fs/promises';
import { basename, dirname, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(scriptDir, '..');
const command = process.argv[2] || 'help';
const args = process.argv.slice(3);

function hasFlag(...names) {
  return args.some((arg) => names.includes(arg));
}

function readArg(names, fallback = undefined) {
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

function envOr(name, fallback) {
  return process.env[name] || fallback;
}

function safeName(value, pattern, label) {
  if (!pattern.test(value)) {
    throw new Error(`${label} has an unsafe value.`);
  }
}

function shellSingleQuote(value) {
  return `'${String(value).replaceAll("'", "'\\''")}'`;
}

async function exists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

async function listFiles(root) {
  const entries = await readdir(root, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await listFiles(path)));
    } else if (entry.isFile()) {
      files.push(path);
    }
  }
  return files;
}

async function run(cmd, cmdArgs = [], options = {}) {
  await new Promise((resolveRun, rejectRun) => {
    const child = spawn(cmd, cmdArgs, {
      cwd: options.cwd || repoRoot,
      env: { ...process.env, ...options.env },
      stdio: options.stdio || 'inherit',
      shell: false,
    });
    if (options.input) {
      child.stdin.end(options.input);
    }
    child.on('error', rejectRun);
    child.on('exit', (code) => {
      if (code === 0) resolveRun();
      else rejectRun(new Error(`${options.label || cmd} failed with exit code ${code}`));
    });
  });
}

async function capture(cmd, cmdArgs = [], options = {}) {
  return await new Promise((resolveRun) => {
    const child = spawn(cmd, cmdArgs, {
      cwd: options.cwd || repoRoot,
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
    if (options.input) {
      child.stdin.end(options.input);
    } else {
      child.stdin.end();
    }
    child.on('error', (error) => resolveRun({ ok: false, stdout, stderr, error }));
    child.on('exit', (code) => resolveRun({ ok: code === 0, code, stdout, stderr }));
  });
}

async function readDotEnv(envPath = '.env') {
  const path = resolve(repoRoot, envPath);
  if (!(await exists(path))) return {};
  const text = await readFile(path, 'utf8');
  const env = {};
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#') || !line.includes('=')) continue;
    const [name, ...rest] = line.split('=');
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(name.trim())) continue;
    let value = rest.join('=').trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    env[name.trim()] = value;
  }
  return env;
}

async function loadEnv(envPath = '.env') {
  const env = await readDotEnv(envPath);
  for (const [key, value] of Object.entries(env)) {
    if (process.env[key] === undefined) process.env[key] = value;
  }
}

async function sha256(path) {
  const hash = createHash('sha256');
  await new Promise((resolveHash, rejectHash) => {
    const stream = createReadStream(path);
    stream.on('data', (chunk) => hash.update(chunk));
    stream.on('error', rejectHash);
    stream.on('end', resolveHash);
  });
  return hash.digest('hex');
}

function toReleaseRelative(root, path) {
  return relative(root, path).split(sep).join('/');
}

function isJarMetadata(rel) {
  return rel === 'META-INF/MANIFEST.MF';
}

async function writeLines(path, lines) {
  await writeFile(path, `${lines.join('\n')}\n`, 'utf8');
}

async function buildArtifacts() {
  const outputDir = readArg(['--output-dir', '-OutputDir'], 'infra/artifacts');
  const skipTests = hasFlag('--skip-tests', '-SkipTests');
  const artifactRoot = resolve(repoRoot, outputDir);
  const releaseDir = join(artifactRoot, 'margins-release');
  const backendDir = join(releaseDir, 'back');
  const frontendDir = join(releaseDir, 'front');
  const runtimeDir = join(releaseDir, 'runtime');
  const systemdDir = join(runtimeDir, 'systemd');
  const nginxDir = join(runtimeDir, 'nginx');

  await mkdir(artifactRoot, { recursive: true });
  await rm(releaseDir, { recursive: true, force: true });
  await mkdir(backendDir, { recursive: true });
  await mkdir(frontendDir, { recursive: true });
  await mkdir(systemdDir, { recursive: true });
  await mkdir(nginxDir, { recursive: true });

  if (!skipTests) {
    await run('node', ['scripts/local.mjs', 'back-test']);
  }
  await run('node', ['scripts/local.mjs', 'back-test', 'bootJar']);
  await run(process.platform === 'win32' ? 'npm.cmd' : 'npm', ['run', 'build'], { cwd: join(repoRoot, 'front') });

  const jarFiles = (await readdir(join(repoRoot, 'back/build/libs')))
    .filter((file) => file.endsWith('.jar') && !file.endsWith('-plain.jar'))
    .map((file) => join(repoRoot, 'back/build/libs', file));
  if (jarFiles.length === 0) throw new Error('Backend jar was not found under back/build/libs');
  jarFiles.sort();
  await cp(jarFiles[0], join(backendDir, 'margins-back.jar'));
  await cp(join(repoRoot, 'front/dist'), join(frontendDir, 'dist'), { recursive: true });
  await cp(join(repoRoot, 'infra/docker/mysql-compose.yml'), join(releaseDir, 'mysql-compose.yml'));

  await writeLines(join(releaseDir, 'README.md'), [
    '# Margins Release',
    '',
    '## Contents',
    '',
    '- back/margins-back.jar',
    '- front/dist/',
    '- mysql-compose.yml',
    '- runtime/env.example',
    '- runtime/systemd/margins-back.service.example',
    '- runtime/nginx/margins.conf.example',
    '- manifest.txt',
    '- checksums.sha256',
    '',
    '## Raspberry Pi Layout',
    '',
    'Node deploy commands extract each zip into /opt/margins/releases/<timestamp> and switch /opt/margins/current to that release.',
  ]);

  await writeLines(join(runtimeDir, 'env.example'), [
    'MARGINS_DB_URL=jdbc:mysql://localhost:3306/margins?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC',
    'MARGINS_MYSQL_USER=margins',
    'MARGINS_MYSQL_PASSWORD=change-me',
    'MARGINS_AUTH_JWT_SECRET=change-me',
    'MARGINS_SINGLE_USER_USERNAME=reader',
    'MARGINS_SINGLE_USER_PASSWORD=change-me',
    'OPENAI_API_KEY=',
    'OPENAI_MODEL=gpt-5.5',
    'MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED=false',
    'MARGINS_BOOK_SEARCH_PROVIDER=kakao',
    'SPRING_PROFILES_ACTIVE=prod',
  ]);

  await writeLines(join(systemdDir, 'margins-back.service.example'), [
    '[Unit]',
    'Description=Margins backend',
    'After=network-online.target',
    'Wants=network-online.target',
    '',
    '[Service]',
    'Type=simple',
    'User=margins',
    'Group=margins',
    'WorkingDirectory=/opt/margins/current',
    'EnvironmentFile=/opt/margins/.env',
    'ExecStart=/usr/bin/java -jar /opt/margins/current/back/margins-back.jar',
    'Restart=on-failure',
    'RestartSec=5',
    '',
    '[Install]',
    'WantedBy=multi-user.target',
  ]);

  await writeLines(join(nginxDir, 'margins.conf.example'), [
    'server {',
    '  listen 80;',
    '  server_name _;',
    '',
    '  root /opt/margins/current/front/dist;',
    '  index index.html;',
    '',
    '  location /api/ {',
    '    proxy_pass http://127.0.0.1:8080;',
    '    proxy_set_header Host $host;',
    '    proxy_set_header X-Real-IP $remote_addr;',
    '    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;',
    '    proxy_set_header X-Forwarded-Proto $scheme;',
    '  }',
    '',
    '  location /assets/ {',
    '    add_header Cache-Control "public, max-age=31536000, immutable";',
    '    try_files $uri =404;',
    '  }',
    '',
    '  location = /index.html {',
    '    add_header Cache-Control "no-cache, no-store, must-revalidate";',
    '    try_files /index.html =404;',
    '  }',
    '',
    '  location / {',
    '    add_header Cache-Control "no-cache, no-store, must-revalidate";',
    '    try_files $uri /index.html;',
    '  }',
    '}',
  ]);

  const commit = await capture('git', ['-C', repoRoot, 'rev-parse', '--short', 'HEAD']);
  await writeLines(join(releaseDir, 'manifest.txt'), [
    `builtAt=${new Date().toISOString()}`,
    `sourceCommit=${commit.ok ? commit.stdout.trim() : ''}`,
    'backendJar=back/margins-back.jar',
    'frontendDist=front/dist',
    'mysqlCompose=mysql-compose.yml',
    'runtimeEnv=runtime/env.example',
    'runtimeSystemd=runtime/systemd/margins-back.service.example',
    'runtimeNginx=runtime/nginx/margins.conf.example',
  ]);

  const checksumLines = [];
  for (const file of (await listFiles(releaseDir)).sort()) {
    if (basename(file) === 'checksums.sha256') continue;
    checksumLines.push(`${await sha256(file)}  ${toReleaseRelative(releaseDir, file)}`);
  }
  await writeFile(join(releaseDir, 'checksums.sha256'), `${checksumLines.join('\n')}\n`, 'ascii');

  const zipPath = join(artifactRoot, 'margins-release.zip');
  await rm(zipPath, { force: true });
  await run('jar', ['--create', '--file', zipPath, '-C', releaseDir, '.']);
  console.log(zipPath);
}

async function extractZip(zipPath, targetDir) {
  await rm(targetDir, { recursive: true, force: true });
  await mkdir(targetDir, { recursive: true });
  await run('jar', ['--extract', '--file', zipPath], { cwd: targetDir });
}

async function verifyArtifacts() {
  const artifactPath = resolve(repoRoot, readArg(['--artifact-path', '-ArtifactPath'], 'infra/artifacts/margins-release.zip'));
  const verifyRoot = join(repoRoot, 'infra/artifacts/verify', randomUUID().replaceAll('-', ''));
  const listing = await capture('jar', ['--list', '--file', artifactPath]);
  if (!listing.ok) throw new Error(`Artifact zip listing failed: ${listing.stderr}`);
  for (const entry of listing.stdout.split(/\r?\n/).filter(Boolean)) {
    if (entry.includes('\\')) throw new Error(`Artifact zip entry must use forward slash separators: ${entry}`);
  }

  try {
    await extractZip(artifactPath, verifyRoot);
    const artifactFiles = await listFiles(verifyRoot);
    const required = [
      'back/margins-back.jar',
      'front/dist/index.html',
      'mysql-compose.yml',
      'runtime/env.example',
      'runtime/systemd/margins-back.service.example',
      'runtime/nginx/margins.conf.example',
      'manifest.txt',
      'checksums.sha256',
      'README.md',
    ];
    for (const entry of required) {
      if (!(await exists(join(verifyRoot, ...entry.split('/'))))) throw new Error(`Missing artifact entry: ${entry}`);
    }
    for (const file of artifactFiles) {
      const rel = toReleaseRelative(verifyRoot, file);
      if (/(^|[/\\])\.env($|\.)|\.pem$|\.p8$|\.ppk$|id_rsa$|id_ed25519$/.test(rel)) {
        throw new Error(`Artifact contains forbidden secret-like file: ${rel}`);
      }
    }
    if ((await stat(join(verifyRoot, 'back/margins-back.jar'))).size <= 0) throw new Error('Backend jar is empty');
    const assetsDir = join(verifyRoot, 'front/dist/assets');
    if (!(await exists(assetsDir)) || (await readdir(assetsDir)).length === 0) throw new Error('Frontend dist assets are missing');
    const manifestText = await readFile(join(verifyRoot, 'manifest.txt'), 'utf8');
    for (const needle of ['builtAt=', 'sourceCommit=', 'backendJar=back/margins-back.jar', 'frontendDist=front/dist']) {
      if (!manifestText.includes(needle)) throw new Error(`Manifest is missing required entry: ${needle}`);
    }
    const envText = await readFile(join(verifyRoot, 'runtime/env.example'), 'utf8');
    for (const needle of ['MARGINS_DB_URL=', 'MARGINS_MYSQL_USER=', 'MARGINS_MYSQL_PASSWORD=change-me', 'MARGINS_AUTH_JWT_SECRET=change-me', 'MARGINS_SINGLE_USER_PASSWORD=change-me', 'OPENAI_API_KEY=']) {
      if (!envText.includes(needle)) throw new Error(`Runtime env example is missing required variable: ${needle}`);
    }
    if (/OPENAI_API_KEY=sk-[A-Za-z0-9_-]+/.test(envText)) throw new Error('Runtime env example appears to contain an OpenAI API key');
    const textExtensions = new Set(['.css', '.html', '.js', '.json', '.md', '.txt', '.xml', '.yml', '.yaml', '.example', '']);
    for (const file of artifactFiles) {
      const rel = toReleaseRelative(verifyRoot, file);
      const ext = rel.includes('.') ? `.${rel.split('.').pop().toLowerCase()}` : '';
      if (!textExtensions.has(ext)) continue;
      const text = await readFile(file, 'utf8');
      for (const marker of ['BEGIN OPENSSH PRIVATE KEY', 'BEGIN RSA PRIVATE KEY', 'BEGIN EC PRIVATE KEY', 'OPENAI_API_KEY=sk-']) {
        if (text.includes(marker)) throw new Error(`Artifact text file contains forbidden secret marker: ${rel}`);
      }
      for (const pattern of [/^MARGINS_AUTH_JWT_SECRET=(?!change-me\s*$).+/m, /^MARGINS_MYSQL_PASSWORD=(?!change-me\s*$).+/m, /^MARGINS_SINGLE_USER_PASSWORD=(?!change-me\s*$).+/m]) {
        if (pattern.test(text)) throw new Error(`Artifact text file contains forbidden secret value: ${rel}`);
      }
    }
    const checksumText = await readFile(join(verifyRoot, 'checksums.sha256'), 'utf8');
    let count = 0;
    for (const line of checksumText.split(/\r?\n/)) {
      if (!line.trim()) continue;
      const match = line.match(/^([a-f0-9]{64})\s+(.+)$/);
      if (!match) throw new Error(`Invalid checksum line: ${line}`);
      const [, expected, rel] = match;
      const target = join(verifyRoot, ...rel.split('/'));
      if (!(await exists(target))) throw new Error(`Checksum target is missing: ${rel}`);
      const actual = await sha256(target);
      if (actual !== expected) throw new Error(`Checksum mismatch for ${rel}`);
      count += 1;
    }
    const expectedCount = artifactFiles.filter((file) => {
      const rel = toReleaseRelative(verifyRoot, file);
      return basename(file) !== 'checksums.sha256' && !isJarMetadata(rel);
    }).length;
    if (count !== expectedCount) throw new Error(`Checksum target count mismatch: expected ${expectedCount} but found ${count}`);
    console.log(`Artifact verified: ${artifactPath}`);
  } finally {
    await rm(verifyRoot, { recursive: true, force: true });
  }
}

function deploymentEnv() {
  const deployDir = process.env.MARGINS_DEPLOY_DIR;
  const { host, user } = sshTargetEnv();
  const manager = process.env.MARGINS_SERVICE_MANAGER;
  for (const [name, value] of Object.entries({ MARGINS_DEPLOY_DIR: deployDir, MARGINS_SERVICE_MANAGER: manager })) {
    if (!value) throw new Error(`Missing required environment variable: ${name}`);
  }
  safeName(deployDir, /^\/[A-Za-z0-9_./-]+$/, 'MARGINS_DEPLOY_DIR');
  if (deployDir === '/') throw new Error('MARGINS_DEPLOY_DIR must not be /');
  if (!['systemd', 'manual', 'artifact'].includes(manager)) throw new Error('MARGINS_SERVICE_MANAGER must be one of: systemd, manual, artifact');
  return { host, user, deployDir, manager };
}

function sshTargetEnv() {
  const host = process.env.MARGINS_DEPLOY_HOST;
  const user = process.env.MARGINS_DEPLOY_USER;
  for (const [name, value] of Object.entries({ MARGINS_DEPLOY_HOST: host, MARGINS_DEPLOY_USER: user })) {
    if (!value) throw new Error(`Missing required environment variable: ${name}`);
  }
  safeName(host, /^[A-Za-z0-9_.-]+$/, 'MARGINS_DEPLOY_HOST');
  safeName(user, /^[A-Za-z0-9_.-]+$/, 'MARGINS_DEPLOY_USER');
  return { host, user };
}

async function sshOptions() {
  const options = ['-o', 'BatchMode=yes', '-o', 'ConnectTimeout=10', '-o', 'StrictHostKeyChecking=accept-new'];
  if (process.env.MARGINS_DEPLOY_SSH_KEY) {
    if (!(await exists(process.env.MARGINS_DEPLOY_SSH_KEY))) throw new Error('MARGINS_DEPLOY_SSH_KEY must point to an existing private key file');
    options.push('-i', resolve(process.env.MARGINS_DEPLOY_SSH_KEY));
  }
  return options;
}

function deployRemoteCommand({ deployDir, manager, releaseRetainCount, rollback, rollbackReleaseId }) {
  const backendService = envOr('MARGINS_BACKEND_SERVICE', 'margins-back');
  const frontendService = process.env.MARGINS_FRONTEND_SERVICE;
  safeName(backendService, /^[A-Za-z0-9_.@-]+$/, 'MARGINS_BACKEND_SERVICE');
  if (frontendService) safeName(frontendService, /^[A-Za-z0-9_.@-]+$/, 'MARGINS_FRONTEND_SERVICE');
  const restart = manager === 'systemd'
    ? [`sudo systemctl restart ${shellSingleQuote(backendService)}`, frontendService ? `sudo systemctl restart ${shellSingleQuote(frontendService)}` : ''].filter(Boolean).join('\n')
    : manager === 'manual'
      ? "printf 'manual-service-manager'"
      : "printf 'artifact-deployed'";
  if (rollback) {
    if (rollbackReleaseId) {
      return `set -e
cd ${shellSingleQuote(deployDir)}
rollback_dir="releases/${rollbackReleaseId}"
test -d "$rollback_dir"
if [ -e current ] && [ ! -L current ]; then
  legacy_dir="releases/legacy-$(date +%Y%m%d%H%M%S)"
  rm -rf "$legacy_dir"
  mv current "$legacy_dir"
fi
ln -sfn "$rollback_dir" current
${restart}`;
    }
    return `set -e
cd ${shellSingleQuote(deployDir)}
current_target=$(readlink current || true)
rollback_dir=$(find releases -mindepth 1 -maxdepth 1 -type d | sort -r | awk -v current="$current_target" '$0 != current { print; exit }')
test -n "$rollback_dir"
test -d "$rollback_dir"
if [ -e current ] && [ ! -L current ]; then
  legacy_dir="releases/legacy-$(date +%Y%m%d%H%M%S)"
  rm -rf "$legacy_dir"
  mv current "$legacy_dir"
fi
ln -sfn "$rollback_dir" current
${restart}`;
  }
  return `set -e
cd ${shellSingleQuote(deployDir)}
release_id=$(date +%Y%m%d%H%M%S)
release_dir="releases/$release_id"
release_retain_count=${releaseRetainCount}
mkdir -p releases
rm -rf "$release_dir"
mkdir -p "$release_dir"
unzip -oq margins-release.zip -d "$release_dir"
if [ -e current ] && [ ! -L current ]; then
  legacy_dir="releases/legacy-$release_id"
  rm -rf "$legacy_dir"
  mv current "$legacy_dir"
fi
ln -sfn "$release_dir" current
${restart}
find releases -mindepth 1 -maxdepth 1 -type d | sort -r | awk -v retain="$release_retain_count" 'NR > retain' | while IFS= read -r old_release; do
  rm -rf "$old_release"
done`;
}

async function deployPi() {
  await loadEnv(readArg(['--env-path', '-EnvPath'], '.env'));
  const rollback = hasFlag('--rollback', '-Rollback');
  const dryRun = hasFlag('--dry-run', '-DryRun');
  const sshPreflight = hasFlag('--ssh-preflight', '-SshPreflight');
  const smokeHealthUrl = readArg(['--smoke-health-url', '-SmokeHealthUrl'], process.env.MARGINS_DEPLOY_HEALTH_URL || '');
  const artifactPath = resolve(repoRoot, readArg(['--artifact-path', '-ArtifactPath'], 'infra/artifacts/margins-release.zip'));
  const rollbackReleaseId = readArg(['--rollback-release-id', '-RollbackReleaseId'], '');
  if (rollbackReleaseId && !/^\d{14}$/.test(rollbackReleaseId)) throw new Error('RollbackReleaseId must be a 14 digit release timestamp');
  const env = deploymentEnv();
  const releaseRetainCount = Number(readArg(['--release-retain-count', '-ReleaseRetainCount'], process.env.MARGINS_RELEASE_RETAIN_COUNT || '5'));
  if (!Number.isInteger(releaseRetainCount) || releaseRetainCount < 1) throw new Error('ReleaseRetainCount must be at least 1');
  if (!rollback && !(await exists(artifactPath))) throw new Error(`Artifact not found: ${artifactPath}`);
  const options = await sshOptions();
  const target = `${env.user}@${env.host}`;
  const remoteZip = `${env.deployDir}/margins-release.zip`;
  const remoteCommand = deployRemoteCommand({ ...env, releaseRetainCount, rollback, rollbackReleaseId });
  if (dryRun) {
    console.log('Dry run passed.');
    console.log(`Mode: ${rollback ? 'rollback' : 'deploy'}`);
    if (!rollback) console.log(`Artifact: ${artifactPath}`);
    console.log(`Target: ${target}`);
    console.log(`SSH key: ${process.env.MARGINS_DEPLOY_SSH_KEY ? 'configured' : 'default agent or SSH config'}`);
    if (!rollback) console.log(`Remote zip: ${remoteZip}`);
    console.log(`Service manager: ${env.manager}`);
    console.log(`Backend service: ${envOr('MARGINS_BACKEND_SERVICE', 'margins-back')}`);
    if (process.env.MARGINS_FRONTEND_SERVICE) console.log(`Frontend service: ${process.env.MARGINS_FRONTEND_SERVICE}`);
    if (rollback) console.log(`Rollback release: ${rollbackReleaseId || 'previous'}`);
    else console.log(`Release retain count: ${releaseRetainCount}`);
    console.log(`Smoke health URL: ${smokeHealthUrl ? 'configured' : 'not configured'}`);
    console.log('Remote command:');
    console.log(remoteCommand);
    return;
  }
  if (sshPreflight) {
    await run('ssh', [...options, target, "printf 'margins-ssh-ok'"]);
    console.log('SSH preflight passed for Raspberry Pi target.');
    return;
  }
  if (!rollback) {
    await run('ssh', [...options, target, `mkdir -p ${shellSingleQuote(env.deployDir)}`]);
    await run('scp', [...options, artifactPath, `${target}:${remoteZip}`]);
  }
  await run('ssh', [...options, target, remoteCommand]);
  if (smokeHealthUrl) {
    await smokeHealth(smokeHealthUrl, Number(readArg(['--smoke-attempts', '-SmokeAttempts'], '12')), Number(readArg(['--smoke-delay-seconds', '-SmokeDelaySeconds'], '5')));
    console.log('Deploy smoke passed for configured health URL.');
  }
}

async function smokeHealth(url, attempts, delaySeconds) {
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    try {
      const response = await fetch(url);
      if (response.status >= 200 && response.status < 400) return;
    } catch {
      // retry
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, delaySeconds * 1000));
  }
  throw new Error('Deploy smoke failed for configured health URL.');
}

async function auditDeployDryRun() {
  const artifactPath = readArg(['--artifact-path', '-ArtifactPath'], 'harness/artifacts/deploy-dry-run/margins-release.zip');
  const fullArtifactPath = resolve(repoRoot, artifactPath);
  await mkdir(dirname(fullArtifactPath), { recursive: true });
  if (!(await exists(fullArtifactPath))) await writeFile(fullArtifactPath, 'dry-run-placeholder', 'ascii');
  const oldEnv = { ...process.env };
  try {
    process.env.MARGINS_DEPLOY_HOST = 'dry-run.local';
    process.env.MARGINS_DEPLOY_USER = 'margins';
    process.env.MARGINS_DEPLOY_DIR = '/opt/margins';
    process.env.MARGINS_SERVICE_MANAGER = 'systemd';
    process.env.MARGINS_BACKEND_SERVICE = 'margins-back';
    process.env.MARGINS_FRONTEND_SERVICE = 'margins-front';
    process.env.MARGINS_DEPLOY_HEALTH_URL = 'https://dry-run.local/api/health';
    process.env.MARGINS_DEPLOY_SSH_KEY = join(repoRoot, 'harness/artifacts/deploy-dry-run/dry-run-key');
    await writeFile(process.env.MARGINS_DEPLOY_SSH_KEY, 'dry-run-placeholder-key', 'ascii');
    const result = await capture('node', ['scripts/deploy.mjs', 'pi', '--env-path', '.env.does-not-exist', '--artifact-path', artifactPath, '--smoke-attempts', '3', '--smoke-delay-seconds', '1', '--release-retain-count', '4', '--dry-run']);
    if (!result.ok) throw new Error(`Deploy dry-run failed: ${result.stdout}${result.stderr}`);
    const text = result.stdout;
    for (const needle of ['Dry run passed.', 'Target: margins@dry-run.local', 'SSH key: configured', 'Remote zip: /opt/margins/margins-release.zip', 'Service manager: systemd', 'Backend service: margins-back', 'Frontend service: margins-front', 'Release retain count: 4', 'Smoke health URL: configured', 'release_dir="releases/$release_id"', 'ln -sfn "$release_dir" current', "sudo systemctl restart 'margins-back'", "sudo systemctl restart 'margins-front'"]) {
      if (!text.includes(needle)) throw new Error(`Deploy dry-run output missing required text: ${needle}`);
    }
    if (text.includes(process.env.MARGINS_DEPLOY_HEALTH_URL) || text.includes(process.env.MARGINS_DEPLOY_SSH_KEY)) throw new Error('Deploy dry-run leaked sensitive local value');
    console.log('# Deploy Dry-Run Audit');
    console.log('');
    console.log(`Artifact checked: ${artifactPath}`);
    console.log('Service manager checked: systemd');
    console.log('Smoke health checked: configured without printing URL');
    console.log('Remote command checked: unzip and backend/frontend restart');
    console.log('');
    console.log('PASS: Raspberry Pi deploy dry-run output contract is consistent.');
  } finally {
    for (const key of Object.keys(process.env)) {
      if (!(key in oldEnv)) delete process.env[key];
    }
    Object.assign(process.env, oldEnv);
  }
}

async function uploadProdEnv() {
  await loadEnv(readArg(['--deploy-env-path', '-DeployEnvPath'], '.env'));
  const dryRun = hasFlag('--dry-run', '-DryRun');
  const runtimeEnvPath = resolve(repoRoot, readArg(['--runtime-env-path', '-RuntimeEnvPath'], '.env.production'));
  const remoteEnvPath = readArg(['--remote-env-path', '-RemoteEnvPath'], process.env.MARGINS_REMOTE_ENV_PATH || '/opt/margins/.env');
  const noBackup = hasFlag('--no-backup', '-NoBackup');
  const text = await readFile(runtimeEnvPath, 'utf8');
  const keys = text.split(/\r?\n/).map((line) => line.match(/^([A-Za-z_][A-Za-z0-9_]*)=/)?.[1]).filter(Boolean);
  for (const key of ['MARGINS_DB_URL', 'MARGINS_MYSQL_USER', 'MARGINS_MYSQL_PASSWORD', 'MARGINS_AUTH_JWT_SECRET', 'MARGINS_SINGLE_USER_USERNAME', 'MARGINS_SINGLE_USER_PASSWORD', 'SPRING_PROFILES_ACTIVE']) {
    if (!keys.includes(key)) throw new Error(`Runtime env file is missing required key: ${key}`);
  }
  const { host, user } = sshTargetEnv();
  if (!/^\/[A-Za-z0-9_./-]+$/.test(remoteEnvPath) || remoteEnvPath === '/') throw new Error('RemoteEnvPath must be a safe absolute remote file path');
  const options = await sshOptions();
  const target = `${user}@${host}`;
  if (dryRun) {
    console.log('Runtime env upload dry run passed.');
    console.log(`Target: ${target}`);
    console.log(`Remote env path: ${remoteEnvPath}`);
    console.log(`Backup: ${noBackup ? 'disabled' : 'enabled'}`);
    console.log(`SSH key: ${process.env.MARGINS_DEPLOY_SSH_KEY ? 'configured' : 'default agent or SSH config'}`);
    console.log(`Runtime env keys: ${keys.sort().join(',')}`);
    return;
  }
  const remoteTmp = `/tmp/margins.env.${randomUUID().replaceAll('-', '')}`;
  const remoteDir = dirname(remoteEnvPath);
  await run('scp', [...options, runtimeEnvPath, `${target}:${remoteTmp}`]);
  const backupFlag = noBackup ? '0' : '1';
  const remoteCommand = `set -e
remote_env=${shellSingleQuote(remoteEnvPath)}
remote_tmp=${shellSingleQuote(remoteTmp)}
remote_dir=${shellSingleQuote(remoteDir)}
backup_enabled=${shellSingleQuote(backupFlag)}
mkdir -p "$remote_dir"
if [ "$backup_enabled" = "1" ] && [ -f "$remote_env" ]; then
  cp "$remote_env" "$remote_env.backup.$(date +%Y%m%d%H%M%S)"
fi
mv "$remote_tmp" "$remote_env"
chmod 600 "$remote_env"
printf 'runtime_env=updated\\n'
printf 'runtime_env_path=%s\\n' "$remote_env"
printf 'runtime_env_keys='
sed -n 's/^\\([A-Za-z_][A-Za-z0-9_]*\\)=.*/\\1/p' "$remote_env" | sort | paste -sd ',' -
printf '\\nruntime_env_perms='
stat -c '%a %U:%G' "$remote_env"
printf '\\n'`;
  await run('ssh', [...options, target, remoteCommand]);
}

async function applySchema() {
  await loadEnv(readArg(['--env-path', '-EnvPath'], '.env'));
  const dryRun = hasFlag('--dry-run', '-DryRun');
  const { host, user } = sshTargetEnv();
  const container = readArg(['--remote-mysql-container', '-RemoteMysqlContainer'], process.env.MARGINS_REMOTE_MYSQL_CONTAINER || 'margins-mysql');
  const database = readArg(['--mysql-database', '-MysqlDatabase'], process.env.MARGINS_REMOTE_MYSQL_DATABASE || process.env.MARGINS_MYSQL_DATABASE || 'margins');
  const mysqlUser = readArg(['--mysql-user', '-MysqlUser'], process.env.MARGINS_REMOTE_MYSQL_USER || process.env.MARGINS_MYSQL_USER || 'margins');
  const mysqlPassword = readArg(['--mysql-password', '-MysqlPassword'], process.env.MARGINS_REMOTE_MYSQL_PASSWORD || process.env.MARGINS_MYSQL_PASSWORD || '');
  safeName(container, /^[A-Za-z0-9_.-]+$/, 'RemoteMysqlContainer');
  safeName(database, /^[A-Za-z0-9_.-]+$/, 'MysqlDatabase');
  safeName(mysqlUser, /^[A-Za-z0-9_.-]+$/, 'MysqlUser');
  const options = await sshOptions();
  const target = `${user}@${host}`;
  const schemaFiles = (await readdir(join(repoRoot, 'db/schema'))).filter((file) => file.endsWith('.sql')).sort().map((file) => join(repoRoot, 'db/schema', file));
  if (hasFlag('--apply-seed', '-ApplySeed')) schemaFiles.push(join(repoRoot, 'db/seed/001_seed_mvp_data.sql'));
  if (dryRun) {
    console.log('Raspberry Pi schema apply dry run passed.');
    console.log(`Target: ${target}`);
    console.log(`Container: ${container}`);
    console.log(`Database: ${database}`);
    console.log(`MySQL user: ${mysqlUser}`);
    console.log(`Password source: ${mysqlPassword ? 'provided environment' : 'remote container environment'}`);
    console.log(`SQL files: ${schemaFiles.map((file) => relative(repoRoot, file).replaceAll('\\', '/')).join(',')}`);
    console.log(`SSH key: ${process.env.MARGINS_DEPLOY_SSH_KEY ? 'configured' : 'default agent or SSH config'}`);
    return;
  }
  const remoteCommand = mysqlPassword
    ? `sh -c 'IFS= read -r mysql_password; docker exec -i -e MYSQL_PWD="$mysql_password" ${shellSingleQuote(container)} mysql --default-character-set=utf8mb4 -u ${shellSingleQuote(mysqlUser)} ${shellSingleQuote(database)}'`
    : `docker exec -i ${shellSingleQuote(container)} sh -c 'test -n "$MYSQL_USER" && test -n "$MYSQL_PASSWORD" && test -n "$MYSQL_DATABASE" && mysql --default-character-set=utf8mb4 -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'`;
  for (const file of schemaFiles) {
    console.log(`Applying ${relative(repoRoot, file)} to Raspberry Pi MySQL container ${container}`);
    const sql = await readFile(file);
    await run('ssh', [...options, target, remoteCommand], {
      input: mysqlPassword ? `${mysqlPassword}\n${sql}` : sql,
      label: `ssh schema apply ${relative(repoRoot, file).replaceAll('\\', '/')}`,
    });
  }
  console.log('Raspberry Pi schema apply completed.');
}

async function main() {
  if (command === 'help') {
    console.log('Commands: build, verify, dry-run, pi, upload-env, apply-schema');
    return;
  }
  if (command === 'build') return buildArtifacts();
  if (command === 'verify') return verifyArtifacts();
  if (command === 'dry-run') return auditDeployDryRun();
  if (command === 'pi') return deployPi();
  if (command === 'upload-env') return uploadProdEnv();
  if (command === 'apply-schema') return applySchema();
  throw new Error(`Unknown command: ${command}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
