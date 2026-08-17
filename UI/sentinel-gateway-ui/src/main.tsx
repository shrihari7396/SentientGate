import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './app/App';
import './index.css';

async function deferRender() {
  // Only start MSW mock service worker when explicitly enabled via env flag
  if (import.meta.env.VITE_USE_MOCKS !== 'true') {
    return;
  }
  
  const { worker } = await import('./mocks/browser');
  return worker.start({
    onUnhandledRequest: 'bypass',
  });
}

deferRender().then(() => {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
});
