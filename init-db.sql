-- Создание схемы БД для системы корпоративного обучения
-- Разделение на схемы: auth и users

-- Создание схем
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS courses;
CREATE SCHEMA IF NOT EXISTS communication;

-- ============================================
-- СХЕМА USERS
-- ============================================

-- Создание таблицы users в схеме users (базовая версия)
CREATE TABLE IF NOT EXISTS users.users (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Таблица должностей
CREATE TABLE IF NOT EXISTS users.position (
    position_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    grade VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица отделов (без внешних ключей сначала, добавим позже)
CREATE TABLE IF NOT EXISTS users.department (
    department_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    manager_id VARCHAR(36),
    parent_department_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Расширение таблицы users
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS email VARCHAR(255) UNIQUE;
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS position_id VARCHAR(36);
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS department_id VARCHAR(36);
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS hire_date DATE;
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'active';
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users.users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Добавление внешних ключей для users
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_position') THEN
        ALTER TABLE users.users ADD CONSTRAINT fk_user_position FOREIGN KEY (position_id) REFERENCES users.position(position_id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_department') THEN
        ALTER TABLE users.users ADD CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES users.department(department_id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_department_manager') THEN
        ALTER TABLE users.department ADD CONSTRAINT fk_department_manager FOREIGN KEY (manager_id) REFERENCES users.users(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_department_parent') THEN
        ALTER TABLE users.department ADD CONSTRAINT fk_department_parent FOREIGN KEY (parent_department_id) REFERENCES users.department(department_id) ON DELETE SET NULL;
    END IF;
END $$;

-- Таблица настроек пользователя
CREATE TABLE IF NOT EXISTS users.user_settings (
    user_id VARCHAR(36) PRIMARY KEY,
    email_notifications BOOLEAN DEFAULT true,
    push_notifications BOOLEAN DEFAULT true,
    language VARCHAR(10) DEFAULT 'ru',
    timezone VARCHAR(50) DEFAULT 'Europe/Moscow',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

-- Таблица ролей пользователя
CREATE TABLE IF NOT EXISTS users.user_roles (
    user_id VARCHAR(36),
    role_name VARCHAR(50),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_name),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

-- ============================================
-- СХЕМА AUTH
-- ============================================

-- Таблица сессий аутентификации
CREATE TABLE IF NOT EXISTS auth.auth_session (
    session_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    jwt_token TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    ip_address VARCHAR(45),
    user_agent TEXT,
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

-- Индекс для быстрого поиска активных сессий
CREATE INDEX IF NOT EXISTS idx_auth_session_user_status ON auth.auth_session(user_id, status);
CREATE INDEX IF NOT EXISTS idx_auth_session_expires_at ON auth.auth_session(expires_at);

-- Таблица refresh токенов
CREATE TABLE IF NOT EXISTS auth.refresh_token (
    token_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT false,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

-- Индексы для refresh_token
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON auth.refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON auth.refresh_token(refresh_token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON auth.refresh_token(expires_at);

-- Таблица политик безопасности
CREATE TABLE IF NOT EXISTS auth.security_policy (
    policy_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    policy_name VARCHAR(255) NOT NULL UNIQUE,
    max_login_attempts INTEGER DEFAULT 5,
    password_min_length INTEGER DEFAULT 8,
    session_timeout_minutes INTEGER DEFAULT 1440,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица попыток входа
CREATE TABLE IF NOT EXISTS auth.login_attempt (
    attempt_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36),
    email VARCHAR(255),
    ip_address VARCHAR(45),
    success BOOLEAN NOT NULL,
    attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_attempt_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE SET NULL
);

-- Индексы для login_attempt
CREATE INDEX IF NOT EXISTS idx_login_attempt_user ON auth.login_attempt(user_id);
CREATE INDEX IF NOT EXISTS idx_login_attempt_email ON auth.login_attempt(email);
CREATE INDEX IF NOT EXISTS idx_login_attempt_ip ON auth.login_attempt(ip_address);
CREATE INDEX IF NOT EXISTS idx_login_attempt_time ON auth.login_attempt(attempt_time);

-- Вставка дефолтной политики безопасности
INSERT INTO auth.security_policy (policy_name, max_login_attempts, password_min_length, session_timeout_minutes)
VALUES ('default', 5, 8, 1440)
ON CONFLICT (policy_name) DO NOTHING;

-- Создание функции для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION users.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггеры для автоматического обновления updated_at
DROP TRIGGER IF EXISTS update_users_updated_at ON users.users;
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users.users
    FOR EACH ROW EXECUTE FUNCTION users.update_updated_at_column();

DROP TRIGGER IF EXISTS update_user_settings_updated_at ON users.user_settings;
CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON users.user_settings
    FOR EACH ROW EXECUTE FUNCTION users.update_updated_at_column();

-- Создание функции для очистки устаревших сессий
CREATE OR REPLACE FUNCTION auth.cleanup_expired_sessions()
RETURNS void AS $$
BEGIN
    DELETE FROM auth.auth_session WHERE expires_at < CURRENT_TIMESTAMP;
    DELETE FROM auth.refresh_token WHERE expires_at < CURRENT_TIMESTAMP OR revoked = true;
END;
$$ language 'plpgsql';

-- ============================================
-- СХЕМА COURSES
-- ============================================

-- Категории курсов
CREATE TABLE IF NOT EXISTS courses.category (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

-- Теги
CREATE TABLE IF NOT EXISTS courses.tag (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Курсы
CREATE TABLE IF NOT EXISTS courses.course (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id VARCHAR(36),
    difficulty VARCHAR(50),
    duration_minutes INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    allowed_roles_csv TEXT,
    allowed_department_ids_csv TEXT,
    specialization VARCHAR(255),
    instructions TEXT,
    aggregator_url TEXT,
    cover_url TEXT,
    company_cost NUMERIC(12,2),
    partner_name VARCHAR(255),
    partner_location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_category FOREIGN KEY (category_id) REFERENCES courses.category(id) ON DELETE SET NULL
);

-- Расширение таблицы courses.course
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS instructions TEXT;
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS aggregator_url TEXT;
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS cover_url TEXT;
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS company_cost NUMERIC(12,2);
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS partner_name VARCHAR(255);
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS partner_location VARCHAR(255);
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE courses.course ADD COLUMN IF NOT EXISTS end_date DATE;

-- Таблица связей курс-тег
CREATE TABLE IF NOT EXISTS courses.course_tag (
    course_id VARCHAR(36) NOT NULL,
    tag_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (course_id, tag_id),
    CONSTRAINT fk_course_tag_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_tag_tag FOREIGN KEY (tag_id) REFERENCES courses.tag(id) ON DELETE CASCADE
);

-- Учебные траектории
CREATE TABLE IF NOT EXISTS courses.learning_path (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    target_audience VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Связь траектория-курс
CREATE TABLE IF NOT EXISTS courses.learning_path_course (
    learning_path_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (learning_path_id, course_id),
    CONSTRAINT fk_lp_course_lp FOREIGN KEY (learning_path_id) REFERENCES courses.learning_path(id) ON DELETE CASCADE,
    CONSTRAINT fk_lp_course_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

-- Модули курса
CREATE TABLE IF NOT EXISTS courses.module (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    order_index INTEGER NOT NULL,
    CONSTRAINT fk_module_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

-- Уроки
CREATE TABLE IF NOT EXISTS courses.lesson (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    module_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    video_url TEXT,
    duration_minutes INTEGER,
    lesson_type VARCHAR(50),
    order_index INTEGER NOT NULL,
    CONSTRAINT fk_lesson_module FOREIGN KEY (module_id) REFERENCES courses.module(id) ON DELETE CASCADE
);

-- Назначения курсов сотрудникам
CREATE TABLE IF NOT EXISTS courses.course_assignment (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    assigned_by VARCHAR(36) NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_assignment_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_assignment_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_assignment_user ON courses.course_assignment(user_id);
CREATE INDEX IF NOT EXISTS idx_course_assignment_course ON courses.course_assignment(course_id);

-- Заявки на назначение курсов (модерация HR/Admin)
CREATE TABLE IF NOT EXISTS courses.course_assignment_request (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    requested_by VARCHAR(36) NOT NULL,
    comment TEXT,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by VARCHAR(36),
    reviewer_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    CONSTRAINT fk_course_assignment_request_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_assignment_request_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_assignment_request_user ON courses.course_assignment_request(user_id);
CREATE INDEX IF NOT EXISTS idx_course_assignment_request_status ON courses.course_assignment_request(status);
CREATE INDEX IF NOT EXISTS idx_course_assignment_request_created_at ON courses.course_assignment_request(created_at DESC);

-- Политика лимитов назначения курсов
CREATE TABLE IF NOT EXISTS courses.assignment_policy (
    id INTEGER PRIMARY KEY,
    max_courses_per_quarter INTEGER NOT NULL DEFAULT 3,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO courses.assignment_policy (id, max_courses_per_quarter)
VALUES (1, 3)
ON CONFLICT (id) DO NOTHING;

-- Прогресс по урокам
CREATE TABLE IF NOT EXISTS courses.user_progress (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    lesson_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    progress_percentage INTEGER NOT NULL DEFAULT 0,
    time_spent_seconds BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_progress_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_progress_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_progress_lesson FOREIGN KEY (lesson_id) REFERENCES courses.lesson(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_progress_user ON courses.user_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_user_progress_course ON courses.user_progress(course_id);

-- Записи на курсы (enrollments)
CREATE TABLE IF NOT EXISTS courses.enrollment (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completion_date TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_enrollment_user ON courses.enrollment(user_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_course ON courses.enrollment(course_id);

-- Сертификаты
CREATE TABLE IF NOT EXISTS courses.certificate (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    certificate_url TEXT NOT NULL,
    pdf_bytes BYTEA NOT NULL,
    hash VARCHAR(128) NOT NULL UNIQUE,
    CONSTRAINT fk_certificate_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_certificate_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_certificate_user ON courses.certificate(user_id);

-- История обучения
CREATE TABLE IF NOT EXISTS courses.learning_history (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    action VARCHAR(100) NOT NULL,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    CONSTRAINT fk_learning_history_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_learning_history_user_time ON courses.learning_history(user_id, "timestamp");

-- ============================================
-- СХЕМА COMMUNICATION
-- ============================================

-- Чат-комнаты
CREATE TABLE IF NOT EXISTS communication.chat_room (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    course_id VARCHAR(36),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_room_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE SET NULL
);

-- Для course-room допускаем только одну комнату на курс
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_room_course_unique
    ON communication.chat_room(course_id)
    WHERE course_id IS NOT NULL AND type = 'COURSE';

CREATE INDEX IF NOT EXISTS idx_chat_room_updated_at ON communication.chat_room(updated_at DESC);

-- Участники чата
CREATE TABLE IF NOT EXISTS communication.chat_participant (
    user_id VARCHAR(36) NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(30) NOT NULL DEFAULT 'PARTICIPANT',
    PRIMARY KEY (user_id, room_id),
    CONSTRAINT fk_chat_participant_room FOREIGN KEY (room_id) REFERENCES communication.chat_room(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_participant_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_participant_user ON communication.chat_participant(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participant_room ON communication.chat_participant(room_id);

-- Сообщения
CREATE TABLE IF NOT EXISTS communication.chat_message (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    room_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    attachments_json TEXT,
    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES communication.chat_room(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_message_room_time ON communication.chat_message(room_id, "timestamp" DESC);
