INSERT INTO courses.category (id, name, description)
VALUES
    ('21000000-0000-4000-8000-000000000001', 'Безопасность', 'Обязательные курсы по безопасности'),
    ('21000000-0000-4000-8000-000000000002', 'Производство', 'Курсы для производственных подразделений организации'),
    ('21000000-0000-4000-8000-000000000003', 'Цифровые навыки', 'Курсы по цифровым инструментам')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO courses.tag (id, name)
VALUES
    ('22000000-0000-4000-8000-000000000001', 'обязательный'),
    ('22000000-0000-4000-8000-000000000002', 'онлайн'),
    ('22000000-0000-4000-8000-000000000003', 'практика')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name;

INSERT INTO courses.course (
    id,
    title,
    description,
    category_id,
    difficulty,
    duration_minutes,
    status,
    allowed_roles_csv,
    allowed_department_ids_csv,
    specialization,
    instructions,
    aggregator_url,
    cover_url,
    company_cost,
    partner_name,
    partner_location,
    start_date,
    end_date,
    created_at,
    updated_at
)
VALUES
    (
        '30000000-0000-4000-8000-000000000001',
        'Охрана труда: практический поток',
        'Назначенный курс для сотрудников производственного участка с ближайшей датой старта потока.',
        '21000000-0000-4000-8000-000000000001',
        'BEGINNER',
        180,
        'ACTIVE',
        'USER',
        '20000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000001',
        'Изучите материалы, выполните итоговую проверку и задайте вопрос эксперту в чате курса.',
        'https://learning.company.local/courses/occupational-safety',
        NULL,
        15000.00,
        'Корпоративный учебный центр',
        'Онлайн',
        CURRENT_DATE + 1,
        CURRENT_DATE + 3,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '30000000-0000-4000-8000-000000000002',
        'Цифровые инструменты производственного сотрудника',
        'Курс по электронным заявкам, журналам смены и внутренним цифровым сервисам предприятия.',
        '21000000-0000-4000-8000-000000000003',
        'INTERMEDIATE',
        240,
        'ACTIVE',
        'USER',
        '20000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000001',
        'Пройдите следующий урок и проверьте обновление прогресса в личном кабинете.',
        'https://learning.company.local/courses/digital-tools',
        NULL,
        22000.00,
        'Digital Academy',
        'Онлайн',
        CURRENT_DATE - 1,
        CURRENT_DATE + 2,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '30000000-0000-4000-8000-000000000003',
        'Вводный курс по промышленной безопасности',
        'Завершенный архивный курс для демонстрации истории обучения сотрудника.',
        '21000000-0000-4000-8000-000000000001',
        'BEGINNER',
        120,
        'ARCHIVED',
        'USER',
        '20000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000001',
        'Курс завершен, запись хранится в истории обучения.',
        'https://learning.company.local/courses/industrial-safety',
        NULL,
        12000.00,
        'Safety Expert',
        'Онлайн',
        CURRENT_DATE - 40,
        CURRENT_DATE - 35,
        CURRENT_TIMESTAMP - INTERVAL '45 days',
        CURRENT_TIMESTAMP - INTERVAL '35 days'
    ),
    (
        '30000000-0000-4000-8000-000000000004',
        'Эксплуатация оборудования: архивный поток',
        'Архивный курс с истекшими сроками для вкладки архива назначенных курсов.',
        '21000000-0000-4000-8000-000000000002',
        'ADVANCED',
        300,
        'ARCHIVED',
        'USER',
        '20000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000001',
        'Поток завершен, новые участники не подключаются.',
        'https://learning.company.local/courses/equipment-operation',
        NULL,
        28000.00,
        'ПромТех Обучение',
        'Учебный класс',
        CURRENT_DATE - 20,
        CURRENT_DATE - 15,
        CURRENT_TIMESTAMP - INTERVAL '25 days',
        CURRENT_TIMESTAMP - INTERVAL '15 days'
    ),
    (
        '30000000-0000-4000-8000-000000000005',
        'Бережливое производство для смены',
        'Доступный курс для самостоятельного выбора сотрудником. После выбора заявка попадет на согласование HR.',
        '21000000-0000-4000-8000-000000000002',
        'INTERMEDIATE',
        210,
        'ACTIVE',
        'USER',
        '20000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000001',
        'Выберите курс в разделе "Мой выбор", дождитесь согласования и приступайте к обучению.',
        'https://learning.company.local/courses/lean-production',
        NULL,
        18000.00,
        'Lean Lab',
        'Онлайн',
        CURRENT_DATE + 7,
        CURRENT_DATE + 9,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    category_id = EXCLUDED.category_id,
    difficulty = EXCLUDED.difficulty,
    duration_minutes = EXCLUDED.duration_minutes,
    status = EXCLUDED.status,
    allowed_roles_csv = EXCLUDED.allowed_roles_csv,
    allowed_department_ids_csv = EXCLUDED.allowed_department_ids_csv,
    specialization = EXCLUDED.specialization,
    instructions = EXCLUDED.instructions,
    aggregator_url = EXCLUDED.aggregator_url,
    cover_url = EXCLUDED.cover_url,
    company_cost = EXCLUDED.company_cost,
    partner_name = EXCLUDED.partner_name,
    partner_location = EXCLUDED.partner_location,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO courses.course_tag (course_id, tag_id)
VALUES
    ('30000000-0000-4000-8000-000000000001', '22000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000001', '22000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000002', '22000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000002', '22000000-0000-4000-8000-000000000003'),
    ('30000000-0000-4000-8000-000000000003', '22000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000005', '22000000-0000-4000-8000-000000000003')
ON CONFLICT (course_id, tag_id) DO NOTHING;

INSERT INTO courses.module (id, course_id, title, order_index)
VALUES
    ('31000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', 'Основы охраны труда', 1),
    ('31000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000002', 'Цифровая смена', 1),
    ('31000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000003', 'Промышленная безопасность', 1),
    ('31000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000004', 'Оборудование', 1),
    ('31000000-0000-4000-8000-000000000005', '30000000-0000-4000-8000-000000000005', 'Бережливые практики', 1)
ON CONFLICT (id) DO UPDATE
SET course_id = EXCLUDED.course_id,
    title = EXCLUDED.title,
    order_index = EXCLUDED.order_index;

INSERT INTO courses.lesson (id, module_id, title, content, video_url, duration_minutes, lesson_type, order_index)
VALUES
    ('32000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000001', 'Вводный инструктаж', 'Ключевые правила безопасной работы.', NULL, 45, 'THEORY', 1),
    ('32000000-0000-4000-8000-000000000002', '31000000-0000-4000-8000-000000000001', 'Практические ситуации', 'Разбор типовых рисков на рабочем месте.', NULL, 75, 'PRACTICE', 2),
    ('32000000-0000-4000-8000-000000000003', '31000000-0000-4000-8000-000000000001', 'Итоговая проверка', 'Контрольные вопросы по курсу.', NULL, 60, 'QUIZ', 3),
    ('32000000-0000-4000-8000-000000000004', '31000000-0000-4000-8000-000000000002', 'Работа с заявками', 'Как использовать цифровые инструменты смены.', NULL, 80, 'THEORY', 1),
    ('32000000-0000-4000-8000-000000000005', '31000000-0000-4000-8000-000000000002', 'Отчеты и контроль', 'Практика по заполнению электронных форм.', NULL, 80, 'PRACTICE', 2),
    ('32000000-0000-4000-8000-000000000006', '31000000-0000-4000-8000-000000000002', 'Итоговое задание', 'Закрепление материала.', NULL, 80, 'QUIZ', 3),
    ('32000000-0000-4000-8000-000000000007', '31000000-0000-4000-8000-000000000003', 'Требования безопасности', 'Базовые требования промышленной безопасности.', NULL, 60, 'THEORY', 1),
    ('32000000-0000-4000-8000-000000000008', '31000000-0000-4000-8000-000000000003', 'Проверка знаний', 'Итоговый тест.', NULL, 60, 'QUIZ', 2),
    ('32000000-0000-4000-8000-000000000009', '31000000-0000-4000-8000-000000000004', 'Архивный материал', 'Материал завершенного потока.', NULL, 120, 'THEORY', 1),
    ('32000000-0000-4000-8000-000000000010', '31000000-0000-4000-8000-000000000005', 'Потери и улучшения', 'Как находить потери в процессе.', NULL, 70, 'THEORY', 1),
    ('32000000-0000-4000-8000-000000000011', '31000000-0000-4000-8000-000000000005', 'Практика 5S', 'Практическое задание по 5S.', NULL, 70, 'PRACTICE', 2),
    ('32000000-0000-4000-8000-000000000012', '31000000-0000-4000-8000-000000000005', 'Итоговая проверка', 'Проверка понимания бережливых практик.', NULL, 70, 'QUIZ', 3)
ON CONFLICT (id) DO UPDATE
SET module_id = EXCLUDED.module_id,
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    video_url = EXCLUDED.video_url,
    duration_minutes = EXCLUDED.duration_minutes,
    lesson_type = EXCLUDED.lesson_type,
    order_index = EXCLUDED.order_index;

INSERT INTO courses.course_assignment (
    id,
    user_id,
    course_id,
    assigned_by,
    due_date,
    status,
    created_at
)
VALUES
    ('40000000-0000-4000-8000-000000000001', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 3, 'ASSIGNED', CURRENT_TIMESTAMP),
    ('40000000-0000-4000-8000-000000000002', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ('40000000-0000-4000-8000-000000000003', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000003', '11111111-1111-4111-8111-111111111111', CURRENT_DATE - 35, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '40 days'),
    ('40000000-0000-4000-8000-000000000004', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000004', '11111111-1111-4111-8111-111111111111', CURRENT_DATE - 15, 'OVERDUE', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    ('40000000-0000-4000-8000-000000000005', '66666666-6666-4666-8666-666666666666', '30000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 3, 'ASSIGNED', CURRENT_TIMESTAMP),
    ('40000000-0000-4000-8000-000000000006', '77777777-7777-4777-8777-777777777777', '30000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 3, 'ASSIGNED', CURRENT_TIMESTAMP),
    ('40000000-0000-4000-8000-000000000007', '88888888-8888-4888-8888-888888888888', '30000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 3, 'ASSIGNED', CURRENT_TIMESTAMP),
    ('40000000-0000-4000-8000-000000000008', '99999999-9999-4999-8999-999999999999', '30000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 3, 'ASSIGNED', CURRENT_TIMESTAMP),
    ('40000000-0000-4000-8000-000000000009', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', '30000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 3, 'ASSIGNED', CURRENT_TIMESTAMP),
    ('40000000-0000-4000-8000-000000000010', '66666666-6666-4666-8666-666666666666', '30000000-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ('40000000-0000-4000-8000-000000000011', '77777777-7777-4777-8777-777777777777', '30000000-0000-4000-8000-000000000002', '11111111-1111-4111-8111-111111111111', CURRENT_DATE + 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    course_id = EXCLUDED.course_id,
    assigned_by = EXCLUDED.assigned_by,
    due_date = EXCLUDED.due_date,
    status = EXCLUDED.status,
    created_at = EXCLUDED.created_at;

INSERT INTO courses.enrollment (
    id,
    user_id,
    course_id,
    enrollment_date,
    completion_date,
    status
)
VALUES
    ('41000000-0000-4000-8000-000000000001', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000002', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000002', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000003', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000003', CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '35 days', 'COMPLETED'),
    ('41000000-0000-4000-8000-000000000004', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000004', CURRENT_TIMESTAMP - INTERVAL '20 days', NULL, 'CANCELLED'),
    ('41000000-0000-4000-8000-000000000005', '66666666-6666-4666-8666-666666666666', '30000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000006', '77777777-7777-4777-8777-777777777777', '30000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000007', '88888888-8888-4888-8888-888888888888', '30000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000008', '99999999-9999-4999-8999-999999999999', '30000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000009', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', '30000000-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000010', '66666666-6666-4666-8666-666666666666', '30000000-0000-4000-8000-000000000002', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'ACTIVE'),
    ('41000000-0000-4000-8000-000000000011', '77777777-7777-4777-8777-777777777777', '30000000-0000-4000-8000-000000000002', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'ACTIVE')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    course_id = EXCLUDED.course_id,
    enrollment_date = EXCLUDED.enrollment_date,
    completion_date = EXCLUDED.completion_date,
    status = EXCLUDED.status;

INSERT INTO courses.user_progress (
    id,
    user_id,
    course_id,
    lesson_id,
    status,
    progress_percentage,
    time_spent_seconds,
    updated_at
)
VALUES
    ('42000000-0000-4000-8000-000000000001', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000002', '32000000-0000-4000-8000-000000000004', 'COMPLETED', 100, 2400, CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    ('42000000-0000-4000-8000-000000000002', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000003', '32000000-0000-4000-8000-000000000007', 'COMPLETED', 100, 3600, CURRENT_TIMESTAMP - INTERVAL '36 days'),
    ('42000000-0000-4000-8000-000000000003', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', '30000000-0000-4000-8000-000000000003', '32000000-0000-4000-8000-000000000008', 'COMPLETED', 100, 1800, CURRENT_TIMESTAMP - INTERVAL '35 days')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    course_id = EXCLUDED.course_id,
    lesson_id = EXCLUDED.lesson_id,
    status = EXCLUDED.status,
    progress_percentage = EXCLUDED.progress_percentage,
    time_spent_seconds = EXCLUDED.time_spent_seconds,
    updated_at = EXCLUDED.updated_at;

INSERT INTO courses.learning_history (
    id,
    user_id,
    action,
    "timestamp",
    details
)
VALUES
    ('44000000-0000-4000-8000-000000000001', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', 'COURSE_ASSIGNED', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Назначен курс "Цифровые инструменты производственного сотрудника"'),
    ('44000000-0000-4000-8000-000000000002', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', 'LESSON_COMPLETED', CURRENT_TIMESTAMP - INTERVAL '12 hours', 'Завершен урок "Работа с заявками"'),
    ('44000000-0000-4000-8000-000000000003', '0268bbe0-0aee-419e-9765-10ef5f25ddd9', 'COURSE_COMPLETED', CURRENT_TIMESTAMP - INTERVAL '35 days', 'Завершен курс "Вводный курс по промышленной безопасности"')
ON CONFLICT (id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    action = EXCLUDED.action,
    "timestamp" = EXCLUDED."timestamp",
    details = EXCLUDED.details;

UPDATE courses.assignment_policy
SET max_courses_per_quarter = 4,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;
