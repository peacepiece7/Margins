import { FormEvent, useEffect, useState } from 'react';
import { marginsRepository } from '../../repository/marginsRepository';
import type { LoginResponse } from '../../types/models/auth';
import { testAttr } from '../../utils/testAttrs';
import { SessionWorkbench } from './SessionWorkbench';

const AUTH_STORAGE_KEY = 'margins.auth';

function readAuthSession() {
  if (typeof window === 'undefined') {
    return undefined;
  }

  const value = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!value) {
    return undefined;
  }

  try {
    return JSON.parse(value) as LoginResponse;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return undefined;
  }
}

export function LoginGate() {
  const [authSession, setAuthSession] = useState<LoginResponse | undefined>();
  const [username, setUsername] = useState('test-reader');
  const [password, setPassword] = useState('reader');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => {
    setAuthSession(readAuthSession());
  }, []);

  async function submitLogin(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(undefined);

    try {
      const result = await marginsRepository.login(username.trim(), password.trim());
      window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(result));
      setAuthSession(result);
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    window.localStorage.removeItem('margins.selectedSessionId');
    setAuthSession(undefined);
  }

  if (authSession) {
    return (
      <>
        <div className="border-b border-stone-300 bg-white px-5 py-2" {...testAttr('auth-session-bar')}>
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 text-sm">
            <div>
              <span className="font-medium">{authSession.displayName}</span>
              <span className="ml-2 text-stone-500">{authSession.authMode}</span>
            </div>
            <button
              className="rounded border border-stone-300 px-3 py-1.5 text-xs font-medium hover:border-stone-700"
              onClick={logout}
              type="button"
              {...testAttr('logout-submit')}
            >
              Logout
            </button>
          </div>
        </div>
        <SessionWorkbench />
      </>
    );
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center px-5 py-8">
      <form className="grid gap-4 rounded border border-stone-300 bg-white p-5" onSubmit={submitLogin} {...testAttr('login-form')}>
        <div>
          <h1 className="text-2xl font-semibold">Margins</h1>
          <p className="text-sm text-stone-600">Reading record workspace</p>
        </div>
        {error && (
          <div className="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-800" {...testAttr('login-error')}>
            {error}
          </div>
        )}
        <input
          className="rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
          onChange={(event) => setUsername(event.target.value)}
          placeholder="Username"
          value={username}
          {...testAttr('login-username-input')}
        />
        <input
          className="rounded border border-stone-300 px-3 py-2 text-sm outline-none focus:border-stone-700"
          onChange={(event) => setPassword(event.target.value)}
          placeholder="Password"
          type="password"
          value={password}
          {...testAttr('login-password-input')}
        />
        <button
          className="rounded bg-stone-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
          disabled={loading || !username.trim() || !password.trim()}
          type="submit"
          {...testAttr('login-submit')}
        >
          Login
        </button>
      </form>
    </main>
  );
}
