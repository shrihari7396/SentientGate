
import { apiClient } from '@/shared/api/client';
import { usePolling } from '@/shared/hooks/usePolling';

export interface LogEntry {
  id: string;
  timestamp: string;
  uuid: string;
  method: string;
  endpoint: string;
  status: number;
  latency: number;
  routeId: string;
  threatFlagged: boolean;
  headers?: Record<string, string>;
  kafkaEventId?: string;
  threatDetails?: any;
  blacklistStatus?: any;
}

export function useLogs(
  pathFilter: string,
  statusFilter: string,
  uuidFilter: string,
  isLive: boolean
) {
  const fetchLogs = async () => {
    const params = new URLSearchParams();
    if (pathFilter) params.append('path', pathFilter);
    if (statusFilter) params.append('status', statusFilter);
    if (uuidFilter) params.append('uuid', uuidFilter);
    params.append('page', '0');
    params.append('size', '50');
    params.append('sortBy', 'occurredAt');
    params.append('direction', 'DESC');
    
    return apiClient.get('/logs', { params }).then(r => r.data.content as LogEntry[]);
  };

  const queryKey = ['logs', pathFilter, statusFilter, uuidFilter];

  const pollingData = usePolling(queryKey, fetchLogs, isLive ? 2000 : 0);

  // When live mode is OFF (0 interval doesn't trigger polling), just rely on the query's initial fetch
  return pollingData;
}
