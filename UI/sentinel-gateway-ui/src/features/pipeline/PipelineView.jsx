import React from 'react';
import { ArrowRight, ShieldCheck, ListFilter, Activity, Lock, Cpu, Database, Server, Zap, Globe } from 'lucide-react';
import { motion } from 'framer-motion';

const PipelineStep = ({ icon: Icon, title, description, status = 'active', delay = 0 }) => (
    <motion.div
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ delay, duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
        className="relative group flex-1 lg:min-w-[320px] shrink-0"
    >
        <div className={`glass-card p-10 rounded-[2.5rem] border-slate-200 dark:border-white/5 relative z-10 h-full flex flex-col ${status === 'active' ? 'border-purple-500/10 shadow-[0_0_50px_rgba(168,85,247,0.05)]' : 'opacity-40 grayscale'}`}>
            <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-purple-500/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />

            <div className="flex items-center gap-5 mb-8">
                <div className={`p-4 rounded-2xl ${status === 'active' ? 'bg-purple-100 dark:bg-purple-600/10 text-purple-400 group-hover:purple-glow' : 'bg-slate-500/10 text-slate-500 dark:text-slate-500'} border border-slate-200 dark:border-white/5 transition-all duration-500 group-hover:scale-110`}>
                    <Icon className="w-8 h-8" />
                </div>
                <div>
                    <h4 className="text-lg font-black tracking-tight text-slate-900 dark:text-white">{title}</h4>
                    <span className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-slate-600 font-black">SENTINEL_NODE</span>
                </div>
            </div>

            <p className="text-sm text-slate-500 dark:text-slate-500 leading-relaxed font-medium flex-1">
                {description}
            </p>

            <div className="mt-8 flex items-center justify-between">
                <span className="text-[9px] font-black uppercase tracking-widest text-slate-400 dark:text-slate-600 border border-slate-200 dark:border-white/5 px-3 py-1 rounded-full">Active Mode</span>
                <div className="flex gap-1">
                    <div className="w-1 h-1 rounded-full bg-purple-500 animate-pulse" />
                    <div className="w-1 h-1 rounded-full bg-purple-500/40" />
                    <div className="w-1 h-1 rounded-full bg-purple-500/10" />
                </div>
            </div>
        </div>

        <div className="absolute top-1/2 -right-8 -translate-y-1/2 text-slate-800 lg:block hidden z-20">
            <div className="relative">
                <div className="absolute inset-0 bg-purple-500/20 blur-[15px] animate-pulse rounded-full" />
                <ArrowRight className="w-8 h-8 text-purple-600 relative z-10 animate-horizontal-bounce" />
            </div>
        </div>
    </motion.div>
);

const PipelineView = () => {
    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-16 pb-20"
        >
            <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
                <div>
                    <h2 className="text-4xl font-black tracking-tighter leading-none uppercase">Execution <span className="gradient-text">Pipeline</span></h2>
                    <p className="text-slate-400 dark:text-slate-600 dark:text-slate-400 mt-4 font-medium text-lg max-w-2xl leading-relaxed">
                        Visualizing the SentientGate logic sequence. Each filter executes with transactional integrity before routing to microservices.
                    </p>
                </div>
                <div className="px-6 py-2.5 glass rounded-2xl border-purple-200 dark:border-purple-500/20 shadow-2xl">
                    <span className="text-xs font-black text-purple-400 uppercase tracking-[0.4em]">SYNC_MODE_V4</span>
                </div>
            </div>

            <div className="flex flex-col lg:flex-row gap-12 py-10 relative overflow-x-auto pb-10 snap-x">
                <div className="absolute top-1/2 left-0 w-max min-w-full h-[1px] bg-gradient-to-r from-transparent via-purple-500/10 to-transparent lg:block hidden -translate-y-1/2 z-0"></div>

                <PipelineStep
                    icon={ShieldCheck}
                    title="Edge Fire"
                    description="Real-time visitor analysis. Cross-references Redis pool for global identity isolation."
                    delay={0.1}
                />
                <PipelineStep
                    icon={Lock}
                    title="JTI Vault"
                    description="Cryptographic token extraction. Validates JTI integrity and session persistence."
                    delay={0.2}
                />
                <PipelineStep
                    icon={Activity}
                    title="Rate Pulse"
                    description="Throughput stabilization. Enforces per-second token bucket quotas on every origin."
                    delay={0.3}
                />
                <PipelineStep
                    icon={ListFilter}
                    title="Shadow Log"
                    description="Metadata telemetry. Asynchronously streams request analytics to Kafka for audit trails."
                    delay={0.4}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 mt-12">
                <div className="lg:col-span-7 glass-card p-10 rounded-[3rem] border-slate-200 dark:border-white/5 bg-gradient-to-br from-blue-500/5 to-transparent relative overflow-hidden group">
                    <div className="absolute top-0 right-0 p-12 opacity-5 scale-150 rotate-12 group-hover:scale-110 transition-transform duration-1000">
                        <Database className="w-64 h-64" />
                    </div>
                    <h3 className="text-xl font-bold mb-10 flex items-center gap-4">
                        <Server className="w-6 h-6 text-blue-400" />
                        Infrastructure Fabric
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10">
                        {[
                            { name: 'Kafka Cluster', status: 'SYNCHRONIZED', icon: Zap, color: 'text-purple-500' },
                            { name: 'Redis Cache', status: 'LOW_LATENCY', icon: Activity, color: 'text-green-500' },
                            { name: 'Identity DB', status: 'REPLICATED', icon: Database, color: 'text-blue-500' },
                            { name: 'Sentinel Node', status: 'HEALTHY', icon: ShieldCheck, color: 'text-orange-500' },
                        ].map((node) => (
                            <div key={node.name} className="p-6 rounded-[2rem] bg-white/[0.03] border border-slate-200 dark:border-white/5 flex flex-col gap-4 hover:bg-slate-200 dark:bg-white/5 transition-colors group/node">
                                <div className="flex justify-between items-center">
                                    <div className={`p-2.5 rounded-xl bg-slate-200 dark:bg-white/5 ${node.color} group-hover/node:purple-glow transition-all`}>
                                        <node.icon className="w-5 h-5" />
                                    </div>
                                    <span className="text-[9px] font-black text-slate-500 dark:text-slate-500 uppercase tracking-widest">{node.status}</span>
                                </div>
                                <p className="font-bold text-slate-800 dark:text-slate-200 tracking-tight text-lg">{node.name}</p>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="lg:col-span-5 glass-card p-10 rounded-[3rem] border-slate-200 dark:border-white/5 relative overflow-hidden flex flex-col justify-center gap-8">
                    <div className="absolute inset-0 bg-gradient-to-tr from-purple-600/5 to-transparent blur-[20px]" />
                    <h3 className="text-xl font-bold flex items-center gap-4 relative z-10">
                        <Globe className="w-6 h-6 text-purple-400" />
                        Propagation Logic
                    </h3>
                    <p className="text-base text-slate-500 dark:text-slate-500 leading-relaxed font-medium relative z-10">
                        SentenceGate utilizes a <span className="text-slate-100 italic">Reactive Filter Chain</span> pattern. Every request is isolated in its own context, ensuring that high-throughput streams (like Admin registrations) do not impact lower-priority traffic.
                    </p>
                    <div className="p-6 rounded-2xl bg-purple-500/5 border border-purple-500/10 italic text-[11px] text-slate-500 dark:text-slate-500 leading-relaxed relative z-10">
                        "Logic ensures 100% auditability without compromising the 15ms latency budget per request bucket."
                    </div>
                </div>
            </div>
        </motion.div>
    );
};

export default PipelineView;
