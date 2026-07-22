import { ChatApiError, createChatClient, parseActorId } from './chat-api';

describe('chat API client', () => {
  it('posts the backend contract with the actor header', async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(
        JSON.stringify({
          messages: [
            {
              type: 'PARAGRAPH',
              title: null,
              content: 'Hello',
              items: null,
              metadata: null,
            },
          ],
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );

    const client = createChatClient({
      baseUrl: 'http://example.test/',
      fetcher,
    });
    const result = await client('7', '  hello  ');

    expect(fetcher).toHaveBeenCalledWith('http://example.test/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Actor-Id': '7' },
      body: JSON.stringify({ message: 'hello' }),
    });
    expect(result.messages[0].items).toEqual([]);
    expect(result.messages[0].metadata).toEqual({});
  });

  it.each(['', '0', '-1', '1.5', 'abc'])(
    'rejects invalid actor ID %j',
    (value) => {
      expect(() => parseActorId(value)).toThrow(ChatApiError);
    },
  );

  it('reports HTTP failures', async () => {
    const client = createChatClient({
      fetcher: vi
        .fn<typeof fetch>()
        .mockResolvedValue(new Response('', { status: 503 })),
    });

    await expect(client('1', 'hello')).rejects.toMatchObject({ status: 503 });
  });

  it('rejects malformed responses', async () => {
    const client = createChatClient({
      fetcher: vi.fn<typeof fetch>().mockResolvedValue(
        new Response(JSON.stringify({ message: 'wrong shape' }), {
          status: 200,
        }),
      ),
    });

    await expect(client('1', 'hello')).rejects.toThrow('invalid response');
  });
});
