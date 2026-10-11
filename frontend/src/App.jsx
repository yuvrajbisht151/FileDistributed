import { useCallback, useEffect, useRef, useState } from 'react';
import OverviewPage from './pages/OverviewPage.jsx';
import FilesPage from './pages/FilesPage.jsx';
import NodesPage from './pages/NodesPage.jsx';
import FileDetailsModal from './components/FileDetailsModal.jsx';
import ConfirmDialog from './components/ConfirmDialog.jsx';
import ToastStack from './components/ToastStack.jsx';

const NAV = [
  { id: 'overview', label: 'Overview', icon: '📊' },
  { id: 'files', label: 'File Manager', icon: '📁' },
  { id: 'nodes', label: 'Storage Nodes', icon: '🗄' },
];

export default function App() {
  const [view, setView] = useState('overview');
  const [refreshKey, setRefreshKey] = useState(0);
  const [selectedFileId, setSelectedFileId] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [toasts, setToasts] = useState([]);
  const toastTimers = useRef({});

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), []);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
    clearTimeout(toastTimers.current[id]);
    delete toastTimers.current[id];
  }, []);

  const notify = useCallback((type, message) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    setToasts((prev) => [...prev.slice(-4), { id, type, message }]);
    toastTimers.current[id] = setTimeout(() => dismiss(id), 4500);
  }, [dismiss]);

  useEffect(() => () => Object.values(toastTimers.current).forEach(clearTimeout), []);

  function handleConfirm() {
    const action = confirm?.onConfirm;
    setConfirm(null);
    action?.();
  }

  return (
    <div className="min-h-screen md:flex">
      {/* Sidebar (desktop) */}
      <aside className="hidden w-64 shrink-0 border-r border-white/5 bg-navy-900/60 p-6 backdrop-blur md:flex md:flex-col">
        <Brand />
        <nav className="mt-10 space-y-1">
          {NAV.map((item) => (
            <button
              key={item.id}
              onClick={() => setView(item.id)}
              className={`flex w-full items-center gap-3 rounded-xl px-4 py-2.5 text-left text-sm font-medium transition
                ${view === item.id ? 'bg-accent/20 text-white shadow-inner ring-1 ring-accent/30' : 'text-slate-400 hover:bg-white/5 hover:text-slate-100'}`}
            >
              <span>{item.icon}</span>
              {item.label}
            </button>
          ))}
        </nav>
        <div className="mt-auto rounded-xl bg-white/5 p-4 text-xs leading-relaxed text-slate-400">
          Three logical nodes on one machine. Replication factor 2, 1 MiB chunks.
        </div>
      </aside>

      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
        {/* Top bar (mobile) */}
        <header className="sticky top-0 z-30 border-b border-white/5 bg-navy-950/80 px-4 py-3 backdrop-blur md:hidden">
          <Brand compact />
          <nav className="mt-3 flex gap-2 overflow-x-auto pb-1">
            {NAV.map((item) => (
              <button
                key={item.id}
                onClick={() => setView(item.id)}
                className={`shrink-0 rounded-full px-4 py-1.5 text-xs font-medium transition
                  ${view === item.id ? 'bg-accent text-white' : 'bg-white/5 text-slate-300'}`}
              >
                {item.label}
              </button>
            ))}
          </nav>
        </header>

        <main className="mx-auto w-full max-w-7xl flex-1 p-4 sm:p-6 lg:p-10">
          <PageHeader view={view} onRefresh={refresh} />
          {view === 'overview' && (
            <OverviewPage refreshKey={refreshKey} onOpenFile={setSelectedFileId} onNavigate={setView} />
          )}
          {view === 'files' && (
            <FilesPage
              refreshKey={refreshKey}
              onRefresh={refresh}
              onOpenFile={setSelectedFileId}
              notify={notify}
              askConfirm={setConfirm}
            />
          )}
          {view === 'nodes' && (
            <NodesPage refreshKey={refreshKey} onRefresh={refresh} notify={notify} askConfirm={setConfirm} />
          )}
        </main>
      </div>

      {selectedFileId && (
        <FileDetailsModal fileId={selectedFileId} onClose={() => setSelectedFileId(null)} notify={notify} />
      )}
      <ConfirmDialog config={confirm} onCancel={() => setConfirm(null)} onConfirm={handleConfirm} />
      <ToastStack toasts={toasts} onDismiss={dismiss} />
    </div>
  );
}

function Brand({ compact }) {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-accent to-violet-800 text-lg shadow-lg shadow-accent/30">◈</div>
      <div>
        <p className={`font-bold text-white ${compact ? 'text-sm' : 'text-base'}`}>DFS Dashboard</p>
        {!compact && <p className="text-xs text-slate-400">Distributed File Storage</p>}
      </div>
    </div>
  );
}

function PageHeader({ view, onRefresh }) {
  const titles = {
    overview: ['Overview', 'Live numbers calculated from the backend database.'],
    files: ['File Manager', 'Upload, search, verify, download and delete files.'],
    nodes: ['Storage Nodes', 'Node health, utilization, and failure simulation.'],
  };
  const [title, subtitle] = titles[view];
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">{title}</h1>
        <p className="mt-1 text-sm text-slate-400">{subtitle}</p>
      </div>
      <button className="btn-ghost text-xs" onClick={onRefresh}>↻ Refresh</button>
    </div>
  );
}
