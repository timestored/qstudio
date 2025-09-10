import React from 'react';
import ReactDOM from 'react-dom/client';
import DashboardApp from './components/DashboardApp';

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

root.render(
  <React.StrictMode>
    <DashboardApp />
  </React.StrictMode>
);