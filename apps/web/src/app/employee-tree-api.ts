export interface EmployeeTreeNode {
  id: number;
  employeeCode: string;
  fullName: string;
  department: string;
  managerId: number | null;
  children: EmployeeTreeNode[];
}

export interface EmployeeTreeClientOptions {
  baseUrl?: string;
  fetcher?: typeof fetch;
}

export class EmployeeTreeApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'EmployeeTreeApiError';
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function parsePositiveInteger(value: unknown, field: string): number {
  if (!Number.isSafeInteger(value) || (value as number) < 1) {
    throw new EmployeeTreeApiError(
      `The employee tree contains an invalid ${field}.`,
    );
  }
  return value as number;
}

function parseTreeNode(value: unknown): EmployeeTreeNode {
  if (
    !isRecord(value) ||
    typeof value.employeeCode !== 'string' ||
    typeof value.fullName !== 'string' ||
    typeof value.department !== 'string' ||
    !Array.isArray(value.children)
  ) {
    throw new EmployeeTreeApiError(
      'The employee tree service returned an invalid response.',
    );
  }

  const managerId =
    value.managerId === null
      ? null
      : parsePositiveInteger(value.managerId, 'manager ID');

  return {
    id: parsePositiveInteger(value.id, 'employee ID'),
    employeeCode: value.employeeCode,
    fullName: value.fullName,
    department: value.department,
    managerId,
    children: value.children.map(parseTreeNode),
  };
}

export function createEmployeeTreeClient(
  options: EmployeeTreeClientOptions = {},
) {
  const fetcher = options.fetcher ?? fetch;
  const baseUrl = (
    options.baseUrl ??
    import.meta.env.VITE_API_BASE_URL ??
    ''
  ).replace(/\/$/, '');

  return async function getEmployeeTree(
    actorId: number,
    signal?: AbortSignal,
  ): Promise<EmployeeTreeNode> {
    let response: Response;
    try {
      response = await fetcher(`${baseUrl}/api/employees/tree`, {
        headers: { 'X-Actor-Id': String(actorId) },
        signal,
      });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        throw error;
      }
      throw new EmployeeTreeApiError(
        'Unable to load the employee tree. Confirm that the backend is running.',
      );
    }

    if (!response.ok) {
      throw new EmployeeTreeApiError(
        `Employee tree request failed (${response.status}).`,
        response.status,
      );
    }

    try {
      return parseTreeNode(await response.json());
    } catch (error) {
      if (error instanceof EmployeeTreeApiError) throw error;
      throw new EmployeeTreeApiError(
        'The employee tree service returned invalid JSON.',
      );
    }
  };
}

export type GetEmployeeTree = ReturnType<typeof createEmployeeTreeClient>;
