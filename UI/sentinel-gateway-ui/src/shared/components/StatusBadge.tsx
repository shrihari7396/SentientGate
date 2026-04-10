import { ReactNode } from 'react';
import clsx from 'clsx';

export type StatusType = 'BLOCKED' | 'ALLOWED' | 'MONITORING' | 'UP' | 'DOWN';

export function StatusBadge({ status }: { status: StatusType }) {
  const isRed = status === 'BLOCKED' || status === 'DOWN';
  const isGreen = status === 'ALLOWED' || status === 'UP';
  const isAmber = status === 'MONITORING';

  return (
    <span
      className={clsx(
        'text-xs font-mono font-semibold px-2 py-0.5 rounded-sm uppercase',
        isRed && 'bg-red/20 text-red',
        isGreen && 'bg-teal/20 text-teal',
        isAmber && 'bg-amber/20 text-amber'
      )}
    >
      {status}
    </span>
  );
}
