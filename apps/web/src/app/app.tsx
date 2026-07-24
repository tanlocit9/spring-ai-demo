import { useCallback, useEffect, useMemo, useState } from 'react';
import { BroadcastComposer, ChatPanel } from './chat-ui';
import {
  createEmployeeTreeClient,
  type EmployeeTreeNode,
  type GetEmployeeTree,
} from './employee-tree-api';
import { EmployeeTreeView } from './employee-tree-view';
import { useChatPanels } from './use-chat-panels';

export interface AppProps {
  getEmployeeTree?: GetEmployeeTree;
}

export function App({ getEmployeeTree: getEmployeeTreeOverride }: AppProps = {}) {
  const { panels, setActorId, setDraft, submitPanel, broadcast } =
    useChatPanels();
  const getEmployeeTree = useMemo(
    () => getEmployeeTreeOverride ?? createEmployeeTreeClient(),
    [getEmployeeTreeOverride],
  );
  const [tree, setTree] = useState<EmployeeTreeNode | null>(null);
  const [treeLoading, setTreeLoading] = useState(true);
  const [treeError, setTreeError] = useState<string | null>(null);
  const [treeRequest, setTreeRequest] = useState(0);

  const retryTree = useCallback(() => {
    setTreeRequest((request) => request + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setTreeLoading(true);
    setTreeError(null);

    getEmployeeTree(1, controller.signal)
      .then((response) => setTree(response))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return;
        setTreeError(
          error instanceof Error
            ? error.message
            : 'An unexpected employee tree error occurred.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setTreeLoading(false);
      });

    return () => controller.abort();
  }, [getEmployeeTree, treeRequest]);

  return (
    <main className="app-shell">
      <header className="hero">
        <div>
          <span className="eyebrow">Spring AI · Multi-agent console</span>
          <h1>Ask once. Compare every response.</h1>
          <p>
            Chat independently with each actor, or broadcast one prompt to all
            panels at the same time.
          </p>
        </div>
        <div className="connection-badge">
          <span aria-hidden="true" />
          API ready at /api/chat
        </div>
      </header>

      <EmployeeTreeView
        tree={tree}
        loading={treeLoading}
        error={treeError}
        onRetry={retryTree}
      />

      <BroadcastComposer panelCount={panels.length} onBroadcast={broadcast} />

      <section className="panel-grid" aria-label="Agent chat panels">
        {panels.map((panel) => (
          <ChatPanel
            key={panel.id}
            panel={panel}
            onActorIdChange={(value) => setActorId(panel.id, value)}
            onDraftChange={(value) => setDraft(panel.id, value)}
            onSubmit={() => submitPanel(panel.id)}
          />
        ))}
      </section>
    </main>
  );
}

export default App;
