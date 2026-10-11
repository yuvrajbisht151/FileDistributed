import StatusBadge from './StatusBadge.jsx';
import { formatBytes } from '../utils/format.js';

export default function NodeCard({ node, busy, onToggle }) {
  const online = node.status === 'ONLINE';
  return (
    <div className={`panel flex flex-col gap-5 transition ${online ? '' : 'border-rose-500/20'}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className={`flex h-11 w-11 items-center justify-center rounded-xl text-lg ${online ? 'bg-emerald-500/15' : 'bg-rose-500/15'}`}>🗄</div>
          <div>
            <h3 className="text-lg font-semibold text-white">{node.name}</h3>
            <p className="max-w-[220px] truncate text-xs text-slate-500" title={node.storageDirectory}>{node.storageDirectory}</p>
          </div>
        </div>
        <StatusBadge value={node.status} />
      </div>

      <div>
        <div className="mb-2 flex justify-between text-xs text-slate-400">
          <span>Storage used</span>
          <span className="text-slate-200">{formatBytes(node.storedBytes)} · {node.utilizationPercent}% of cluster</span>
        </div>
        <div className="h-2.5 overflow-hidden rounded-full bg-white/5">
          <div className="h-full rounded-full bg-gradient-to-r from-accent to-violet-400 transition-all" style={{ width: `${Math.min(node.utilizationPercent, 100)}%` }} />
        </div>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <Stat label="Chunks" value={node.totalChunks} />
        <Stat label="Primary" value={node.primaryChunks} tone="text-violet-300" />
        <Stat label="Replica" value={node.replicaChunks} tone="text-sky-300" />
      </div>

      <button
        className={online ? 'btn-danger w-full' : 'btn-primary w-full'}
        disabled={busy}
        onClick={() => onToggle(node)}
      >
        {busy ? 'Working…' : online ? 'Simulate failure' : 'Restore node'}
      </button>
    </div>
  );
}

function Stat({ label, value, tone = 'text-white' }) {
  return (
    <div className="rounded-xl bg-navy-950/60 p-3 text-center">
      <p className={`text-xl font-bold ${tone}`}>{value}</p>
      <p className="text-[11px] uppercase tracking-wider text-slate-500">{label}</p>
    </div>
  );
}
