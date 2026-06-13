import { createRoot } from 'react-dom/client';
import { LoginGate } from './components/views/LoginGate';
import './index.css';

createRoot(document.getElementById('root') as HTMLElement).render(<LoginGate />);
