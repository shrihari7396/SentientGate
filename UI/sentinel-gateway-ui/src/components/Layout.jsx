import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, ListFilter, ShieldAlert, Activity, Settings, Bell, Search, User } from 'lucide-react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs) {
    return twMerge(clsx(inputs));
}

const Layout = () => {
    const navItems = [
        { name: 'Dashboard', path: '/', icon: LayoutDashboard },
        { name: 'Admin Logs', path: '/logs', icon: ListFilter },
        { name: 'Blacklist', path: '/blacklist', icon: ShieldAlert },
        { name: 'Request Flow', path: '/flow', icon: Activity },
    ];

    return (
        <div className="min-h-screen flex bg-[#030303] text-slate-200">
            <div className="mesh-grid" />
            <div className="vignette" />

            {/* Sidebar */}
            <aside className="w-72 border-r border-white/5 bg-[#050505]/80 backdrop-blur-2xl flex flex-col sticky top-0 h-screen z-50">
                <div className="p-8">
                    <div className="flex items-center gap-3 group cursor-pointer">
                        <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-purple-600 to-blue-600 flex items-center justify-center purple-glow group-hover:scale-110 transition-transform duration-500">
                            <ShieldAlert className="w-6 h-6 text-white" />
                        </div>
                        <div>
                            <h1 className="text-xl font-black tracking-tighter gradient-text uppercase">Sentient</h1>
                            <p className="text-[10px] text-slate-500 font-bold tracking-[0.2em] uppercase -mt-1">Gate Core</p>
                        </div>
                    </div>
                </div>

                <nav className="flex-1 px-4 mt-4 space-y-2">
                    <p className="px-4 text-[10px] font-black text-slate-600 uppercase tracking-widest mb-4">Monitoring</p>
                    {navItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) =>
                                cn(
                                    "flex items-center gap-3 px-4 py-3.5 rounded-2xl transition-all duration-300 group relative overflow-hidden",
                                    isActive
                                        ? "bg-purple-600/10 text-white shadow-[inset_0_0_20px_rgba(168,85,247,0.05)]"
                                        : "text-slate-500 hover:text-slate-300 hover:bg-white/[0.02]"
                                )
                            }
                        >
                            {({ isActive }) => (
                                <>
                                    {isActive && <div className="absolute left-0 top-1/4 bottom-1/4 w-1 bg-purple-500 rounded-full" />}
                                    <item.icon className={cn("w-5 h-5 transition-colors duration-300", isActive ? "text-purple-400" : "group-hover:text-purple-400/50")} />
                                    <span className="font-semibold tracking-tight">{item.name}</span>
                                    {isActive && <div className="ml-auto w-1.5 h-1.5 rounded-full bg-purple-500 animate-pulse shadow-[0_0_10px_rgba(168,85,247,0.8)]" />}
                                </>
                            )}
                        </NavLink>
                    ))}
                </nav>

                <div className="p-6 mt-auto">
                    <div className="glass-card p-4 rounded-2xl border-white/10 bg-gradient-to-br from-purple-500/5 to-transparent">
                        <p className="text-xs font-bold text-purple-400 mb-1">PRO PROTECTION</p>
                        <p className="text-[10px] text-slate-500 leading-relaxed mb-3">Your security infrastructure is operating at 99.9% efficiency.</p>
                        <div className="h-1 bg-white/5 rounded-full overflow-hidden">
                            <div className="h-full w-[99.9%] bg-purple-500 shadow-[0_0_10px_rgba(168,85,247,0.5)]" />
                        </div>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 flex flex-col relative z-10 w-full overflow-x-hidden">
                <header className="h-20 border-b border-white/5 flex items-center justify-between px-10 bg-[#050505]/40 backdrop-blur-xl sticky top-0 z-40">
                    <div className="flex items-center gap-6 flex-1">
                        <div className="relative w-96 group">
                            <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-purple-400 transition-colors" />
                            <input
                                type="text"
                                placeholder="Search logs, IPs, or events..."
                                className="w-full bg-white/[0.03] border border-white/5 rounded-2xl pl-12 pr-4 py-2.5 text-xs outline-none focus:border-purple-500/30 focus:bg-white/[0.05] transition-all"
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-6">
                        <button className="relative p-2.5 rounded-xl hover:bg-white/5 text-slate-400 transition-all group">
                            <Bell className="w-5 h-5 group-hover:text-white" />
                            <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-red-500 rounded-full border-2 border-[#050505] shadow-[0_0_10px_rgba(239,68,68,0.5)]"></span>
                        </button>
                        <div className="h-8 w-[1px] bg-white/5" />
                        <div className="flex items-center gap-3 pl-2">
                            <div className="text-right">
                                <p className="text-xs font-bold text-white leading-none">Admin Core</p>
                                <p className="text-[10px] text-green-500 font-bold uppercase tracking-tighter mt-1 animate-pulse">Online</p>
                            </div>
                            <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-slate-800 to-slate-950 border border-white/10 flex items-center justify-center hover:scale-105 transition-transform cursor-pointer">
                                <User className="w-5 h-5 text-slate-400" />
                            </div>
                        </div>
                    </div>
                </header>

                <div className="p-10 max-w-screen-2xl mx-auto w-full">
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default Layout;
