import { useEffect, useState } from 'react';
import clsx from 'clsx';
interface MetricCardProps {
  label: string;
  value: string | number;
  unit?: string;
  trend?: number[];
  accentColor?: 'teal' | 'amber' | 'red' | 'blue';
}


const hexMap = {
  teal: '#00E5CC',
  amber: '#F59E0B',
  red: '#EF4444',
  blue: '#58A6FF',
};

export function MetricCard({ label, value, unit, trend, accentColor = 'teal' }: MetricCardProps) {
  const [isFlashing, setIsFlashing] = useState(false);

  useEffect(() => {
    setIsFlashing(true);
    const timer = setTimeout(() => setIsFlashing(false), 400);
    return () => clearTimeout(timer);
  }, [value]);

  const textColor = `text-${accentColor}`;

  return (
    <div
      className={clsx(
        'relative bg-surface border border-border rounded p-4 overflow-hidden transition-colors',
        isFlashing && 'flash-teal'
      )}
      style={isFlashing ? { backgroundColor: `${hexMap[accentColor]}33` } : undefined}
    >
      <div className="flex justify-between items-start">
        <h3 className="text-xs uppercase tracking-widest text-muted">{label}</h3>
        {trend && trend.length > 0 && (
          <div className="h-5 w-10">
            {/* Using a simple custom SVG implementation for sparkline or Recharts */}
            <svg viewBox="0 0 40 20" className="w-full h-full overflow-visible">
              <polyline
                fill="none"
                stroke={hexMap[accentColor]}
                strokeWidth="1.5"
                points={trend.map((val, i) => `${(i / (trend.length - 1)) * 40},${20 - (val / Math.max(...trend)) * 20}`).join(' ')}
              />
            </svg>
          </div>
        )}
      </div>
      
      <div className="mt-2 flex items-baseline gap-1.5">
        <span className={clsx('text-3xl font-mono font-semibold', textColor)}>{value}</span>
        {unit && <span className="text-sm text-muted">{unit}</span>}
      </div>
    </div>
  );
}
