INSERT INTO employees (id, employee_code, full_name, department, manager_id) VALUES
    (1, 'E001', 'Alice Nguyen', 'Executive', NULL),
    (2, 'E002', 'Bob Tran', 'Engineering', 1),
    (3, 'E003', 'Carol Pham', 'Product', 1),
    (4, 'E004', 'David Kim', 'Engineering', 2),
    (5, 'E005', 'David Kim', 'Engineering', 2),
    (6, 'E006', 'Eva Le', 'Product', 3)
ON CONFLICT (id) DO UPDATE SET
    employee_code = EXCLUDED.employee_code,
    full_name = EXCLUDED.full_name,
    department = EXCLUDED.department,
    manager_id = EXCLUDED.manager_id,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(pg_get_serial_sequence('employees', 'id'), (SELECT MAX(id) FROM employees));

INSERT INTO reports (
    employee_id,
    report_type,
    report_date,
    completed_tasks,
    next_plans,
    blockers,
    content
) VALUES
    (2, 'DAILY', CURRENT_DATE - INTERVAL '4 days', '["Reviewed team delivery risks", "Synced with product"]'::jsonb, '["Prepare weekly summary"]'::jsonb, '[]'::jsonb, 'Reviewed team delivery risks and synced with product.'),
    (4, 'DAILY', CURRENT_DATE - INTERVAL '4 days', '["Implemented report draft persistence", "Added repository tests"]'::jsonb, '["Wire Spring AI tools"]'::jsonb, '[]'::jsonb, 'Implemented report draft persistence and added repository tests.'),
    (5, 'DAILY', CURRENT_DATE - INTERVAL '4 days', '["Investigated duplicate employee resolution", "Documented edge cases"]'::jsonb, '["Pair on access checks"]'::jsonb, '["Need final UX copy"]'::jsonb, 'Investigated duplicate employee resolution and documented edge cases.'),
    (6, 'DAILY', CURRENT_DATE - INTERVAL '4 days', '["Prepared customer feedback summary"]'::jsonb, '["Prioritize report filters"]'::jsonb, '[]'::jsonb, 'Prepared customer feedback summary.')
ON CONFLICT DO NOTHING;

INSERT INTO pto_records (employee_id, pto_date, pto_type, reason, status) VALUES
    (4, CURRENT_DATE - INTERVAL '3 days', 'SICK_LEAVE', 'Demo sick leave record for weekly validation.', 'APPROVED'),
    (6, CURRENT_DATE - INTERVAL '3 days', 'ANNUAL_LEAVE', 'Demo annual leave record for weekly validation.', 'APPROVED')
ON CONFLICT DO NOTHING;
