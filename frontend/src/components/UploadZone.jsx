import { useRef, useState } from 'react';
import { uploadFile } from '../services/api.js';
import { formatBytes } from '../utils/format.js';

/** Drag-and-drop or click-to-browse upload. Files upload one after another with real progress. */
export default function UploadZone({ onUploaded, notify }) {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);
  const [items, setItems] = useState([]);
  const busy = items.some((i) => i.status === 'uploading');

  async function handleFiles(fileList) {
    const files = Array.from(fileList || []);
    if (files.length === 0) return;
    const batch = files.map((f, idx) => ({
      key: `${Date.now()}-${idx}-${f.name}`,
      name: f.name,
      size: f.size,
      progress: 0,
      status: 'queued',
      error: null,
    }));
    setItems((prev) => [...batch, ...prev].slice(0, 8));

    for (let i = 0; i < files.length; i++) {
      const key = batch[i].key;
      const patch = (changes) =>
        setItems((prev) => prev.map((it) => (it.key === key ? { ...it, ...changes } : it)));
      patch({ status: 'uploading' });
      try {
        await uploadFile(files[i], (pct) => patch({ progress: pct }));
        patch({ status: 'done', progress: 100 });
        notify('success', `Uploaded ${files[i].name}`);
      } catch (err) {
        patch({ status: 'error', error: err.message });
        notify('error', `Upload failed for ${files[i].name}: ${err.message}`);
      }
    }
    onUploaded?.();
  }

  function onDrop(e) {
    e.preventDefault();
    setDragging(false);
    if (!busy) handleFiles(e.dataTransfer.files);
  }

  return (
    <div className="panel">
      <div
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => !busy && inputRef.current?.click()}
        onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !busy) inputRef.current?.click(); }}
        role="button"
        tabIndex={0}
        className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-10 text-center transition
          ${dragging ? 'border-accent bg-accent/10' : 'border-white/10 bg-navy-950/40 hover:border-accent/50 hover:bg-white/5'}
          ${busy ? 'cursor-wait opacity-80' : ''}`}
      >
        <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-accent to-violet-800 text-2xl shadow-lg shadow-accent/30">⬆</div>
        <p className="font-semibold text-white">{dragging ? 'Drop to upload' : 'Drag files here or click to browse'}</p>
        <p className="mt-1 text-xs text-slate-400">Files are split into 1 MiB chunks and each chunk is stored on two nodes.</p>
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => { handleFiles(e.target.files); e.target.value = ''; }}
        />
      </div>

      {items.length > 0 && (
        <ul className="mt-4 space-y-3">
          {items.map((it) => (
            <li key={it.key} className="rounded-xl bg-navy-950/60 px-4 py-3">
              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate text-slate-200">{it.name}</span>
                <span className="shrink-0 text-xs text-slate-400">{formatBytes(it.size)}</span>
              </div>
              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/5">
                <div
                  className={`h-full rounded-full transition-all ${it.status === 'error' ? 'bg-rose-500' : 'bg-gradient-to-r from-accent to-violet-400'}`}
                  style={{ width: `${it.status === 'error' ? 100 : it.progress}%` }}
                />
              </div>
              <p className={`mt-1.5 text-xs ${it.status === 'error' ? 'text-rose-300' : 'text-slate-400'}`}>
                {it.status === 'queued' && 'Waiting…'}
                {it.status === 'uploading' && `Uploading ${it.progress}%`}
                {it.status === 'done' && 'Stored successfully'}
                {it.status === 'error' && `Failed: ${it.error}`}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
