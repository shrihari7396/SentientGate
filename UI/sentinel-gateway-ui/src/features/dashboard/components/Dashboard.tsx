import { MetricCard } from '@/shared/components/MetricCard';
import { useDashboardMetrics } from '../hooks/useDashboardMetrics';
import { AreaChart, Area, XAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { CopyableUUID } from '@/shared/components/CopyableUUID';
import { TTLCountdown } from '@/shared/components/TTLCountdown';
import { StatusBadge } from '@/shared/components/StatusBadge';
import { AlertTriangle } from 'lucide-react';
import { motion } from 'framer-motion';

const THREAT_COLORS = {
  'Rate Anomaly': '#F59E0B',
  'Pattern Repeat': '#EF4444',
  'Suspicious Access': '#58A6FF',
  'AI Escalation': '#A855F7',
};

export default function Dashboard() {
  const { metrics, traffic, threatDist, recentBlocks } = useDashboardMetrics();

  const mData = metrics.data;
  const tData = traffic.data || [];
  const distData = threatDist.data || [];
  const rbData = recentBlocks.data || [];

  return (
    <motion.div 
      className="p-6 max-w-[1600px] mx-auto space-y-6"
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
    >
      <header className="mb-8">
        <h1 className="text-2xl font-sans font-semibold text-text-primary">Command Center</h1>
        <p className="text-sm text-text-muted mt-1">Real-time gateway telemetry and threat metrics.</p>
      </header>

      {/* Top row metric cards */}
      <motion.div 
        className="grid grid-cols-1 md:grid-cols-4 gap-4"
        initial="hidden" animate="visible"
        variants={{ visible: { transition: { staggerChildren: 0.05 } } }}
      >
        <MetricCard 
          label="Requests / min" 
          value={mData?.requestsPerMin ?? 0} 
          accentColor="teal" 
        />
        <MetricCard 
          label="Blocked threats" 
          value={mData?.blockedThreats ?? 0} 
          accentColor="red" 
        />
        <MetricCard 
          label="P99 Latency" 
          value={mData?.p99Latency ?? 0} 
          unit="ms" 
          accentColor="amber" 
        />
        <MetricCard 
          label="Active services" 
          value={mData?.activeServices ?? 0} 
          accentColor="blue" 
        />
      </motion.div>

      <div className="grid grid-cols-1 lg:grid-cols-10 gap-6">
        {/* Traffic Velocity Chart */}
        <div className="lg:col-span-6 bg-surface border border-border rounded p-5 relative overflow-hidden bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMiIgY3k9IjIiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wMykiLz48L3N2Zz4=')]">
          <h3 className="text-xs font-semibold tracking-widest uppercase text-text-muted mb-6">Traffic Velocity</h3>
          <div className="h-[280px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={tData} margin={{ top: 5, right: 0, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorCore" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#00E5CC" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#00E5CC" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorThreat" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#EF4444" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#EF4444" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="timestamp" stroke="#30363D" tick={{fill: '#7D8590', fontSize: 12, fontFamily: 'JetBrains Mono'}} tickLine={false} axisLine={false} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#1C2128', borderColor: '#30363D', borderRadius: '4px', fontFamily: 'JetBrains Mono', fontSize: '12px' }}
                  itemStyle={{ color: '#E6EDF3' }}
                />
                <Area type="monotone" dataKey="coreFlow" stroke="#00E5CC" fillOpacity={1} fill="url(#colorCore)" isAnimationActive={false} />
                <Area type="monotone" dataKey="threatVectors" stroke="#EF4444" fillOpacity={1} fill="url(#colorThreat)" isAnimationActive={false} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Threat Distribution */}
        <div className="lg:col-span-4 bg-surface border border-border rounded p-5 flex flex-col">
          <h3 className="text-xs font-semibold tracking-widest uppercase text-text-muted mb-4">Threat Distribution</h3>
          <div className="flex-1 flex items-center justify-center relative">
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie
                  data={distData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={2}
                  dataKey="count"
                  stroke="none"
                >
                  {distData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={THREAT_COLORS[entry.type as keyof typeof THREAT_COLORS] || '#7D8590'} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: '#1C2128', borderColor: '#30363D', borderRadius: '4px', fontFamily: 'JetBrains Mono', fontSize: '12px' }}
                  itemStyle={{ color: '#E6EDF3' }}
                />
              </PieChart>
            </ResponsiveContainer>
            <div className="absolute inset-0 flex items-center justify-center flex-col pointer-events-none">
              <span className="text-3xl font-mono font-semibold text-text-primary">
                {distData.reduce((acc, val) => acc + val.count, 0)}
              </span>
              <span className="text-xs text-text-muted">Total</span>
            </div>
          </div>
          <div className="mt-4 grid grid-cols-2 gap-2">
             {distData.map(d => (
               <div key={d.type} className="flex items-center gap-2 text-xs">
                 <div className="w-2 h-2 rounded-full" style={{backgroundColor: THREAT_COLORS[d.type as keyof typeof THREAT_COLORS]}}></div>
                 <span className="text-text-muted">{d.type}</span>
               </div>
             ))}
          </div>
        </div>
      </div>

      {/* Recent Blocked Events */}
      <div className="bg-surface border border-border rounded overflow-hidden">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <h3 className="text-xs font-semibold tracking-widest uppercase text-text-muted flex items-center gap-2">
            <AlertTriangle size={14} className="text-red" />
            Recent Blocked Events
          </h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-border/50 bg-elevated/50">
                <th className="p-3 text-xs font-semibold text-text-muted w-1/4">UUID</th>
                <th className="p-3 text-xs font-semibold text-text-muted w-1/4">Reason</th>
                <th className="p-3 text-xs font-semibold text-text-muted w-1/4">Blocked At</th>
                <th className="p-3 text-xs font-semibold text-text-muted w-1/4">TTL</th>
              </tr>
            </thead>
            <tbody className="text-sm font-mono text-text-primary divide-y divide-border/50">
              {rbData.length === 0 ? (
                <tr>
                  <td colSpan={4} className="p-4 text-center text-text-muted text-sm font-sans italic">
                    No recent blocks. Gateway is clean.
                  </td>
                </tr>
              ) : (
                rbData.map(block => (
                  <tr key={block.uuid} className="hover:bg-elevated transition-colors">
                    <td className="p-3"><CopyableUUID uuid={block.uuid} /></td>
                    <td className="p-3">
                       <StatusBadge status={block.reason === 'AI_ESCALATION' || block.reason === 'PATTERN_REPEAT' ? 'BLOCKED' : 'MONITORING'} />
                       <span className="ml-2 text-xs text-text-muted">{block.reason}</span>
                    </td>
                    <td className="p-3 text-text-muted">{new Date(block.blockedAt).toLocaleTimeString()}</td>
                    <td className="p-3"><TTLCountdown ttlSeconds={block.ttlSeconds} /></td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </motion.div>
  );
}
