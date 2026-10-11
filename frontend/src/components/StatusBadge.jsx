const STYLES = {
  // file storage status
  STORED: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  PENDING_CLEANUP: 'bg-amber-500/15 text-amber-300 ring-amber-500/30',
  // integrity results
  VERIFIED: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  CORRUPTED: 'bg-rose-500/15 text-rose-300 ring-rose-500/30',
  UNAVAILABLE: 'bg-amber-500/15 text-amber-300 ring-amber-500/30',
  DEGRADED: 'bg-amber-500/15 text-amber-300 ring-amber-500/30',
  // node availability
  ONLINE: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  OFFLINE: 'bg-rose-500/15 text-rose-300 ring-rose-500/30',
  // copy statuses
  OK: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  MISSING: 'bg-rose-500/15 text-rose-300 ring-rose-500/30',
  IO_ERROR: 'bg-rose-500/15 text-rose-300 ring-rose-500/30',
};

const LABELS = {
  PENDING_CLEANUP: 'Pending cleanup',
};

export default function StatusBadge({ value }) {
  if (!value) return null;
  const style = STYLES[value] || 'bg-slate-500/15 text-slate-300 ring-slate-500/30';
  const label = LABELS[value] || value.replace(/_/g, ' ');
  return (
    <span className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ${style}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {label}
    </span>
  );
}
