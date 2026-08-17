import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';
import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { StatusBadge } from '@/shared/components/StatusBadge';
import { X, Brain, Activity, ShieldAlert, CheckCircle2 } from 'lucide-react';
import { CopyableUUID } from '@/shared/components/CopyableUUID';
import { AreaChart, Area, ResponsiveContainer, LineChart, Line, Tooltip } from 'recharts';
import clsx from 'clsx';

export function useUserContext(uuid: string | null) {
  return useQuery({
    queryKey: ['users', uuid, 'context'],
    queryFn: () => apiClient.get(`/users/${uuid}/context`).then(r => r.data),
    enabled: !!uuid,
  });
}

// Global state hook implementation using window custom events
export function useUserContextGlobal() {
  const [uuid, setUuid] = useState<string | null>(null);

  useEffect(() => {
    const handleOpen = (e: Event) => {
      const customEvent = e as CustomEvent;
      setUuid(customEvent.detail);
    };
    const handleClose = () => setUuid(null);
    
    window.addEventListener('open-user-context', handleOpen);
    window.addEventListener('close-user-context', handleClose);
    
    return () => {
       window.removeEventListener('open-user-context', handleOpen);
       window.removeEventListener('close-user-context', handleClose);
    };
  }, []);

  return { uuid, close: () => window.dispatchEvent(new Event('close-user-context')) };
}

export default function UserContextDrawer() {
  const { uuid, close } = useUserContextGlobal();
  const { data: ctx, isLoading } = useUserContext(uuid);

  return (
    <AnimatePresence>
      {uuid && (
        <motion.div
          initial={{ x: 600, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          exit={{ x: 600, opacity: 0 }}
          transition={{ type: "tween", ease: "easeOut", duration: 0.3 }}
          className="fixed top-0 right-0 bottom-0 w-[600px] bg-surface border-l border-border z-50 shadow-2xl flex flex-col"
        >
           <div className="p-6 border-b border-border bg-elevated/30 flex justify-between items-center shrink-0">
             <div>
               <h2 className="text-xl font-sans font-semibold text-text-primary mb-1">User Context Assessment</h2>
               <div className="text-xs text-text-muted font-mono"><CopyableUUID uuid={uuid} maxChars={100} /></div>
             </div>
             <button onClick={close} className="text-text-muted hover:text-text-primary p-2 transition-colors">
               <X size={24} />
             </button>
           </div>
           
           <div className="flex-1 overflow-y-auto p-6 space-y-6">
             {isLoading ? (
               <div className="animate-pulse space-y-6">
                 <div className="h-16 bg-elevated/50 rounded"></div>
                 <div className="h-48 bg-elevated/50 rounded"></div>
               </div>
             ) : ctx ? (
               <>
                 <div className="bg-elevated border border-border rounded p-4 flex justify-between items-center">
                    <div>
                      <span className="text-[10px] uppercase tracking-widest text-text-muted block mb-2">Current Status</span>
                      <StatusBadge status={ctx.status} />
                    </div>
                    {ctx.status === 'BLACKLISTED' && (
                      <button className="px-4 py-2 border border-border bg-surface text-text-primary text-xs font-semibold uppercase rounded hover:bg-elevated transition-colors">
                         Unblock Identity
                      </button>
                    )}
                 </div>

                 {/* Timeline */}
                 <div>
                   <h3 className="text-xs font-semibold tracking-widest uppercase text-text-muted mb-3 flex items-center gap-2">
                     <Activity size={14} /> Request Timeline (10m)
                   </h3>
                   <div className="h-[120px] bg-elevated border border-border rounded p-3 pt-6">
                      <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={ctx.timeline}>
                           <defs>
                             <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                               <stop offset="5%" stopColor="#00E5CC" stopOpacity={0.4}/>
                               <stop offset="95%" stopColor="#00E5CC" stopOpacity={0}/>
                             </linearGradient>
                           </defs>
                           <Tooltip contentStyle={{ backgroundColor: '#1C2128', borderColor: '#30363D' }} itemStyle={{ color: '#00E5CC', fontFamily: 'JetBrains Mono', fontSize: '12px' }} />
                           <Area type="step" dataKey="requestCount" stroke="#00E5CC" fill="url(#colorCount)" />
                        </AreaChart>
                      </ResponsiveContainer>
                   </div>
                 </div>

                 {/* AI Profile */}
                 <div>
                   <h3 className="text-xs font-semibold tracking-widest uppercase text-purple-400 mb-3 flex items-center gap-2">
                     <Brain size={14} /> AI Security Profile
                   </h3>
                   <div className="bg-elevated border border-border rounded p-4 border-l-4 border-l-purple-500">
                     <div className="mb-4">
                       <span className="text-[10px] uppercase text-text-muted tracking-wider block mb-2">Live Anomaly Trajectory</span>
                       <div className="h-[80px]">
                         <ResponsiveContainer width="100%" height="100%">
                           <LineChart data={ctx.aiProfile.anomalyScores}>
                             <Line type="monotone" dataKey="score" stroke="#A855F7" strokeWidth={2} dot={false} />
                           </LineChart>
                         </ResponsiveContainer>
                       </div>
                     </div>
                     <p className="text-sm font-sans text-text-primary leading-relaxed bg-surface/50 p-3 rounded">
                       {ctx.aiProfile.lastAssessment}
                     </p>
                   </div>
                 </div>

                 {/* Active Strategies */}
                 {ctx.activeStrategies?.length > 0 && (
                   <div>
                     <h3 className="text-xs font-semibold tracking-widest uppercase text-text-muted mb-3 flex items-center gap-2">
                       <ShieldAlert size={14} className="text-amber" /> Flagged by Strategies
                     </h3>
                     <div className="flex gap-2 flex-wrap">
                       {ctx.activeStrategies.map((s: string) => (
                         <span key={s} className="bg-amber/10 border border-amber/20 text-amber font-mono text-xs px-2 py-1 rounded">
                           {s}
                         </span>
                       ))}
                     </div>
                   </div>
                 )}

                 {/* Recent Trace */}
                 <div>
                   <h3 className="text-xs font-semibold tracking-widest uppercase text-text-muted mb-3 flex items-center gap-2">
                     Request Trace Sample
                   </h3>
                   <div className="bg-elevated border border-border rounded overflow-hidden">
                     <table className="w-full text-left text-xs font-mono">
                       <tbody className="divide-y divide-border/50">
                         {ctx.recentRequests?.slice(0, 5).map((req: any, i: number) => (
                           <tr key={i} className="text-text-primary hover:bg-surface/50">
                             <td className="p-2 text-text-muted">{new Date(req.timestamp).toLocaleTimeString()}</td>
                             <td className="p-2 text-teal">{req.method}</td>
                             <td className="p-2">{req.endpoint}</td>
                             <td className={clsx("p-2", req.status >= 400 ? 'text-red' : 'text-teal')}>{req.status}</td>
                           </tr>
                         ))}
                       </tbody>
                     </table>
                   </div>
                 </div>
               </>
             ) : (
                <div className="h-full flex items-center justify-center text-text-muted italic flex-col gap-4">
                  <CheckCircle2 size={48} className="text-teal" opacity={0.2} />
                  Context clear. No anomalies registered.
                </div>
             )}
           </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
