import React, { useEffect, useState } from 'react';
import { mgmtApi } from '../api/client';
import { ShieldAlert, UserX, UserCheck, Search, AlertTriangle, Plus } from 'lucide-react';

const BlacklistView = () => {
    const [blacklist, setBlacklist] = useState([]);
    const [loading, setLoading] = useState(true);
    const [newUuid, setNewUuid] = useState('');

    const fetchBlacklist = () => {
        setLoading(true);
        mgmtApi.getBlacklist()
            .then(res => {
                setBlacklist(res.data);
                setLoading(false);
            })
            .catch(err => {
                console.error(err);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchBlacklist();
    }, []);

    const handleBlock = (e) => {
        e.preventDefault();
        if (!newUuid) return;
        mgmtApi.blockUuid(newUuid).then(() => {
            setNewUuid('');
            fetchBlacklist();
        });
    };

    const handleUnblock = (uuid) => {
        mgmtApi.unblockUuid(uuid).then(() => {
            fetchBlacklist();
        });
    };

    return (
        <div className="space-y-6 animate-in">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Redis Blacklist</h2>
                    <p className="text-slate-400 mt-2">Manage blocked identities and prevent unauthorized access.</p>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-1 space-y-6">
                    <div className="glass-card p-6 rounded-2xl">
                        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                            <Plus className="w-5 h-5 text-purple-400" />
                            Block New Identity
                        </h3>
                        <form onSubmit={handleBlock} className="space-y-4">
                            <div>
                                <label className="text-xs text-slate-500 uppercase font-bold mb-1 block">Visitor UUID</label>
                                <input
                                    type="text"
                                    value={newUuid}
                                    onChange={(e) => setNewUuid(e.target.value)}
                                    placeholder="e.g. 550e8400-e29b-41d4-a716..."
                                    className="w-full bg-[#0d0d0f] border border-white/10 rounded-lg px-4 py-2 text-sm text-slate-200 outline-none focus:border-purple-500/50"
                                    required
                                />
                            </div>
                            <button
                                type="submit"
                                className="w-full bg-purple-600 hover:bg-purple-700 text-white font-medium py-2 rounded-lg transition-colors flex items-center justify-center gap-2"
                            >
                                <ShieldAlert className="w-4 h-4" />
                                Add to Blacklist
                            </button>
                        </form>
                    </div>

                    <div className="glass-card p-6 rounded-2xl border-orange-500/20 bg-orange-500/[0.02]">
                        <h3 className="text-lg font-semibold mb-2 flex items-center gap-2 text-orange-400">
                            <AlertTriangle className="w-5 h-5" />
                            Caution
                        </h3>
                        <p className="text-sm text-slate-400 leading-relaxed">
                            Blacklisting a UUID will immediately drop all incoming requests from that identity across all services. This action takes effect globally within milliseconds via Redis.
                        </p>
                    </div>
                </div>

                <div className="lg:col-span-2 glass-card rounded-2xl overflow-hidden border border-white/5">
                    <div className="p-6 border-b border-white/5 flex justify-between items-center">
                        <h3 className="font-semibold">Currently Blacklisted</h3>
                        <span className="bg-white/5 px-3 py-1 rounded-full text-xs text-slate-400">
                            {blacklist.length} Identities
                        </span>
                    </div>
                    <div className="divide-y divide-white/5 max-h-[600px] overflow-y-auto">
                        {loading ? (
                            <div className="p-8 text-center text-slate-500">Loading blacklist...</div>
                        ) : blacklist.length === 0 ? (
                            <div className="p-12 text-center text-slate-500 italic">No identities blocked.</div>
                        ) : blacklist.map((uuid) => (
                            <div key={uuid} className="p-4 flex items-center justify-between hover:bg-white/[0.01] transition-colors">
                                <div className="flex items-center gap-4">
                                    <div className="w-10 h-10 rounded-full bg-red-500/10 flex items-center justify-center">
                                        <UserX className="w-5 h-5 text-red-500" />
                                    </div>
                                    <div>
                                        <code className="text-slate-200 text-sm">{uuid}</code>
                                        <p className="text-[10px] text-slate-500 mt-0.5">MANUAL_BLOCK • PERSISTENT</p>
                                    </div>
                                </div>
                                <button
                                    onClick={() => handleUnblock(uuid)}
                                    className="px-3 py-1.5 text-xs font-medium text-slate-400 hover:text-green-400 hover:bg-green-500/10 rounded-lg transition-all flex items-center gap-2"
                                >
                                    <UserCheck className="w-4 h-4" />
                                    Unblock
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default BlacklistView;
