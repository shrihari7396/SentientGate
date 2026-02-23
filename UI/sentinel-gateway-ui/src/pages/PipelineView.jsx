import React from 'react';
import { ArrowRight, ShieldCheck, ListFilter, Activity, Lock, Cpu, Database } from 'lucide-react';

const PipelineStep = ({ icon: Icon, title, description, status = 'active', delay = 0 }) => (
    <div
        className={`relative group animate-in`}
        style={{ animationDelay: `${delay}s` }}
    >
        <div className={`glass-card p-6 rounded-2xl border-white/5 relative z-10 ${status === 'active' ? 'border-purple-500/20 shadow-[0_0_30px_rgba(168,85,247,0.05)]' : ''}`}>
            <div className="flex items-center gap-4 mb-4">
                <div className={`p-3 rounded-xl ${status === 'active' ? 'bg-purple-600/10 text-purple-400' : 'bg-slate-500/10 text-slate-500'}`}>
                    <Icon className="w-6 h-6" />
                </div>
                <div>
                    <h4 className="font-semibold text-slate-200">{title}</h4>
                    <span className="text-[10px] uppercase tracking-wider text-slate-500 font-bold">Global Filter</span>
                </div>
            </div>
            <p className="text-sm text-slate-400 leading-relaxed">
                {description}
            </p>
        </div>
        <div className="absolute top-1/2 -right-12 -translate-y-1/2 text-slate-800 lg:block hidden">
            <ArrowRight className="w-8 h-8 animate-pulse text-purple-900" />
        </div>
    </div>
);

const PipelineView = () => {
    return (
        <div className="space-y-8 animate-in">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Request Pipeline</h2>
                    <p className="text-slate-400 mt-2">Visualization of the SentientGate security filter chain.</p>
                </div>
                <div className="px-4 py-2 glass rounded-lg text-xs font-mono text-purple-400 border-purple-500/20">
                    ORDERED_EXECUTION_MODE
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-12 py-8 relative">
                <div className="absolute top-1/2 left-0 w-full h-0.5 bg-gradient-to-r from-purple-500/5 via-purple-500/10 to-transparent lg:block hidden -translate-y-1/2 z-0"></div>

                <PipelineStep
                    icon={ShieldCheck}
                    title="Blacklist Filter"
                    description="Checks incoming visitor ID against the Redis global blacklist. Blocks high-risk identities instantly."
                    delay={0.1}
                />
                <PipelineStep
                    icon={Lock}
                    title="JWT Extraction"
                    description="Parses JTI from Authorization header and verifies token status against Redis blacklist."
                    delay={0.2}
                />
                <PipelineStep
                    icon={Activity}
                    title="Rate Limiter"
                    description="Enforces dynamic per-IP and per-Path throughput limits using Redis token buckets."
                    delay={0.3}
                />
                <PipelineStep
                    icon={ListFilter}
                    title="Sentient Filter"
                    description="Last stage logger that captures all request metadata and sends it to Kafka for long-term storage."
                    delay={0.4}
                />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mt-12">
                <div className="glass-card p-8 rounded-2xl">
                    <h3 className="text-lg font-semibold mb-6 flex items-center gap-2">
                        <Cpu className="w-5 h-5 text-blue-400" />
                        Infrastructure Layer
                    </h3>
                    <div className="space-y-4">
                        <div className="flex items-center justify-between p-4 bg-white/5 rounded-xl border border-white/5">
                            <span className="text-sm">Kafka Broker</span>
                            <span className="text-xs text-green-400 font-mono">CONNECTED</span>
                        </div>
                        <div className="flex items-center justify-between p-4 bg-white/5 rounded-xl border border-white/5">
                            <span className="text-sm">Redis Cluster</span>
                            <span className="text-xs text-green-400 font-mono">SYNCHRONIZED</span>
                        </div>
                        <div className="flex items-center justify-between p-4 bg-white/5 rounded-xl border border-white/5">
                            <span className="text-sm">PostgreSQL DB</span>
                            <span className="text-xs text-green-400 font-mono">READY</span>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-8 rounded-2xl">
                    <h3 className="text-lg font-semibold mb-6 flex items-center gap-2">
                        <Database className="w-5 h-5 text-orange-400" />
                        Persistence Overview
                    </h3>
                    <p className="text-sm text-slate-400 leading-loose">
                        All gateway logs are eventually persisted in the <code className="text-purple-400">gateway_logs</code> table.
                        The system uses <span className="text-slate-200">Kafka-to-DB batching</span> to handle high throughput without impacting
                        API latency. Security alerts are prioritized and processed in a dedicated real-time stream.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default PipelineView;
