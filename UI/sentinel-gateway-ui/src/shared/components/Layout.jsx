import React, { useState, useEffect, useMemo, useRef } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
    LayoutDashboard,
    ListFilter,
    ShieldAlert,
    Activity,
    Bell,
    Search,
    User,
    Sun,
    Moon,
    Server,
    Network,
    Zap,
    Layers,
    Database,
    Menu,
    X,
    Command,
    ArrowRight,
} from 'lucide-react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { motion, AnimatePresence } from 'framer-motion';

function cn(...inputs) {
    return twMerge(clsx(inputs));
}

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

const Layout = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const searchInputRef = useRef(null);

    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [commandQuery, setCommandQuery] = useState('');

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

    useEffect(() => {
        setSidebarOpen(false);
        setCommandQuery('');
    }, [location.pathname]);

    useEffect(() => {
        const onKeyDown = (event) => {
            if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
                event.preventDefault();
                searchInputRef.current?.focus();
            }
        };

        window.addEventListener('keydown', onKeyDown);
        return () => window.removeEventListener('keydown', onKeyDown);
    }, []);

    const toggleTheme = () => setTheme(theme === 'dark' ? 'light' : 'dark');

    const matchedItems = useMemo(() => {
        const query = commandQuery.trim().toLowerCase();
        if (!query) return [];
        return navItems.filter((item) => item.name.toLowerCase().includes(query) || item.path.toLowerCase().includes(query));
    }, [commandQuery]);

    const currentPageLabel = useMemo(() => {
        const exactMatch = navItems.find((item) => item.path === location.pathname);
        if (exactMatch) return exactMatch.name;

        const nestedMatch = navItems.find(
            (item) => item.path !== '/' && location.pathname.startsWith(item.path),
        );

        return nestedMatch?.name || 'Overview';
    }, [location.pathname]);

    const onSearchSubmit = (event) => {
        event.preventDefault();

        if (!commandQuery.trim()) {
            searchInputRef.current?.focus();
            return;
        }

        const exactMatch = navItems.find(
            (item) =>
                item.name.toLowerCase() === commandQuery.trim().toLowerCase() ||
                item.path.toLowerCase() === commandQuery.trim().toLowerCase(),
        );

        const route = exactMatch?.path || matchedItems[0]?.path;
        if (route) {
            navigate(route);
        }
    };

    return (
        <div className="min-h-screen flex bg-slate-50 text-slate-900 dark:bg-[#030303] dark:text-slate-200 transition-colors duration-500">
            <div className="mesh-grid" />
            <div className="vignette" />

            <div className="fixed top-[-10%] left-[-10%] w-[40%] h-[40%] bg-purple-600/10 dark:bg-purple-600/20 rounded-full blur-[120px] mix-blend-multiply dark:mix-blend-screen animate-float pointer-events-none z-0" />
            <div
                className="fixed bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-600/10 dark:bg-blue-600/20 rounded-full blur-[120px] mix-blend-multiply dark:mix-blend-screen animate-float pointer-events-none z-0"
                style={{ animationDelay: '2s' }}
            />

            <AnimatePresence>
                {sidebarOpen && (
                    <motion.button
                        type="button"
                        aria-label="Close sidebar"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={() => setSidebarOpen(false)}
                        className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm lg:hidden"
                    />
                )}
            </AnimatePresence>

            <aside
                className={cn(
                    'w-72 border-r border-slate-200 dark:border-white/5 bg-white/80 dark:bg-[#050505]/80 backdrop-blur-2xl flex flex-col h-screen z-50 transition-transform duration-300 overflow-y-auto fixed top-0 left-0 lg:sticky lg:translate-x-0',
                    sidebarOpen ? 'translate-x-0' : '-translate-x-full',
                )}
            >
                <div className="p-8 shrink-0 flex items-center justify-between">
                    <div className="flex items-center gap-4 group cursor-pointer">
                        <div className="w-12 h-12 rounded-[1.25rem] bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-lg group-hover:shadow-purple-500/50 group-hover:scale-[1.05] transition-all duration-500 relative overflow-hidden">
                            <div className="absolute inset-0 bg-white/20 opacity-0 group-hover:opacity-100 transition-opacity" />
                            <ShieldAlert className="w-6 h-6 text-white relative z-10" />
                        </div>
                        <div>
                            <h1 className="text-2xl font-black tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-slate-900 via-purple-800 to-slate-900 dark:from-white dark:via-purple-200 dark:to-white">
                                Sentient
                            </h1>
                            <p className="text-[10px] text-slate-500 font-bold tracking-[0.2em] uppercase -mt-0.5">Gateway Core</p>
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={() => setSidebarOpen(false)}
                        className="lg:hidden p-2 rounded-xl text-slate-500 hover:bg-slate-100 dark:hover:bg-white/10"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <nav className="flex-1 px-4 space-y-1.5 pb-8">
                    <p className="px-4 text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest mb-4">Command Center</p>
                    {navItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) =>
                                cn(
                                    'flex items-center gap-3 px-4 py-3 rounded-[1rem] transition-all duration-300 group relative overflow-hidden',
                                    item.isSubItem
                                        ? 'ml-6 pl-5 text-sm border-l border-slate-200 dark:border-white/10 rounded-l-none my-0.5 py-2.5'
                                        : 'font-semibold tracking-wide',
                                    isActive
                                        ? 'text-purple-700 dark:text-purple-300 bg-purple-50 dark:bg-purple-900/20'
                                        : 'text-slate-500 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-white/[0.04]',
                                )
                            }
                        >
                            {({ isActive }) => (
                                <>
                                    {isActive && (
                                        <motion.div
                                            layoutId="leftBar"
                                            className="absolute left-0 top-1/4 bottom-1/4 w-1 bg-purple-500 rounded-r-full z-10"
                                        />
                                    )}
                                    <item.icon
                                        className={cn(
                                            'transition-colors duration-300 relative z-10',
                                            item.isSubItem ? 'w-4 h-4' : 'w-5 h-5',
                                            isActive ? 'text-purple-600 dark:text-purple-400' : 'group-hover:text-purple-500/70',
                                        )}
                                    />
                                    <span className="relative z-10">{item.name}</span>
                                </>
                            )}
                        </NavLink>
                    ))}
                </nav>

                <div className="p-6 mt-auto">
                    <div className="relative overflow-hidden p-5 rounded-[1.5rem] bg-gradient-to-br from-purple-500/10 via-fuchsia-500/5 to-transparent border border-purple-500/10">
                        <div className="absolute top-0 right-0 p-4 opacity-10">
                            <Activity className="w-16 h-16" />
                        </div>
                        <p className="text-xs font-bold text-slate-900 dark:text-white mb-1 tracking-wide">System Health</p>
                        <p className="text-[10px] text-slate-500 leading-relaxed mb-4 pr-4">All nodes operating at optimal efficiency.</p>
                        <div className="h-1.5 bg-slate-200 dark:bg-white/10 rounded-full overflow-hidden">
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: '99.9%' }}
                                transition={{ duration: 1.5, ease: 'easeOut' }}
                                className="h-full bg-gradient-to-r from-purple-500 to-indigo-500 relative"
                            >
                                <div className="absolute inset-0 bg-white/30 animate-pulse" />
                            </motion.div>
                        </div>
                    </div>
                </div>
            </aside>

            <main className="flex-1 flex flex-col relative z-10 w-full overflow-x-hidden min-h-screen lg:ml-0">
                <header className="h-auto min-h-24 flex flex-wrap gap-4 shrink-0 items-center justify-between px-4 sm:px-6 lg:px-10 py-4 bg-background/60 backdrop-blur-xl border-b border-border sticky top-0 z-30">
                    <div className="flex items-center gap-3">
                        <button
                            type="button"
                            onClick={() => setSidebarOpen(true)}
                            className="lg:hidden p-3 rounded-[1rem] hover:bg-slate-100 dark:hover:bg-white/10 text-slate-500 transition-colors"
                        >
                            <Menu className="w-5 h-5" />
                        </button>

                        <div className="hidden sm:block">
                            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">Active Panel</p>
                            <h2 className="text-lg font-bold tracking-tight text-slate-900 dark:text-white">{currentPageLabel}</h2>
                        </div>
                    </div>

                    <div className="flex-1 min-w-[220px] max-w-xl">
                        <form onSubmit={onSearchSubmit} className="relative group">
                            <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-purple-500 transition-colors" />
                            <input
                                ref={searchInputRef}
                                type="text"
                                value={commandQuery}
                                onChange={(event) => setCommandQuery(event.target.value)}
                                placeholder="Jump to dashboard, logs, registry..."
                                className="w-full bg-slate-100/50 dark:bg-white/5 border border-transparent shadow-sm dark:shadow-none rounded-2xl pl-12 pr-28 py-3 text-sm outline-none focus:border-purple-500/30 focus:bg-white dark:focus:bg-white/10 transition-all placeholder:text-slate-400"
                            />
                            <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1 text-[10px] text-slate-400 font-bold">
                                <Command className="w-3 h-3" />
                                <span>K</span>
                            </div>

                            <AnimatePresence>
                                {commandQuery.trim() && (
                                    <motion.div
                                        initial={{ opacity: 0, y: 8 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        exit={{ opacity: 0, y: 8 }}
                                        className="absolute left-0 right-0 mt-2 bg-white dark:bg-[#0f0f10] border border-slate-200 dark:border-white/10 rounded-2xl shadow-2xl overflow-hidden z-50"
                                    >
                                        {matchedItems.length > 0 ? (
                                            matchedItems.slice(0, 5).map((item) => (
                                                <button
                                                    key={item.path}
                                                    type="button"
                                                    onClick={() => navigate(item.path)}
                                                    className="w-full px-4 py-3 text-left flex items-center justify-between hover:bg-slate-100 dark:hover:bg-white/5 transition-colors"
                                                >
                                                    <div className="flex items-center gap-3">
                                                        <item.icon className="w-4 h-4 text-slate-500" />
                                                        <span className="text-sm font-medium text-slate-700 dark:text-slate-200">{item.name}</span>
                                                    </div>
                                                    <ArrowRight className="w-4 h-4 text-slate-400" />
                                                </button>
                                            ))
                                        ) : (
                                            <div className="px-4 py-3 text-sm text-slate-500">No matching section found.</div>
                                        )}
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </form>
                    </div>

                    <div className="flex items-center gap-2 sm:gap-4 ml-auto">
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

                        <div className="hidden sm:flex items-center gap-3 bg-slate-100/50 dark:bg-white/5 pr-4 pl-1.5 py-1.5 rounded-full border border-border">
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

                <div className="p-4 sm:p-6 lg:p-10 mx-auto w-full max-w-[1600px] flex-1">
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default Layout;
