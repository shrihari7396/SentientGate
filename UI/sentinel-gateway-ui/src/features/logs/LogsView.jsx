import React, { useState, useMemo } from 'react';
import { Search, Filter, Clock, Shield, Globe, RefreshCcw, LogIn, UserPlus, Zap, ArrowRight, Eye, Activity, ChevronLeft, ChevronRight } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { logApi } from '../../shared/api/client';
import { useReactTable, getCoreRowModel, getPaginationRowModel, getSortedRowModel, flexRender, createColumnHelper } from '@tanstack/react-table';
import { useSearchParams } from 'react-router-dom';

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

const columnHelper = createColumnHelper();

const columns = [
    columnHelper.accessor('occurredAt', {
        header: 'Timeline',
        cell: info => {
            const date = info.getValue() ? new Date(info.getValue()) : null;
            return (
                <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-slate-200 dark:bg-white/5 text-slate-500 dark:text-slate-500 group-hover:text-purple-400 group-hover:bg-purple-100 dark:bg-purple-600/10 transition-colors">
                        <Clock className="w-3.5 h-3.5" />
                    </div>
                    <div className="flex flex-col">
                        <span className="text-slate-800 dark:text-slate-200 font-bold tracking-tighter text-sm">
                            {date ? date.toLocaleTimeString([], { hour12: false }) : 'N/A'}
                        </span>
                        <span className="text-[10px] text-slate-400 dark:text-slate-600 font-black uppercase tracking-tighter">
                            {date ? date.toLocaleDateString() : 'UNKNOWN'}
                        </span>
                    </div>
                </div>
            );
        }
    }),
    columnHelper.accessor('path', {
        header: 'Transaction Detail',
        cell: info => {
            const method = info.row.original.method;
            const routeId = info.row.original.routeId;
            return (
                <div className="flex items-center gap-4">
                    <span className="font-mono text-[9px] px-2 py-1 rounded bg-slate-900 border border-slate-200 dark:border-white/5 text-slate-500 dark:text-slate-500 group-hover:text-slate-900 dark:text-white transition-colors">
                        {method}
                    </span>
                    <div className="flex flex-col">
                        <span className="text-slate-700 dark:text-slate-300 font-bold group-hover:text-slate-900 dark:text-white transition-colors truncate max-w-[200px]" title={info.getValue()}>
                            {info.getValue()}
                        </span>
                        <span className="text-[10px] text-slate-400 dark:text-slate-600 font-bold italic">Route: {routeId || 'SENTINEL_ROOT'}</span>
                    </div>
                </div>
            );
        }
    }),
    columnHelper.accessor('clientIp', {
        header: 'Origin',
        cell: info => (
            <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-blue-500/5 border border-blue-500/10">
                    <Globe className="w-3.5 h-3.5 text-blue-500/60" />
                </div>
                <span className="text-xs font-bold text-slate-400 dark:text-slate-600 font-mono tracking-tighter">{info.getValue()}</span>
            </div>
        )
    }),
    columnHelper.accessor('statusCode', {
        header: 'Response',
        cell: info => {
            const latencyMs = info.row.original.latencyMs;
            return (
                <div className="flex items-center gap-6">
                    <StatusBadge code={info.getValue()} />
                    <div className="flex flex-col">
                        <div className="flex items-center gap-1.5">
                            <Zap className={`w-3 h-3 ${latencyMs > 200 ? 'text-orange-500' : 'text-slate-400 dark:text-slate-600'}`} />
                            <span className={`text-[11px] font-black ${latencyMs > 200 ? 'text-orange-500' : 'text-slate-400 dark:text-slate-600'}`}>
                                {latencyMs}ms
                            </span>
                        </div>
                        <span className="text-[9px] font-black text-slate-700 uppercase tracking-tighter">Execution</span>
                    </div>
                </div>
            );
        }
    }),
    columnHelper.display({
        id: 'actions',
        header: () => <div className="text-right">Actions</div>,
        cell: () => (
            <div className="text-right">
                <button className="p-2.5 rounded-xl bg-white/[0.03] border border-slate-200 dark:border-white/5 hover:border-purple-500/30 hover:bg-purple-100 dark:bg-purple-600/10 hover:text-purple-400 transition-all group/btn">
                    <ArrowRight className="w-4 h-4 group-hover/btn:translate-x-1 transition-transform" />
                </button>
            </div>
        )
    })
];

const LogsView = () => {
    const [searchParams] = useSearchParams();
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [filterPath, setFilterPath] = useState(searchParams.get('path') || '');
    const [filterStatus, setFilterStatus] = useState('');
    const [selectedLog, setSelectedLog] = useState(null);
    const [pagination, setPagination] = useState({
        pageIndex: 0,
        pageSize: 50,
    });

    React.useEffect(() => {
        const queryPath = searchParams.get('path') || '';
        setFilterPath(queryPath);
        setPagination((prev) => ({ ...prev, pageIndex: 0 }));
    }, [searchParams]);

    const { data: logsResponse, isLoading: loading, isFetching } = useQuery({
        queryKey: ['logs', pagination.pageIndex, pagination.pageSize, filterPath, filterStatus],
        queryFn: async () => {
            const res = await logApi.getRawLogs(pagination.pageIndex, pagination.pageSize, filterPath, filterStatus || undefined);
            return res;
        },
        refetchInterval: autoRefresh ? 3000 : false,
        keepPreviousData: true, // Smoother pagination
    });

    const logs = logsResponse?.content || [];
    const totalElements = logsResponse?.totalElements || 0;

    // Synthetic stats
    const errors = useMemo(() => logs.filter(l => l?.statusCode >= 400).length, [logs]);
    const avgLatency = useMemo(() => {
        const total = logs.reduce((acc, l) => acc + (l?.latencyMs || 0), 0);
        return logs.length ? Math.round(total / logs.length) : 0;
    }, [logs]);

    const table = useReactTable({
        data: logs,
        columns,
        getCoreRowModel: getCoreRowModel(),
        pageCount: logsResponse?.totalPages ?? -1,
        state: {
            pagination,
        },
        onPaginationChange: setPagination,
        manualPagination: true,
    });

    const quickFilters = [
        { label: 'Global', path: '', icon: Globe },
        { label: 'Authentication', path: 'login', icon: LogIn },
        { label: 'Onboarding', path: 'register', icon: UserPlus },
    ];

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-10 pb-20"
        >
            {/* Header Section */}
            <div className="flex flex-col md:flex-row justify-between items-end gap-6">
                <div>
                    <h2 className="text-4xl font-extrabold tracking-tight">TRAFFIC <span className="text-purple-500 underline underline-offset-8 decoration-white/10">LEDGER</span></h2>
                    <p className="text-slate-500 dark:text-slate-500 mt-3 font-medium text-sm border-l-2 border-purple-500/30 pl-4 tracking-tight">
                        Comprehensive audit trail of every bit navigating the SentientGate.
                    </p>
                </div>

                <div className="flex items-center gap-4">
                    <div className="glass p-1.5 rounded-2xl flex gap-1">
                        {quickFilters.map((f) => (
                            <button
                                key={f.label}
                                onClick={() => setFilterPath(f.path)}
                                className={`px-5 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${filterPath === f.path ? 'bg-purple-600 text-slate-900 dark:text-white shadow-xl scale-105' : 'text-slate-500 dark:text-slate-500 hover:text-slate-700 dark:text-slate-300'
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
                            : 'bg-slate-200 dark:bg-white/5 text-slate-500 dark:text-slate-500 border-slate-300 dark:border-white/10'
                            }`}
                    >
                        <RefreshCcw className={`w-4 h-4 ${(autoRefresh && isFetching) ? 'animate-spin-slow' : ''}`} />
                        {autoRefresh ? 'Live' : 'Stopped'}
                    </button>
                    <button
                        onClick={() => {
                            setFilterPath('');
                            setFilterStatus('');
                            setSelectedLog(null);
                            setPagination((prev) => ({ ...prev, pageIndex: 0 }));
                        }}
                        className="px-4 py-2.5 rounded-2xl border border-slate-300 dark:border-white/10 text-slate-500 dark:text-slate-400 text-xs font-black uppercase tracking-widest hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10 transition-all"
                    >
                        Reset
                    </button>
                </div>
            </div>

            {/* Mini Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {[
                    { label: 'Total Events', value: totalElements, icon: Activity, color: 'text-blue-500' },
                    { label: 'Anomalies', value: errors, icon: Zap, color: 'text-red-500' },
                    { label: 'Avg Latency', value: `${avgLatency}ms`, icon: Clock, color: 'text-purple-500' },
                ].map((s) => (
                    <div key={s.label} className="glass-card p-6 rounded-3xl flex items-center justify-between group">
                        <div>
                            <p className="text-[10px] font-black text-slate-500 dark:text-slate-500 uppercase tracking-widest">{s.label}</p>
                            <p className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{s.value}</p>
                        </div>
                        <div className={`p-3 rounded-2xl bg-white/[0.03] border border-slate-200 dark:border-white/5 ${s.color} group-hover:scale-110 transition-transform`}>
                            <s.icon className="w-5 h-5" />
                        </div>
                    </div>
                ))}
            </div>

            {/* Search & Filter Bar */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                <div className="lg:col-span-8 relative group">
                    <Search className="w-5 h-5 absolute left-5 top-1/2 -translate-y-1/2 text-slate-500 dark:text-slate-500 group-focus-within:text-purple-500 transition-colors" />
                    <input
                        type="text"
                        value={filterPath}
                        onChange={(e) => {
                            setFilterPath(e.target.value);
                            setPagination(prev => ({ ...prev, pageIndex: 0 }));
                        }}
                        placeholder="Search by endpoint path (e.g. /login)..."
                        className="w-full bg-white/[0.02] border border-slate-200 dark:border-white/5 rounded-3xl pl-14 pr-6 py-4 text-sm text-slate-100 outline-none focus:border-purple-500/30 focus:bg-white/[0.04] transition-all shadow-2xl font-medium"
                    />
                </div>
                <div className="lg:col-span-4 relative">
                    <Filter className="w-5 h-5 absolute left-5 top-1/2 -translate-y-1/2 text-slate-500 dark:text-slate-500" />
                    <select
                        value={filterStatus}
                        onChange={(e) => {
                            setFilterStatus(e.target.value);
                            setPagination(prev => ({ ...prev, pageIndex: 0 }));
                        }}
                        className="w-full bg-white/[0.02] border border-slate-200 dark:border-white/5 rounded-3xl pl-14 pr-10 py-4 text-sm text-slate-400 dark:text-slate-600 dark:text-slate-400 outline-none focus:border-purple-500/30 focus:bg-white/[0.04] appearance-none transition-all shadow-2xl font-bold uppercase tracking-tighter"
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

            {/* Main Logs TanStack Table */}
            <div className="glass-card rounded-[2.5rem] border-slate-200 dark:border-white/5 shadow-[0_32px_64px_-12px_rgba(0,0,0,0.6)] overflow-hidden relative">
                <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-purple-500/10 to-transparent" />

                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            {table.getHeaderGroups().map(headerGroup => (
                                <tr key={headerGroup.id} className="bg-white/[0.02] text-slate-500 dark:text-slate-500 text-[10px] uppercase font-black tracking-[0.2em] border-b border-slate-200 dark:border-white/5">
                                    {headerGroup.headers.map(header => (
                                        <th key={header.id} className={`px-8 py-6 ${header.id === 'actions' ? 'w-24' : ''}`}>
                                            {header.isPlaceholder
                                                ? null
                                                : flexRender(
                                                    header.column.columnDef.header,
                                                    header.getContext()
                                                )}
                                        </th>
                                    ))}
                                </tr>
                            ))}
                        </thead>
                        <tbody className="text-sm divide-y divide-white/[0.03]">
                            <AnimatePresence mode="popLayout">
                                {loading && logs.length === 0 ? (
                                    Array(5).fill(0).map((_, i) => (
                                        <tr key={i} className="animate-pulse">
                                            <td colSpan={columns.length} className="px-8 py-10 h-16 bg-white/[0.01]"></td>
                                        </tr>
                                    ))
                                ) : logs.length === 0 ? (
                                    <tr>
                                        <td colSpan={columns.length} className="px-8 py-20 text-center">
                                            <div className="flex flex-col items-center gap-4 opacity-30">
                                                <Eye className="w-12 h-12" />
                                                <p className="font-bold uppercase tracking-widest text-xs">No Events Captured</p>
                                            </div>
                                        </td>
                                    </tr>
                                ) : (
                                    table.getRowModel().rows.map(row => (
                                        <motion.tr
                                            layout
                                            initial={{ opacity: 0, x: -10 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            transition={{ duration: 0.2 }}
                                            key={row.id}
                                        className="hover:bg-white/[0.02] transition-all group cursor-pointer"
                                        onClick={() => setSelectedLog(row.original)}
                                    >
                                            {row.getVisibleCells().map(cell => (
                                                <td key={cell.id} className="px-8 py-5 whitespace-nowrap">
                                                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                                </td>
                                            ))}
                                        </motion.tr>
                                    ))
                                )}
                            </AnimatePresence>
                        </tbody>
                    </table>
                </div>

                {/* React-Table Pagination Footer */}
                <div className="p-6 bg-white/[0.01] flex justify-between items-center border-t border-slate-200 dark:border-white/5">
                    <p className="text-[10px] text-slate-400 dark:text-slate-600 font-black uppercase tracking-widest">
                        Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()} ({totalElements} total)
                    </p>
                    <div className="flex gap-2">
                        <button
                            onClick={() => table.previousPage()}
                            disabled={!table.getCanPreviousPage()}
                            className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-white/5 border border-slate-200 dark:border-white/5 text-[10px] font-black uppercase tracking-widest text-slate-500 dark:text-slate-500 hover:text-slate-900 dark:text-white transition-all disabled:opacity-30 flex items-center gap-1"
                        >
                            <ChevronLeft className="w-3 h-3" /> Prev
                        </button>
                        <button
                            onClick={() => table.nextPage()}
                            disabled={!table.getCanNextPage()}
                            className="px-4 py-2 rounded-xl bg-purple-600 border border-purple-500/30 text-[10px] font-black uppercase tracking-widest text-slate-900 dark:text-white shadow-xl hover:scale-105 transition-all disabled:opacity-30 disabled:hover:scale-100 flex items-center gap-1"
                        >
                            Next <ChevronRight className="w-3 h-3" />
                        </button>
                    </div>
                </div>
            </div>

            <AnimatePresence>
                {selectedLog && (
                    <motion.div
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: 10 }}
                        className="glass-card rounded-[2rem] p-6 border border-slate-200 dark:border-white/5"
                    >
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-5">
                            <div>
                                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">Selected Event</p>
                                <p className="font-mono text-sm text-slate-700 dark:text-slate-200 mt-1">{selectedLog.path || 'Unknown path'}</p>
                            </div>
                            <button
                                onClick={() => setSelectedLog(null)}
                                className="px-4 py-2 rounded-xl border border-slate-300 dark:border-white/10 text-[10px] font-black uppercase tracking-widest text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/10 transition-all self-start"
                            >
                                Dismiss
                            </button>
                        </div>
                        <pre className="bg-slate-100 dark:bg-black/30 border border-slate-200 dark:border-white/10 rounded-2xl p-4 text-xs text-slate-700 dark:text-slate-300 overflow-x-auto">
                            {JSON.stringify(selectedLog, null, 2)}
                        </pre>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
};

export default LogsView;
