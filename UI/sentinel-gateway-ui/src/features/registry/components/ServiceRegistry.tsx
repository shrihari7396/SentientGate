import { useState } from 'react';
import { useEurekaServices, useEurekaInstances, useActuatorHealth, EurekaService } from '../hooks/useEurekaServices';
import { StatusBadge } from '@/shared/components/StatusBadge';
import { Globe, HeartPulse, ExternalLink, HardDrive } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';

export default function ServiceRegistry() {
  const { services } = useEurekaServices();
  const sData = services.data || [];
  
  const [selectedService, setSelectedService] = useState<string | null>(null);

  const { data: instances } = useEurekaInstances(selectedService);
  const { data: health } = useActuatorHealth(selectedService);

  const total = sData.length;
  const upCount = sData.filter((s: EurekaService) => s.status === 'UP').length;

  return (
    <motion.div 
      className="p-6 max-w-[1600px] mx-auto h-full flex flex-col"
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
    >
      <header className="mb-6 flex justify-between items-end">
        <div>
          <h1 className="text-2xl font-sans font-semibold text-text-primary flex items-center gap-3">
            <Globe size={28} className="text-blue" /> Service Registry
          </h1>
          <p className="text-sm text-text-muted mt-1">Microservices discovered via Netflix Eureka.</p>
        </div>
        <div className="flex gap-4">
          <div className="bg-surface border border-border px-4 py-2 rounded text-center">
             <div className="text-xl font-mono text-text-primary">{total}</div>
             <div className="text-[10px] uppercase text-text-muted tracking-wider">Total Apps</div>
          </div>
          <div className="bg-surface border border-border px-4 py-2 rounded text-center">
             <div className="text-xl font-mono text-teal">{upCount}</div>
             <div className="text-[10px] uppercase text-text-muted tracking-wider">Healthy</div>
          </div>
        </div>
      </header>

      <div className="flex-1 flex gap-6 overflow-hidden">
        {/* Service Grid */}
        <div className="flex-1 overflow-y-auto min-h-0">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {sData.map((s: EurekaService) => (
              <div 
                key={s.id} 
                onClick={() => setSelectedService(s.id === selectedService ? null : s.id)}
                className={clsx(
                  "bg-surface border rounded p-4 cursor-pointer transition-all hover:bg-elevated/50",
                  selectedService === s.id ? "border-teal shadow-[0_0_15px_rgba(0,229,204,0.1)]" : "border-border"
                )}
              >
                <div className="flex justify-between items-start mb-4">
                  <h3 className="text-lg font-mono font-semibold text-text-primary">{s.name}</h3>
                  <StatusBadge status={s.status} />
                </div>
                <div className="flex justify-between items-center text-xs font-mono">
                  <span className="text-text-muted">APP ID: {s.id}</span>
                  <span className="bg-elevated px-2 py-0.5 rounded border border-border/50 text-text-primary">
                    {s.instanceCount} nodes
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Actuator Panel */}
        <AnimatePresence>
          {selectedService && (
            <motion.div
              initial={{ width: 0, opacity: 0 }}
              animate={{ width: 440, opacity: 1 }}
              exit={{ width: 0, opacity: 0 }}
              className="bg-surface border border-border rounded overflow-hidden flex flex-col shrink-0"
            >
              <div className="p-4 border-b border-border bg-elevated/30 flex justify-between items-center">
                 <h3 className="text-sm font-semibold tracking-widest uppercase text-text-primary flex items-center gap-2">
                   <HeartPulse size={16} className="text-teal" /> Actuator Panel
                 </h3>
                 <span className="text-xs font-mono text-text-muted">{selectedService}</span>
              </div>
              <div className="flex-1 overflow-y-auto p-4 space-y-6">
                
                <div>
                  <h4 className="text-xs font-semibold uppercase text-text-muted mb-3 flex items-center gap-2">
                    <HardDrive size={14} /> Active Instances
                  </h4>
                  <div className="space-y-3">
                    {instances?.map((inst, i) => (
                      <div key={i} className="bg-elevated border border-border rounded p-3">
                        <div className="flex justify-between items-center mb-2">
                           <span className="font-mono text-sm text-text-primary">{inst.host}:{inst.port}</span>
                           <StatusBadge status={inst.status as any} />
                        </div>
                        <div className="flex justify-between items-center text-xs text-text-muted mt-2">
                          <a href={inst.homepage} target="_blank" rel="noreferrer" className="flex items-center gap-1 hover:text-teal transition-colors">
                            {inst.homepage} <ExternalLink size={10} />
                          </a>
                          <span>HB: {new Date(inst.lastHeartbeat).toLocaleTimeString()}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {health && (
                  <div>
                    <h4 className="text-xs font-semibold uppercase text-text-muted mb-3">Health Tree</h4>
                    <div className="bg-elevated border border-border rounded p-3 overflow-x-auto text-xs font-mono">
                      <pre className="text-text-primary">{JSON.stringify(health, null, 2)}</pre>
                    </div>
                  </div>
                )}
                
              </div>
            </motion.div>
          )}
        </AnimatePresence>

      </div>
    </motion.div>
  );
}
