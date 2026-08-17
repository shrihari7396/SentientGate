import { useEffect, useState } from 'react';
import clsx from 'clsx';
import { StatusBadge } from './StatusBadge';

export function TTLCountdown({ ttlSeconds }: { ttlSeconds: number }) {
  const [timeLeft, setTimeLeft] = useState(ttlSeconds);

  useEffect(() => {
    setTimeLeft(ttlSeconds);
  }, [ttlSeconds]);

  useEffect(() => {
    if (timeLeft <= 0) return;
    
    const timer = setInterval(() => {
      setTimeLeft(prev => Math.max(0, prev - 1));
    }, 1000);
    
    return () => clearInterval(timer);
  }, [timeLeft]);

  if (timeLeft === 0) {
    return <StatusBadge status="ALLOWED" />;
  }

  const mins = Math.floor(timeLeft / 60);
  const secs = timeLeft % 60;
  const isExpiring = timeLeft < 60;

  return (
    <span className={clsx('font-mono', isExpiring ? 'text-red' : 'text-text-primary')}>
      {mins}m {secs}s
    </span>
  );
}
