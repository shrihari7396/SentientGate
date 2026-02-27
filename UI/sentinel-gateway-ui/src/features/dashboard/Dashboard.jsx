import React, { useState } from 'react';
import { Activity, ShieldCheck, ShieldAlert, Zap, ArrowUpRight, ArrowDownRight, Terminal, Box, Database, Cpu } from 'lucide-react';
import { motion } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import StatsChart from './StatsChart';
import { logApi } from '../../shared/api/client';

const StatCard = ({ title, value, change, icon: Icon, trend, delay = 0 }) => (
    <motion.div
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        whileHover={{ scale: 1.02, y: -5 }}
        transition={{ delay, duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
        className="glass-card p-7 rounded-[2.5rem] relative group cursor-default hover:border-purple-500/30 transition-all duration-300"
    >
        <div className="absolute top-0 right-0 p-8 opacity-[0.03] group-hover:opacity-10 transition-opacity duration-700">
            <Icon className="w-32 h-32" />
        </div>

        <div className="flex justify-between items-start mb-6">
            <div className="w-14 h-14 bg-gradient-to-br from-indigo-500/10 to-purple-500/10 rounded-[1.25rem] flex items-center justify-center border border-purple-500/20 group-hover:scale-110 group-hover:rotate-3 transition-transform duration-500 group-hover:shadow-[0_0_20px_rgba(168,85,247,0.3)]">
                <Icon className="w-7 h-7 text-purple-600 dark:text-purple-400" />
            </div>
            {trend && (
                <span className={`flex items-center text-[10px] font-bold px-3 py-1.5 rounded-full tracking-wider ${trend === 'up' ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20'
                    }`}>
                    {trend === 'up' ? <ArrowUpRight className="w-3.5 h-3.5 mr-1" /> : <ArrowDownRight className="w-3.5 h-3.5 mr-1" />}
                    {change}%
                </span>
            )}
        </div>

        <h3 className="text-slate-500 text-[10px] font-black uppercase tracking-[0.2em]">{title}</h3>
        <div className="flex items-baseline gap-2 mt-2">
            <p className="text-4xl font-black tracking-tighter text-slate-900 dark:text-white">
                {value}
            </p>
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse shadow-[0_0_10px_rgba(16,185,129,0.8)]" />
        </div>

        <div className="mt-6 flex items-center gap-3">
            <div className="h-1.5 w-full bg-slate-100 dark:bg-white/5 rounded-full overflow-hidden">
                <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: '70%' }}
                    transition={{ duration: 1.5, delay: delay + 0.5, ease: "easeOut" }}
                    className="h-full bg-gradient-to-r from-purple-500 to-indigo-500 relative"
                >
                    <div className="absolute inset-0 bg-white/20 animate-shimmer" />
                </motion.div>
            </div>
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">70%</span>
        </div>
    </motion.div>
);

const Dashboard = () => {
    // React Query for polling dashboard stats
    const { data: stats, isLoading: loading, isError: error, refetch } = useQuery({
        queryKey: ['dashboardStats'],
        queryFn: async () => {
            const end = new Date();
            const start = new Date(Date.now() - 3600000); // Last hour
            return await logApi.getDashboardSummary(start.toISOString(), end.toISOString());
        },
        refetchInterval: 5000, // Poll every 5s
    });

    const [chartData, setChartData] = useState([]);

    const formatValue = (val) => {
        if (!val && val !== 0) return '0';
        if (val >= 1000000) return (val / 1000000).toFixed(1) + 'M';
        if (val >= 1000) return (val / 1000).toFixed(1) + 'k';
        return val;
    };

    if (error && loading) {
        return (
            <div className="flex flex-col items-center justify-center h-[60vh] text-center space-y-6">
                <div className="w-20 h-20 rounded-[1.5rem] bg-rose-500/10 flex items-center justify-center text-rose-500 animate-[pulse-slow] border border-rose-500/20 shadow-[0_0_30px_rgba(225,29,72,0.2)]">
                    <ShieldAlert className="w-10 h-10" />
                </div>
                <h2 className="text-3xl font-black text-slate-900 dark:text-white tracking-tight">System Link Failure</h2>
                <p className="text-slate-500 max-w-md font-medium">The neural link to the logging core has been severed. Check your infrastructure clusters.</p>
                <button onClick={() => refetch()} className="px-8 py-3 bg-slate-900 dark:bg-white text-white dark:text-slate-900 rounded-[1rem] shadow-lg hover:shadow-xl transition-all font-bold text-sm tracking-wide">Reconnect Stream</button>
            </div>
        );
    }

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-12 pb-20"
        >
            <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-12">
                <div className="relative group">
                    <div className="absolute -inset-4 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-3xl blur-2xl opacity-10 group-hover:opacity-20 transition-opacity duration-1000" />
                    <div className="relative">
                        <h2 className="text-5xl lg:text-6xl font-black tracking-tighter leading-none flex items-center gap-4">
                            NETWORK <span className="gradient-text">OS</span>
                            <div className="h-3 w-3 rounded-full bg-emerald-500 animate-pulse shadow-[0_0_15px_rgba(16,185,129,0.6)] mt-2" />
                        </h2>
                        <p className="text-slate-500 mt-4 font-medium text-lg max-w-xl leading-relaxed">
                            Sentinel analytics engine processing <span className="text-indigo-500 dark:text-indigo-400 font-semibold">multi-vector traffic</span> with neural precision.
                        </p>
                    </div>
                </div>
                <div className="flex bg-slate-100/50 dark:bg-white/5 border border-slate-200 dark:border-white/10 rounded-2xl p-1.5 backdrop-blur-xl">
                    <button className="px-6 py-2.5 bg-white dark:bg-white/10 text-slate-900 dark:text-white rounded-xl text-xs font-bold leading-none transition-all shadow-sm flex items-center gap-2">
                        <div className="w-1.5 h-1.5 rounded-full bg-purple-500 animate-ping" />
                        Live Data
                    </button>
                    <button className="px-6 py-2.5 text-slate-500 hover:text-slate-900 dark:hover:text-slate-200 rounded-xl text-xs font-bold leading-none transition-all">
                        History
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
                <StatCard
                    title="Throughput"
                    value={`${stats?.throughput || 0}/s`}
                    change="12.4"
                    icon={Activity}
                    trend="up"
                    delay={0.1}
                />
                <StatCard
                    title="Security Blocks"
                    value={formatValue(stats?.securityBlocks || 0)}
                    change="5.2"
                    icon={ShieldAlert}
                    trend="up"
                    delay={0.2}
                />
                <StatCard
                    title="P99 Response"
                    value={`${Math.round(stats?.p99Latency || 0)}ms`}
                    change="14.8"
                    icon={Zap}
                    trend="down"
                    delay={0.3}
                />
                <StatCard
                    title="Total Traffic"
                    value={formatValue(stats?.totalTraffic || 0)}
                    change="3.1"
                    icon={Box}
                    trend="up"
                    delay={0.4}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                <div className="lg:col-span-8 glass-card p-10 rounded-[3.5rem] border-slate-200 dark:border-white/5 relative group overflow-hidden">
                    <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-purple-500/50 to-transparent" />
                    <div className="absolute -right-20 -top-20 w-64 h-64 bg-purple-600/5 blur-[100px] pointer-events-none" />

                    <div className="flex justify-between items-center mb-10">
                        <div>
                            <h3 className="text-xl font-bold flex items-center gap-3 text-slate-900 dark:text-white tracking-tight">
                                <div className="w-2 h-2 rounded-full bg-purple-500 shadow-[0_0_10px_rgba(168,85,247,0.8)]" />
                                Traffic Velocity
                            </h3>
                            <p className="text-slate-500 dark:text-slate-500 text-xs mt-1 font-medium italic opacity-60">Global request distribution in 1m windows</p>
                        </div>
                        <div className="flex gap-8">
                            <div className="flex items-center gap-2.5">
                                <div className="w-2 h-2 rounded-full bg-purple-500 shadow-[0_0_12px_rgba(168,85,247,0.6)]" />
                                <span className="text-[10px] text-slate-400 dark:text-slate-600 dark:text-slate-400 font-black uppercase tracking-[0.15em]">Core Flow</span>
                            </div>
                            <div className="flex items-center gap-2.5">
                                <div className="w-2 h-2 rounded-full bg-red-500 shadow-[0_0_12px_rgba(239,68,68,0.6)]" />
                                <span className="text-[10px] text-slate-400 dark:text-slate-600 dark:text-slate-400 font-black uppercase tracking-[0.15em]">Threat Vectors</span>
                            </div>
                        </div>
                    </div>

                    <div className="h-[400px] w-full relative overflow-hidden">
                        {loading && chartData?.length === 0 && (
                            <div className="absolute inset-0 flex items-center justify-center bg-[#0d0d0f]/20 backdrop-blur-sm z-10 rounded-2xl">
                                <div className="flex flex-col items-center gap-4">
                                    <div className="w-10 h-10 border-4 border-purple-500/30 border-t-purple-500 rounded-full animate-spin" />
                                    <span className="text-[10px] font-black text-purple-400 uppercase tracking-widest">Syncing Data</span>
                                </div>
                            </div>
                        )}
                        <StatsChart />
                    </div>
                </div>

                <div className="lg:col-span-4 space-y-10">
                    <div className="glass-card p-9 rounded-[3rem] border-slate-200 dark:border-white/5 bg-gradient-to-br from-purple-600/10 via-transparent to-transparent relative overflow-hidden">
                        <div className="absolute inset-0 bg-grid-white opacity-[0.02] pointer-events-none" />
                        <h3 className="text-lg font-bold mb-10 flex items-center gap-3 text-slate-900 dark:text-white tracking-tight">
                            <Terminal className="w-5 h-5 text-blue-400" />
                            Cluster Health
                        </h3>
                        <div className="space-y-8">
                            {[
                                { label: 'Processing Units', value: 42, color: 'from-blue-600 to-indigo-600', icon: Cpu },
                                { label: 'Memory Persistence', value: 28, color: 'from-purple-600 to-fuchsia-600', icon: Database },
                                { label: 'Node Integrity', value: 85, color: 'from-green-600 to-emerald-600', icon: ShieldCheck },
                            ].map((item) => (
                                <div key={item.label} className="space-y-3">
                                    <div className="flex justify-between items-center px-1">
                                        <span className="text-[10px] text-slate-500 dark:text-slate-500 font-black uppercase tracking-[0.15em]">{item.label}</span>
                                        <span className="text-xs font-black text-slate-800 dark:text-slate-200">{item.value}%</span>
                                    </div>
                                    <div className="h-2 w-full bg-slate-200 dark:bg-white/5 rounded-full overflow-hidden p-[1px] border border-slate-200 dark:border-white/5">
                                        <motion.div
                                            initial={{ width: 0 }}
                                            animate={{ width: `${item.value}%` }}
                                            transition={{ duration: 1.5, ease: [0.16, 1, 0.3, 1] }}
                                            className={`h-full rounded-full bg-gradient-to-r ${item.color} shadow-[0_0_15px_rgba(0,0,0,0.5)]`}
                                        />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="glass-card p-9 rounded-[3rem] border-orange-500/20 bg-orange-500/[0.02] relative overflow-hidden group">
                        <div className="absolute -right-16 -bottom-16 w-48 h-48 bg-orange-500/10 blur-[80px] group-hover:bg-orange-500/20 transition-all duration-700" />
                        <div className="flex items-center gap-4 mb-8 relative">
                            <div className="w-14 h-14 rounded-[1.25rem] bg-orange-500/10 flex items-center justify-center text-orange-500 border border-orange-500/20 shadow-[0_0_30px_rgba(249,115,22,0.15)] group-hover:scale-110 transition-transform duration-500">
                                <ShieldAlert className="w-7 h-7" />
                            </div>
                            <div>
                                <h4 className="font-bold text-slate-900 dark:text-white tracking-tight text-lg">Active Threats</h4>
                                <p className="text-[10px] text-orange-500/60 font-black uppercase tracking-[0.2em]">Live Overwatch</p>
                            </div>
                        </div>
                        <p className="text-sm text-slate-400 dark:text-slate-600 dark:text-slate-400 mb-8 leading-relaxed relative font-medium">
                            {stats?.securityBlocks > 0
                                ? <span className="text-slate-800 dark:text-slate-200 font-bold">{stats.securityBlocks} anomalous patterns</span>
                                : "No active threats detected."}
                            {" "}Identity tracing core is currently identifying potential attack surfaces across global nodes.
                        </p>
                        <button className="w-full py-4 bg-orange-500/5 hover:bg-orange-500/10 border border-orange-500/20 rounded-2xl text-[10px] font-black text-orange-400 transition-all uppercase tracking-[0.25em] relative overflow-hidden group/btn">
                            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent -translate-x-full group-hover/btn:animate-shimmer" />
                            Neural Trace
                        </button>
                    </div>
                </div>
            </div>
        </motion.div>
    );
};

export default Dashboard;
