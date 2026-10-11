import { api } from '../services/api.js';
import { formatBytes, formatDate } from '../utils/format.js';
import StatusBadge from './StatusBadge.jsx';

export default function FileTable({ files, verifyResults, busyId, onOpen, onVerify, onDelete }) {
  return (
    <div className="-mx-5 overflow-x-auto">
      <table className="min-w-full divide-y divide-white/5">
        <thead>
          <tr>
            <th className="th">Filename</th>
            <th className="th">Size</th>
            <th className="th">Chunks</th>
            <th className="th">Uploaded</th>
            <th className="th">Storage</th>
            <th className="th">Integrity</th>
            <th className="th text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-white/5">
          {files.map((f) => {
            const report = verifyResults[f.id];
            const busy = busyId === f.id;
            return (
              <tr key={f.id} className="transition hover:bg-white/[0.03]">
                <td className="td max-w-[260px]">
                  <button onClick={() => onOpen(f.id)} className="block max-w-full truncate text-left font-medium text-slate-100 hover:text-accent-soft" title={f.originalFilename}>
                    {f.originalFilename}
                  </button>
                </td>
                <td className="td whitespace-nowrap">{formatBytes(f.fileSize)}</td>
                <td className="td">{f.chunkCount}</td>
                <td className="td whitespace-nowrap">{formatDate(f.uploadedAt)}</td>
                <td className="td"><StatusBadge value={f.status} /></td>
                <td className="td">
                  {report ? <StatusBadge value={report.status} /> : <span className="text-xs text-slate-500">Not checked</span>}
                </td>
                <td className="td">
                  <div className="flex justify-end gap-2">
                    <a href={api.downloadUrl(f.id)} className="btn-ghost px-3 py-1.5 text-xs" title="Download">Download</a>
                    <button className="btn-ghost px-3 py-1.5 text-xs" disabled={busy} onClick={() => onVerify(f)}>
                      {busy ? 'Checking…' : 'Verify'}
                    </button>
                    <button className="btn-danger px-3 py-1.5 text-xs" disabled={busy} onClick={() => onDelete(f)}>Delete</button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
