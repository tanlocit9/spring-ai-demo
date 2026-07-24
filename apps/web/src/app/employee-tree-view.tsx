import type { EmployeeTreeNode } from './employee-tree-api';

interface EmployeeBranchProps {
  employee: EmployeeTreeNode;
  root?: boolean;
}

function EmployeeBranch({ employee, root = false }: EmployeeBranchProps) {
  return (
    <li className="employee-branch">
      <article className={`employee-card${root ? ' employee-card-root' : ''}`}>
        <div className="employee-avatar" aria-hidden="true">
          {employee.fullName
            .split(/\s+/)
            .slice(0, 2)
            .map((part) => part[0])
            .join('')
            .toUpperCase()}
        </div>
        <div className="employee-copy">
          <strong>{employee.fullName}</strong>
          <span>{employee.department}</span>
        </div>
        <dl className="employee-details">
          <div>
            <dt>Code</dt>
            <dd>{employee.employeeCode}</dd>
          </div>
          <div>
            <dt>Manager</dt>
            <dd>{employee.managerId ?? 'Top level'}</dd>
          </div>
        </dl>
      </article>

      {employee.children.length > 0 && (
        <ul className="employee-children" aria-label={`Reports to ${employee.fullName}`}>
          {employee.children.map((child) => (
            <EmployeeBranch key={child.id} employee={child} />
          ))}
        </ul>
      )}
    </li>
  );
}

interface EmployeeTreeViewProps {
  tree: EmployeeTreeNode | null;
  loading: boolean;
  error: string | null;
  onRetry: () => void;
}

export function EmployeeTreeView({
  tree,
  loading,
  error,
  onRetry,
}: EmployeeTreeViewProps) {
  return (
    <section className="employee-tree-section" aria-labelledby="employee-tree-title">
      <header className="employee-tree-header">
        <div>
          <span className="eyebrow">Organization overview</span>
          <h2 id="employee-tree-title">Manager tree</h2>
          <p>The reporting hierarchy visible to demo actor 1.</p>
        </div>
        {tree && !loading && (
          <span className="tree-status">
            <span aria-hidden="true" /> Live database view
          </span>
        )}
      </header>

      {loading && (
        <div className="tree-loading" role="status">
          <span aria-hidden="true" /> Loading manager tree…
        </div>
      )}

      {!loading && error && (
        <div className="tree-error" role="alert">
          <div>
            <strong>Manager tree unavailable</strong>
            <p>{error}</p>
          </div>
          <button type="button" onClick={onRetry}>
            Retry
          </button>
        </div>
      )}

      {!loading && !error && tree && (
        <div className="employee-tree-scroll">
          <ul className="employee-tree" aria-label="Employee manager hierarchy">
            <EmployeeBranch employee={tree} root />
          </ul>
        </div>
      )}
    </section>
  );
}
