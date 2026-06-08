INSERT INTO users.position (position_id, title, description, grade)
VALUES
    (
        '10000000-0000-4000-8000-000000000001',
        'Оператор производственной линии',
        'Участник обучения, выбирает и проходит назначенные курсы',
        'employee'
    ),
    (
        '10000000-0000-4000-8000-000000000002',
        'Администратор системы обучения',
        'Управляет заявками, пользователями и параметрами обучения',
        'admin'
    ),
    (
        '10000000-0000-4000-8000-000000000003',
        'HR-менеджер',
        'Назначает обучение сотрудникам и контролирует заявки',
        'hr'
    ),
    (
        '10000000-0000-4000-8000-000000000004',
        'Технолог производства',
        'Создает и редактирует учебные курсы',
        'technolog'
    ),
    (
        '10000000-0000-4000-8000-000000000005',
        'Эксперт по промышленной безопасности',
        'Ведет курсы и сопровождает участников обучения',
        'expert'
    ),
    (
        '10000000-0000-4000-8000-000000000006',
        'Руководитель производственного участка',
        'Контролирует обучение сотрудников подразделения',
        'manager'
    )
ON CONFLICT (position_id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    grade = EXCLUDED.grade;

INSERT INTO users.department (department_id, name, description, manager_id, parent_department_id)
VALUES (
    '20000000-0000-4000-8000-000000000001',
    'Производственный участок',
    'Основное производственное подразделение организации',
    '44444444-4444-4444-8444-444444444444',
    NULL
)
ON CONFLICT (department_id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    manager_id = EXCLUDED.manager_id,
    parent_department_id = EXCLUDED.parent_department_id;

INSERT INTO users.users (
    id,
    username,
    email,
    first_name,
    middle_name,
    last_name,
    position_id,
    department_id,
    hire_date,
    status
)
VALUES
    (
        '0268bbe0-0aee-419e-9765-10ef5f25ddd9',
        'denis',
        'denis@company.local',
        'Денис',
        'Дмитриевич',
        'Свинцов',
        '10000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000001',
        DATE '2024-09-01',
        'active'
    ),
    (
        '11111111-1111-4111-8111-111111111111',
        'ivan',
        'ivan@company.local',
        'Иван',
        'Александрович',
        'Петров',
        '10000000-0000-4000-8000-000000000002',
        '20000000-0000-4000-8000-000000000001',
        DATE '2022-03-15',
        'active'
    ),
    (
        '22222222-2222-4222-8222-222222222222',
        'hr',
        'hr@company.local',
        'Мария',
        'Сергеевна',
        'Кузнецова',
        '10000000-0000-4000-8000-000000000003',
        '20000000-0000-4000-8000-000000000001',
        DATE '2023-02-10',
        'active'
    ),
    (
        '33333333-3333-4333-8333-333333333333',
        'technolog',
        'technolog@company.local',
        'Алексей',
        'Викторович',
        'Орлов',
        '10000000-0000-4000-8000-000000000004',
        '20000000-0000-4000-8000-000000000001',
        DATE '2021-11-20',
        'active'
    ),
    (
        '44444444-4444-4444-8444-444444444444',
        'manager',
        'manager@company.local',
        'Сергей',
        'Николаевич',
        'Морозов',
        '10000000-0000-4000-8000-000000000006',
        '20000000-0000-4000-8000-000000000001',
        DATE '2020-05-18',
        'active'
    ),
    (
        '55555555-5555-4555-8555-555555555555',
        'expert',
        'expert@company.local',
        'Наталья',
        'Павловна',
        'Соколова',
        '10000000-0000-4000-8000-000000000005',
        '20000000-0000-4000-8000-000000000001',
        DATE '2022-09-12',
        'active'
    ),
    (
        '66666666-6666-4666-8666-666666666666',
        'student1',
        'student1@company.local',
        'Анна',
        'Игоревна',
        'Васильева',
        '10000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000001',
        DATE '2024-10-01',
        'active'
    ),
    (
        '77777777-7777-4777-8777-777777777777',
        'student2',
        'student2@company.local',
        'Павел',
        'Денисович',
        'Никитин',
        '10000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000001',
        DATE '2024-11-05',
        'active'
    ),
    (
        '88888888-8888-4888-8888-888888888888',
        'student3',
        'student3@company.local',
        'Елена',
        'Андреевна',
        'Федорова',
        '10000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000001',
        DATE '2025-01-15',
        'active'
    ),
    (
        '99999999-9999-4999-8999-999999999999',
        'student4',
        'student4@company.local',
        'Илья',
        'Олегович',
        'Ковалев',
        '10000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000001',
        DATE '2025-02-03',
        'active'
    ),
    (
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
        'student5',
        'student5@company.local',
        'Ольга',
        'Михайловна',
        'Смирнова',
        '10000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000001',
        DATE '2025-03-12',
        'active'
    )
ON CONFLICT (id) DO UPDATE
SET username = EXCLUDED.username,
    email = EXCLUDED.email,
    first_name = EXCLUDED.first_name,
    middle_name = EXCLUDED.middle_name,
    last_name = EXCLUDED.last_name,
    position_id = EXCLUDED.position_id,
    department_id = EXCLUDED.department_id,
    hire_date = EXCLUDED.hire_date,
    status = EXCLUDED.status;

DELETE FROM users.user_roles
WHERE user_id IN (
    '0268bbe0-0aee-419e-9765-10ef5f25ddd9',
    '11111111-1111-4111-8111-111111111111',
    '22222222-2222-4222-8222-222222222222',
    '33333333-3333-4333-8333-333333333333',
    '44444444-4444-4444-8444-444444444444',
    '55555555-5555-4555-8555-555555555555',
    '66666666-6666-4666-8666-666666666666',
    '77777777-7777-4777-8777-777777777777',
    '88888888-8888-4888-8888-888888888888',
    '99999999-9999-4999-8999-999999999999',
    'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'
);

INSERT INTO users.user_roles (user_id, role_name)
VALUES
    ('0268bbe0-0aee-419e-9765-10ef5f25ddd9', 'USER'),
    ('11111111-1111-4111-8111-111111111111', 'USER'),
    ('11111111-1111-4111-8111-111111111111', 'ADMIN'),
    ('22222222-2222-4222-8222-222222222222', 'HR'),
    ('33333333-3333-4333-8333-333333333333', 'TECHNOLOG'),
    ('44444444-4444-4444-8444-444444444444', 'MANAGER'),
    ('55555555-5555-4555-8555-555555555555', 'EXPERT'),
    ('66666666-6666-4666-8666-666666666666', 'USER'),
    ('77777777-7777-4777-8777-777777777777', 'USER'),
    ('88888888-8888-4888-8888-888888888888', 'USER'),
    ('99999999-9999-4999-8999-999999999999', 'USER'),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'USER');

INSERT INTO users.user_settings (
    user_id,
    email_notifications,
    push_notifications,
    language,
    timezone
)
VALUES
    ('0268bbe0-0aee-419e-9765-10ef5f25ddd9', true, true, 'ru', 'Europe/Saratov'),
    ('11111111-1111-4111-8111-111111111111', true, false, 'ru', 'Europe/Saratov'),
    ('22222222-2222-4222-8222-222222222222', true, true, 'ru', 'Europe/Saratov'),
    ('33333333-3333-4333-8333-333333333333', true, true, 'ru', 'Europe/Saratov'),
    ('44444444-4444-4444-8444-444444444444', true, true, 'ru', 'Europe/Saratov'),
    ('55555555-5555-4555-8555-555555555555', true, true, 'ru', 'Europe/Saratov'),
    ('66666666-6666-4666-8666-666666666666', true, true, 'ru', 'Europe/Saratov'),
    ('77777777-7777-4777-8777-777777777777', true, true, 'ru', 'Europe/Saratov'),
    ('88888888-8888-4888-8888-888888888888', true, true, 'ru', 'Europe/Saratov'),
    ('99999999-9999-4999-8999-999999999999', true, true, 'ru', 'Europe/Saratov'),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', true, true, 'ru', 'Europe/Saratov')
ON CONFLICT (user_id) DO UPDATE
SET email_notifications = EXCLUDED.email_notifications,
    push_notifications = EXCLUDED.push_notifications,
    language = EXCLUDED.language,
    timezone = EXCLUDED.timezone;
