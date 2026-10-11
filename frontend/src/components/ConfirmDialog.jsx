export default function ConfirmDialog({ config, onCancel, onConfirm }) {
  if (!config) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <div className="panel w-full max-w-md">
        <h3 className="text-lg font-semibold text-white">{config.title}</h3>
        <p className="mt-2 text-sm leading-relaxed text-slate-400">{config.message}</p>
        <div className="mt-6 flex justify-end gap-3">
          <button className="btn-ghost" onClick={onCancel}>Cancel</button>
          <button className={config.danger ? 'btn-danger' : 'btn-primary'} onClick={onConfirm}>
            {config.confirmLabel || 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  );
}
