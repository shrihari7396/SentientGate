import React, { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { actuatorApi } from '../../shared/api/actuatorApi';
import { ArrowLeft, Server, Activity, Database, Cpu, MemoryStick, AlertTriangle, ShieldCheck } from 'lucide-react';
import { toast } from 'sonner';

const InfoGroup = ({ title, data, icon: Icon }) => (
    <div className="glass-card p-6 rounded-[2rem] space-y-4">
        <h4 className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400 border-b border-slate-200 dark:border-white/5 pb-4">
            {Icon && <Icon className="w-4 h-4" />}
            {title}
        </h4>
        <div className="space-y-3">
            {Object.entries(data || {}).map(([key, value]) => {
                if (typeof value === 'object') return null; // Simplified render
                return (
                    <div key={key} className="flex justify-between items-center break-all gap-4">
                        <span className="text-[10px] uppercase font-bold text-slate-400 w-1/3 shrink-0">{key}</span>
                        <span className="text-sm font-mono text-slate-800 dark:text-slate-200 text-right">{value?.toString()}</span>
                    </div>
                );
            })}
            {Object.keys(data || {}).length === 0 && (
                <p className="text-xs text-slate-500 italic">No metrics available.</p>
            )}
        </div>
    </div>
);

const ServiceDetailView = () => {
    const { serviceName } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    const instance = location.state?.instance;
    const actuatorUrl = location.state?.actuatorUrl;

    if (!actuatorUrl) {
        return (
            <div className="flex flex-col items-center justify-center h-[50vh] text-center">
                <AlertTriangle className="w-16 h-16 text-orange-500 mb-4" />
                <h2 className="text-2xl font-bold">Actuator URL Not Found</h2>
                <button onClick={() => navigate(-1)} className="mt-6 px-6 py-2 bg-slate-800 text-white rounded-xl">Go Back</button>
            </div>
        );
    }

    // Queries
    const { data: healthData, isLoading: healthLoading } = useQuery({
        queryKey: ['actuatorHealth', actuatorUrl],
        queryFn: () => actuatorApi.getHealth(actuatorUrl).then(res => res.data),
        refetchInterval: 10000,
        onError: () => toast.error('Failed to fetch Health metrics.')
    });

    const { data: infoData } = useQuery({
        queryKey: ['actuatorInfo', actuatorUrl],
        queryFn: () => actuatorApi.getInfo(actuatorUrl).then(res => res.data),
    });

    // Helper to grab specific metric (e.g., system.cpu.usage, jvm.memory.used) individually
    // For a real production app, you might want to fetch these dynamically if they exist.
    const { data: cpuData } = useQuery({
        queryKey: ['actuatorMetricCpu', actuatorUrl],
        queryFn: () => actuatorApi.getMetricDetail(actuatorUrl, 'system.cpu.usage').then(res => res.data),
        retry: false
    });

    const isUp = healthData?.status === 'UP';

    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="space-y-8 pb-20">
            {/* Header */}
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
                <div>
                    <button
                        onClick={() => navigate(-1)}
                        className="mb-4 flex items-center gap-2 text-xs font-bold text-slate-500 hover:text-slate-900 dark:hover:text-white transition-colors"
                    >
                        <ArrowLeft className="w-4 h-4" /> Back to Registry
                    </button>
                    <div className="flex items-center gap-4">
                        <div className={`p-4 rounded-2xl border flex items-center justify-center ${isUp ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-500' : 'bg-rose-500/10 border-rose-500/20 text-rose-500'}`}>
                            {isUp ? <ShieldCheck className="w-8 h-8" /> : <AlertTriangle className="w-8 h-8" />}
                        </div>
                        <div>
                            <h2 className="text-4xl font-black uppercase tracking-tighter text-slate-900 dark:text-white">{serviceName}</h2>
                            <p className="font-mono text-sm text-slate-500 mt-1">{instance?.instanceId || actuatorUrl}</p>
                        </div>
                    </div>
                </div>

                <div className="glass-card px-6 py-4 rounded-2xl flex items-center gap-4">
                    <span className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">Status</span>
                    <div className="flex items-center gap-2">
                        <div className={`w-3 h-3 rounded-full ${isUp ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`} />
                        <span className={`font-bold uppercase tracking-widest text-sm ${isUp ? 'text-emerald-500' : 'text-rose-500'}`}>{healthData?.status || 'UNKNOWN'}</span>
                    </div>
                </div>
            </div>

            {/* Content Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left Column: Health Details */}
                <div className="lg:col-span-1 space-y-6">
                    <InfoGroup title="System Health" icon={Activity} data={healthData?.components?.diskSpace?.details || { status: isUp ? 'Operational' : 'Failing' }} />
                    <InfoGroup title="Application Info" icon={Server} data={infoData?.app || infoData?.build || infoData || { message: 'No info exposed' }} />
                </div>

                {/* Right Column: Telemetry & Metrics (Mock visual layout for actuator metrics) */}
                <div className="lg:col-span-2 glass-card p-10 rounded-[3rem] border border-slate-200 dark:border-white/5 relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/5 blur-[100px] pointer-events-none rounded-full" />

                    <h3 className="text-xl font-bold flex items-center gap-3 text-slate-900 dark:text-white tracking-tight mb-8">
                        <MemoryStick className="w-5 h-5 text-indigo-500" />
                        Live Telemetry
                    </h3>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* CPU Widget */}
                        <div className="bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-white/5 rounded-3xl p-6">
                            <h4 className="text-[10px] font-black uppercase tracking-widest text-slate-500 flex items-center gap-2 mb-4">
                                <Cpu className="w-3.5 h-3.5" /> CPU Usage
                            </h4>
                            <div className="flex items-end gap-3 mb-4">
                                <span className="text-3xl font-black text-slate-900 dark:text-white">
                                    {cpuData?.measurements ? (cpuData.measurements[0].value * 100).toFixed(1) : '0.0'}
                                </span>
                                <span className="text-sm font-bold text-slate-400 mb-1">%</span>
                            </div>
                            <div className="h-2 w-full bg-slate-200 dark:bg-white/10 rounded-full overflow-hidden">
                                <motion.div
                                    className="h-full bg-indigo-500"
                                    initial={{ width: 0 }}
                                    animate={{ width: `${cpuData?.measurements ? (cpuData.measurements[0].value * 100) : 0}%` }}
                                />
                            </div>
                        </div>

                        {/* Ping / Response Time Widget placeholder */}
                        <div className="bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-white/5 rounded-3xl p-6">
                            <h4 className="text-[10px] font-black uppercase tracking-widest text-slate-500 flex items-center gap-2 mb-4">
                                <Activity className="w-3.5 h-3.5" /> Core Status
                            </h4>
                            <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed font-medium">
                                The actuator endpoint is confirming telemetry receipt. To view detailed JVM memory analytics, ensure the target service exposes <code className="bg-slate-200 dark:bg-white/10 px-1 rounded text-slate-800 dark:text-slate-200">jvm.memory.used</code> via management config.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </motion.div>
    );
};

export default ServiceDetailView;
