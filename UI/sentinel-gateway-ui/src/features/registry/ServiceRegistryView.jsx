import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { actuatorApi } from '../../shared/api/actuatorApi';
import { Network, ServerCog, Activity, WifiOff, Settings2, Globe, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

const ServiceRegistryView = () => {
    const [eurekaUrl, setEurekaUrl] = useState('http://localhost:8761/');
    const [activeUrl, setActiveUrl] = useState('');
    const navigate = useNavigate();

    const { data, isLoading, isError, refetch, isFetching } = useQuery({
        queryKey: ['eurekaApps', activeUrl],
        queryFn: () => actuatorApi.getEurekaApps(activeUrl),
        enabled: !!activeUrl,
        retry: 1,
        onError: () => toast.error('Failed to connect to Eureka Server. Check CORS and the URL.'),
    });

    const handleConnect = (e) => {
        e.preventDefault();
        if (!eurekaUrl) return;
        setActiveUrl(eurekaUrl);
    };

    // Extract applications array gracefully 
    const apps = data?.applications?.application || [];

    // Normalize if only one app is returned (Eureka sometimes returns an object instead of array for length=1)
    const appList = Array.isArray(apps) ? apps : [apps];

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="space-y-10 pb-20"
        >
            {/* Header Area */}
            <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8">
                <div className="relative group">
                    <div className="absolute -inset-4 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-3xl blur-2xl opacity-10 group-hover:opacity-20 transition-opacity duration-1000" />
                    <div className="relative">
                        <h2 className="text-4xl lg:text-5xl font-black tracking-tighter leading-none flex items-center gap-4">
                            SERVICE <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-500 to-cyan-400">REGISTRY</span>
                        </h2>
                        <p className="text-slate-500 mt-4 font-medium text-lg max-w-xl leading-relaxed">
                            Discover and monitor microservices via Eureka and Spring Boot Actuator.
                        </p>
                    </div>
                </div>
            </div>

            {/* Connection Input Card */}
            <div className="glass-card p-6 md:p-8 rounded-[2.5rem] border border-slate-200 dark:border-white/5 shadow-xl relative overflow-hidden">
                <div className="absolute top-0 right-0 p-8 opacity-5">
                    <Network className="w-32 h-32" />
                </div>

                <form onSubmit={handleConnect} className="relative z-10 flex flex-col md:flex-row gap-4">
                    <div className="flex-1 relative group">
                        <Globe className="absolute left-6 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-cyan-500 transition-colors duration-300" />
                        <input
                            type="text"
                            value={eurekaUrl}
                            onChange={(e) => setEurekaUrl(e.target.value)}
                            placeholder="Enter Eureka Server URL (e.g. http://localhost:8761)"
                            className="w-full bg-slate-100 dark:bg-white/5 border border-slate-200 dark:border-white/10 rounded-2xl pl-16 pr-6 py-4 text-sm font-mono text-slate-800 dark:text-slate-200 focus:outline-none focus:border-cyan-500/50 focus:bg-white dark:focus:bg-white/10 transition-all placeholder:text-slate-400"
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={isFetching}
                        className="px-10 py-4 bg-gradient-to-r from-blue-600 to-cyan-600 hover:from-blue-500 hover:to-cyan-500 text-white rounded-2xl font-black uppercase tracking-widest text-xs shadow-lg shadow-cyan-500/25 transition-all disabled:opacity-50 flex items-center justify-center gap-3 shrink-0 group"
                    >
                        {isFetching ? (
                            <>
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                Synchronizing
                            </>
                        ) : (
                            <>
                                Connect <Globe className="w-4 h-4 group-hover:rotate-12 transition-transform" />
                            </>
                        )}
                    </button>
                </form>
            </div>

            {/* Results UI */}
            <AnimatePresence mode="wait">
                {isError && (
                    <motion.div
                        initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }}
                        className="glass-card p-8 rounded-[2.5rem] flex flex-col items-center justify-center text-center space-y-4 border-rose-500/20 bg-rose-500/[0.02]"
                    >
                        <WifiOff className="w-12 h-12 text-rose-500 mb-2" />
                        <h3 className="text-xl font-bold text-slate-900 dark:text-white">Connection Refused</h3>
                        <p className="text-slate-500 dark:text-slate-400 max-w-md">Ensure the Eureka Server is running and allows CORS requests from this UI origin.</p>
                    </motion.div>
                )}

                {activeUrl && !isError && appList.length === 0 && !isLoading && (
                    <motion.div
                        initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }}
                        className="glass-card p-12 rounded-[2.5rem] flex flex-col items-center justify-center text-center space-y-4"
                    >
                        <ServerCog className="w-16 h-16 text-slate-300 dark:text-slate-700 mb-4" />
                        <h3 className="text-2xl font-black text-slate-500 tracking-tight">No Services Registered</h3>
                        <p className="text-slate-400">The Eureka server is online, but no microservices are currently registered.</p>
                    </motion.div>
                )}

                {appList.length > 0 && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
                        className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6"
                    >
                        {appList.map((app, idx) => {
                            // Normalize instances
                            const instances = Array.isArray(app.instance) ? app.instance : [app.instance];

                            return (
                                <div key={app.name || idx} className="glass-card p-6 rounded-[2rem] border border-slate-200 dark:border-white/5 hover:border-cyan-500/30 transition-colors group relative overflow-hidden flex flex-col">
                                    <div className="absolute top-0 right-0 w-32 h-32 bg-cyan-500/5 blur-[50px] pointer-events-none" />

                                    <div className="flex justify-between items-start mb-6 z-10">
                                        <div className="flex items-center gap-4">
                                            <div className="w-12 h-12 rounded-[1.2rem] bg-cyan-500/10 flex items-center justify-center border border-cyan-500/20 text-cyan-600 dark:text-cyan-400">
                                                <Settings2 className="w-6 h-6" />
                                            </div>
                                            <div>
                                                <h3 className="text-lg font-black tracking-tighter uppercase text-slate-900 dark:text-white">{app.name}</h3>
                                                <p className="text-[10px] text-slate-500 font-bold uppercase tracking-[0.2em] mt-0.5">
                                                    {instances.length} INSTANCE{instances.length !== 1 ? 'S' : ''}
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="space-y-3 z-10 flex-1">
                                        {instances.map(inst => {
                                            const isUp = inst.status === 'UP';
                                            return (
                                                <div key={inst.instanceId} className="bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-white/5 rounded-2xl p-4 flex items-center justify-between group/inst">
                                                    <div className="flex flex-col truncate pr-4">
                                                        <span className="text-xs font-mono font-bold text-slate-700 dark:text-slate-300 truncate">
                                                            {inst.instanceId || `${inst.ipAddr}:${inst.port?.['$']}`}
                                                        </span>
                                                        <span className="text-[10px] text-slate-500 mt-1 truncate">{inst.healthCheckUrl}</span>
                                                    </div>

                                                    <div className="flex items-center gap-3 shrink-0">
                                                        <div className={`px-2 py-1 rounded text-[9px] font-black uppercase tracking-widest ${isUp ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' : 'bg-rose-500/10 text-rose-600 dark:text-rose-400'}`}>
                                                            {inst.status}
                                                        </div>
                                                        <button
                                                            className="w-8 h-8 rounded-full bg-slate-200 dark:bg-white/10 flex items-center justify-center hover:bg-cyan-500 hover:text-white transition-colors"
                                                            title="View Actuator Telemetry"
                                                            onClick={() => navigate(`/infrastructure/service/${app.name}`, {
                                                                state: {
                                                                    instance: inst,
                                                                    actuatorUrl: inst.healthCheckUrl?.replace('/health', '') || `${inst.homePageUrl}actuator`
                                                                }
                                                            })}
                                                        >
                                                            <Activity className="w-4 h-4" />
                                                        </button>
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            );
                        })}
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
};

export default ServiceRegistryView;
