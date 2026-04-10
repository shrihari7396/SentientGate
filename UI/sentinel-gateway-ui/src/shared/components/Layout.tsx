import { ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import UserContextDrawer from '../../features/threat/components/UserContextDrawer';

export function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />
      <main className="flex-1 overflow-y-auto w-full relative">
        <div className="absolute inset-0 pointer-events-none vignette"></div>
        <div className="absolute inset-0 pointer-events-none mesh-grid"></div>
        <div className="relative z-10 w-full min-h-full">
          {children}
        </div>
      </main>
      <UserContextDrawer />
    </div>
  );
}
