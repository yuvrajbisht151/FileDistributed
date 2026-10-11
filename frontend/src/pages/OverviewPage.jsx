import { useEffect, useState } from 'react';
import { api } from '../services/api.js';
import { formatBytes, formatDate } from '../utils/format.js';
import StatCard from '../components/StatCard.jsx';
import Spinner from '../components/Spinner.jsx';
import EmptyState from '../components/EmptyState.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import { DistributionChart, StorageUsageChart } from '../components/Charts.jsx';

export default function OverviewPage({ refreshKey, onOpenFile, onNavigate }) {
  const [stats, setStats] = useState(null);
  const [nodes, setNodes] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([api.stats(), api.listNodes()])
      .then(([s, n]) => {
        if (cancelled) return;
        setStats(s);
        setNodes(n);
        setError(null);
      })
      .catch((e) => !cancelled && setError(e.message));
    return () => { cancelled = true; };
  }, [refreshKey]);

  if (error) return <ErrorPanel message={error} />;
  if (!stats || !nodes) return <div className="panel"><Spinner label="Loading dashboard…" /></div>;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total files" value={stats.totalFiles} hint={`${formatBytes(stats.logicalBytes)} of original data`} />
        <StatCard label="Total chunks" value={stats.totalChunks} hint={`${stats.primaryChunks} primary · ${stats.replicaChunks} replica`} accent="from-sky-500/25 to-sky-500/0" />
        <StatCard label="Online nodes" value={`${stats.onlineNodes} / ${stats.totalNodes}`} hint={stats.onlineNodes < stats.totalNodes ? 'Some nodes are simulated offline' : 'All nodes healthy'} accent="from-emerald-500/25 to-emerald-500/0" />
        <StatCard label="Physical storage" value={formatBytes(stats.physicalBytes)} hint="Includes replica copies" accent="from-fuchsia-500/25 to-fuchsia-500/0" />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="panel">
          <h2 className="text-base font-semibold text-white">Storage usage by node</h2>
          <p className="mb-5 text-xs text-slate-400">Share of all stored bytes (primary + replica) held by each node.</p>
          {nodes.length ? <StorageUsageChart nodes={nodes} /> : <EmptyState title="No nodes yet" />}
        </section>

        <section className="panel">
          <h2 className="text-base font-semibold text-white">Primary vs replica distribution</h2>
          <p className="mb-5 text-xs text-slate-400">Each chunk has one primary copy and one backup copy on a different node.</p>
          {stats.totalChunks > 0 ? <DistributionChart nodes={nodes} /> : <EmptyState title="No chunks stored yet" message="Upload a file to see how chunks are spread across nodes." />}
        </section>
      </div>

      <section className="panel">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-white">Recent uploads</h2>
            <p className="text-xs text-slate-400">Click a file to see its chunks.</p>
          </div>
          <button className="btn-ghost text-xs" onClick={() => onNavigate('files')}>Open file manager</button>
        </div>
        {stats.recentUploads.length === 0 ? (
          <EmptyState title="Nothing uploaded yet" message="Drop a file into the File Manager to get started." action={<button className="btn-primary" onClick={() => onNavigate('files')}>Go to File Manager</button>} />
        ) : (
          <ul className="divide-y divide-white/5">
            {stats.recentUploads.map((f) => (
              <li key={f.id}>
                <button onClick={() => onOpenFile(f.id)} className="flex w-full flex-wrap items-center justify-between gap-3 py-3 text-left transition hover:bg-white/[0.03]">
                  <div className="min-w-0">
                    <p className="truncate font-medium text-slate-100">{f.originalFilename}</p>
                    <p className="text-xs text-slate-500">{formatDate(f.uploadedAt)} · {f.chunkCount} chunks</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm text-slate-300">{formatBytes(f.fileSize)}</span>
                    <StatusBadge value={f.status} />
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

export function ErrorPanel({ message }) {
  return (
    <div className="panel border-rose-500/30">
      <p className="font-semibold text-rose-300">Something went wrong</p>
      <p className="mt-1 text-sm text-slate-400">{message}</p>
    </div>
  );
}
