import { FormEvent, useEffect, useState } from 'react';
import { useI18n } from '../../i18n';
import { marginsRepository } from '../../repository/marginsRepository';
import type { LoginResponse } from '../../types/models/auth';
import { testAttr } from '../../utils/testAttrs';
import { ReadingPortal } from './ReadingPortal';

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
  const { locale, setLocale, t } = useI18n();
  const [authSession, setAuthSession] = useState<LoginResponse | undefined>();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
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
      setError(loginError instanceof Error ? loginError.message : t('loginFailed'));
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
        <div className="border-b border-stone-300/80 bg-stone-50/95 px-5 py-2 backdrop-blur" {...testAttr('auth-session-bar')}>
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 text-sm">
            <div>
              <span className="font-medium">{authSession.displayName}</span>
              <span className="ml-2 text-stone-500">{t('authMode')}: {authSession.authMode}</span>
            </div>
            <div className="flex items-center gap-2">
              <LanguageToggle locale={locale} setLocale={setLocale} label={t('language')} />
              <button
                className="rounded border border-stone-300 bg-white px-3 py-1.5 text-xs font-medium hover:border-stone-700"
                onClick={logout}
                type="button"
                {...testAttr('logout-submit')}
              >
                {t('logout')}
              </button>
            </div>
          </div>
        </div>
        <ReadingPortal />
      </>
    );
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center px-5 py-8">
      <form className="grid gap-5 rounded border border-stone-300 bg-stone-50/95 p-6 shadow-[0_24px_80px_rgba(23,23,23,0.10)]" onSubmit={submitLogin} {...testAttr('login-form')}>
        <div>
          <div className="flex items-start justify-between gap-3">
            <h1 className="font-display text-5xl font-semibold tracking-normal">Margins</h1>
            <LanguageToggle locale={locale} setLocale={setLocale} label={t('language')} />
          </div>
          <p className="mt-3 text-sm leading-6 text-stone-600">{t('loginSubtitle')}</p>
        </div>
        {error && (
          <div className="rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-800" {...testAttr('login-error')}>
            {error}
          </div>
        )}
        <input
          className="rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
          onChange={(event) => setUsername(event.target.value)}
          placeholder={t('username')}
          value={username}
          {...testAttr('login-username-input')}
        />
        <input
          className="rounded border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-stone-700"
          onChange={(event) => setPassword(event.target.value)}
          placeholder={t('password')}
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
          {t('login')}
        </button>
      </form>
    </main>
  );
}

interface LanguageToggleProps {
  label: string;
  locale: 'en' | 'ko';
  setLocale: (locale: 'en' | 'ko') => void;
}

function LanguageToggle({ label, locale, setLocale }: LanguageToggleProps) {
  return (
    <div className="inline-grid grid-cols-2 rounded border border-stone-300 bg-white p-0.5" aria-label={label}>
      {(['en', 'ko'] as const).map((option) => (
        <button
          aria-pressed={locale === option}
          className={`min-h-7 min-w-9 rounded px-2 text-xs font-semibold ${locale === option ? 'bg-stone-950 text-white' : 'text-stone-500 hover:text-stone-950'}`}
          key={option}
          onClick={() => setLocale(option)}
          type="button"
        >
          {option.toUpperCase()}
        </button>
      ))}
    </div>
  );
}
