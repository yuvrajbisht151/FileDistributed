export default function StatCard({ label, value, hint, accent = 'from-violet-500/30 to-violet-500/0' }) {
  return (
    <div className="panel relative overflow-hidden">
      <div className={`pointer-events-none absolute inset-0 bg-gradient-to-br ${accent}`} />
      <div className="relative">
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p>
        <p className="mt-2 text-3xl font-bold text-white">{value}</p>
        {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
      </div>
    </div>
  );
}
