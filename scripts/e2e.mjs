import { spawn } from 'node:child_process';
import { Socket } from 'node:net';

const args = process.argv.slice(2);

function readArg(names, fallback) {
  for (let index = 0; index < args.length; index += 1) {
    if (names.includes(args[index])) return args[index + 1] ?? fallback;
  }
  return fallback;
}

const mysqlPort = readArg(['--mysql-port', '-MysqlPort'], '3307');
const backendPort = readArg(['--backend-port', '-BackendPort'], '18080');
const frontendPort = readArg(['--frontend-port', '-FrontendPort'], '15173');
const timeoutSeconds = Number(readArg(['--timeout-seconds', '-TimeoutSeconds'], '180'));
const keepStarted = args.includes('--keep-started-processes') || args.includes('-KeepStartedProcesses');
const reuseExisting = args.includes('--reuse-existing-services') || args.includes('-ReuseExistingServices');

function bin(name) {
  return process.platform === 'win32' ? `${name}.cmd` : name;
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

async function httpOk(url) {
  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(5000) });
    return response.status >= 200 && response.status < 300;
  } catch {
    return false;
  }
}

async function tcpOpen(port) {
  return await new Promise((resolve) => {
    const socket = new Socket();
    socket.setTimeout(1000);
    socket.once('connect', () => {
      socket.destroy();
      resolve(true);
    });
    socket.once('timeout', () => {
      socket.destroy();
      resolve(false);
    });
    socket.once('error', () => {
      socket.destroy();
      resolve(false);
    });
    socket.connect(Number(port), '127.0.0.1');
  });
}

async function waitHttp(name, url) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  while (Date.now() < deadline) {
    if (await httpOk(url)) {
      console.log(`${name} is ready: ${url}`);
      return;
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 2000));
  }
  throw new Error(`Timed out waiting for ${name} at ${url}`);
}

function start(name, cmd, cmdArgs, options = {}) {
  console.log(`Starting ${name}`);
  const child = spawn(cmd, cmdArgs, {
    cwd: options.cwd || process.cwd(),
    env: { ...process.env, ...options.env },
    stdio: 'inherit',
    detached: process.platform !== 'win32',
    shell: false,
  });
  child.once('error', (error) => {
    console.error(`${name} failed to start: ${error.message}`);
  });
  return child;
}

function waitForExit(child, timeoutMs) {
  if (!child || child.exitCode !== null || child.signalCode !== null) {
    return Promise.resolve(true);
  }

  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      child.off('exit', onExit);
      resolve(false);
    }, timeoutMs);

    function onExit() {
      clearTimeout(timeout);
      resolve(true);
    }

    child.once('exit', onExit);
  });
}

async function stop(child) {
  if (!child?.pid) return;

  if (process.platform === 'win32') {
    if (child.exitCode !== null || child.signalCode !== null) return;
    await new Promise((resolve) => {
      const killer = spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], {
        stdio: 'ignore',
        shell: false,
      });
      killer.on('error', resolve);
      killer.on('exit', resolve);
    });
    await waitForExit(child, 5000);
    return;
  }

  try {
    process.kill(-child.pid, 'SIGTERM');
  } catch (error) {
    if (error.code !== 'ESRCH') {
      console.error(`Failed to send SIGTERM to process group ${child.pid}: ${error.message}`);
    }
  }

  if (await waitForExit(child, 5000)) {
    return;
  }

  try {
    process.kill(-child.pid, 'SIGKILL');
  } catch (error) {
    if (error.code !== 'ESRCH') {
      console.error(`Failed to send SIGKILL to process group ${child.pid}: ${error.message}`);
    }
  }
  await waitForExit(child, 5000);
}

const started = [];

try {
  process.env.MARGINS_MYSQL_PORT = mysqlPort;
  process.env.SPRING_PROFILES_ACTIVE = process.env.SPRING_PROFILES_ACTIVE || 'local';
  process.env.SERVER_PORT = backendPort;
  process.env.MARGINS_BACKEND_URL = `http://localhost:${backendPort}`;
  process.env.MARGINS_FRONTEND_PORT = frontendPort;
  process.env.MARGINS_FRONT_URL = `http://localhost:${frontendPort}`;

  const backendAlreadyOpen = await tcpOpen(backendPort);
  const frontendAlreadyOpen = await tcpOpen(frontendPort);
  if ((backendAlreadyOpen || frontendAlreadyOpen) && !reuseExisting) {
    throw new Error(`Backend/frontend port already in use. Stop stale services or pass --reuse-existing-services explicitly. Ports: ${backendPort}, ${frontendPort}`);
  }
  if (reuseExisting && backendAlreadyOpen && !(await httpOk(`http://localhost:${backendPort}/api/health`))) {
    throw new Error(`Backend port ${backendPort} is occupied but did not answer the expected health check.`);
  }
  if (reuseExisting && frontendAlreadyOpen && !(await httpOk(`http://localhost:${frontendPort}`))) {
    throw new Error(`Frontend port ${frontendPort} is occupied but did not answer the expected HTTP check.`);
  }

  await run('node', ['scripts/local.mjs', 'db-up', '--apply-schema']);

  if (!backendAlreadyOpen) {
    const backend = start('backend', 'node', ['scripts/local.mjs', 'back-dev'], {
      env: {
        MARGINS_MYSQL_PORT: mysqlPort,
        SERVER_PORT: backendPort,
        SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE,
      },
    });
    started.push(backend);
  }
  await waitHttp('Backend', `http://localhost:${backendPort}/api/health`);

  if (!frontendAlreadyOpen) {
    const frontend = start('frontend', bin('npm'), ['run', 'dev'], {
      cwd: 'front',
      env: {
        MARGINS_BACKEND_URL: `http://localhost:${backendPort}`,
        MARGINS_FRONTEND_PORT: frontendPort,
      },
    });
    started.push(frontend);
  }
  await waitHttp('Frontend', `http://localhost:${frontendPort}`);

  await run(bin('npm'), ['run', 'e2e'], {
    cwd: 'front',
    env: {
      MARGINS_BACKEND_URL: `http://localhost:${backendPort}`,
      MARGINS_FRONTEND_PORT: frontendPort,
      MARGINS_FRONT_URL: `http://localhost:${frontendPort}`,
    },
  });
  console.log('PASS: full-stack E2E completed.');
} finally {
  if (!keepStarted) {
    for (const child of started.reverse()) {
      await stop(child);
    }
  }
}
