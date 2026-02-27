import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { actuatorApi } from '../../shared/api/actuatorApi';
import { Activity, ShieldCheck, AlertTriangle, Database, Hash, RefreshCcw } from 'lucide-react';
import { toast } from 'sonner';

const PostgresView = () => {
    const [actuatorUrl, setActuatorUrl] = useState('http://localhost:8010/logging-service/actuator');
    const [activeUrl, setActiveUrl] = useState('');

    const { data: healthData, isLoading, isError, isFetching } = useQuery({
        queryKey: ['dbHealth', activeUrl],
        queryFn: () => actuatorApi.getHealth(activeUrl).then(res => res.data),
        enabled: !!activeUrl,
        refetchInterval: 5000,
        onError: () => toast.error('Failed to resolve PostgreSQL Actuator endpoint.')
    });

    const handleConnect = (e) => {
        e.preventDefault();
        if (!actuatorUrl) return;
        setActiveUrl(actuatorUrl);
    };

    const dbDetails = healthData?.components?.db || healthData?.components?.postgresql;
    const isUp = healthData?.status === 'UP';

    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="space-y-10 pb-20">
            {/* Header Area */}
            <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8">
                <div className="relative group">
                    <div className="absolute -inset-4 bg-gradient-to-r from-blue-600 to-indigo-500 rounded-3xl blur-2xl opacity-10 group-hover:opacity-20 transition-opacity duration-1000" />
                    <div className="relative">
                        <h2 className="text-4xl lg:text-5xl font-black tracking-tighter leading-none flex items-center gap-4">
                            POSTGRE<span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-600 to-indigo-400">SQL</span>
                        </h2>
                        <p className="text-slate-500 mt-4 font-medium text-lg max-w-xl leading-relaxed">
                            Relational persistent storage telemetry and connection pool data.
                        </p>
                    </div>
                </div>
            </div>

            {/* Connection Input Card */}
            <div className="glass-card p-6 md:p-8 rounded-[2.5rem] border border-slate-200 dark:border-white/5 shadow-xl relative overflow-hidden">
                <div className="absolute top-0 right-0 p-8 opacity-5">
                    <Database className="w-32 h-32" />
                </div>

                <form onSubmit={handleConnect} className="relative z-10 flex flex-col md:flex-row gap-4">
                    <div className="flex-1 relative group">
                        <Hash className="absolute left-6 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-blue-500 transition-colors duration-300" />
                        <input
                            type="text"
                            value={actuatorUrl}
                            onChange={(e) => setActuatorUrl(e.target.value)}
                            placeholder="Data Node Actuator URL (e.g. http://localhost:8084/actuator)"
                            className="w-full bg-slate-100 dark:bg-white/5 border border-slate-200 dark:border-white/10 rounded-2xl pl-16 pr-6 py-4 text-sm font-mono text-slate-800 dark:text-slate-200 focus:outline-none focus:border-blue-500/50 focus:bg-white dark:focus:bg-white/10 transition-all placeholder:text-slate-400"
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={isFetching && !activeUrl}
                        className="px-10 py-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white rounded-2xl font-black uppercase tracking-widest text-xs shadow-lg shadow-blue-500/25 transition-all flex items-center justify-center gap-3 shrink-0"
                    >
                        {isFetching && !activeUrl ? 'Connecting...' : 'Stream Telemetry'}
                    </button>
                </form>
            </div>

            {/* Data Visualization */}
            <AnimatePresence mode="wait">
                {activeUrl && isError && (
                    <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0 }} className="glass-card p-10 rounded-[3rem] text-center space-y-4">
                        <AlertTriangle className="w-16 h-16 text-rose-500 mx-auto" />
                        <h3 className="text-2xl font-bold text-slate-900 dark:text-white">Database Unreachable</h3>
                        <p className="text-slate-500">The Postgres actuator endpoint refused connection or timed out.</p>
                    </motion.div>
                )}

                {activeUrl && !isError && healthData && (
                    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {/* Status Card */}
                        <div className="glass-card p-8 rounded-[2.5rem] flex flex-col justify-between group overflow-hidden relative">
                            <div className={`absolute -right-10 -top-10 w-32 h-32 blur-[40px] opacity-20 group-hover:opacity-40 transition-opacity duration-500 ${isUp ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                            <div className="flex justify-between items-start z-10 mb-8">
                                <div className={`w-14 h-14 rounded-2xl flex items-center justify-center border shadow-sm ${isUp ? 'bg-emerald-50 dark:bg-emerald-500/10 border-emerald-200 dark:border-emerald-500/20 text-emerald-600 dark:text-emerald-400' : 'bg-rose-50 dark:bg-rose-500/10 border-rose-200 dark:border-rose-500/20 text-rose-500'}`}>
                                    {isUp ? <ShieldCheck className="w-7 h-7" /> : <AlertTriangle className="w-7 h-7" />}
                                </div>
                                <span className={`text-[10px] font-black tracking-[0.2em] px-3 py-1.5 rounded-full ${isUp ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'}`}>
                                    {healthData.status}
                                </span>
                            </div>
                            <div className="z-10 mt-auto">
                                <h3 className="text-sm font-bold text-slate-500 uppercase tracking-widest mb-1">Database State</h3>
                                <p className="text-3xl font-black tracking-tight text-slate-900 dark:text-white">
                                    {isUp ? 'Operational' : 'Failing'}
                                </p>
                            </div>
                        </div>

                        {/* Node Info Card */}
                        <div className="glass-card p-8 rounded-[2.5rem] flex flex-col justify-between group overflow-hidden relative md:col-span-2 lg:col-span-2">
                            <div className="absolute top-0 right-0 p-8 opacity-[0.02]">
                                <Database className="w-32 h-32" />
                            </div>
                            <h3 className="text-xs font-black uppercase tracking-[0.2em] text-slate-500 mb-6 flex items-center gap-2 z-10">
                                <Activity className="w-4 h-4" /> Node Telemetry
                            </h3>

                            <div className="grid grid-cols-2 gap-8 z-10">
                                <div>
                                    <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">DB Engine</p>
                                    <p className="font-mono text-sm text-slate-800 dark:text-slate-200 bg-slate-100 dark:bg-white/5 p-3 rounded-xl border border-slate-200 dark:border-white/5 truncate">
                                        {dbDetails?.details?.database || 'PostgreSQL'}
                                    </p>
                                </div>
                                <div>
                                    <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">Connection Query</p>
                                    <p className="font-mono text-sm text-slate-800 dark:text-slate-200 bg-slate-100 dark:bg-white/5 p-3 rounded-xl border border-slate-200 dark:border-white/5 truncate">
                                        {dbDetails?.details?.validationQuery || 'SELECT 1'}
                                    </p>
                                </div>
                            </div>

                            <div className="mt-8 flex items-center justify-between border-t border-slate-200 dark:border-white/5 pt-6 z-10">
                                <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                    Last Polled: {new Date().toLocaleTimeString()}
                                </div>
                                {isFetching && <RefreshCcw className="w-4 h-4 text-blue-500 animate-spin" />}
                            </div>
                        </div>

                        {/* Extended Telemetry Grid - Mocked values awaiting Prometheus/Micrometer backend connections */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                            {[
                                { label: 'Active Connections', value: '18', unit: 'Pools', trend: '+2', color: 'from-blue-500 to-indigo-500' },
                                { label: 'Cache Hit Ratio', value: '98.5', unit: '% Cached', trend: 'Stable', color: 'from-emerald-500 to-teal-500' },
                                { label: 'Deadlocks', value: '0', unit: 'Issues', trend: 'Zero', color: 'from-purple-500 to-fuchsia-500' },
                                { label: 'Buffer Allocation', value: '8', unit: 'GB', trend: 'Maxed', color: 'from-orange-500 to-rose-500' }
                            ].map((stat, idx) => (
                                <motion.div
                                    initial={{ opacity: 0, y: 20 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    transition={{ delay: 0.1 * idx }}
                                    key={stat.label}
                                    className="glass-card p-6 rounded-[2rem] border border-slate-200 dark:border-white/5 relative group overflow-hidden"
                                >
                                    <div className={`absolute bottom-0 left-0 w-full h-1 bg-gradient-to-r ${stat.color} opacity-40 group-hover:opacity-100 transition-opacity duration-300`} />
                                    <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-4">{stat.label}</h4>
                                    <div className="flex items-baseline gap-2">
                                        <span className="text-3xl font-black text-slate-900 dark:text-white tracking-tighter">{stat.value}</span>
                                        <span className="text-xs font-bold text-slate-500">{stat.unit}</span>
                                    </div>
                                    <div className="mt-4 flex items-center justify-between">
                                        <span className="text-[10px] font-bold text-slate-500 bg-slate-100 dark:bg-white/5 px-2 py-1 rounded-md">{stat.trend}</span>
                                    </div>
                                </motion.div>
                            ))}
                        </div>

                        {/* Network Flow Visualization */}
                        <div className="glass-card p-8 rounded-[2.5rem] relative overflow-hidden flex flex-col items-center justify-center min-h-[250px] border border-slate-200 dark:border-white/5">
                            <div className="absolute inset-0 bg-grid-white opacity-[0.02] pointer-events-none" />
                            <div className="absolute top-1/2 left-0 w-full h-[1px] bg-slate-200 dark:bg-white/5" />

                            <div className="flex w-full max-w-2xl justify-between items-center z-10 px-8">
                                <div className="flex flex-col items-center gap-4">
                                    <div className="w-20 h-20 rounded-[1.5rem] bg-indigo-50 dark:bg-indigo-500/10 border border-indigo-200 dark:border-indigo-500/20 flex flex-col items-center justify-center text-indigo-500 shadow-lg">
                                        <span className="text-[10px] font-black uppercase tracking-widest mb-1">Commits</span>
                                        <span className="text-xl font-bold tracking-tighter">8.2k/s</span>
                                    </div>
                                </div>

                                <div className="flex-1 px-8 relative h-10 flex items-center justify-center">
                                    <div className="w-full h-1 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 rounded-full overflow-hidden relative">
                                        <div className="absolute inset-0 bg-white/40 w-1/4 animate-[shimmer_2s_infinite_reverse]" />
                                    </div>
                                    <div className="absolute -top-4 bg-indigo-100 dark:bg-indigo-500/10 border border-indigo-200 dark:border-indigo-500/20 px-3 py-1 rounded-full text-[10px] font-black text-indigo-600 dark:text-indigo-400 uppercase tracking-widest">
                                        4,500 TPS
                                    </div>
                                </div>

                                <div className="flex flex-col items-center gap-4">
                                    <div className="w-20 h-20 rounded-[1.5rem] bg-orange-50 dark:bg-orange-500/10 border border-orange-200 dark:border-orange-500/20 flex flex-col items-center justify-center text-orange-500 shadow-lg">
                                        <span className="text-[10px] font-black uppercase tracking-widest mb-1">Rollbacks</span>
                                        <span className="text-xl font-bold tracking-tighter">12/s</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
};

export default PostgresView;
