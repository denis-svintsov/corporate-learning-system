CREATE SCHEMA IF NOT EXISTS users;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users.position (
    position_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    grade VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS users.department (
    department_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    manager_id VARCHAR(36),
    parent_department_id VARCHAR(36),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_department_id) REFERENCES users.department(department_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS users.users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255),
    email VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    position_id VARCHAR(36),
    department_id VARCHAR(36),
    hire_date DATE,
    status VARCHAR(255),
    CONSTRAINT fk_user_position FOREIGN KEY (position_id) REFERENCES users.position(position_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES users.department(department_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_users_department ON users.users(department_id);
CREATE INDEX IF NOT EXISTS idx_users_position ON users.users(position_id);
CREATE INDEX IF NOT EXISTS idx_users_status ON users.users(status);

CREATE TABLE IF NOT EXISTS users.user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON users.user_roles(user_id);

CREATE TABLE IF NOT EXISTS users.user_settings (
    user_id VARCHAR(36) PRIMARY KEY,
    email_notifications BOOLEAN,
    push_notifications BOOLEAN,
    language VARCHAR(255),
    timezone VARCHAR(255),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);
