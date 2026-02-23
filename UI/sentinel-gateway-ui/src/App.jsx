import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import LogsView from './pages/LogsView';
import BlacklistView from './pages/BlacklistView';
import PipelineView from './pages/PipelineView';
import './index.css';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="logs" element={<LogsView />} />
          <Route path="blacklist" element={<BlacklistView />} />
          <Route path="flow" element={<PipelineView />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
