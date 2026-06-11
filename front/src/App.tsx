import { createRoot } from 'react-dom/client';
import { SessionWorkbench } from './components/views/SessionWorkbench';
import './index.css';

createRoot(document.getElementById('root') as HTMLElement).render(<SessionWorkbench />);
