import clsx from 'clsx';

export type StatusType = 'BLOCKED' | 'ALLOWED' | 'MONITORING' | 'UP' | 'DOWN' | 'DEGRADED' | 'STARTING' | 'BLACKLISTED';

export function StatusBadge({ status }: { status: StatusType | string }) {
  const s = status as string;
  const isRed = s === 'BLOCKED' || s === 'DOWN' || s === 'BLACKLISTED';
  const isGreen = s === 'ALLOWED' || s === 'UP';
  const isAmber = s === 'MONITORING' || s === 'DEGRADED' || s === 'STARTING';

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
