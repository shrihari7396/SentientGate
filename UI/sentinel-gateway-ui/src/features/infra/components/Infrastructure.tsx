import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useInfraHealth } from '../hooks/useInfraHealth';
import { StatusBadge } from '@/shared/components/StatusBadge';
import { Server, Database, Activity, MessagesSquare, LayoutGrid, TerminalSquare } from 'lucide-react';
import clsx from 'clsx';
import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';

export default function Infrastructure() {
  const { kafka, redis, postgres } = useInfraHealth();
  const [activeTab, setActiveTab] = useState<'KAFKA' | 'REDIS' | 'POSTGRES'>('KAFKA');

  const kData = kafka.data;
  const rData = redis.data;
  const pData = postgres.data;

  return (
    <motion.div 
      className="p-6 max-w-[1600px] mx-auto h-full flex flex-col"
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
    >
      <header className="mb-6 flex justify-between items-end">
        <div>
          <h1 className="text-2xl font-sans font-semibold text-text-primary flex items-center gap-3">
            <Server size={28} className="text-blue" /> Infrastructure
          </h1>
          <p className="text-sm text-text-muted mt-1">Deep health monitoring of Kafka, Redis, PostgreSQL.</p>
        </div>
      </header>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-border/50 mb-6 shrink-0">
         {(['KAFKA', 'REDIS', 'POSTGRES'] as const).map(tab => (
           <button 
             key={tab}
             onClick={() => setActiveTab(tab)}
             className={clsx(
               "px-6 py-3 text-sm font-semibold tracking-widest uppercase transition-colors relative",
               activeTab === tab ? "text-teal" : "text-text-muted hover:text-text-primary"
             )}
           >
             {tab}
             {activeTab === tab && (
               <motion.div layoutId="infra-tab-indicator" className="absolute bottom-0 left-0 right-0 h-0.5 bg-teal"></motion.div>
             )}
           </button>
         ))}
      </div>

      <div className="flex-1 overflow-y-auto">
        <AnimatePresence mode="wait">
          
          {/* KAFKA TAB */}
          {activeTab === 'KAFKA' && (
            <motion.div key="kafka" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-6">
               <div className="bg-surface border border-border rounded p-6">
                 <div className="flex justify-between items-center mb-6">
                   <h2 className="text-lg font-sans font-semibold text-text-primary flex items-center gap-2">
                     <MessagesSquare className="text-teal" size={20} /> Kafka Event Bus
                   </h2>
                   <StatusBadge status={kData?.status || 'UP'} />
                 </div>

                 <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                   <div>
                     <h3 className="text-xs font-semibold uppercase text-text-muted mb-3 flex items-center gap-2">
                       <LayoutGrid size={14} /> Active Topics
                     </h3>
                     <table className="w-full text-left bg-elevated rounded overflow-hidden">
                       <thead className="bg-background">
                         <tr>
                           <th className="p-2 text-[10px] text-text-muted uppercase tracking-wider">Topic</th>
                           <th className="p-2 text-[10px] text-text-muted uppercase tracking-wider">Partitions</th>
                           <th className="p-2 text-[10px] text-text-muted uppercase tracking-wider">Lag</th>
                           <th className="p-2 text-[10px] text-text-muted uppercase tracking-wider">Msg/s</th>
                         </tr>
                       </thead>
                       <tbody className="divide-y divide-border/50 text-xs font-mono text-text-primary">
                         {kData?.topics?.map((t: any) => (
                           <tr key={t.name}>
                             <td className="p-2">{t.name}</td>
                             <td className="p-2">{t.partitions}</td>
                             <td className="p-2 text-red">{t.lag}</td>
                             <td className="p-2">{t.throughput}</td>
                           </tr>
                         ))}
                       </tbody>
                     </table>
                   </div>
                   
                   <div>
                     <h3 className="text-xs font-semibold uppercase text-text-muted mb-3 flex items-center gap-2">
                       <TerminalSquare size={14} /> Recent Audit Log
                     </h3>
                     <div className="bg-elevated border border-border rounded h-[200px] p-3 overflow-y-auto font-mono text-[10px] whitespace-pre-wrap text-text-primary space-y-1">
                       {kData?.recentAudit?.map((log: string, i: number) => (
                         <div key={i} className="text-teal/80 border-b border-border/30 pb-1">{log}</div>
                       ))}
                     </div>
                   </div>
                 </div>
               </div>
            </motion.div>
          )}

          {/* REDIS TAB */}
          {activeTab === 'REDIS' && (
            <motion.div key="redis" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-6">
               <div className="bg-surface border border-border rounded p-6">
                 <div className="flex justify-between items-center mb-6">
                   <h2 className="text-lg font-sans font-semibold text-text-primary flex items-center gap-2">
                     <Database className="text-red" size={20} /> Redis Cache
                   </h2>
                   <div className="flex items-center gap-4">
                     <span className="text-xs font-mono text-text-muted">Latency: {rData?.latency}ms</span>
                     <StatusBadge status={rData?.status || 'UP'} />
                   </div>
                 </div>

                 <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                   <div className="bg-elevated border border-border p-4 rounded text-center">
                     <div className="text-2xl font-mono text-red mb-1">{rData?.blacklistKeys}</div>
                     <div className="text-[10px] uppercase text-text-muted">Blacklist Keys</div>
                   </div>
                   <div className="bg-elevated border border-border p-4 rounded text-center">
                     <div className="text-2xl font-mono text-teal mb-1">{rData?.tokenBuckets?.toLocaleString()}</div>
                     <div className="text-[10px] uppercase text-text-muted">Token Buckets</div>
                   </div>
                   <div className="bg-elevated border border-border p-4 rounded flex items-center justify-center relative">
                     <ResponsiveContainer width={100} height={100}>
                       <PieChart>
                         <Pie data={[{value: rData?.hitRatio || 0.9}, {value: Number((1 - (rData?.hitRatio || 0.9)).toFixed(2))}]} cx="50%" cy="50%" innerRadius={35} outerRadius={45} stroke="none" dataKey="value">
                           <Cell fill="#00E5CC" />
                           <Cell fill="#EF4444" />
                         </Pie>
                       </PieChart>
                     </ResponsiveContainer>
                     <div className="absolute flex flex-col items-center justify-center">
                       <span className="text-sm font-mono text-text-primary">{(rData?.hitRatio * 100)?.toFixed(1)}%</span>
                       <span className="text-[10px] text-text-muted">Hit Ratio</span>
                     </div>
                   </div>
                 </div>
               </div>
            </motion.div>
          )}

          {/* POSTGRES TAB */}
          {activeTab === 'POSTGRES' && (
            <motion.div key="postgres" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-6">
               <div className="bg-surface border border-border rounded p-6">
                 <div className="flex justify-between items-center mb-6">
                   <h2 className="text-lg font-sans font-semibold text-text-primary flex items-center gap-2">
                     <Database className="text-blue" size={20} /> PostgreSQL User Identity
                   </h2>
                   <StatusBadge status={pData?.status || 'UP'} />
                 </div>

                 <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                   <div className="bg-elevated border border-border p-4 rounded">
                     <h3 className="text-[10px] uppercase text-text-muted mb-4 flex items-center gap-2"><Activity size={12}/> Connection Pool</h3>
                     <div className="space-y-3">
                       <div>
                         <div className="flex justify-between text-xs font-mono mb-1">
                           <span className="text-teal">Active</span>
                           <span className="text-text-primary">{pData?.activeConnections}</span>
                         </div>
                         <div className="w-full bg-background rounded h-1"><div className="bg-teal h-full" style={{width: `${(pData?.activeConnections/pData?.maxConnections)*100}%`}}></div></div>
                       </div>
                       <div>
                         <div className="flex justify-between text-xs font-mono mb-1">
                           <span className="text-amber">Idle</span>
                           <span className="text-text-primary">{pData?.idleConnections}</span>
                         </div>
                         <div className="w-full bg-background rounded h-1"><div className="bg-amber h-full" style={{width: `${(pData?.idleConnections/pData?.maxConnections)*100}%`}}></div></div>
                       </div>
                     </div>
                   </div>
                   <div className="bg-elevated border border-border p-4 rounded text-center flex flex-col justify-center">
                     <div className="text-3xl font-mono text-text-primary mb-1">{pData?.totalIdentityRecords?.toLocaleString()}</div>
                     <div className="text-[10px] uppercase text-text-muted">Total Identities</div>
                   </div>
                   <div className="bg-elevated border border-border p-4 rounded text-center flex flex-col justify-center">
                     <div className="text-sm font-mono text-text-primary mb-1">{pData?.lastWrite ? new Date(pData.lastWrite).toLocaleTimeString() : '-'}</div>
                     <div className="text-[10px] uppercase text-text-muted">Last Write</div>
                   </div>
                 </div>
               </div>
            </motion.div>
          )}
          
        </AnimatePresence>
      </div>
    </motion.div>
  );
}
