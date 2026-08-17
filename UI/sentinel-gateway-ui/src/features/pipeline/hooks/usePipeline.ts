import { apiClient } from '@/shared/api/client';
import { usePolling } from '@/shared/hooks/usePolling';

export interface PipelineStage {
  stage: string;
  requestsToday: number;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  lastError: string | null;
}

export interface PipelineEvent {
  id: string;
  timestamp: string;
  uuid: string;
  status: 'PASSED' | 'BLOCKED';
  stage: string;
}

export function usePipeline() {
  const stats = usePolling(['pipeline', 'stats'], () => apiClient.get('/pipeline/stats').then(r => r.data), 5000);
  const events = usePolling(['pipeline', 'events'], () => apiClient.get('/pipeline/events').then(r => r.data), 2000);

  return { stats, events };
}
