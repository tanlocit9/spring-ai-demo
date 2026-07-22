export const RICH_TEXT_TYPES = [
  'TEXT',
  'HEADING',
  'PARAGRAPH',
  'SECTION',
  'CHECKLIST',
  'STATUS',
  'EMPLOYEE_OPTIONS',
  'ERROR',
] as const;

export type RichTextType = (typeof RICH_TEXT_TYPES)[number];

export interface RichTextMessage {
  type: RichTextType;
  title: string | null;
  content: string | null;
  items: string[];
  metadata: Record<string, unknown>;
}

export interface ChatResponse {
  messages: RichTextMessage[];
}

export interface ChatClientOptions {
  baseUrl?: string;
  fetcher?: typeof fetch;
}

export class ChatApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'ChatApiError';
  }
}

const richTextTypes = new Set<string>(RICH_TEXT_TYPES);

export function parseActorId(value: string): number {
  const normalized = value.trim();
  if (!/^\d+$/.test(normalized)) {
    throw new ChatApiError('Actor ID must be a positive whole number.');
  }

  const actorId = Number(normalized);
  if (!Number.isSafeInteger(actorId) || actorId < 1) {
    throw new ChatApiError('Actor ID must be a positive whole number.');
  }

  return actorId;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function parseMessage(value: unknown): RichTextMessage {
  if (!isRecord(value) || typeof value.type !== 'string') {
    throw new ChatApiError('The chat service returned an invalid message.');
  }

  const type = richTextTypes.has(value.type) ? value.type : 'ERROR';
  const items = Array.isArray(value.items)
    ? value.items.filter((item): item is string => typeof item === 'string')
    : [];

  return {
    type: type as RichTextType,
    title: typeof value.title === 'string' ? value.title : null,
    content:
      typeof value.content === 'string'
        ? value.content
        : richTextTypes.has(value.type)
          ? null
          : `Unsupported response type: ${value.type}`,
    items,
    metadata: isRecord(value.metadata) ? value.metadata : {},
  };
}

function parseResponse(value: unknown): ChatResponse {
  if (!isRecord(value) || !Array.isArray(value.messages)) {
    throw new ChatApiError('The chat service returned an invalid response.');
  }

  return { messages: value.messages.map(parseMessage) };
}

export function createChatClient(options: ChatClientOptions = {}) {
  const fetcher = options.fetcher ?? fetch;
  const baseUrl = (
    options.baseUrl ??
    import.meta.env.VITE_API_BASE_URL ??
    ''
  ).replace(/\/$/, '');

  return async function sendChat(
    actorIdInput: string,
    messageInput: string,
  ): Promise<ChatResponse> {
    const actorId = parseActorId(actorIdInput);
    const message = messageInput.trim();
    if (!message) {
      throw new ChatApiError('Enter a message before sending.');
    }

    let response: Response;
    try {
      response = await fetcher(`${baseUrl}/api/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Actor-Id': String(actorId),
        },
        body: JSON.stringify({ message }),
      });
    } catch {
      throw new ChatApiError(
        'Unable to reach the chat service. Confirm that the backend is running.',
      );
    }

    if (!response.ok) {
      throw new ChatApiError(
        `Chat request failed (${response.status}).`,
        response.status,
      );
    }

    try {
      return parseResponse(await response.json());
    } catch (error) {
      if (error instanceof ChatApiError) {
        throw error;
      }
      throw new ChatApiError('The chat service returned invalid JSON.');
    }
  };
}

export type SendChat = ReturnType<typeof createChatClient>;
