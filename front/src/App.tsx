import { createRoot } from 'react-dom/client';
import { LoginGate } from './components/views/LoginGate';
import { I18nProvider } from './i18n';
import './index.css';

createRoot(document.getElementById('root') as HTMLElement).render(
  <I18nProvider>
    <LoginGate />
  </I18nProvider>,
);
