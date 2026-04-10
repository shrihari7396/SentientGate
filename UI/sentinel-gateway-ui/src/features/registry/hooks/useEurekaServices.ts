import { apiClient } from '@/shared/api/client';
import { usePolling } from '@/shared/hooks/usePolling';
import { useQuery } from '@tanstack/react-query';

export interface EurekaService {
  id: string;
  name: string;
  instanceCount: number;
  status: 'UP' | 'DOWN' | 'STARTING';
}

export interface EurekaInstance {
  host: string;
  port: number;
  status: string;
  homepage: string;
  lastHeartbeat: string;
}

export function useEurekaServices() {
  const services = usePolling(['registry', 'services'], () => apiClient.get('/registry/services').then(r => r.data), 10000);
  return { services };
}

export function useEurekaInstances(serviceId: string | null) {
  return useQuery({
    queryKey: ['registry', 'instances', serviceId],
    queryFn: () => apiClient.get(`/registry/services/${serviceId}`).then(r => r.data as EurekaInstance[]),
    enabled: !!serviceId,
  });
}

export function useActuatorHealth(serviceId: string | null) {
  return useQuery({
    queryKey: ['registry', 'actuator', serviceId],
    queryFn: () => apiClient.get(`/registry/actuator/${serviceId}/health`).then(r => r.data),
    enabled: !!serviceId,
  });
}
