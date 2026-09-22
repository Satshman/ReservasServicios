import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './estilos.css';

const raiz = document.getElementById('root');
if (!raiz) {
  throw new Error('No se encontró el nodo #root en index.html');
}

createRoot(raiz).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
