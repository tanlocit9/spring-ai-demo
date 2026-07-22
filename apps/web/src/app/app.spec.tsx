import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import type { SendChat } from './chat-api';
import { BroadcastComposer, ChatPanel, RichMessage } from './chat-ui';
import { useChatPanels } from './use-chat-panels';

function TestApp({ sendChat }: { sendChat: SendChat }) {
  const { panels, setActorId, setDraft, submitPanel, broadcast } =
    useChatPanels(sendChat);

  return (
    <>
      <BroadcastComposer panelCount={panels.length} onBroadcast={broadcast} />
      {panels.map((panel) => (
        <ChatPanel
          key={panel.id}
          panel={panel}
          onActorIdChange={(value) => setActorId(panel.id, value)}
          onDraftChange={(value) => setDraft(panel.id, value)}
          onSubmit={() => submitPanel(panel.id)}
        />
      ))}
    </>
  );
}

const response = {
  messages: [
    {
      type: 'STATUS' as const,
      title: 'Report status',
      content: 'Ready',
      items: [],
      metadata: { status: 'success' },
    },
  ],
};

describe('multi-panel chat', () => {
  it('sends a panel message with that panel actor ID only', async () => {
    const sendChat = vi.fn<SendChat>().mockResolvedValue(response);
    render(<TestApp sendChat={sendChat} />);

    fireEvent.change(screen.getByLabelText('Agent panel 1 actor ID'), {
      target: { value: '42' },
    });
    fireEvent.change(screen.getAllByLabelText('Message this panel')[0], {
      target: { value: 'show my report' },
    });
    fireEvent.click(screen.getAllByRole('button', { name: 'Send' })[0]);

    await waitFor(() =>
      expect(sendChat).toHaveBeenCalledWith('42', 'show my report'),
    );
    expect(sendChat).toHaveBeenCalledTimes(1);
    expect(await screen.findByText('Report status')).toBeTruthy();
  });

  it('broadcasts concurrently with every panel actor ID', async () => {
    const pending: Array<() => void> = [];
    const sendChat = vi.fn<SendChat>().mockImplementation(
      () =>
        new Promise((resolve) => {
          pending.push(() => resolve(response));
        }),
    );
    render(<TestApp sendChat={sendChat} />);

    fireEvent.change(screen.getByLabelText('Shared message'), {
      target: { value: 'hello agents' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Send to all 3' }));

    await waitFor(() => expect(sendChat).toHaveBeenCalledTimes(3));
    expect(sendChat.mock.calls).toEqual([
      ['1', 'hello agents'],
      ['2', 'hello agents'],
      ['3', 'hello agents'],
    ]);
    expect(screen.getAllByText('You · broadcast')).toHaveLength(3);

    pending.forEach((resolve) => resolve());
    await waitFor(() =>
      expect(screen.getAllByText('Report status')).toHaveLength(3),
    );
  });

  it('renders checklist items and metadata', () => {
    render(
      <RichMessage
        message={{
          type: 'CHECKLIST',
          title: 'Next plan',
          content: null,
          items: ['Review report', 'Submit PTO'],
          metadata: { date: '2026-07-22' },
        }}
      />,
    );

    expect(screen.getByText('Review report')).toBeTruthy();
    expect(screen.getByText('2026-07-22')).toBeTruthy();
  });
});
