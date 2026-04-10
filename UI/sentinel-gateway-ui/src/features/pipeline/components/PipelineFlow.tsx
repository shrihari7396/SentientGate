import { usePipeline, PipelineStage, PipelineEvent } from '../hooks/usePipeline';

import { Network, ArrowRight, ShieldCheck, ShieldAlert, Cpu } from 'lucide-react';
import clsx from 'clsx';
import { StatusBadge } from '@/shared/components/StatusBadge';

const STAGES = [
  { id: 'Request In', desc: 'Ingestion & Rate Limit Init' },
  { id: 'Edge Fire', desc: 'Redis Blacklist evaluation' },
  { id: 'JTI Vault', desc: 'JWT tampering detection' },
  { id: 'Rate Pulse', desc: 'Distributed token bucket' },
  { id: 'Shadow Log', desc: 'Kafka async emit' },
  { id: 'Response Out', desc: 'Proxy to microservices' }
];

export default function PipelineFlow() {
  const { stats, events } = usePipeline();
  const sData = stats.data || [];
  const eData = (events.data || []) as PipelineEvent[];

  const getStageStats = (stageName: string) => sData.find((s: any) => s.stage === stageName);

  return (
    <div className="p-6 max-w-[1600px] mx-auto h-full flex flex-col">
      <header className="mb-8">
        <h1 className="text-2xl font-sans font-semibold text-text-primary flex items-center gap-3">
          <Network size={28} className="text-blue" />
          Execution Pipeline
        </h1>
        <p className="text-sm text-text-muted mt-1">Live representation of the gateway filter chain and request flow.</p>
      </header>

      <div className="flex-1 flex flex-col justify-center min-h-0 relative">
        <div className="flex items-center justify-between relative z-10 w-full overflow-x-auto pb-10 custom-scrollbar">

          {/* Animated path line in background */}
          <div className="absolute top-1/2 left-[5%] right-[5%] h-px bg-border -z-10 translate-y-[-50%]"></div>

          {STAGES.map((s: any, i) => {
            const stat: PipelineStage | undefined = getStageStats(s.id);
            const isDown = stat?.status === 'DOWN';
            return (
              <div key={s.id} className="flex flex-col items-center flex-1 min-w-[180px] shrink-0 group relative">
                <div
                  className={clsx(
                    "w-full max-w-[160px] bg-surface border rounded p-4 flex flex-col items-center transition-all",
                    isDown ? "border-red/50 shadow-[0_0_15px_rgba(239,68,68,0.2)]" : "border-border hover:border-teal/50"
                  )}
                >
                  <Cpu size={24} className={clsx("mb-2", isDown ? "text-red" : "text-teal")} />
                  <span className="font-mono text-sm font-semibold text-text-primary text-center leading-tight mb-1">{s.id}</span>
                  <span className="text-[10px] text-text-muted text-center tracking-wide">{s.desc}</span>

                  {stat && (
                    <div className="mt-4 pt-3 border-t border-border/50 w-full flex flex-col items-center gap-1">
                      <span className="text-xl font-mono text-text-primary">{stat.requestsToday.toLocaleString()}</span>
                      <StatusBadge status={stat.status} />
                    </div>
                  )}

                  {isDown && stat?.lastError && (
                    <div className="absolute -bottom-16 left-1/2 -translate-x-1/2 bg-red/10 border border-red/20 text-red text-[10px] p-2 rounded whitespace-nowrap shadow-lg">
                      {stat.lastError}
                    </div>
                  )}
                </div>

                {i < STAGES.length - 1 && (
                  <ArrowRight className="absolute -right-4 top-[80px] text-border -z-10 bg-background px-1" size={24} />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Bottom Live Event Ticker */}
      <div className="mt-auto shrink-0 bg-surface border border-border rounded overflow-hidden flex items-center h-10">
        <div className="bg-elevated px-4 h-full flex items-center border-r border-border shrink-0 z-10 shadow-[4px_0_12px_rgba(0,0,0,0.5)]">
          <span className="text-xs font-semibold tracking-widest uppercase text-text-muted mr-2">Live Flow</span>
          <div className="w-2 h-2 rounded-full bg-teal animate-ping"></div>
        </div>
        <div className="flex-1 overflow-hidden relative">
          <div className="flex gap-8 whitespace-nowrap px-4 w-max animate-[shimmer_20s_linear_infinite]" style={{ animationDirection: 'reverse' }}>
            {eData.map(ev => (
              <div key={ev.id} className="flex items-center gap-2">
                <span className="text-text-muted font-mono text-[10px]">[{new Date(ev.timestamp).toLocaleTimeString()}]</span>
                <span className="font-mono text-xs">{ev.uuid}</span>
                {ev.status === 'PASSED' ? (
                  <span className="text-teal text-xs font-mono flex items-center gap-1">→ <ShieldCheck size={12} /> PASSED</span>
                ) : (
                  <span className="text-red text-xs font-mono flex items-center gap-1">→ <ShieldAlert size={12} /> BLOCKED at {ev.stage}</span>
                )}
              </div>
            ))}
            {eData.length === 0 && <span className="text-xs text-text-muted tracking-widest uppercase py-2">Waiting for traffic...</span>}
          </div>
        </div>
      </div>
    </div>
  );
}
