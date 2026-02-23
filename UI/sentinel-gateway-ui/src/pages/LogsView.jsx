import React, { useEffect, useState, useRef } from 'react';
import { logApi } from '../api/client';
import { Search, Filter, Clock, Shield, Globe, RefreshCcw, LogIn, UserPlus, Zap, ArrowRight, Eye, Activity } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const StatusBadge = ({ code }) => {
    const isError = code >= 400;
    const isPending = code === undefined;

    return (
        <div className={`px-3 py-1 rounded-full text-[10px] font-black tracking-widest flex items-center gap-2 ${isError
            ? 'bg-red-500/10 text-red-500 border border-red-500/20 shadow-[0_0_10px_rgba(239,68,68,0.1)]'
            : 'bg-green-500/10 text-green-500 border border-green-500/20 shadow-[0_0_10px_rgba(34,197,94,0.1)]'
            }`}>
            <span className={`w-1.5 h-1.5 rounded-full ${isError ? 'bg-red-500' : 'bg-green-500'}`} />
            {code}
        </div>
    );
};

const LogsView = () => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [filterPath, setFilterPath] = useState('');
    const [filterStatus, setFilterStatus] = useState('');
    const [page, setPage] = useState(0);
    const [stats, setStats] = useState({ total: 0, errors: 0, avgLatency: 0 });

    const refreshTimer = useRef(null);

    const fetchLogs = (isInitial = false) => {
        if (isInitial) setLoading(true);
        logApi.getRawLogs(page, 50, filterPath, filterStatus || undefined)
            .then(res => {
                const newLogs = res.data?.content || [];
                setLogs(newLogs);

                // Synthetic stats for UI "Wow"
                const errors = newLogs.filter(l => l?.statusCode >= 400).length;
                const latency = newLogs.reduce((acc, l) => acc + (l?.latencyMs || 0), 0) / (newLogs.length || 1);
                setStats({ total: res.data?.totalElements || 0, errors, avgLatency: Math.round(latency) });

                if (isInitial) setLoading(false);
            })
            .catch(err => {
                console.error(err);
                if (isInitial) setLoading(false);
            });
    };

    useEffect(() => {
        fetchLogs(true);
    }, [page, filterPath, filterStatus]);

    useEffect(() => {
        if (autoRefresh) {
            refreshTimer.current = setInterval(() => {
                fetchLogs(false);
            }, 3000);
        } else {
            clearInterval(refreshTimer.current);
        }
        return () => clearInterval(refreshTimer.current);
    }, [autoRefresh, page, filterPath, filterStatus]);

    const quickFilters = [
        { label: 'Global', path: '', icon: Globe },
        { label: 'Authentication', path: 'login', icon: LogIn },
        { label: 'Onboarding', path: 'register', icon: UserPlus },
    ];

    return (
        <div className="space-y-10 animate-in pb-20">
            {/* Header Section */}
            <div className="flex flex-col md:flex-row justify-between items-end gap-6">
                <div>
                    <h2 className="text-4xl font-extrabold tracking-tight">TRAFFIC <span className="text-purple-500 underline underline-offset-8 decoration-white/10">LEDGER</span></h2>
                    <p className="text-slate-500 mt-3 font-medium text-sm border-l-2 border-purple-500/30 pl-4 tracking-tight">
                        Comprehensive audit trail of every bit navigating the SentientGate.
                    </p>
                </div>

                <div className="flex items-center gap-4">
                    <div className="glass p-1.5 rounded-2xl flex gap-1">
                        {quickFilters.map((f) => (
                            <button
                                key={f.label}
                                onClick={() => setFilterPath(f.path)}
                                className={`px-5 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${filterPath === f.path ? 'bg-purple-600 text-white shadow-xl scale-105' : 'text-slate-500 hover:text-slate-300'
                                    }`}
                            >
                                {f.label}
                            </button>
                        ))}
                    </div>

                    <button
                        onClick={() => setAutoRefresh(!autoRefresh)}
                        className={`flex items-center gap-3 px-6 py-2.5 rounded-2xl border transition-all text-xs font-black uppercase tracking-widest ${autoRefresh
                            ? 'bg-green-500/10 text-green-400 border-green-500/20'
                            : 'bg-white/5 text-slate-500 border-white/10'
                            }`}
                    >
                        <RefreshCcw className={`w-4 h-4 ${autoRefresh ? 'animate-spin-slow' : ''}`} />
                        {autoRefresh ? 'Live' : 'Stopped'}
                    </button>
                </div>
            </div>

            {/* Mini Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {[
                    { label: 'Total Events', value: stats.total, icon: Activity, color: 'text-blue-500' },
                    { label: 'Anomalies', value: stats.errors, icon: Zap, color: 'text-red-500' },
                    { label: 'Avg Latency', value: `${stats.avgLatency}ms`, icon: Clock, color: 'text-purple-500' },
                ].map((s) => (
                    <div key={s.label} className="glass-card p-6 rounded-3xl flex items-center justify-between group">
                        <div>
                            <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">{s.label}</p>
                            <p className="text-2xl font-bold text-white mt-1">{s.value}</p>
                        </div>
                        <div className={`p-3 rounded-2xl bg-white/[0.03] border border-white/5 ${s.color} group-hover:scale-110 transition-transform`}>
                            <s.icon className="w-5 h-5" />
                        </div>
                    </div>
                ))}
            </div>

            {/* Search & Filter Bar */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                <div className="lg:col-span-8 relative group">
                    <Search className="w-5 h-5 absolute left-5 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-purple-500 transition-colors" />
                    <input
                        type="text"
                        value={filterPath}
                        onChange={(e) => setFilterPath(e.target.value)}
                        placeholder="Search by endpoint path (e.g. /login)..."
                        className="w-full bg-white/[0.02] border border-white/5 rounded-3xl pl-14 pr-6 py-4 text-sm text-slate-100 outline-none focus:border-purple-500/30 focus:bg-white/[0.04] transition-all shadow-2xl font-medium"
                    />
                </div>
                <div className="lg:col-span-4 relative">
                    <Filter className="w-5 h-5 absolute left-5 top-1/2 -translate-y-1/2 text-slate-500" />
                    <select
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                        className="w-full bg-white/[0.02] border border-white/5 rounded-3xl pl-14 pr-10 py-4 text-sm text-slate-400 outline-none focus:border-purple-500/30 focus:bg-white/[0.04] appearance-none transition-all shadow-2xl font-bold uppercase tracking-tighter"
                    >
                        <option value="">Status Code: ALL</option>
                        <option value="200">200 OK</option>
                        <option value="201">201 CREATED</option>
                        <option value="403">403 FORBIDDEN</option>
                        <option value="429">429 RATE LIMITED</option>
                        <option value="500">500 CRITICAL</option>
                    </select>
                </div>
            </div>

            {/* Main Logs Table */}
            <div className="glass-card rounded-[2.5rem] border-white/5 shadow-[0_32px_64px_-12px_rgba(0,0,0,0.6)] overflow-hidden relative">
                <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-purple-500/10 to-transparent" />

                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-white/[0.02] text-slate-500 text-[10px] uppercase font-black tracking-[0.2em] border-b border-white/5">
                                <th className="px-8 py-6">Timeline</th>
                                <th className="px-8 py-6">Transaction Detail</th>
                                <th className="px-8 py-6">Origin</th>
                                <th className="px-8 py-6">Response</th>
                                <th className="px-8 py-6 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="text-sm divide-y divide-white/[0.03]">
                            <AnimatePresence mode="popLayout">
                                {loading ? (
                                    Array(5).fill(0).map((_, i) => (
                                        <tr key={i} className="animate-pulse">
                                            <td colSpan="5" className="px-8 py-10 h-16 bg-white/[0.01]"></td>
                                        </tr>
                                    ))
                                ) : logs.length === 0 ? (
                                    <tr>
                                        <td colSpan="5" className="px-8 py-20 text-center">
                                            <div className="flex flex-col items-center gap-4 opacity-30">
                                                <Eye className="w-12 h-12" />
                                                <p className="font-bold uppercase tracking-widest text-xs">No Events Captured</p>
                                            </div>
                                        </td>
                                    </tr>
                                ) : logs.map((log, idx) => (
                                    <motion.tr
                                        layout
                                        initial={{ opacity: 0, x: -10 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ delay: idx * 0.02 }}
                                        key={log.id}
                                        className="hover:bg-white/[0.02] transition-all group"
                                    >
                                        <td className="px-8 py-5 whitespace-nowrap">
                                            <div className="flex items-center gap-3">
                                                <div className="p-2 rounded-lg bg-white/5 text-slate-500 group-hover:text-purple-400 group-hover:bg-purple-600/10 transition-colors">
                                                    <Clock className="w-3.5 h-3.5" />
                                                </div>
                                                <div className="flex flex-col">
                                                    <span className="text-slate-200 font-bold tracking-tighter text-sm">
                                                        {log?.occurredAt ? new Date(log.occurredAt).toLocaleTimeString([], { hour12: false }) : 'N/A'}
                                                    </span>
                                                    <span className="text-[10px] text-slate-600 font-black uppercase tracking-tighter">
                                                        {log?.occurredAt ? new Date(log.occurredAt).toLocaleDateString() : 'UNKNOWN'}
                                                    </span>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-8 py-5">
                                            <div className="flex items-center gap-4">
                                                <span className="font-mono text-[9px] px-2 py-1 rounded bg-slate-900 border border-white/5 text-slate-500 group-hover:text-white transition-colors">
                                                    {log.method}
                                                </span>
                                                <div className="flex flex-col">
                                                    <span className="text-slate-300 font-bold group-hover:text-white transition-colors truncate max-w-[300px]" title={log.path}>
                                                        {log.path}
                                                    </span>
                                                    <span className="text-[10px] text-slate-600 font-bold italic">Route: {log.routeId || 'SENTINEL_ROOT'}</span>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-8 py-5">
                                            <div className="flex items-center gap-3">
                                                <div className="p-2 rounded-full bg-blue-500/5 border border-blue-500/10">
                                                    <Globe className="w-3.5 h-3.5 text-blue-500/60" />
                                                </div>
                                                <span className="text-xs font-bold text-slate-400 font-mono tracking-tighter">{log.clientIp}</span>
                                            </div>
                                        </td>
                                        <td className="px-8 py-5">
                                            <div className="flex items-center gap-6">
                                                <StatusBadge code={log.statusCode} />
                                                <div className="flex flex-col">
                                                    <div className="flex items-center gap-1.5">
                                                        <Zap className={`w-3 h-3 ${log.latencyMs > 200 ? 'text-orange-500' : 'text-slate-600'}`} />
                                                        <span className={`text-[11px] font-black ${log.latencyMs > 200 ? 'text-orange-500' : 'text-slate-400'}`}>
                                                            {log.latencyMs}ms
                                                        </span>
                                                    </div>
                                                    <span className="text-[9px] font-black text-slate-700 uppercase tracking-tighter">Execution</span>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-8 py-5 text-right">
                                            <button className="p-2.5 rounded-xl bg-white/[0.03] border border-white/5 hover:border-purple-500/30 hover:bg-purple-600/10 hover:text-purple-400 transition-all group/btn">
                                                <ArrowRight className="w-4 h-4 group-hover/btn:translate-x-1 transition-transform" />
                                            </button>
                                        </td>
                                    </motion.tr>
                                ))}
                            </AnimatePresence>
                        </tbody>
                    </table>
                </div>

                {/* Pagination Shadow Footer */}
                <div className="p-6 bg-white/[0.01] flex justify-between items-center border-t border-white/5">
                    <p className="text-[10px] text-slate-600 font-black uppercase tracking-widest">
                        Batch Analysis: {logs.length} of {stats.total} total
                    </p>
                    <div className="flex gap-2">
                        <button className="px-4 py-2 rounded-xl bg-white/5 border border-white/5 text-[10px] font-black uppercase tracking-widest text-slate-500 hover:text-white transition-all disabled:opacity-30" disabled>Previous</button>
                        <button className="px-4 py-2 rounded-xl bg-purple-600 border border-purple-500/30 text-[10px] font-black uppercase tracking-widest text-white shadow-xl hover:scale-105 transition-all">Next Page</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LogsView;
