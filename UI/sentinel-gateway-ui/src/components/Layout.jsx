import React, { useState, useEffect } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { LayoutDashboard, ListFilter, ShieldAlert, Activity, Settings, Bell, Search, User, Sun, Moon } from 'lucide-react';
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
            <aside className="w-72 border-r border-slate-200 dark:border-white/5 bg-white/80 dark:bg-[#050505]/80 backdrop-blur-2xl flex flex-col sticky top-0 h-screen z-50 transition-colors duration-500">
                <div className="p-8">
                    <div className="flex items-center gap-3 group cursor-pointer">
                        <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-purple-600 to-blue-600 flex items-center justify-center purple-glow group-hover:scale-110 transition-transform duration-500 shadow-xl">
                            <ShieldAlert className="w-6 h-6 text-white" />
                        </div>
                        <div>
                            <h1 className="text-xl font-black tracking-tighter gradient-text uppercase">Sentient</h1>
                            <p className="text-[10px] text-slate-500 font-bold tracking-[0.2em] uppercase -mt-1">Gate Core</p>
                        </div>
                    </div>
                </div>

                <nav className="flex-1 px-4 mt-4 space-y-2">
                    <p className="px-4 text-[10px] font-black text-slate-400 dark:text-slate-600 uppercase tracking-widest mb-4 transition-colors">Monitoring</p>
                    {navItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) =>
                                cn(
                                    "flex items-center gap-3 px-4 py-3.5 rounded-2xl transition-all duration-300 group relative overflow-hidden",
                                    isActive
                                        ? "text-purple-700 dark:text-white shadow-[inset_0_0_20px_rgba(168,85,247,0.05)]"
                                        : "text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 hover:bg-slate-100 dark:hover:bg-white/[0.02]"
                                )
                            }
                        >
                            {({ isActive }) => (
                                <>
                                    {isActive && (
                                        <motion.div
                                            layoutId="activeNavIndicator"
                                            className="absolute inset-0 bg-purple-600/10 rounded-2xl"
                                            initial={false}
                                            transition={{ type: "spring", stiffness: 300, damping: 30 }}
                                        />
                                    )}
                                    {isActive && <motion.div layoutId="leftBar" className="absolute left-0 top-1/4 bottom-1/4 w-1 bg-purple-500 rounded-full z-10" />}
                                    <item.icon className={cn("w-5 h-5 transition-colors duration-300 relative z-10", isActive ? "text-purple-400" : "group-hover:text-purple-400/50")} />
                                    <span className="font-semibold tracking-tight relative z-10">{item.name}</span>
                                    {isActive && <div className="ml-auto w-1.5 h-1.5 rounded-full bg-purple-500 animate-pulse shadow-[0_0_10px_rgba(168,85,247,0.8)] relative z-10" />}
                                </>
                            )}
                        </NavLink>
                    ))}
                </nav>

                <div className="p-6 mt-auto">
                    <div className="glass-card p-4 rounded-2xl bg-gradient-to-br from-purple-500/5 to-transparent">
                        <p className="text-xs font-bold text-purple-600 dark:text-purple-400 mb-1">PRO PROTECTION</p>
                        <p className="text-[10px] text-slate-500 leading-relaxed mb-3">Your security infrastructure is operating at 99.9% efficiency.</p>
                        <div className="h-1 bg-slate-200 dark:bg-white/5 rounded-full overflow-hidden">
                            <div className="h-full w-[99.9%] bg-purple-500 shadow-[0_0_10px_rgba(168,85,247,0.5)]" />
                        </div>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 flex flex-col relative z-10 w-full overflow-x-hidden">
                <header className="h-20 border-b border-slate-200 dark:border-white/5 flex items-center justify-between px-10 bg-white/40 dark:bg-[#050505]/40 backdrop-blur-xl sticky top-0 z-40 transition-colors duration-500">
                    <div className="flex items-center gap-6 flex-1">
                        <div className="relative w-96 group">
                            <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-purple-500 dark:group-focus-within:text-purple-400 transition-colors" />
                            <input
                                type="text"
                                placeholder="Search logs, IPs, or events..."
                                className="w-full bg-white dark:bg-white/[0.03] border border-slate-200 dark:border-white/5 shadow-sm dark:shadow-none rounded-2xl pl-12 pr-4 py-2.5 text-xs outline-none focus:border-purple-500/30 focus:shadow-md dark:focus:bg-white/[0.05] transition-all"
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-6">
                        <button
                            onClick={toggleTheme}
                            className="relative p-2.5 rounded-xl hover:bg-slate-100 dark:hover:bg-white/5 text-slate-500 dark:text-slate-400 transition-all group"
                            title="Toggle Theme"
                        >
                            {theme === 'dark' ? (
                                <Sun className="w-5 h-5 group-hover:text-yellow-400 transition-colors" />
                            ) : (
                                <Moon className="w-5 h-5 group-hover:text-purple-600 transition-colors" />
                            )}
                        </button>

                        <button className="relative p-2.5 rounded-xl hover:bg-slate-100 dark:hover:bg-white/5 text-slate-500 dark:text-slate-400 transition-all group">
                            <Bell className="w-5 h-5 group-hover:text-slate-900 dark:group-hover:text-white" />
                            <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white dark:border-[#050505] shadow-[0_0_10px_rgba(239,68,68,0.5)]"></span>
                        </button>
                        <div className="h-8 w-[1px] bg-slate-200 dark:bg-white/5" />
                        <div className="flex items-center gap-3 pl-2">
                            <div className="text-right">
                                <p className="text-xs font-bold text-slate-900 dark:text-white leading-none">Admin Core</p>
                                <p className="text-[10px] text-green-500 font-bold uppercase tracking-tighter mt-1 animate-pulse">Online</p>
                            </div>
                            <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-200 dark:from-slate-800 dark:to-slate-950 border border-slate-200 dark:border-white/10 flex items-center justify-center hover:scale-105 transition-transform cursor-pointer shadow-sm">
                                <User className="w-5 h-5 text-slate-500 dark:text-slate-400" />
                            </div>
                        </div>
                    </div>
                </header>

                <div className="p-10 max-w-screen-2xl mx-auto w-full flex-1 relative overflow-hidden">
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={location.pathname}
                            initial={{ opacity: 0, y: 15 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -15 }}
                            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
                            className="h-full w-full"
                        >
                            <Outlet />
                        </motion.div>
                    </AnimatePresence>
                </div>
            </main>
        </div>
    );
};

export default Layout;
