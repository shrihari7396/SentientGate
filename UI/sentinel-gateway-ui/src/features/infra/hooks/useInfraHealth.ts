import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';

export function useInfraHealth() {
  const kafka = useQuery({ queryKey: ['infra', 'kafka'], queryFn: () => apiClient.get('/infra/kafka').then(r => r.data), refetchInterval: 5000 });
  const redis = useQuery({ queryKey: ['infra', 'redis'], queryFn: () => apiClient.get('/infra/redis').then(r => r.data), refetchInterval: 5000 });
  const postgres = useQuery({ queryKey: ['infra', 'postgres'], queryFn: () => apiClient.get('/infra/postgres').then(r => r.data), refetchInterval: 5000 });

  return { kafka, redis, postgres };
}
