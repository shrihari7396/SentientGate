import { NavLink } from 'react-router-dom';
import { LayoutDashboard, ScrollText, GitBranch, ShieldAlert, Settings, Sun, Moon } from 'lucide-react';
import clsx from 'clsx';
import { useState } from 'react';

const NAV_ITEMS = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/' },
  { icon: ScrollText, label: 'Traffic Ledger', path: '/logs' },
  { icon: GitBranch, label: 'Pipeline', path: '/pipeline' },
  { icon: ShieldAlert, label: 'Threat Intel', path: '/threat', accent: true, pulse: true },
];

export function Sidebar() {
  const [isDark, setIsDark] = useState(() => document.documentElement.classList.contains('dark'));

  const toggleTheme = () => {
    const nextIsDark = !isDark;
    setIsDark(nextIsDark);
    if (nextIsDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  };

  return (
    <div className="w-[220px] bg-surface h-full border-r border-border flex flex-col">
      <div className="h-16 flex items-center px-6 border-b border-border">
        <div className="w-8 h-8 bg-teal/20 rounded mr-3 flex items-center justify-center border border-teal/40">
          <ShieldAlert size={16} className="text-teal" />
        </div>
        <span className="font-sans font-bold tracking-wide text-text-primary">SentientGate</span>
      </div>

      <nav className="flex-1 py-6 flex flex-col gap-1 px-3">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              clsx(
                'flex items-center px-3 py-2.5 rounded text-sm font-medium transition-colors relative',
                isActive
                  ? 'bg-elevated text-teal before:absolute before:left-0 before:top-2 before:bottom-2 before:w-1 before:bg-teal before:rounded-r'
                  : 'text-text-muted hover:bg-elevated hover:text-text-primary'
              )
            }
          >
            <item.icon size={18} className={clsx('mr-3', item.accent ? 'text-teal' : 'opacity-70')} />
            {item.label}
            
            {item.pulse && (
              <span className="ml-auto flex h-2 w-2 relative">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-red"></span>
              </span>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="p-3 border-t border-border flex flex-col gap-1">
        <button className="flex items-center w-full px-3 py-2.5 rounded text-sm font-medium text-text-muted hover:bg-elevated hover:text-text-primary transition-colors">
          <Settings size={18} className="mr-3 opacity-70" />
          Settings
        </button>
        <button 
          onClick={toggleTheme}
          className="flex items-center w-full px-3 py-2.5 rounded text-sm font-medium text-text-muted hover:bg-elevated hover:text-text-primary transition-colors"
        >
          {isDark ? <Sun size={18} className="mr-3 opacity-70" /> : <Moon size={18} className="mr-3 opacity-70" />}
          Theme
        </button>
      </div>
    </div>
  );
}
