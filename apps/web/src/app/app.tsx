import { BroadcastComposer, ChatPanel } from './chat-ui';
import { useChatPanels } from './use-chat-panels';

export function App() {
  const { panels, setActorId, setDraft, submitPanel, broadcast } =
    useChatPanels();

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
