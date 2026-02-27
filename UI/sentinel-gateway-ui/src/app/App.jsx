import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';

import Layout from '../shared/components/Layout';
import Dashboard from '../features/dashboard/Dashboard';
import LogsView from '../features/logs/LogsView';
import BlacklistView from '../features/blacklist/BlacklistView';
import PipelineView from '../features/pipeline/PipelineView';
import InfrastructureView from '../features/infrastructure/InfrastructureView';
import ServiceRegistryView from '../features/registry/ServiceRegistryView';
import ServiceDetailView from '../features/registry/ServiceDetailView';
import KafkaView from '../features/kafka/KafkaView';
import RedisView from '../features/redis/RedisView';
import PostgresView from '../features/postgres/PostgresView';
import '../index.css';

// Create a client
const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="logs" element={<LogsView />} />
            <Route path="blacklist" element={<BlacklistView />} />
            <Route path="flow" element={<PipelineView />} />
            <Route path="infrastructure" element={<InfrastructureView />} />
            <Route path="infrastructure/kafka" element={<KafkaView />} />
            <Route path="infrastructure/redis" element={<RedisView />} />
            <Route path="infrastructure/postgres" element={<PostgresView />} />
            <Route path="registry" element={<ServiceRegistryView />} />
            <Route path="infrastructure/service/:serviceName" element={<ServiceDetailView />} />
          </Route>
        </Routes>
      </BrowserRouter>
      <Toaster theme="dark" position="top-right" richColors />
    </QueryClientProvider>
  );
}

export default App;
