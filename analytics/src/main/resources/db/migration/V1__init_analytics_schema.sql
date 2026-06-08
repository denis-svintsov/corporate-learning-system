CREATE SCHEMA IF NOT EXISTS analytics;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS analytics.course_stat (
    course_id VARCHAR(255) PRIMARY KEY,
    assignments BIGINT NOT NULL DEFAULT 0,
    completions BIGINT NOT NULL DEFAULT 0,
    lessons_completed BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS analytics.user_activity (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    activity_date DATE NOT NULL,
    lessons_completed BIGINT NOT NULL DEFAULT 0,
    courses_completed BIGINT NOT NULL DEFAULT 0,
    assignments_received BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_user_activity_day UNIQUE (user_id, activity_date)
);

CREATE INDEX IF NOT EXISTS idx_user_activity_user_date
    ON analytics.user_activity(user_id, activity_date DESC);

CREATE TABLE IF NOT EXISTS analytics.learning_report (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    report_type VARCHAR(100) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    generated_by VARCHAR(255),
    format VARCHAR(20) NOT NULL,
    payload_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_learning_report_generated_at
    ON analytics.learning_report(generated_at DESC);
