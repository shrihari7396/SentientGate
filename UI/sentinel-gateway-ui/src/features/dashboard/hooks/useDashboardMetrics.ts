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

/**
 * Fetches dashboard metrics from the GatewayMetricsController.
 * Falls back to LoggingService stats to supplement gateway-only data.
 */
async function fetchMetrics(): Promise<DashboardMetrics> {
  // Primary: gateway metrics (blacklist count from Redis)
  const gatewayMetrics = await apiClient.get('/dashboard/metrics').then(r => r.data);

  // Supplement with LoggingService stats if available
  try {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
    const stats = await apiClient.get('/logs/stats/summary', {
      params: { start: oneHourAgo.toISOString(), end: now.toISOString() }
    }).then(r => r.data);

    return {
      requestsPerMin: stats.throughput || gatewayMetrics.requestsPerMin,
      blockedThreats: gatewayMetrics.blockedThreats || stats.securityBlocks,
      p99Latency: stats.p99Latency ? String(stats.p99Latency.toFixed(1)) : gatewayMetrics.p99Latency,
      activeServices: gatewayMetrics.activeServices,
    };
  } catch {
    // LoggingService unavailable, return gateway-only data
    return gatewayMetrics;
  }
}

/**
 * Fetches traffic velocity data from LoggingService via gateway.
 * Maps TimeBucketStats to the TrafficData format the chart expects.
 */
async function fetchTraffic(): Promise<TrafficData[]> {
  const now = new Date();
  const twentyMinAgo = new Date(now.getTime() - 20 * 60 * 1000);

  try {
    const data = await apiClient.get('/logs/stats/velocity', {
      params: { start: twentyMinAgo.toISOString(), end: now.toISOString() }
    }).then(r => r.data);

    return data.map((bucket: any) => ({
      timestamp: new Date(bucket.minute).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      coreFlow: bucket.requestCount || 0,
      threatVectors: (bucket.rateLimitedCount || 0) + (bucket.errorCount || 0),
    }));
  } catch {
    return [];
  }
}

/**
 * Fetches threat distribution from the blacklist.
 * Aggregates by blocking reason to build the pie chart data.
 */
async function fetchThreatDist(): Promise<ThreatDist[]> {
  try {
    const entries = await apiClient.get('/threat/blacklist').then(r => r.data);
    const counts: Record<string, number> = {};
    entries.forEach((entry: any) => {
      const reason = entry.reason || 'UNKNOWN';
      counts[reason] = (counts[reason] || 0) + 1;
    });
    return Object.entries(counts).map(([type, count]) => ({ type, count }));
  } catch {
    return [];
  }
}

/**
 * Fetches recently blocked entries from the blacklist.
 */
async function fetchRecentBlocks(): Promise<RecentBlock[]> {
  try {
    return await apiClient.get('/threat/blacklist').then(r => r.data);
  } catch {
    return [];
  }
}

export function useDashboardMetrics() {
  const metrics = usePolling(['dashboard', 'metrics'], fetchMetrics, 3000);
  const traffic = usePolling(['dashboard', 'traffic'], fetchTraffic, 5000);
  const threatDist = usePolling(['dashboard', 'threat-dist'], fetchThreatDist, 5000);
  const recentBlocks = usePolling(['dashboard', 'recent-blocks'], fetchRecentBlocks, 3000);

  return { metrics, traffic, threatDist, recentBlocks };
}
