import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import App from './app';
import type { SendChat } from './chat-api';
import { BroadcastComposer, ChatPanel, RichMessage } from './chat-ui';
import type { GetEmployeeTree } from './employee-tree-api';
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

const employeeTree = {
  id: 1,
  employeeCode: 'E001',
  fullName: 'Alice Nguyen',
  department: 'Executive',
  managerId: null,
  children: [
    {
      id: 2,
      employeeCode: 'E002',
      fullName: 'Bob Tran',
      department: 'Engineering',
      managerId: 1,
      children: [],
    },
  ],
};

describe('employee tree on initial view', () => {
  it('loads actor 1 and displays the hierarchy above broadcast', async () => {
    const getEmployeeTree = vi
      .fn<GetEmployeeTree>()
      .mockResolvedValue(employeeTree);

    render(<App getEmployeeTree={getEmployeeTree} />);

    expect(screen.getByText('Loading manager tree…')).toBeTruthy();
    expect(await screen.findByText('Alice Nguyen')).toBeTruthy();
    expect(screen.getByText('Bob Tran')).toBeTruthy();
    expect(getEmployeeTree).toHaveBeenCalledWith(1, expect.any(AbortSignal));

    const headings = screen.getAllByRole('heading');
    expect(
      headings.indexOf(screen.getByRole('heading', { name: 'Manager tree' })),
    ).toBeLessThan(
      headings.indexOf(
        screen.getByRole('heading', { name: 'Broadcast to every panel' }),
      ),
    );
  });

  it('shows an error and retries the tree request', async () => {
    const getEmployeeTree = vi
      .fn<GetEmployeeTree>()
      .mockRejectedValueOnce(new Error('Database unavailable'))
      .mockResolvedValueOnce(employeeTree);

    render(<App getEmployeeTree={getEmployeeTree} />);

    expect(await screen.findByText('Manager tree unavailable')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Alice Nguyen')).toBeTruthy();
    expect(getEmployeeTree).toHaveBeenCalledTimes(2);
  });
});

describe('multi-panel chat', () => {
  it('smooth-scrolls a panel when its transcript changes', () => {
    const scrollIntoView = vi.fn();
    const panel = {
      id: 'panel-1',
      name: 'Agent panel 1',
      actorId: '1',
      draft: '',
      transcript: [],
      pendingCount: 0,
    };
    const props = {
      onActorIdChange: vi.fn(),
      onDraftChange: vi.fn(),
      onSubmit: vi.fn().mockResolvedValue(undefined),
    };
    const { rerender } = render(<ChatPanel panel={panel} {...props} />);
    const transcriptEnd = screen.getByTestId('panel-1-transcript-end');
    Object.defineProperty(transcriptEnd, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    });

    rerender(
      <ChatPanel
        panel={{
          ...panel,
          transcript: [
            {
              id: 'message-1',
              role: 'user',
              source: 'panel',
              content: 'Hello agent',
            },
          ],
        }}
        {...props}
      />,
    );

    expect(scrollIntoView).toHaveBeenCalledWith({
      behavior: 'smooth',
      block: 'end',
    });
  });

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
