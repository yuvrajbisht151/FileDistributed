import { useEffect, useState } from 'react';
import { api } from '../services/api.js';
import UploadZone from '../components/UploadZone.jsx';
import FileTable from '../components/FileTable.jsx';
import EmptyState from '../components/EmptyState.jsx';
import Spinner from '../components/Spinner.jsx';
import { ErrorPanel } from './OverviewPage.jsx';

export default function FilesPage({ refreshKey, onRefresh, onOpenFile, notify, askConfirm }) {
  const [files, setFiles] = useState(null);
  const [search, setSearch] = useState('');
  const [debounced, setDebounced] = useState('');
  const [error, setError] = useState(null);
  const [verifyResults, setVerifyResults] = useState({});
  const [busyId, setBusyId] = useState(null);

  // Debounce the search box so we do not call the API on every keystroke.
  useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 300);
    return () => clearTimeout(t);
  }, [search]);

  useEffect(() => {
    let cancelled = false;
    api.listFiles(debounced)
      .then((data) => { if (!cancelled) { setFiles(data); setError(null); } })
      .catch((e) => !cancelled && setError(e.message));
    return () => { cancelled = true; };
  }, [debounced, refreshKey]);

  async function verify(file) {
    setBusyId(file.id);
    try {
      const report = await api.verifyFile(file.id);
      setVerifyResults((prev) => ({ ...prev, [file.id]: report }));
      const tone = report.status === 'VERIFIED' ? 'success' : report.status === 'CORRUPTED' ? 'error' : 'warning';
      notify(tone, `${file.originalFilename}: ${report.status}`);
    } catch (e) {
      notify('error', e.message);
    } finally {
      setBusyId(null);
    }
  }

  function requestDelete(file) {
    askConfirm({
      title: 'Delete this file?',
      message: `"${file.originalFilename}" and all of its primary and replica chunks will be removed. This cannot be undone.`,
      confirmLabel: 'Delete file',
      danger: true,
      onConfirm: () => doDelete(file),
    });
  }

  async function doDelete(file) {
    setBusyId(file.id);
    try {
      const result = await api.deleteFile(file.id);
      notify('success', result.message);
      onRefresh();
    } catch (e) {
      // 409 means some copies are on offline nodes: the file is kept and marked for cleanup.
      notify('warning', e.message);
      onRefresh();
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="space-y-6">
      <UploadZone onUploaded={onRefresh} notify={notify} />

      <section className="panel">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-base font-semibold text-white">Files</h2>
            <p className="text-xs text-slate-400">{files ? `${files.length} file${files.length === 1 ? '' : 's'}` : 'Loading…'}</p>
          </div>
          <input
            className="input max-w-xs"
            placeholder="Search by filename…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search files by name"
          />
        </div>

        {error && <ErrorPanel message={error} />}
        {!error && !files && <Spinner label="Loading files…" />}
        {files && files.length === 0 && (
          <EmptyState
            title={debounced ? 'No files match your search' : 'No files stored yet'}
            message={debounced ? 'Try a different name.' : 'Upload a file above to split it into chunks and distribute them.'}
          />
        )}
        {files && files.length > 0 && (
          <FileTable
            files={files}
            verifyResults={verifyResults}
            busyId={busyId}
            onOpen={onOpenFile}
            onVerify={verify}
            onDelete={requestDelete}
          />
        )}
      </section>
    </div>
  );
}
