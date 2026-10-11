import { formatBytes } from '../utils/format.js';

/** Horizontal bars: how much data each node holds, as a share of the cluster total. */
export function StorageUsageChart({ nodes }) {
  return (
    <div className="space-y-4">
      {nodes.map((n) => (
        <div key={n.id}>
          <div className="mb-1.5 flex justify-between text-sm">
            <span className="font-medium text-slate-200">{n.name}</span>
            <span className="text-slate-400">{formatBytes(n.storedBytes)} · {n.utilizationPercent}%</span>
          </div>
          <div className="h-3 overflow-hidden rounded-full bg-white/5">
            <div className={`h-full rounded-full transition-all ${n.online ? 'bg-gradient-to-r from-accent to-violet-400' : 'bg-rose-500/60'}`} style={{ width: `${Math.min(n.utilizationPercent, 100)}%` }} />
          </div>
        </div>
      ))}
    </div>
  );
}

/** Stacked bars per node: primary copies (purple) and replica copies (sky blue). */
export function DistributionChart({ nodes }) {
  const max = Math.max(1, ...nodes.map((n) => n.totalChunks));
  return (
    <div>
      <div className="space-y-4">
        {nodes.map((n) => (
          <div key={n.id}>
            <div className="mb-1.5 flex justify-between text-sm">
              <span className="font-medium text-slate-200">{n.name}</span>
              <span className="text-slate-400">{n.primaryChunks} primary · {n.replicaChunks} replica</span>
            </div>
            <div className="flex h-3 overflow-hidden rounded-full bg-white/5">
              <div className="h-full bg-violet-500 transition-all" style={{ width: `${(n.primaryChunks / max) * 100}%` }} />
              <div className="h-full bg-sky-400 transition-all" style={{ width: `${(n.replicaChunks / max) * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
      <div className="mt-4 flex gap-4 text-xs text-slate-400">
        <span className="flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-sm bg-violet-500" />Primary copies</span>
        <span className="flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-sm bg-sky-400" />Replica copies</span>
      </div>
    </div>
  );
}
