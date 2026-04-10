import { usePolling } from '@/shared/hooks/usePolling';
import { apiClient } from '@/shared/api/client';

export interface Strategy {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  lastFired: string;
  firesToday: number;
}

export interface AnomalyEvent {
  uuid: string;
  timestamp: string;
  source: 'HEURISTIC' | 'AI_MODEL';
  strategyFired?: string;
  anomalyScore: number;
  decision: 'BLACKLISTED' | 'ALLOWED' | 'MONITORING';
  context?: string;
  reasoningText?: string;
}

export interface BlacklistEntry {
  uuid: string;
  reason: string;
  blockedAt: string;
  ttlSeconds: number;
}

export function useThreatIntel() {
  const stats = usePolling(['threat', 'stats'], () => apiClient.get('/threat/stats').then(r => r.data), 10000);
  const strategies = usePolling(['threat', 'strategies'], () => apiClient.get('/threat/strategies').then(r => r.data), 5000);
  const feed = usePolling(['threat', 'feed'], () => apiClient.get('/threat/feed').then(r => r.data), 2000);
  const blacklist = usePolling(['threat', 'blacklist'], () => apiClient.get('/threat/blacklist').then(r => r.data), 5000);

  const toggleStrategy = async (id: string) => {
    await apiClient.post(`/threat/strategies/${id}/toggle`);
    strategies.refetch();
  };

  const unblockUuid = async (uuid: string) => {
    await apiClient.delete(`/threat/blacklist/${uuid}`);
    blacklist.refetch();
  };

  return { stats, strategies, feed, blacklist, toggleStrategy, unblockUuid };
}
