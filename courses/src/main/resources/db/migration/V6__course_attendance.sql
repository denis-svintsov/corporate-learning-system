CREATE TABLE IF NOT EXISTS courses.course_attendance (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    course_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    comment VARCHAR(255),
    marked_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_course_attendance_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE,
    CONSTRAINT uq_course_attendance_user_date UNIQUE (course_id, user_id, attendance_date)
);

CREATE INDEX IF NOT EXISTS idx_course_attendance_course_date
    ON courses.course_attendance(course_id, attendance_date);

INSERT INTO courses.course_attendance (
    id,
    course_id,
    user_id,
    attendance_date,
    status,
    comment,
    marked_by,
    created_at,
    updated_at
)
VALUES
    ('45000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000002', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', DATE '2026-06-18' - 2, 'PRESENT', 'Загрузил данные пропуска дефектоскопа в программный комплекс', '55555555-5555-4555-8555-555555555555', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '2 days', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '2 days'),
    ('45000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000002', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', DATE '2026-06-18' - 1, 'PRESENT', 'Отработал классификацию дефектов и стресс-коррозионных признаков', '55555555-5555-4555-8555-555555555555', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '1 day', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '1 day'),
    ('45000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000002', '66666666-6666-4666-8666-666666666666', DATE '2026-06-18' - 2, 'PRESENT', 'Загрузила данные пропуска дефектоскопа в программный комплекс', '55555555-5555-4555-8555-555555555555', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '2 days', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '2 days'),
    ('45000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000002', '66666666-6666-4666-8666-666666666666', DATE '2026-06-18' - 1, 'PRESENT', 'Отработала классификацию дефектов и стресс-коррозионных признаков', '55555555-5555-4555-8555-555555555555', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '1 day', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '1 day'),
    ('45000000-0000-4000-8000-000000000005', '30000000-0000-4000-8000-000000000002', '77777777-7777-4777-8777-777777777777', DATE '2026-06-18' - 2, 'PRESENT', 'Присутствовал на разборе структуры технического отчета', '55555555-5555-4555-8555-555555555555', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '2 days', TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+04' - INTERVAL '2 days')
ON CONFLICT (course_id, user_id, attendance_date) DO UPDATE
SET status = EXCLUDED.status,
    comment = EXCLUDED.comment,
    marked_by = EXCLUDED.marked_by,
    updated_at = EXCLUDED.updated_at;
