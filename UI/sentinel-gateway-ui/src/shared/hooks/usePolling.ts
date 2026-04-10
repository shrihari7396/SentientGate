import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';

export function usePolling<T>(
  queryKey: string[],
  fetchFn: () => Promise<T>,
  intervalMs: number = 3000
) {
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  const query = useQuery({
    queryKey,
    queryFn: fetchFn,
    refetchInterval: intervalMs,
    refetchIntervalInBackground: false,
  });

  useEffect(() => {
    if (query.data) {
      setLastUpdated(new Date());
    }
  }, [query.data]);

  return {
    data: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    lastUpdated,
    refetch: query.refetch,
  };
}
