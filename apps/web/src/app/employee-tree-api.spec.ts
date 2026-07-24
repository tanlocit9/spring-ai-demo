import {
    EmployeeTreeApiError,
    createEmployeeTreeClient,
} from './employee-tree-api';

const tree = {
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

describe('employee tree API client', () => {
  it('gets the recursive tree with the actor header', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValue(new Response(JSON.stringify(tree), { status: 200 }));
    const client = createEmployeeTreeClient({
      baseUrl: 'http://example.test/',
      fetcher,
    });

    await expect(client(1)).resolves.toEqual(tree);
    expect(fetcher).toHaveBeenCalledWith(
      'http://example.test/api/employees/tree',
      expect.objectContaining({ headers: { 'X-Actor-Id': '1' } }),
    );
  });

  it('reports HTTP failures', async () => {
    const client = createEmployeeTreeClient({
      fetcher: vi
        .fn<typeof fetch>()
        .mockResolvedValue(new Response('', { status: 404 })),
    });

    await expect(client(99)).rejects.toMatchObject({ status: 404 });
  });

  it('reports network failures', async () => {
    const client = createEmployeeTreeClient({
      fetcher: vi.fn<typeof fetch>().mockRejectedValue(new TypeError('offline')),
    });

    await expect(client(1)).rejects.toThrow('Unable to load');
  });

  it('rejects malformed nested nodes', async () => {
    const client = createEmployeeTreeClient({
      fetcher: vi.fn<typeof fetch>().mockResolvedValue(
        new Response(
          JSON.stringify({ ...tree, children: [{ id: 'invalid' }] }),
          { status: 200 },
        ),
      ),
    });

    await expect(client(1)).rejects.toBeInstanceOf(EmployeeTreeApiError);
  });
});
