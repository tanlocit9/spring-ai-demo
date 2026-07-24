import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { RichTextMessage } from './chat-api';
import type { ChatPanelState, TranscriptEntry } from './use-chat-panels';

function Metadata({ metadata }: { metadata: Record<string, unknown> }) {
  const entries = Object.entries(metadata);
  if (entries.length === 0) return null;

  return (
    <dl className="metadata">
      {entries.map(([key, value]) => (
        <div key={key}>
          <dt>{key}</dt>
          <dd>{typeof value === 'string' ? value : JSON.stringify(value)}</dd>
        </div>
      ))}
    </dl>
  );
}

export function RichMessage({ message }: { message: RichTextMessage }) {
  const body = message.content ? <p>{message.content}</p> : null;
  const items = message.items.length ? (
    <ul className={message.type === 'CHECKLIST' ? 'checklist' : undefined}>
      {message.items.map((item, index) => (
        <li key={`${item}-${index}`}>
          {message.type === 'CHECKLIST' && <span aria-hidden="true">✓</span>}
          {item}
        </li>
      ))}
    </ul>
  ) : null;

  if (message.type === 'HEADING') {
    return (
      <section className="rich-message heading-message">
        <h3>{message.title ?? message.content ?? 'Response'}</h3>
        {message.title && body}
        {items}
        <Metadata metadata={message.metadata} />
      </section>
    );
  }

  return (
    <section
      className={`rich-message rich-${message.type.toLowerCase()}`}
      aria-label={`${message.type.toLowerCase()} response`}
    >
      {message.title && <h4>{message.title}</h4>}
      {body}
      {items}
      <Metadata metadata={message.metadata} />
    </section>
  );
}

function TranscriptItem({ entry }: { entry: TranscriptEntry }) {
  if (entry.role === 'user') {
    return (
      <article className="transcript-entry user-entry">
        <span className="entry-label">
          You {entry.source === 'broadcast' ? '· broadcast' : ''}
        </span>
        <p>{entry.content}</p>
      </article>
    );
  }

  if (entry.role === 'error') {
    return (
      <article className="transcript-entry error-entry" role="alert">
        <span className="entry-label">Request error</span>
        <p>{entry.content}</p>
      </article>
    );
  }

  return (
    <article className="transcript-entry assistant-entry">
      <span className="entry-label">Agent</span>
      {entry.messages.length ? (
        entry.messages.map((message, index) => (
          <RichMessage key={`${entry.id}-${index}`} message={message} />
        ))
      ) : (
        <p className="empty-response">The agent returned no messages.</p>
      )}
    </article>
  );
}

interface ChatPanelProps {
  panel: ChatPanelState;
  onActorIdChange: (value: string) => void;
  onDraftChange: (value: string) => void;
  onSubmit: () => Promise<void>;
}

export function ChatPanel({
  panel,
  onActorIdChange,
  onDraftChange,
  onSubmit,
}: ChatPanelProps) {
  const transcriptEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const transcriptEnd = transcriptEndRef.current;
    if (typeof transcriptEnd?.scrollIntoView === 'function') {
      transcriptEnd.scrollIntoView({
        behavior: 'smooth',
        block: 'end',
      });
    }
  }, [panel.transcript, panel.pendingCount]);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void onSubmit();
  };

  return (
    <section className="chat-panel" aria-labelledby={`${panel.id}-title`}>
      <header className="panel-header">
        <div>
          <span className="eyebrow">Independent session</span>
          <h2 id={`${panel.id}-title`}>{panel.name}</h2>
        </div>
        <label className="actor-field">
          <span>Actor ID</span>
          <input
            aria-label={`${panel.name} actor ID`}
            inputMode="numeric"
            value={panel.actorId}
            onChange={(event) => onActorIdChange(event.target.value)}
          />
        </label>
      </header>

      <div
        className="transcript"
        aria-live="polite"
        aria-busy={panel.pendingCount > 0}
      >
        {panel.transcript.length === 0 ? (
          <div className="empty-state">
            <strong>Ready for actor {panel.actorId || '—'}</strong>
            <span>Send here, or use the broadcast box above.</span>
          </div>
        ) : (
          panel.transcript.map((entry) => (
            <TranscriptItem key={entry.id} entry={entry} />
          ))
        )}
        {panel.pendingCount > 0 && (
          <div className="thinking" role="status">
            <span /> Agent is handling {panel.pendingCount}{' '}
            {panel.pendingCount === 1 ? 'request' : 'requests'}…
          </div>
        )}
        <div
          ref={transcriptEndRef}
          data-testid={`${panel.id}-transcript-end`}
          aria-hidden="true"
        />
      </div>

      <form className="panel-composer" onSubmit={submit}>
        <label htmlFor={`${panel.id}-message`}>Message this panel</label>
        <div>
          <textarea
            id={`${panel.id}-message`}
            rows={2}
            placeholder="Ask about reports, PTO, employees…"
            value={panel.draft}
            onChange={(event) => onDraftChange(event.target.value)}
          />
          <button type="submit" disabled={!panel.draft.trim()}>
            Send
          </button>
        </div>
      </form>
    </section>
  );
}

interface BroadcastComposerProps {
  panelCount: number;
  onBroadcast: (message: string) => Promise<void>;
}

export function BroadcastComposer({
  panelCount,
  onBroadcast,
}: BroadcastComposerProps) {
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const outgoing = message.trim();
    if (!outgoing || sending) return;

    setMessage('');
    setSending(true);
    try {
      await onBroadcast(outgoing);
    } finally {
      setSending(false);
    }
  };

  return (
    <form className="broadcast-composer" onSubmit={submit}>
      <div className="broadcast-copy">
        <span className="broadcast-icon" aria-hidden="true">
          ↗
        </span>
        <div>
          <span className="eyebrow">Shared chat box</span>
          <h2>Broadcast to every panel</h2>
          <p>
            One message becomes {panelCount} concurrent requests, each using its
            panel’s actor ID.
          </p>
        </div>
      </div>
      <div className="broadcast-input">
        <label htmlFor="broadcast-message">Shared message</label>
        <textarea
          id="broadcast-message"
          rows={2}
          placeholder="Send the same prompt to all agents…"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
        />
        <button type="submit" disabled={!message.trim() || sending}>
          {sending ? `Sending to ${panelCount}…` : `Send to all ${panelCount}`}
        </button>
      </div>
    </form>
  );
}
