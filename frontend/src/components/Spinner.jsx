export default function Spinner({ label = 'Loading…' }) {
  return (
    <div className="flex items-center justify-center gap-3 py-10 text-sm text-slate-400">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-accent/30 border-t-accent" />
      {label}
    </div>
  );
}
