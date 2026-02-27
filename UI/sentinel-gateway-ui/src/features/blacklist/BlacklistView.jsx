import React, { useState } from 'react';
import { mgmtApi } from '../../shared/api/client';
import { ShieldAlert, UserX, UserCheck, Search, AlertTriangle, Plus, Hash, Clock, ArrowRight } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';

const BlacklistView = () => {
    const queryClient = useQueryClient();
    const [newUuid, setNewUuid] = useState('');

    // Fetch Blacklist
    const { data: blacklist = [], isLoading: loading } = useQuery({
        queryKey: ['blacklist'],
        queryFn: async () => {
            const res = await mgmtApi.getBlacklist();
            return res;
        }
    });

    // Block Mutation
    const blockMutation = useMutation({
        mutationFn: (uuid) => mgmtApi.blockUuid(uuid),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['blacklist'] });
            toast.success('Identity Isolated', {
                description: 'The UUID has been added to the global blacklist.'
            });
            setNewUuid('');
        },
        onError: (err) => {
            toast.error('Isolation Failed', {
                description: err.response?.data?.message || err.message || 'An error occurred.'
            });
        }
    });

    // Unblock Mutation
    const unblockMutation = useMutation({
        mutationFn: (uuid) => mgmtApi.unblockUuid(uuid),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['blacklist'] });
            toast.success('Access Restored', {
                description: 'The identity has been removed from the blacklist.'
            });
        },
        onError: (err) => {
            toast.error('Restore Failed', {
                description: err.response?.data?.message || err.message || 'An error occurred.'
            });
        }
    });

    const handleBlock = (e) => {
        e.preventDefault();
        if (!newUuid || blockMutation.isPending) return;
        blockMutation.mutate(newUuid);
    };

    const handleUnblock = (uuid) => {
        unblockMutation.mutate(uuid);
    };

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-12 pb-20"
        >
            <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
                <div>
                    <h2 className="text-4xl font-black tracking-tighter leading-none">
                        SECURITY <span className="gradient-text">BLACKLIST</span>
                    </h2>
                    <p className="text-slate-400 dark:text-slate-600 mt-4 font-medium text-lg max-w-xl leading-relaxed">
                        Identity isolation layer. Manage blocked UUIDs and instantly protect your perimeter.
                    </p>
                </div>
                <div className="px-6 py-2.5 glass rounded-2xl flex items-center gap-3 border-orange-500/20 shadow-[0_0_30px_rgba(249,115,22,0.05)]">
                    <div className="w-2 h-2 rounded-full bg-orange-500 animate-pulse shadow-[0_0_10px_rgba(249,115,22,0.8)]" />
                    <span className="text-[10px] font-black text-orange-500 uppercase tracking-[0.2em]">Live Synchronization</span>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                <div className="lg:col-span-4 space-y-8">
                    <div className="glass-card p-10 rounded-[3rem] border-slate-200 dark:border-white/5 overflow-hidden group">
                        <div className="absolute top-0 right-0 w-32 h-32 bg-purple-500/5 blur-[80px] group-hover:bg-purple-500/10 transition-colors" />
                        <h3 className="text-xl font-bold mb-8 flex items-center gap-4">
                            <div className="p-3 bg-purple-100 dark:bg-purple-600/10 rounded-2xl border border-purple-200 dark:border-purple-500/20">
                                <Plus className="w-6 h-6 text-purple-400" />
                            </div>
                            Isolate Identity
                        </h3>
                        <form onSubmit={handleBlock} className="space-y-8">
                            <div className="space-y-3">
                                <label className="text-[10px] text-slate-500 dark:text-slate-500 uppercase font-black tracking-widest px-1">Visitor UUID</label>
                                <div className="relative group">
                                    <Hash className="w-4 h-4 absolute left-5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-600 group-focus-within:text-purple-400 transition-colors" />
                                    <input
                                        type="text"
                                        value={newUuid}
                                        onChange={(e) => setNewUuid(e.target.value)}
                                        placeholder="e.g. 550e8400-e29b-41d4..."
                                        className="w-full bg-white/[0.02] border border-slate-200 dark:border-white/5 rounded-2xl pl-12 pr-6 py-4 text-xs text-slate-800 dark:text-slate-200 outline-none focus:border-purple-500/30 focus:bg-white/[0.04] transition-all font-mono"
                                        required
                                    />
                                </div>
                            </div>
                            <button
                                type="submit"
                                disabled={blockMutation.isPending}
                                className="w-full bg-white text-black font-black uppercase tracking-widest py-4 rounded-2xl transition-all flex items-center justify-center gap-3 hover:scale-[1.02] hover:shadow-[0_20px_40px_-10px_rgba(255,255,255,0.2)] disabled:opacity-50"
                            >
                                <ShieldAlert className="w-5 h-5" />
                                {blockMutation.isPending ? 'EXECUTING...' : 'EXECUTE BLOCK'}
                            </button>
                        </form>
                    </div>

                    <div className="glass-card p-8 rounded-[2.5rem] border-orange-500/20 bg-orange-500/[0.02] relative overflow-hidden">
                        <div className="absolute -top-10 -right-10 w-40 h-40 bg-orange-500/5 blur-[50px]" />
                        <h3 className="text-base font-bold mb-4 flex items-center gap-3 text-orange-400">
                            <AlertTriangle className="w-5 h-5" />
                            Precautionary Notice
                        </h3>
                        <p className="text-sm text-slate-500 dark:text-slate-500 leading-relaxed font-medium">
                            Blacklisting a UUID propagates a <span className="text-slate-700 dark:text-slate-300">global dropping rule</span> across all Sentinel filters within 5ms. Actions are logged and attributed to your admin session.
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-8 glass-card rounded-[3rem] overflow-hidden border border-slate-200 dark:border-white/5 min-h-[600px] flex flex-col shadow-2xl relative">
                    <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-red-500/20 to-transparent" />

                    <div className="p-10 border-b border-slate-200 dark:border-white/5 flex justify-between items-center bg-white/[0.01]">
                        <div>
                            <h3 className="text-xl font-bold tracking-tight">Active Isolations</h3>
                            <p className="text-[10px] text-slate-500 dark:text-slate-500 font-bold uppercase tracking-widest mt-1 italic">Real-time Redis Pool View</p>
                        </div>
                        <div className="flex items-center gap-4">
                            <span className="w-2.5 h-2.5 rounded-full bg-red-500 shadow-[0_0_10px_rgba(239,68,68,0.5)] animate-pulse" />
                            <span className="bg-slate-200 dark:bg-white/5 border border-slate-300 dark:border-white/10 px-5 py-2 rounded-2xl text-xs font-black text-slate-700 dark:text-slate-300">
                                {blacklist.length} IDENTITIES
                            </span>
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
                        <AnimatePresence mode="popLayout">
                            {loading && blacklist.length === 0 ? (
                                <div className="h-64 flex items-center justify-center">
                                    <div className="flex flex-col items-center gap-4 opacity-20 animate-pulse">
                                        <Search className="w-12 h-12" />
                                        <p className="font-black text-xs uppercase tracking-widest">Scanning Redis...</p>
                                    </div>
                                </div>
                            ) : blacklist.length === 0 ? (
                                <div className="h-64 flex items-center justify-center">
                                    <div className="flex flex-col items-center gap-4 opacity-20">
                                        <UserCheck className="w-12 h-12" />
                                        <p className="font-black text-xs uppercase tracking-widest text-center">No identities isolated.<br /><span className="text-[10px] normal-case font-medium">Infrastructure is currently clean.</span></p>
                                    </div>
                                </div>
                            ) : blacklist.map((uuid, idx) => (
                                <motion.div
                                    layout
                                    initial={{ opacity: 0, scale: 0.95 }}
                                    animate={{ opacity: 1, scale: 1 }}
                                    exit={{ opacity: 0, scale: 0.95 }}
                                    transition={{ delay: idx * 0.05 }}
                                    key={uuid}
                                    className="p-6 rounded-[2rem] bg-white/[0.02] border border-white/[0.04] hover:border-red-500/20 hover:bg-red-500/[0.02] transition-all flex items-center justify-between group/row"
                                >
                                    <div className="flex items-center gap-6">
                                        <div className="w-14 h-14 rounded-2xl bg-red-500/10 flex items-center justify-center border border-red-500/10 group-hover/row:scale-110 transition-transform duration-500 shadow-inner">
                                            <UserX className="w-6 h-6 text-red-500" />
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-3">
                                                <code className="text-slate-100 font-mono text-sm tracking-tighter font-bold">{uuid}</code>
                                                <span className="px-2 py-0.5 rounded text-[8px] font-black bg-red-500/20 text-red-500 uppercase tracking-widest">Manual</span>
                                            </div>
                                            <div className="flex items-center gap-4 mt-2">
                                                <div className="flex items-center gap-1.5 opacity-40 group-hover/row:opacity-100 transition-opacity">
                                                    <Clock className="w-3 h-3" />
                                                    <span className="text-[9px] font-black uppercase tracking-widest">Isolated</span>
                                                </div>
                                                <div className="w-1 h-1 rounded-full bg-slate-700" />
                                                <div className="flex items-center gap-1.5 opacity-40 group-hover/row:opacity-100 transition-opacity">
                                                    <Hash className="w-3 h-3" />
                                                    <span className="text-[9px] font-black uppercase tracking-widest">JTI_RULE_V1</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <button
                                        onClick={() => handleUnblock(uuid)}
                                        disabled={unblockMutation.isPending && unblockMutation.variables === uuid}
                                        className="p-3 rounded-2xl bg-slate-200 dark:bg-white/5 border border-slate-200 dark:border-white/5 text-slate-500 dark:text-slate-500 hover:text-green-500 hover:bg-green-500/10 hover:border-green-500/20 transition-all flex items-center gap-2 group/unblock disabled:opacity-50"
                                    >
                                        <UserCheck className="w-5 h-5 group-hover/unblock:scale-125 transition-transform" />
                                        <span className="text-[10px] font-black uppercase tracking-widest pr-2 hidden sm:block">
                                            {(unblockMutation.isPending && unblockMutation.variables === uuid) ? 'Restoring...' : 'Restore Access'}
                                        </span>
                                        <ArrowRight className="w-3 h-3 opacity-0 group-hover/unblock:opacity-100 group-hover/unblock:translate-x-1 transition-all" />
                                    </button>
                                </motion.div>
                            ))}
                        </AnimatePresence>
                    </div>

                    <div className="p-8 bg-white/[0.01] border-t border-slate-200 dark:border-white/5 text-center">
                        <p className="text-[10px] text-slate-400 dark:text-slate-600 font-black uppercase tracking-[0.3em]">Sentinel Management Interface • Security V4.2</p>
                    </div>
                </div>
            </div>
        </motion.div>
    );
};

export default BlacklistView;
