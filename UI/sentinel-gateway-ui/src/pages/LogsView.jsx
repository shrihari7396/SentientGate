import React, { useEffect, useState, useRef } from 'react';
import { logApi } from '../api/client';
import { Search, Filter, Download, ArrowRight, Clock, Shield, Globe, RefreshCcw, LogIn, UserPlus } from 'lucide-react';

const StatusBadge = ({ code }) => {
    const isError = code >= 400;
    return (
        <span className={`px-2 py-1 rounded-md text-[10px] font-bold ${isError ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 'bg-green-500/10 text-green-400 border border-green-500/20'
            }`}>
            {code}
        </span>
    );
};

const LogsView = () => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [filterPath, setFilterPath] = useState('');
    const [filterStatus, setFilterStatus] = useState('');
    const [page, setPage] = useState(0);

    const refreshTimer = useRef(null);

    const fetchLogs = (isInitial = false) => {
        if (isInitial) setLoading(true);
        logApi.getRawLogs(page, 50, filterPath, filterStatus || undefined)
            .then(res => {
                setLogs(res.data.content);
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

    const toggleAutoRefresh = () => setAutoRefresh(!autoRefresh);

    const quickFilters = [
        { label: 'All Logs', path: '', icon: Filter },
        { label: 'Login', path: 'login', icon: LogIn },
        { label: 'Register', path: 'register', icon: UserPlus },
    ];

    return (
        <div className="space-y-6 animate-in">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Admin Logs</h2>
                    <p className="text-slate-400 mt-2">Currently occurring gateway events and audit trail.</p>
                </div>
                <div className="flex gap-4">
                    <div className="flex bg-[#0d0d0f] border border-white/5 rounded-xl p-1">
                        {quickFilters.map((f) => (
                            <button
                                key={f.label}
                                onClick={() => setFilterPath(f.path)}
                                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-medium transition-all ${filterPath === f.path ? 'bg-purple-600/10 text-purple-400' : 'text-slate-500 hover:text-slate-300'
                                    }`}
                            >
                                <f.icon className="w-3 h-3" />
                                {f.label}
                            </button>
                        ))}
                    </div>

                    <button
                        onClick={toggleAutoRefresh}
                        className={`flex items-center gap-2 px-4 py-2 rounded-xl border transition-all text-xs font-medium ${autoRefresh
                                ? 'bg-green-500/10 text-green-400 border-green-500/20'
                                : 'bg-white/5 text-slate-400 border-white/10'
                            }`}
                    >
                        <RefreshCcw className={`w-3 h-3 ${autoRefresh ? 'animate-spin-slow' : ''}`} />
                        {autoRefresh ? 'Auto-refreshing' : 'Paused'}
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="relative md:col-span-2">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                    <input
                        type="text"
                        value={filterPath}
                        onChange={(e) => setFilterPath(e.target.value)}
                        placeholder="Search by path (e.g. /api/auth/login)..."
                        className="w-full bg-[#0d0d0f] border border-white/10 rounded-xl pl-10 pr-4 py-3 text-sm text-slate-200 outline-none focus:border-purple-500/50 transition-all shadow-inner"
                    />
                </div>
                <div className="relative">
                    <Filter className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                    <select
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                        className="w-full bg-[#0d0d0f] border border-white/10 rounded-xl pl-10 pr-4 py-3 text-sm text-slate-400 outline-none focus:border-purple-500/50 appearance-none transition-all"
                    >
                        <option value="">All Status Codes</option>
                        <option value="200">200 OK</option>
                        <option value="201">201 Created</option>
                        <option value="400">400 Bad Request</option>
                        <option value="401">401 Unauthorized</option>
                        <option value="403">403 Forbidden</option>
                        <option value="429">429 Rate Limited</option>
                        <option value="500">500 Server Error</option>
                    </select>
                </div>
            </div>

            <div className="glass-card rounded-2xl overflow-hidden border border-white/5 shadow-2xl">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-white/5 text-slate-400 text-xs uppercase tracking-wider">
                                <th className="px-6 py-4 font-semibold">Timestamp</th>
                                <th className="px-6 py-4 font-semibold">Method & Path</th>
                                <th className="px-6 py-4 font-semibold">Client IP</th>
                                <th className="px-6 py-4 font-semibold">Status</th>
                                <th className="px-6 py-4 font-semibold">Latency</th>
                                <th className="px-6 py-4 font-semibold">Decision</th>
                            </tr>
                        </thead>
                        <tbody className="text-sm divide-y divide-white/5">
                            {loading ? (
                                Array(5).fill(0).map((_, i) => (
                                    <tr key={i} className="animate-pulse">
                                        <td colSpan="6" className="px-6 py-8 h-12 bg-white/[0.02]"></td>
                                    </tr>
                                ))
                            ) : logs.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="px-6 py-12 text-center text-slate-500 italic">
                                        No logs found matching your criteria.
                                    </td>
                                </tr>
                            ) : logs.map((log) => (
                                <tr key={log.id} className="hover:bg-white/[0.02] transition-colors group">
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center gap-2 text-slate-400 group-hover:text-slate-300">
                                            <Clock className="w-3 h-3 text-purple-500/50" />
                                            {new Date(log.occurredAt).toLocaleTimeString()}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            <span className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-white/5 border border-white/10 text-slate-400">
                                                {log.method}
                                            </span>
                                            <span className="text-slate-200 font-medium truncate max-w-[250px]" title={log.path}>
                                                {log.path}
                                            </span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2 text-slate-400">
                                            <Globe className="w-3 h-3 text-blue-500/50" />
                                            {log.clientIp}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <StatusBadge code={log.statusCode} />
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className={`text-xs font-medium ${log.latencyMs > 200 ? 'text-orange-400' : 'text-slate-400'}`}>
                                            {log.latencyMs}ms
                                        </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center gap-2">
                                            <Shield className={`w-3 h-3 ${log.decision === 'ALLOWED' ? 'text-green-500 shadow-[0_0_8px_rgba(34,197,94,0.4)]' : 'text-red-500 shadow-[0_0_8px_rgba(239,68,68,0.4)]'}`} />
                                            <span className={`text-xs font-semibold ${log.decision === 'ALLOWED' ? 'text-green-500/80' : 'text-red-500/80'}`}>
                                                {log.decision}
                                            </span>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default LogsView;
