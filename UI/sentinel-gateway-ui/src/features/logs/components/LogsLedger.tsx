import { useState } from 'react';
import { useLogs, LogEntry } from '../hooks/useLogs';
import { CopyableUUID } from '@/shared/components/CopyableUUID';
import { StatusBadge } from '@/shared/components/StatusBadge';
import { Search, Play, Square, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';
import { flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';

export default function LogsLedger() {
  const [path, setPath] = useState('');
  const [status, setStatus] = useState('');
  const [uuid, setUuid] = useState('');
  const [liveMode, setLiveMode] = useState(false);
  const [selectedLog, setSelectedLog] = useState<LogEntry | null>(null);

  // Note: we'd debounce in a real app
  const { data: logs = [], isLoading } = useLogs(path, status, uuid, liveMode);

  const columns = [
    {
      accessorKey: 'timestamp',
      header: 'Timestamp',
      cell: (info: any) => {
        const d = new Date(info.getValue());
        return <span className="font-mono text-[11px] text-text-muted">{d.getFullYear()}-{(d.getMonth()+1).toString().padStart(2,'0')}-{d.getDate().toString().padStart(2,'0')} {d.toLocaleTimeString()}.{d.getMilliseconds().toString().padStart(3,'0')}</span>
      },
      size: 140,
    },
    {
      accessorKey: 'uuid',
      header: 'UUID',
      cell: (info: any) => <CopyableUUID uuid={info.getValue()} maxChars={12} />,
      size: 160,
    },
    {
      accessorKey: 'method',
      header: 'Method',
      cell: (info: any) => {
        const m = info.getValue() as string;
        return <span className={clsx("text-[10px] font-mono px-1.5 py-0.5 rounded", m==='GET'?'bg-blue/20 text-blue':m==='POST'?'bg-teal/20 text-teal':'bg-red/20 text-red')}>{m}</span>
      },
      size: 70,
    },
    {
      accessorKey: 'endpoint',
      header: 'Endpoint',
      cell: (info: any) => <span className="truncate max-w-[200px] block font-mono text-xs">{info.getValue()}</span>,
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: (info: any) => {
        const s = info.getValue() as number;
        const color = s < 300 ? 'allowed' : s < 500 ? 'monitoring' : 'blocked';
        return <StatusBadge status={color === 'allowed' ? 'UP' : color === 'monitoring' ? 'MONITORING' : 'DOWN'} />
      },
      size: 80,
    },
    {
      accessorKey: 'latency',
      header: 'Latency',
      cell: (info: any) => {
        const l = info.getValue() as number;
        return <span className={clsx("font-mono text-xs", l < 100 ? "text-teal" : l < 500 ? "text-amber" : "text-red")}>{l}ms</span>
      },
      size: 90,
    },
    {
      accessorKey: 'routeId',
      header: 'Route ID',
      cell: (info: any) => <span className="font-mono text-xs text-text-muted">{info.getValue()}</span>,
      size: 120,
    },
    {
      accessorKey: 'threatFlagged',
      header: 'Threat',
      cell: (info: any) => info.getValue() ? <StatusBadge status="BLOCKED" /> : <span className="text-text-muted">—</span>,
      size: 80,
    }
  ];

  const table = useReactTable({
    data: logs,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  return (
    <div className="flex h-full max-w-[1800px] mx-auto overflow-hidden">
      <div className="flex-1 flex flex-col p-6 overflow-hidden">
        <header className="mb-6 flex justify-between items-end shrink-0">
          <div>
            <h1 className="text-2xl font-sans font-semibold text-text-primary">Traffic Ledger</h1>
            <p className="text-sm text-text-muted mt-1">Full audit trail of every HTTP request.</p>
          </div>
        </header>

        {/* Filter Bar */}
        <div className="bg-surface border border-border rounded flex items-center p-2 gap-3 mb-4 shrink-0">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" size={16} />
            <input 
              type="text" 
              placeholder="Filter by endpoint path..."
              value={path}
              onChange={e => setPath(e.target.value)}
              className="w-full bg-elevated border border-border rounded text-sm px-9 py-2 text-text-primary focus:outline-none focus:border-teal"
            />
          </div>
          <select value={status} onChange={e => setStatus(e.target.value)} className="bg-elevated border border-border rounded text-sm px-3 py-2 text-text-primary focus:outline-none">
            <option value="">All Statuses</option>
            <option value="2xx">2xx Success</option>
            <option value="4xx">4xx Client Error</option>
            <option value="5xx">5xx Server Error</option>
          </select>
          <div className="relative w-48">
             <input 
              type="text" 
              placeholder="Client UUID"
              value={uuid}
              onChange={e => setUuid(e.target.value)}
              className="w-full bg-elevated border border-border rounded text-sm px-3 py-2 text-text-primary focus:outline-none"
            />
          </div>
          <div className="w-px h-8 bg-border"></div>
          <button 
            onClick={() => setLiveMode(!liveMode)}
            className={clsx("flex items-center gap-2 px-3 py-2 rounded text-sm font-medium transition-colors", liveMode ? "bg-teal/20 text-teal" : "bg-elevated text-text-muted hover:text-text-primary")}
          >
            {liveMode ? <Square size={16} /> : <Play size={16} />}
            {liveMode ? 'Live Mode ON' : 'Live Mode OFF'}
          </button>
          {(path || status || uuid) && (
            <button onClick={() => { setPath(''); setStatus(''); setUuid(''); }} className="p-2 text-text-muted hover:text-red transition-colors" title="Clear Filters">
              <X size={16} />
            </button>
          )}
        </div>

        {/* Table */}
        <div className="flex-1 bg-surface border border-border rounded overflow-hidden flex flex-col min-h-0">
          <div className="overflow-y-auto flex-1">
            <table className="w-full text-left border-collapse">
              <thead className="sticky top-0 bg-surfce z-10 shadow-sm border-b border-border">
                {table.getHeaderGroups().map(headerGroup => (
                  <tr key={headerGroup.id} className="bg-elevated/80 backdrop-blur-sm">
                    {headerGroup.headers.map(header => (
                      <th key={header.id} className="p-3 text-xs font-semibold text-text-muted whitespace-nowrap" style={{ width: header.getSize() === 150 ? 'auto' : header.getSize() }}>
                        {flexRender(header.column.columnDef.header, header.getContext())}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-border/50">
                {isLoading ? (
                  Array.from({length: 10}).map((_, i) => (
                    <tr key={i} className="animate-pulse">
                      <td colSpan={columns.length} className="p-4 bg-elevated/20 h-12"></td>
                    </tr>
                  ))
                ) : (
                  table.getRowModel().rows.map(row => (
                    <tr 
                      key={row.id} 
                      onClick={() => setSelectedLog(row.original)}
                      className={clsx(
                        "hover:bg-elevated transition-colors cursor-pointer",
                        selectedLog?.id === row.original.id && "bg-elevated/80 shadow-[inset_2px_0_0_#00E5CC]"
                      )}
                    >
                      {row.getVisibleCells().map(cell => (
                        <td key={cell.id} className="p-3 whitespace-nowrap">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Detail Drawer Side Panel */}
      <AnimatePresence>
        {selectedLog && (
          <motion.div
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 480, opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            className="h-full bg-surface border-l border-border shrink-0 overflow-y-auto"
          >
            <div className="p-6 w-[480px]">
               <div className="flex justify-between items-start mb-6">
                 <div>
                   <h2 className="text-lg font-sans font-semibold text-text-primary mb-1">Request Detail</h2>
                   <div className="text-xs text-text-muted font-mono">{selectedLog.id}</div>
                 </div>
                 <button onClick={() => setSelectedLog(null)} className="text-text-muted hover:text-text-primary p-1">
                   <X size={20} />
                 </button>
               </div>

               <div className="space-y-6">
                 <div className="bg-elevated border border-border rounded p-4">
                   <div className="font-mono text-sm break-all text-text-primary mb-2">
                     <span className="text-teal font-bold">{selectedLog.method}</span> {selectedLog.endpoint}
                   </div>
                   <div className="flex items-center gap-4 text-xs text-text-muted font-mono">
                     <span>Status: <span className={selectedLog.status >= 400 ? 'text-red' : 'text-teal'}>{selectedLog.status}</span></span>
                     <span>Latency: {selectedLog.latency}ms</span>
                   </div>
                 </div>

                 <div>
                   <h3 className="text-xs font-semibold tracking-widest uppercase text-text-muted mb-3">Client Identity</h3>
                   <div className="bg-elevated border border-border rounded p-4">
                     <CopyableUUID uuid={selectedLog.uuid} maxChars={36} />
                   </div>
                 </div>

                 {selectedLog.threatFlagged && (
                   <div className="bg-red/10 border border-red/20 rounded p-4">
                      <h3 className="text-xs font-semibold tracking-widest uppercase text-red mb-2 flex items-center gap-2">
                        Threat Analysis Result
                      </h3>
                      <p className="text-xs text-text-primary font-mono mb-2">Strategy: RateAnomalyStrategy</p>
                      <p className="text-xs text-text-primary font-mono mb-2">Anomaly Score: 0.94</p>
                      <StatusBadge status="BLOCKED" />
                   </div>
                 )}

               </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
