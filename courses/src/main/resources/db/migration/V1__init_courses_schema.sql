CREATE SCHEMA IF NOT EXISTS courses;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS courses.category (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS courses.tag (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS courses.course (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id VARCHAR(36),
    difficulty VARCHAR(255),
    duration_minutes INTEGER,
    status VARCHAR(255) NOT NULL DEFAULT 'DRAFT',
    allowed_roles_csv VARCHAR(255),
    allowed_department_ids_csv VARCHAR(255),
    specialization VARCHAR(255),
    instructions TEXT,
    aggregator_url VARCHAR(255),
    cover_url VARCHAR(255),
    company_cost NUMERIC(38, 2),
    partner_name VARCHAR(255),
    partner_location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_course_category FOREIGN KEY (category_id) REFERENCES courses.category(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS courses.course_tag (
    course_id VARCHAR(36) NOT NULL,
    tag_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (course_id, tag_id),
    CONSTRAINT fk_course_tag_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_tag_tag FOREIGN KEY (tag_id) REFERENCES courses.tag(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS courses.learning_path (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    target_audience VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS courses.learning_path_course (
    learning_path_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (learning_path_id, course_id),
    CONSTRAINT fk_lp_course_lp FOREIGN KEY (learning_path_id) REFERENCES courses.learning_path(id) ON DELETE CASCADE,
    CONSTRAINT fk_lp_course_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS courses.module (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    order_index INTEGER NOT NULL,
    CONSTRAINT fk_module_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS courses.lesson (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    module_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    video_url VARCHAR(255),
    duration_minutes INTEGER,
    lesson_type VARCHAR(255),
    order_index INTEGER NOT NULL,
    CONSTRAINT fk_lesson_module FOREIGN KEY (module_id) REFERENCES courses.module(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS courses.course_assignment (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    assigned_by VARCHAR(255) NOT NULL,
    due_date DATE,
    status VARCHAR(255) NOT NULL DEFAULT 'ASSIGNED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_course_assignment_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_assignment_user ON courses.course_assignment(user_id);
CREATE INDEX IF NOT EXISTS idx_course_assignment_course ON courses.course_assignment(course_id);

CREATE TABLE IF NOT EXISTS courses.course_assignment_request (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    requested_by VARCHAR(255) NOT NULL,
    comment VARCHAR(255),
    due_date DATE,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    reviewed_by VARCHAR(255),
    reviewer_comment VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_course_assignment_request_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_assignment_request_user ON courses.course_assignment_request(user_id);
CREATE INDEX IF NOT EXISTS idx_course_assignment_request_status ON courses.course_assignment_request(status);
CREATE INDEX IF NOT EXISTS idx_course_assignment_request_created_at ON courses.course_assignment_request(created_at DESC);

CREATE TABLE IF NOT EXISTS courses.assignment_policy (
    id INTEGER PRIMARY KEY,
    max_courses_per_quarter INTEGER NOT NULL DEFAULT 3,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO courses.assignment_policy (id, max_courses_per_quarter, updated_at)
VALUES (1, 3, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS courses.enrollment (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    enrollment_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_enrollment_user ON courses.enrollment(user_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_course ON courses.enrollment(course_id);

CREATE TABLE IF NOT EXISTS courses.user_progress (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    lesson_id VARCHAR(36) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'NOT_STARTED',
    progress_percentage INTEGER NOT NULL DEFAULT 0,
    time_spent_seconds BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_progress_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_progress_lesson FOREIGN KEY (lesson_id) REFERENCES courses.lesson(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_progress_user ON courses.user_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_user_progress_course ON courses.user_progress(course_id);

CREATE TABLE IF NOT EXISTS courses.certificate (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    issue_date TIMESTAMP WITH TIME ZONE NOT NULL,
    certificate_url VARCHAR(255) NOT NULL,
    pdf_bytes BYTEA NOT NULL,
    hash VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT fk_certificate_course FOREIGN KEY (course_id) REFERENCES courses.course(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_certificate_user ON courses.certificate(user_id);

CREATE TABLE IF NOT EXISTS courses.learning_history (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    "timestamp" TIMESTAMP WITH TIME ZONE NOT NULL,
    details TEXT
);

CREATE INDEX IF NOT EXISTS idx_learning_history_user_time ON courses.learning_history(user_id, "timestamp");
