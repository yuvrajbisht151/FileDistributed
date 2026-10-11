import { useEffect, useState } from 'react';
import { api } from '../services/api.js';
import { formatBytes, formatDate, shortHash } from '../utils/format.js';
import Spinner from './Spinner.jsx';
import StatusBadge from './StatusBadge.jsx';

export default function FileDetailsModal({ fileId, onClose, notify }) {
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState(null);
  const [report, setReport] = useState(null);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setReport(null);
    api.getFile(fileId)
      .then((d) => !cancelled && setDetail(d))
      .catch((e) => !cancelled && setError(e.message));
    return () => { cancelled = true; };
  }, [fileId]);

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  async function runVerify() {
    setChecking(true);
    try {
      setReport(await api.verifyFile(fileId));
    } catch (e) {
      notify('error', e.message);
    } finally {
      setChecking(false);
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-black/60 p-0 backdrop-blur-sm sm:items-center sm:p-4" onClick={onClose}>
      <div className="panel max-h-[92vh] w-full overflow-y-auto rounded-b-none sm:max-w-4xl sm:rounded-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <p className="text-xs uppercase tracking-wider text-accent-soft">File details</p>
            <h2 className="truncate text-xl font-semibold text-white">{detail?.file.originalFilename || 'Loading…'}</h2>
          </div>
          <button className="btn-ghost px-3 py-1.5" onClick={onClose} aria-label="Close">✕</button>
        </div>

        {error && <p className="mt-6 rounded-xl bg-rose-500/10 p-4 text-sm text-rose-300">{error}</p>}
        {!detail && !error && <Spinner />}

        {detail && (
          <>
            <dl className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Meta label="File size" value={formatBytes(detail.file.fileSize)} />
              <Meta label="Chunks" value={`${detail.file.chunkCount} × ${formatBytes(detail.file.chunkSize)} max`} />
              <Meta label="Uploaded" value={formatDate(detail.file.uploadedAt)} />
              <Meta label="Storage status" value={<StatusBadge value={detail.file.status} />} />
              <Meta label="Overall SHA-256" value={<code className="break-all text-xs text-slate-300">{detail.file.checksumSha256}</code>} wide />
            </dl>

            <div className="mt-6 flex flex-wrap items-center gap-3">
              <a href={api.downloadUrl(fileId)} className="btn-primary">Download</a>
              <button className="btn-ghost" onClick={runVerify} disabled={checking}>
                {checking ? 'Verifying…' : 'Verify integrity'}
              </button>
              {report && <StatusBadge value={report.status} />}
              {report && <span className="text-sm text-slate-400">{report.message}</span>}
            </div>

            {report?.warnings?.length > 0 && (
              <ul className="mt-4 space-y-1 rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-200">
                {report.warnings.map((w) => <li key={w}>• {w}</li>)}
              </ul>
            )}

            <div className="mt-6 overflow-x-auto rounded-xl border border-white/5">
              <table className="min-w-full divide-y divide-white/5">
                <thead className="bg-navy-950/60">
                  <tr>
                    <th className="th">#</th>
                    <th className="th">Size</th>
                    <th className="th">Chunk SHA-256</th>
                    <th className="th">Primary node</th>
                    <th className="th">Replica node</th>
                    {report && <th className="th">Integrity</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {detail.chunks.map((c) => {
                    const row = report?.chunks.find((r) => r.chunkIndex === c.chunkIndex);
                    return (
                      <tr key={c.chunkIndex}>
                        <td className="td font-mono">{c.chunkIndex}</td>
                        <td className="td whitespace-nowrap">{formatBytes(c.chunkSize)}</td>
                        <td className="td font-mono text-xs" title={c.checksumSha256}>{shortHash(c.checksumSha256)}</td>
                        <td className="td"><NodePill name={c.primaryNode} copy={row?.primaryStatus} /></td>
                        <td className="td"><NodePill name={c.replicaNode} copy={row?.replicaStatus} /></td>
                        {report && <td className="td">{row && <StatusBadge value={row.status} />}</td>}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function Meta({ label, value, wide }) {
  return (
    <div className={`rounded-xl bg-navy-950/60 p-4 ${wide ? 'sm:col-span-2 lg:col-span-4' : ''}`}>
      <dt className="text-xs uppercase tracking-wider text-slate-500">{label}</dt>
      <dd className="mt-1 text-sm text-slate-100">{value}</dd>
    </div>
  );
}

function NodePill({ name, copy }) {
  return (
    <span className="inline-flex items-center gap-2 rounded-lg bg-white/5 px-2.5 py-1 text-xs text-slate-200">
      {name}
      {copy && copy !== 'OK' && <StatusBadge value={copy} />}
    </span>
  );
}
