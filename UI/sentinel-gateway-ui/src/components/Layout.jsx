import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, ListFilter, ShieldAlert, Activity, Settings, Menu } from 'lucide-react';
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
        <div className="min-h-screen flex bg-[#0a0a0c] text-slate-200">
            {/* Sidebar */}
            <aside className="w-64 border-r border-white/5 bg-[#0d0d0f] flex flex-col sticky top-0 h-screen">
                <div className="p-6">
                    <h1 className="text-xl font-bold gradient-text">SentientGate</h1>
                    <p className="text-xs text-slate-500 mt-1">Security Monitoring</p>
                </div>

                <nav className="flex-1 px-4 mt-6 space-y-1">
                    {navItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) =>
                                cn(
                                    "flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group",
                                    isActive
                                        ? "bg-purple-600/10 text-purple-400 border border-purple-500/20"
                                        : "text-slate-400 hover:text-slate-200 hover:bg-white/5"
                                )
                            }
                        >
                            <item.icon className="w-5 h-5" />
                            <span className="font-medium">{item.name}</span>
                        </NavLink>
                    ))}
                </nav>

                <div className="p-4 border-t border-white/5">
                    <button className="flex items-center gap-3 px-4 py-2 w-full text-slate-400 hover:text-slate-200 transition-colors">
                        <Settings className="w-5 h-5" />
                        <span className="font-medium">Settings</span>
                    </button>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 flex flex-col">
                <header className="h-16 border-b border-white/5 flex items-center justify-between px-8 bg-[#0d0d0f]/50 backdrop-blur-md sticky top-0 z-50">
                    <div className="flex items-center gap-4 text-sm text-slate-400">
                        <span>Production</span>
                        <span className="w-1 h-1 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]"></span>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-purple-500 to-blue-500 border border-white/10"></div>
                    </div>
                </header>

                <div className="p-8 max-w-7xl mx-auto w-full">
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default Layout;
