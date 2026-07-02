import { createWriteStream } from 'node:fs';
import { access, chmod, mkdir, readFile, readdir, rm, stat } from 'node:fs/promises';
import { get } from 'node:https';
import { basename, dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(scriptDir, '..');
const frontRoot = join(repoRoot, 'front');
const backRoot = join(repoRoot, 'back');
const toolsDir = join(repoRoot, '.tools');
const gradleVersion = process.env.MARGINS_GRADLE_VERSION || '8.10.2';
const gradleName = `gradle-${gradleVersion}`;
const gradleRoot = join(toolsDir, gradleName);
const gradleZip = join(toolsDir, `${gradleName}-bin.zip`);
const gradleUrl = `https://services.gradle.org/distributions/${gradleName}-bin.zip`;

const command = process.argv[2] || 'help';
const args = process.argv.slice(3);
let dotEnv = {};

function bin(name) {
  return process.platform === 'win32' ? `${name}.cmd` : name;
}

function gradleBin() {
  return join(gradleRoot, 'bin', process.platform === 'win32' ? 'gradle.bat' : 'gradle');
}

async function exists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

function run(cmd, cmdArgs = [], options = {}) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(cmd, cmdArgs, {
      cwd: options.cwd || repoRoot,
      env: { ...process.env, ...options.env },
      stdio: options.stdio || 'inherit',
      shell: false,
    });

    child.on('error', rejectRun);
    child.on('exit', (code) => {
      if (code === 0) {
        resolveRun();
      } else {
        rejectRun(new Error(`${cmd} ${cmdArgs.join(' ')} failed with exit code ${code}`));
      }
    });
  });
}

function capture(cmd, cmdArgs = [], options = {}) {
  return new Promise((resolveRun) => {
    const child = spawn(cmd, cmdArgs, {
      cwd: options.cwd || repoRoot,
      env: { ...process.env, ...options.env },
      stdio: ['ignore', 'pipe', 'pipe'],
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
    child.on('error', (error) => {
      resolveRun({ ok: false, stdout, stderr, error });
    });
    child.on('exit', (code) => {
      resolveRun({ ok: code === 0, code, stdout, stderr });
    });
  });
}

async function download(url, outputPath) {
  await mkdir(dirname(outputPath), { recursive: true });
  await new Promise((resolveDownload, rejectDownload) => {
    const request = get(url, (response) => {
      if ([301, 302, 303, 307, 308].includes(response.statusCode || 0) && response.headers.location) {
        response.resume();
        download(response.headers.location, outputPath).then(resolveDownload, rejectDownload);
        return;
      }

      if (response.statusCode !== 200) {
        response.resume();
        rejectDownload(new Error(`Download failed with HTTP ${response.statusCode}: ${url}`));
        return;
      }

      const file = createWriteStream(outputPath);
      response.pipe(file);
      file.on('finish', () => {
        file.close(resolveDownload);
      });
      file.on('error', rejectDownload);
    });

    request.on('error', rejectDownload);
  });
}

async function ensureGradle() {
  if (await exists(gradleBin())) {
    if (process.platform !== 'win32') {
      await chmod(gradleBin(), 0o755);
    }
    return;
  }

  await mkdir(toolsDir, { recursive: true });
  if (!(await exists(gradleZip))) {
    console.log(`Downloading ${gradleUrl}`);
    await download(gradleUrl, gradleZip);
  }

  console.log(`Extracting ${gradleZip}`);
  await rm(gradleRoot, { recursive: true, force: true });
  await run('jar', ['xf', gradleZip], { cwd: toolsDir });
  if (!(await exists(gradleBin()))) {
    throw new Error(`Gradle executable was not found after setup: ${gradleBin()}`);
  }
  if (process.platform !== 'win32') {
    await chmod(gradleBin(), 0o755);
  }
}

function loadDotEnv() {
  return dotEnv;
}

async function readDotEnv() {
  const envPath = join(repoRoot, '.env');
  if (!(await exists(envPath))) {
    return {};
  }

  const env = {};
  const text = await readFile(envPath, 'utf8');
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) {
      continue;
    }

    const match = line.match(/^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/);
    if (!match) {
      continue;
    }

    const [, key, rawValue] = match;
    let value = rawValue.trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    env[key] = value;
  }

  return env;
}

async function doctor() {
  const checks = [
    ['node', ['--version']],
    [bin('npm'), ['--version']],
    ['java', ['-version']],
    ['jar', ['--version']],
    ['docker', ['--version']],
    ['docker', ['compose', 'version']],
  ];

  let failed = false;
  for (const [cmdName, cmdArgs] of checks) {
    const result = await capture(cmdName, cmdArgs);
    const output = `${result.stdout}${result.stderr}`.trim().split('\n')[0] || result.error?.message || '';
    if (result.ok) {
      console.log(`OK ${cmdName} ${cmdArgs.join(' ')}: ${output}`);
    } else {
      failed = true;
      console.log(`MISSING ${cmdName} ${cmdArgs.join(' ')}: ${output}`);
    }
  }

  if (failed) {
    throw new Error('Install missing prerequisites, then run npm run local:doctor again.');
  }
}

async function install() {
  await doctor();
  await ensureGradle();
  await run(bin('npm'), ['install'], { cwd: frontRoot });
  await run(bin('npx'), ['playwright', 'install'], { cwd: frontRoot });
}

async function waitForMysql(containerName, timeoutSeconds) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  while (Date.now() < deadline) {
    const result = await capture('docker', ['inspect', '--format', '{{.State.Health.Status}}', containerName]);
    if (result.ok && result.stdout.trim() === 'healthy') {
      console.log(`${containerName} is healthy`);
      return;
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 2000));
  }
  throw new Error(`Timed out waiting for ${containerName} to become healthy.`);
}

async function applySqlFile(containerName, database, rootPassword, filePath) {
  const containerDir = '/tmp/margins-sql';
  const containerFile = `${containerDir}/${basename(filePath)}`;
  await run('docker', ['exec', containerName, 'sh', '-c', `mkdir -p ${containerDir}`]);
  await run('docker', ['cp', filePath, `${containerName}:${containerFile}`]);
  await run('docker', [
    'exec',
    '-e',
    `MYSQL_PWD=${rootPassword}`,
    '-e',
    `MARGINS_SQL_FILE=${containerFile}`,
    '-e',
    `MARGINS_MYSQL_DATABASE=${database}`,
    containerName,
    'sh',
    '-c',
    'mysql --user=root --default-character-set=utf8mb4 "$MARGINS_MYSQL_DATABASE" < "$MARGINS_SQL_FILE"',
  ]);
}

async function dbUp({ applySchema = false, timeoutSeconds = 120 } = {}) {
  const composeFile = join(repoRoot, 'infra', 'docker', 'mysql-compose.yml');
  const containerName = 'margins-mysql';
  const database = process.env.MARGINS_MYSQL_DATABASE || 'margins';
  const rootPassword = process.env.MARGINS_MYSQL_ROOT_PASSWORD || 'margins-root';
  const port = process.env.MARGINS_MYSQL_PORT || '3306';

  await run('docker', ['compose', '-f', composeFile, 'up', '-d', 'mysql']);
  await waitForMysql(containerName, timeoutSeconds);

  if (applySchema) {
    const schemaDir = join(repoRoot, 'db', 'schema');
    const schemaFiles = (await readdir(schemaDir))
      .filter((file) => file.endsWith('.sql'))
      .sort()
      .map((file) => join(schemaDir, file));
    const seedFile = join(repoRoot, 'db', 'seed', '001_seed_mvp_data.sql');

    for (const file of [...schemaFiles, seedFile]) {
      await stat(file);
      console.log(`Applying ${file}`);
      await applySqlFile(containerName, database, rootPassword, file);
    }
  }

  console.log(`MySQL is ready on port ${port}`);
}

async function dbDown() {
  const composeFile = join(repoRoot, 'infra', 'docker', 'mysql-compose.yml');
  const downArgs = ['compose', '-f', composeFile, 'down'];
  if (args.includes('--volumes')) {
    downArgs.push('--volumes');
  }
  await run('docker', downArgs);
}

async function gradleTask(task = 'test') {
  await ensureGradle();
  await run(gradleBin(), ['--no-daemon', task], {
    cwd: backRoot,
    env: loadDotEnv(),
  });
}

async function frontDev() {
  await run(bin('npm'), ['run', 'dev'], {
    cwd: frontRoot,
    env: {
      MARGINS_BACKEND_URL: process.env.MARGINS_BACKEND_URL || 'http://localhost:8080',
    },
  });
}

async function dev() {
  await dbUp({ applySchema: true });
  await ensureGradle();

  const backend = spawn(gradleBin(), ['--no-daemon', 'bootRun'], {
    cwd: backRoot,
    env: { ...process.env, SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'local' },
    stdio: 'inherit',
  });
  const frontend = spawn(bin('npm'), ['run', 'dev'], {
    cwd: frontRoot,
    env: { ...process.env, MARGINS_BACKEND_URL: process.env.MARGINS_BACKEND_URL || 'http://localhost:8080' },
    stdio: 'inherit',
  });

  const stop = () => {
    backend.kill('SIGTERM');
    frontend.kill('SIGTERM');
  };
  process.on('SIGINT', stop);
  process.on('SIGTERM', stop);

  await new Promise((resolveDev, rejectDev) => {
    let settled = false;
    for (const [name, child] of [
      ['backend', backend],
      ['frontend', frontend],
    ]) {
      child.on('exit', (code) => {
        if (settled) {
          return;
        }
        settled = true;
        stop();
        if (code === 0 || code === null) {
          resolveDev();
        } else {
          rejectDev(new Error(`${name} exited with code ${code}`));
        }
      });
    }
  });
}

async function quality() {
  await gradleTask('test');
  await run(bin('npm'), ['run', 'test:unit'], { cwd: frontRoot });
  await run(bin('npm'), ['run', 'build'], { cwd: frontRoot });
  await run(bin('npm'), ['run', 'verify:production-selectors'], { cwd: frontRoot });
}

async function main() {
  dotEnv = await readDotEnv();
  for (const [key, value] of Object.entries(dotEnv)) {
    if (process.env[key] === undefined) {
      process.env[key] = value;
    }
  }

  if (command === 'help') {
    console.log('Commands: doctor, install, db-up, db-down, back-test, back-dev, front-dev, dev, quality');
    return;
  }
  if (command === 'doctor') return doctor();
  if (command === 'install') return install();
  if (command === 'db-up') return dbUp({ applySchema: args.includes('--apply-schema') });
  if (command === 'db-down') return dbDown();
  if (command === 'back-test') return gradleTask(args[0] || 'test');
  if (command === 'back-dev') return gradleTask('bootRun');
  if (command === 'front-dev') return frontDev();
  if (command === 'dev') return dev();
  if (command === 'quality') return quality();

  throw new Error(`Unknown command: ${command}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
