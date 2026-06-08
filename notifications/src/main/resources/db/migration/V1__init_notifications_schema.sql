CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE IF NOT EXISTS notifications.notification (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(80) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    source_service VARCHAR(80),
    source_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_notification_user_created
    ON notifications.notification(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_user_status
    ON notifications.notification(user_id, status);

CREATE TABLE IF NOT EXISTS notifications.notification_delivery_stat (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    type VARCHAR(80) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    sent_count BIGINT NOT NULL DEFAULT 0,
    read_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_notification_delivery_stat UNIQUE (type, channel)
);
