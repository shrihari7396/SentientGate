import { usePolling } from '@/shared/hooks/usePolling';
import { apiClient } from '@/shared/api/client';

export interface DashboardMetrics {
  requestsPerMin: number;
  blockedThreats: number;
  p99Latency: string;
  activeServices: number;
}

export interface TrafficData {
  timestamp: string;
  coreFlow: number;
  threatVectors: number;
}

export interface ThreatDist {
  type: string;
  count: number;
}

export interface RecentBlock {
  uuid: string;
  reason: string;
  blockedAt: string;
  ttlSeconds: number;
}

async function fetchMetrics(): Promise<DashboardMetrics> {
  return apiClient.get('/dashboard/metrics').then(r => r.data);
}

async function fetchTraffic(): Promise<TrafficData[]> {
  return apiClient.get('/dashboard/traffic').then(r => r.data);
}

async function fetchThreatDist(): Promise<ThreatDist[]> {
  return apiClient.get('/dashboard/threat-dist').then(r => r.data);
}

async function fetchRecentBlocks(): Promise<RecentBlock[]> {
  return apiClient.get('/dashboard/recent-blocks').then(r => r.data);
}

export function useDashboardMetrics() {
  const metrics = usePolling(['dashboard', 'metrics'], fetchMetrics, 3000);
  const traffic = usePolling(['dashboard', 'traffic'], fetchTraffic, 5000);
  const threatDist = usePolling(['dashboard', 'threat-dist'], fetchThreatDist, 5000);
  const recentBlocks = usePolling(['dashboard', 'recent-blocks'], fetchRecentBlocks, 3000);

  return { metrics, traffic, threatDist, recentBlocks };
}
