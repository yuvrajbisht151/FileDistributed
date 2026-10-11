const TONES = {
  success: 'border-emerald-500/30 bg-emerald-500/15 text-emerald-200',
  error: 'border-rose-500/30 bg-rose-500/15 text-rose-200',
  info: 'border-violet-500/30 bg-violet-500/15 text-violet-200',
  warning: 'border-amber-500/30 bg-amber-500/15 text-amber-200',
};

export default function ToastStack({ toasts, onDismiss }) {
  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-[min(92vw,380px)] flex-col gap-2">
      {toasts.map((t) => (
        <div key={t.id} className={`pointer-events-auto flex items-start justify-between gap-3 rounded-xl border px-4 py-3 text-sm shadow-card backdrop-blur ${TONES[t.type] || TONES.info}`}>
          <span>{t.message}</span>
          <button className="text-current opacity-60 hover:opacity-100" onClick={() => onDismiss(t.id)} aria-label="Dismiss">✕</button>
        </div>
      ))}
    </div>
  );
}
