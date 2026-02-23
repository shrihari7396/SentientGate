import React, { useEffect, useState } from 'react';
import { Activity, ShieldCheck, ShieldAlert, Zap, ArrowUpRight, ArrowDownRight, Terminal } from 'lucide-react';
import { motion } from 'framer-motion';
import StatsChart from '../components/StatsChart';

const StatCard = ({ title, value, change, icon: Icon, trend, delay = 0 }) => (
    <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay }}
        className="glass-card p-6 rounded-2xl relative overflow-hidden"
    >
        <div className="flex justify-between items-start mb-4">
            <div className="p-2 bg-white/5 rounded-lg border border-white/5">
                <Icon className="w-6 h-6 text-purple-400" />
            </div>
            {trend && (
                <span className={`flex items-center text-[10px] font-bold px-2 py-1 rounded-full ${trend === 'up' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
                    }`}>
                    {trend === 'up' ? <ArrowUpRight className="w-3 h-3 mr-1" /> : <ArrowDownRight className="w-3 h-3 mr-1" />}
                    {change}%
                </span>
            )}
        </div>
        <h3 className="text-slate-500 text-xs font-bold uppercase tracking-wider">{title}</h3>
        <p className="text-3xl font-bold mt-2 tracking-tight flex items-baseline gap-2">
            {value}
            <span className="text-[10px] text-slate-600 font-normal">REAL-TIME</span>
        </p>
        <div className="absolute bottom-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-purple-500/20 to-transparent"></div>
    </motion.div>
);

const Dashboard = () => {
    return (
        <div className="space-y-8 animate-in">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
                <div>
                    <h2 className="text-4xl font-black tracking-tighter gradient-text">COMMAND CENTER</h2>
                    <p className="text-slate-400 mt-2 font-medium">Monitoring the heartbeat of the SentientGate Infrastructure.</p>
                </div>
                <div className="flex bg-[#0d0d0f] border border-white/5 rounded-xl p-1 shadow-inner">
                    <button className="px-4 py-2 bg-white/5 text-slate-200 rounded-lg text-xs font-bold transition-all">Live Metrics</button>
                    <button className="px-4 py-2 text-slate-500 rounded-lg text-xs font-bold hover:text-slate-300 transition-all">Historical</button>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard title="Throughput" value="1.2k/s" change="12" icon={Activity} trend="up" delay={0.1} />
                <StatCard title="Blocked" value="482" change="8" icon={ShieldAlert} trend="up" delay={0.2} />
                <StatCard title="P99 Latency" value="28ms" change="14" icon={Zap} trend="down" delay={0.3} />
                <StatCard title="Active UVs" value="8.4k" change="3" icon={ShieldCheck} trend="up" delay={0.4} />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 glass-card p-8 rounded-3xl h-[450px] border-white/5 shadow-2xl relative">
                    <div className="flex justify-between items-center mb-8">
                        <h3 className="text-lg font-bold flex items-center gap-2">
                            <Activity className="w-5 h-5 text-purple-500" />
                            Traffic Velocity
                        </h3>
                        <div className="flex gap-4">
                            <div className="flex items-center gap-2">
                                <span className="w-2 h-2 rounded-full bg-purple-500"></span>
                                <span className="text-[10px] text-slate-500 font-bold uppercase">Requests</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className="w-2 h-2 rounded-full bg-red-500"></span>
                                <span className="text-[10px] text-slate-500 font-bold uppercase">Errors</span>
                            </div>
                        </div>
                    </div>
                    <div className="h-[320px]">
                        <StatsChart type="requests" />
                    </div>
                </div>

                <div className="glass-card p-8 rounded-3xl flex flex-col border-white/5 shadow-2xl">
                    <h3 className="text-lg font-bold mb-8 flex items-center gap-2">
                        <Terminal className="w-5 h-5 text-blue-500" />
                        Security Shield
                    </h3>
                    <div className="flex-1 space-y-8">
                        {[
                            { label: 'Rate Limits', value: 85, color: 'bg-purple-500', icon: Activity },
                            { label: 'Auth Failures', value: 12, color: 'bg-blue-500', icon: Zap },
                            { label: 'Blacklist Hits', value: 3, color: 'bg-red-500', icon: ShieldAlert },
                        ].map((item) => (
                            <div key={item.label} className="space-y-3">
                                <div className="flex justify-between items-center">
                                    <div className="flex items-center gap-3">
                                        <div className={`p-2 rounded-lg ${item.color.replace('bg-', 'bg-')}/10`}>
                                            <item.icon className={`w-4 h-4 ${item.color.replace('bg-', 'text-')}`} />
                                        </div>
                                        <span className="text-slate-400 text-sm font-medium">{item.label}</span>
                                    </div>
                                    <span className="font-bold text-slate-200">{item.value}%</span>
                                </div>
                                <div className="h-1.5 w-full bg-white/5 rounded-full overflow-hidden">
                                    <motion.div
                                        initial={{ width: 0 }}
                                        animate={{ width: `${item.value}%` }}
                                        transition={{ duration: 1, ease: "easeOut" }}
                                        className={`h-full ${item.color} shadow-[0_0_10px_rgba(168,85,247,0.3)]`}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="mt-8 p-4 bg-purple-500/5 rounded-2xl border border-purple-500/10">
                        <p className="text-[10px] text-slate-500 leading-relaxed italic">
                            "Sentinel Core is currently processing 1,420 events per bucket. System health is optimal."
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
