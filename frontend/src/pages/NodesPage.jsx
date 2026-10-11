import { useEffect, useState } from 'react';
import { api } from '../services/api.js';
import NodeCard from '../components/NodeCard.jsx';
import Spinner from '../components/Spinner.jsx';
import { ErrorPanel } from './OverviewPage.jsx';

export default function NodesPage({ refreshKey, onRefresh, notify, askConfirm }) {
  const [nodes, setNodes] = useState(null);
  const [error, setError] = useState(null);
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    api.listNodes()
      .then((data) => { if (!cancelled) { setNodes(data); setError(null); } })
      .catch((e) => !cancelled && setError(e.message));
    return () => { cancelled = true; };
  }, [refreshKey]);

  function toggle(node) {
    if (node.status === 'ONLINE') {
      askConfirm({
        title: `Simulate failure of ${node.name}?`,
        message: 'This is a software-level simulation. The node is marked OFFLINE in the database: it gets no new chunks, and its copies are skipped on download and verification.',
        confirmLabel: 'Simulate failure',
        danger: true,
        onConfirm: () => change(node, false),
      });
    } else {
      change(node, true);
    }
  }

  async function change(node, online) {
    setBusyId(node.id);
    try {
      await api.setNodeOnline(node.id, online);
      notify(online ? 'success' : 'warning', `${node.name} is now ${online ? 'ONLINE' : 'OFFLINE'}`);
      onRefresh();
    } catch (e) {
      notify('error', e.message);
    } finally {
      setBusyId(null);
    }
  }

  if (error) return <ErrorPanel message={error} />;
  if (!nodes) return <div className="panel"><Spinner label="Loading storage nodes…" /></div>;

  return (
    <div className="space-y-6">
      <div className="panel">
        <h2 className="text-base font-semibold text-white">Logical storage nodes</h2>
        <p className="mt-1 text-sm text-slate-400">
          These are three folders managed by one backend on one machine, not independent servers.
          Use the buttons to simulate a node failure and watch downloads fall back to replicas.
        </p>
      </div>
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
        {nodes.map((n) => (
          <NodeCard key={n.id} node={n} busy={busyId === n.id} onToggle={toggle} />
        ))}
      </div>
    </div>
  );
}
