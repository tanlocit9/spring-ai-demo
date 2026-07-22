import { useCallback, useMemo, useState } from 'react';
import {
  ChatApiError,
  createChatClient,
  type RichTextMessage,
  type SendChat,
} from './chat-api';

export interface UserTranscriptEntry {
  id: string;
  role: 'user';
  content: string;
  source: 'panel' | 'broadcast';
}

export interface AssistantTranscriptEntry {
  id: string;
  role: 'assistant';
  messages: RichTextMessage[];
}

export interface ErrorTranscriptEntry {
  id: string;
  role: 'error';
  content: string;
}

export type TranscriptEntry =
  UserTranscriptEntry | AssistantTranscriptEntry | ErrorTranscriptEntry;

export interface ChatPanelState {
  id: string;
  name: string;
  actorId: string;
  draft: string;
  transcript: TranscriptEntry[];
  pendingCount: number;
}

const initialPanels: ChatPanelState[] = [
  {
    id: 'panel-1',
    name: 'Agent panel 1',
    actorId: '1',
    draft: '',
    transcript: [],
    pendingCount: 0,
  },
  {
    id: 'panel-2',
    name: 'Agent panel 2',
    actorId: '2',
    draft: '',
    transcript: [],
    pendingCount: 0,
  },
  {
    id: 'panel-3',
    name: 'Agent panel 3',
    actorId: '3',
    draft: '',
    transcript: [],
    pendingCount: 0,
  },
];

let entrySequence = 0;
function nextEntryId(panelId: string) {
  entrySequence += 1;
  return `${panelId}-${Date.now()}-${entrySequence}`;
}

function errorMessage(error: unknown) {
  return error instanceof ChatApiError || error instanceof Error
    ? error.message
    : 'An unexpected chat error occurred.';
}

export function useChatPanels(sendChatOverride?: SendChat) {
  const [panels, setPanels] = useState<ChatPanelState[]>(initialPanels);
  const sendChat = useMemo(
    () => sendChatOverride ?? createChatClient(),
    [sendChatOverride],
  );

  const updatePanel = useCallback(
    (panelId: string, update: (panel: ChatPanelState) => ChatPanelState) => {
      setPanels((current) =>
        current.map((panel) => (panel.id === panelId ? update(panel) : panel)),
      );
    },
    [],
  );

  const setActorId = useCallback(
    (panelId: string, actorId: string) =>
      updatePanel(panelId, (panel) => ({ ...panel, actorId })),
    [updatePanel],
  );

  const setDraft = useCallback(
    (panelId: string, draft: string) =>
      updatePanel(panelId, (panel) => ({ ...panel, draft })),
    [updatePanel],
  );

  const requestPanel = useCallback(
    async (
      panelId: string,
      actorId: string,
      message: string,
      source: 'panel' | 'broadcast',
    ) => {
      updatePanel(panelId, (panel) => ({
        ...panel,
        pendingCount: panel.pendingCount + 1,
        transcript: [
          ...panel.transcript,
          { id: nextEntryId(panelId), role: 'user', content: message, source },
        ],
      }));

      try {
        const response = await sendChat(actorId, message);
        updatePanel(panelId, (panel) => ({
          ...panel,
          transcript: [
            ...panel.transcript,
            {
              id: nextEntryId(panelId),
              role: 'assistant',
              messages: response.messages,
            },
          ],
        }));
      } catch (error) {
        updatePanel(panelId, (panel) => ({
          ...panel,
          transcript: [
            ...panel.transcript,
            {
              id: nextEntryId(panelId),
              role: 'error',
              content: errorMessage(error),
            },
          ],
        }));
      } finally {
        updatePanel(panelId, (panel) => ({
          ...panel,
          pendingCount: Math.max(0, panel.pendingCount - 1),
        }));
      }
    },
    [sendChat, updatePanel],
  );

  const submitPanel = useCallback(
    async (panelId: string) => {
      const panel = panels.find((candidate) => candidate.id === panelId);
      if (!panel || !panel.draft.trim()) return;

      const message = panel.draft.trim();
      setDraft(panelId, '');
      await requestPanel(panel.id, panel.actorId, message, 'panel');
    },
    [panels, requestPanel, setDraft],
  );

  const broadcast = useCallback(
    async (messageInput: string) => {
      const message = messageInput.trim();
      if (!message) return;

      const snapshot = panels.map(({ id, actorId }) => ({ id, actorId }));
      await Promise.allSettled(
        snapshot.map((panel) =>
          requestPanel(panel.id, panel.actorId, message, 'broadcast'),
        ),
      );
    },
    [panels, requestPanel],
  );

  return { panels, setActorId, setDraft, submitPanel, broadcast };
}
