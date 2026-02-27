import React, { useState, useEffect } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { LayoutDashboard, ListFilter, ShieldAlert, Activity, Settings, Bell, Search, User, Sun, Moon, Server, Network, Zap, Layers, Database } from 'lucide-react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { motion, AnimatePresence } from 'framer-motion';

function cn(...inputs) {
    return twMerge(clsx(inputs));
}

const Layout = () => {
    const location = useLocation();

    // Theme Management
    const [theme, setTheme] = useState(() => {
        if (typeof window !== 'undefined') {
            const saved = localStorage.getItem('theme');
            return saved || 'dark';
        }
        return 'dark';
    });

    useEffect(() => {
        const root = window.document.documentElement;
        if (theme === 'dark') {
            root.classList.add('dark');
        } else {
            root.classList.remove('dark');
        }
        localStorage.setItem('theme', theme);
    }, [theme]);

    const toggleTheme = () => setTheme(theme === 'dark' ? 'light' : 'dark');

    const navItems = [
        { name: 'Dashboard', path: '/', icon: LayoutDashboard },
        { name: 'Service Registry', path: '/registry', icon: Network, isSubItem: true },
        { name: 'Infrastructure Node', path: '/infrastructure', icon: Server },
        { name: 'Kafka Cluster', path: '/infrastructure/kafka', icon: Zap, isSubItem: true },
        { name: 'Redis Cache', path: '/infrastructure/redis', icon: Layers, isSubItem: true },
        { name: 'PostgreSQL', path: '/infrastructure/postgres', icon: Database, isSubItem: true },
        { name: 'Admin Logs', path: '/logs', icon: ListFilter },
        { name: 'Blacklist', path: '/blacklist', icon: ShieldAlert },
        { name: 'Request Flow', path: '/flow', icon: Activity },
    ];

    return (
        <div className="min-h-screen flex bg-slate-50 text-slate-900 dark:bg-[#030303] dark:text-slate-200 transition-colors duration-500">
            <div className="mesh-grid" />
            <div className="vignette" />

            {/* Background Orbs */}
            <div className="fixed top-[-10%] left-[-10%] w-[40%] h-[40%] bg-purple-600/10 dark:bg-purple-600/20 rounded-full blur-[120px] mix-blend-multiply dark:mix-blend-screen animate-float pointer-events-none z-0" />
            <div className="fixed bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-600/10 dark:bg-blue-600/20 rounded-full blur-[120px] mix-blend-multiply dark:mix-blend-screen animate-float pointer-events-none z-0" style={{ animationDelay: '2s' }} />

            {/* Sidebar */}
            <aside className="w-72 border-r border-slate-200 dark:border-white/5 bg-white/80 dark:bg-[#050505]/80 backdrop-blur-2xl flex flex-col sticky top-0 h-screen z-50 transition-colors duration-500 overflow-y-auto">
                <div className="p-8 shrink-0">
                    <div className="flex items-center gap-4 group cursor-pointer">
                        <div className="w-12 h-12 rounded-[1.25rem] bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-lg group-hover:shadow-purple-500/50 group-hover:scale-[1.05] transition-all duration-500 relative overflow-hidden">
                            <div className="absolute inset-0 bg-white/20 opacity-0 group-hover:opacity-100 transition-opacity" />
                            <ShieldAlert className="w-6 h-6 text-white relative z-10" />
                        </div>
                        <div>
                            <h1 className="text-2xl font-black tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-slate-900 via-purple-800 to-slate-900 dark:from-white dark:via-purple-200 dark:to-white">Sentient</h1>
                            <p className="text-[10px] text-slate-500 font-bold tracking-[0.2em] uppercase -mt-0.5">Gateway Core</p>
                        </div>
                    </div>
                </div>

                <nav className="flex-1 px-4 space-y-1.5 pb-8">
                    <p className="px-4 text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest mb-4">Command Center</p>
                    {navItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) =>
                                cn(
                                    "flex items-center gap-3 px-4 py-3 rounded-[1rem] transition-all duration-300 group relative overflow-hidden",
                                    item.isSubItem ? "ml-6 pl-5 text-sm border-l border-slate-200 dark:border-white/10 rounded-l-none my-0.5 py-2.5" : "font-semibold tracking-wide",
                                    isActive
                                        ? "text-purple-700 dark:text-purple-300 bg-purple-50 dark:bg-purple-900/20"
                                        : "text-slate-500 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-white/[0.04]"
                                )
                            }
                        >
                            {({ isActive }) => (
                                <>
                                    {isActive && <motion.div layoutId="leftBar" className="absolute left-0 top-1/4 bottom-1/4 w-1 bg-purple-500 rounded-r-full z-10" />}
                                    <item.icon className={cn("transition-colors duration-300 relative z-10", item.isSubItem ? "w-4 h-4" : "w-5 h-5", isActive ? "text-purple-600 dark:text-purple-400" : "group-hover:text-purple-500/70")} />
                                    <span className="relative z-10">{item.name}</span>
                                </>
                            )}
                        </NavLink>
                    ))}
                </nav>

                <div className="p-6 mt-auto">
                    <div className="relative overflow-hidden p-5 rounded-[1.5rem] bg-gradient-to-br from-purple-500/10 via-fuchsia-500/5 to-transparent border border-purple-500/10">
                        <div className="absolute top-0 right-0 p-4 opacity-10"><Activity className="w-16 h-16" /></div>
                        <p className="text-xs font-bold text-slate-900 dark:text-white mb-1 tracking-wide">System Health</p>
                        <p className="text-[10px] text-slate-500 leading-relaxed mb-4 pr-4">All nodes operating at optimal efficiency.</p>
                        <div className="h-1.5 bg-slate-200 dark:bg-white/10 rounded-full overflow-hidden">
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: "99.9%" }}
                                transition={{ duration: 1.5, ease: "easeOut" }}
                                className="h-full bg-gradient-to-r from-purple-500 to-indigo-500 relative"
                            >
                                <div className="absolute inset-0 bg-white/30 animate-pulse" />
                            </motion.div>
                        </div>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 flex flex-col relative z-10 w-full overflow-x-hidden min-h-screen">
                <header className="h-24 flex shrink-0 items-center justify-between px-10 bg-background/60 backdrop-blur-xl border-b border-border sticky top-0 z-40">
                    <div className="flex-1 max-w-md">
                        <div className="relative group flex items-center">
                            <Search className="w-4 h-4 absolute left-4 text-slate-400 group-focus-within:text-purple-500 transition-colors" />
                            <input
                                type="text"
                                placeholder="Search logs, IPs, or events..."
                                className="w-full bg-slate-100/50 dark:bg-white/5 border border-transparent shadow-sm dark:shadow-none rounded-2xl pl-12 pr-4 py-3 text-sm outline-none focus:border-purple-500/30 focus:bg-white dark:focus:bg-white/10 transition-all placeholder:text-slate-400"
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-4">
                        <button
                            onClick={toggleTheme}
                            className="p-3 rounded-[1rem] hover:bg-slate-100 dark:hover:bg-white/10 text-slate-500 transition-colors"
                        >
                            {theme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
                        </button>

                        <button className="relative p-3 rounded-[1rem] hover:bg-slate-100 dark:hover:bg-white/10 text-slate-500 transition-colors">
                            <Bell className="w-5 h-5" />
                            <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-background" />
                        </button>

                        <div className="w-px h-8 bg-border mx-2" />

                        <div className="flex items-center gap-3 bg-slate-100/50 dark:bg-white/5 pr-4 pl-1.5 py-1.5 rounded-full border border-border">
                            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white shadow-md">
                                <User className="w-4 h-4" />
                            </div>
                            <div className="flex flex-col">
                                <span className="text-xs font-bold leading-none">Admin</span>
                                <span className="text-[10px] text-emerald-500 font-medium tracking-wide">Online</span>
                            </div>
                        </div>
                    </div>
                </header>

                <div className="p-10 mx-auto w-full max-w-[1600px] flex-1">
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default Layout;
