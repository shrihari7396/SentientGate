import React from 'react';
import { motion } from 'framer-motion';
import { Server, Activity, Database, Shield, Zap } from 'lucide-react';
import { Link } from 'react-router-dom';

const InfraCard = ({ title, status, icon: Icon, color, delay, path }) => {
    return (
        <Link to={path}>
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay, duration: 0.5, ease: 'easeOut' }}
                className="glass-card p-6 rounded-[2rem] flex flex-col justify-between h-48 group relative overflow-hidden active:scale-95 transition-transform cursor-pointer"
            >
                {/* Background Accent */}
                <div className={`absolute -right-10 -top-10 w-32 h-32 ${color.bg} blur-[40px] opacity-20 group-hover:opacity-40 transition-opacity duration-500`} />

                <div className="flex justify-between items-start z-10">
                    <div className={`w-12 h-12 rounded-2xl ${color.iconBg} flex items-center justify-center border ${color.border} shadow-sm group-hover:scale-110 transition-transform duration-300`}>
                        <Icon className={`w-6 h-6 ${color.text}`} />
                    </div>
                    <span className="text-[10px] font-black tracking-[0.15em] text-slate-500 dark:text-slate-400 uppercase">
                        {status}
                    </span>
                </div>

                <div className="z-10 mt-auto">
                    <h3 className="text-xl font-black tracking-tight text-slate-900 dark:text-white group-hover:underline decoration-2 underline-offset-4">
                        {title}
                    </h3>
                </div>
            </motion.div>
        </Link>
    );
};

const InfrastructureView = () => {
    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="pb-20"
        >
            <div className="relative overflow-hidden glass-card rounded-[3rem] p-10 min-h-[70vh] border border-slate-200 dark:border-white/5">
                {/* Huge Watermark Icon */}
                <div className="absolute -right-20 top-1/2 -translate-y-1/2 opacity-[0.03] dark:opacity-[0.02] pointer-events-none">
                    <Database className="w-[600px] h-[600px] text-slate-900 dark:text-white" strokeWidth={0.5} />
                </div>

                <div className="flex items-center gap-4 mb-12 relative z-10">
                    <div className="w-12 h-12 rounded-2xl bg-blue-500/10 flex items-center justify-center border border-blue-500/20 text-blue-600 dark:text-blue-400">
                        <Server className="w-6 h-6" />
                    </div>
                    <div>
                        <h2 className="text-3xl font-black tracking-tighter text-slate-900 dark:text-white">
                            Infrastructure Fabric
                        </h2>
                        <p className="text-sm font-medium text-slate-500 mt-1">
                            Live telemetry and status of core Sentinel Gateway nodes and dependencies.
                        </p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10 max-w-4xl">
                    <InfraCard
                        title="Kafka Cluster"
                        status="Synchronized"
                        icon={Zap}
                        delay={0.1}
                        path="/infrastructure/kafka"
                        color={{
                            bg: 'bg-purple-500',
                            iconBg: 'bg-purple-50 dark:bg-purple-500/10',
                            text: 'text-purple-600 dark:text-purple-400',
                            border: 'border-purple-200 dark:border-purple-500/20'
                        }}
                    />
                    <InfraCard
                        title="Redis Cache"
                        status="Low_Latency"
                        icon={Activity}
                        delay={0.2}
                        path="/infrastructure/redis"
                        color={{
                            bg: 'bg-emerald-500',
                            iconBg: 'bg-emerald-50 dark:bg-emerald-500/10',
                            text: 'text-emerald-600 dark:text-emerald-400',
                            border: 'border-emerald-200 dark:border-emerald-500/20'
                        }}
                    />
                    <InfraCard
                        title="Identity DB"
                        status="Replicated"
                        icon={Database}
                        delay={0.3}
                        path="/infrastructure/postgres"
                        color={{
                            bg: 'bg-blue-500',
                            iconBg: 'bg-blue-50 dark:bg-blue-500/10',
                            text: 'text-blue-600 dark:text-blue-400',
                            border: 'border-blue-200 dark:border-blue-500/20'
                        }}
                    />
                    <InfraCard
                        title="Sentinel Node"
                        status="Healthy"
                        icon={Shield}
                        delay={0.4}
                        path="#"
                        color={{
                            bg: 'bg-orange-500',
                            iconBg: 'bg-orange-50 dark:bg-orange-500/10',
                            text: 'text-orange-600 dark:text-orange-400',
                            border: 'border-orange-200 dark:border-orange-500/20'
                        }}
                    />
                </div>
            </div>
        </motion.div>
    );
};

export default InfrastructureView;
