CREATE SCHEMA IF NOT EXISTS communication;

CREATE TABLE IF NOT EXISTS communication.chat_room (
    id VARCHAR(255) PRIMARY KEY,
    course_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_room_course_unique
    ON communication.chat_room(course_id)
    WHERE course_id IS NOT NULL AND type = 'COURSE';

CREATE INDEX IF NOT EXISTS idx_chat_room_updated_at ON communication.chat_room(updated_at DESC);

CREATE TABLE IF NOT EXISTS communication.chat_participant (
    user_id VARCHAR(255) NOT NULL,
    room_id VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, room_id),
    CONSTRAINT fk_chat_participant_room FOREIGN KEY (room_id) REFERENCES communication.chat_room(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_participant_user ON communication.chat_participant(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participant_room ON communication.chat_participant(room_id);

CREATE TABLE IF NOT EXISTS communication.chat_message (
    id VARCHAR(255) PRIMARY KEY,
    room_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    "timestamp" TIMESTAMP WITH TIME ZONE,
    message_type VARCHAR(255) NOT NULL,
    attachments_json TEXT,
    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES communication.chat_room(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_message_room_time ON communication.chat_message(room_id, "timestamp" DESC);
CREATE INDEX IF NOT EXISTS idx_chat_message_user ON communication.chat_message(user_id);
