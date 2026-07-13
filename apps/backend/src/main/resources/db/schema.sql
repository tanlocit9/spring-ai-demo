CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(64) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    department VARCHAR(255) NOT NULL,
    manager_id BIGINT REFERENCES employees(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_employees_manager_id ON employees(manager_id);
CREATE INDEX IF NOT EXISTS idx_employees_full_name_lower ON employees(LOWER(full_name));

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'report_type') THEN
        CREATE TYPE report_type AS ENUM ('DAILY', 'WEEKLY');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pto_type') THEN
        CREATE TYPE pto_type AS ENUM ('SICK_LEAVE', 'ANNUAL_LEAVE', 'UNPAID_LEAVE', 'OTHER');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pto_status') THEN
        CREATE TYPE pto_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    report_type report_type NOT NULL,
    report_date DATE,
    period_start DATE,
    period_end DATE,
    completed_tasks JSONB NOT NULL DEFAULT '[]'::jsonb,
    next_plans JSONB NOT NULL DEFAULT '[]'::jsonb,
    blockers JSONB NOT NULL DEFAULT '[]'::jsonb,
    content TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_daily_report_dates CHECK (
        (report_type = 'DAILY' AND report_date IS NOT NULL AND period_start IS NULL AND period_end IS NULL)
        OR
        (report_type = 'WEEKLY' AND report_date IS NULL AND period_start IS NOT NULL AND period_end IS NOT NULL AND period_start <= period_end)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_daily_report_employee_date
    ON reports(employee_id, report_date)
    WHERE report_type = 'DAILY';

CREATE UNIQUE INDEX IF NOT EXISTS uq_weekly_report_employee_period
    ON reports(employee_id, period_start, period_end)
    WHERE report_type = 'WEEKLY';

CREATE INDEX IF NOT EXISTS idx_reports_employee_type ON reports(employee_id, report_type);
CREATE INDEX IF NOT EXISTS idx_reports_report_date ON reports(report_date);
CREATE INDEX IF NOT EXISTS idx_reports_period ON reports(period_start, period_end);

CREATE TABLE IF NOT EXISTS pto_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    pto_date DATE NOT NULL,
    pto_type pto_type NOT NULL,
    reason TEXT,
    status pto_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pto_record_employee_date_type
    ON pto_records(employee_id, pto_date, pto_type);

CREATE INDEX IF NOT EXISTS idx_pto_records_employee_date_status
    ON pto_records(employee_id, pto_date, status);
