CREATE SCHEMA IF NOT EXISTS auth;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS auth.users (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    position_id VARCHAR(255),
    department_id VARCHAR(255),
    hire_date DATE,
    status VARCHAR(255) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth.auth_session (
    session_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    jwt_token TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    ip_address VARCHAR(45),
    user_agent TEXT,
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_session_user_status ON auth.auth_session(user_id, status);
CREATE INDEX IF NOT EXISTS idx_auth_session_expires_at ON auth.auth_session(expires_at);

CREATE TABLE IF NOT EXISTS auth.refresh_token (
    token_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON auth.refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON auth.refresh_token(expires_at);

CREATE TABLE IF NOT EXISTS auth.security_policy (
    policy_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    policy_name VARCHAR(255) NOT NULL UNIQUE,
    max_login_attempts INTEGER DEFAULT 5,
    password_min_length INTEGER DEFAULT 8,
    session_timeout_minutes INTEGER DEFAULT 1440,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO auth.security_policy (policy_name, max_login_attempts, password_min_length, session_timeout_minutes)
VALUES ('default', 5, 8, 1440)
ON CONFLICT (policy_name) DO NOTHING;

CREATE TABLE IF NOT EXISTS auth.login_attempt (
    attempt_id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36),
    email VARCHAR(255),
    ip_address VARCHAR(45),
    success BOOLEAN NOT NULL,
    attempt_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_attempt_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_login_attempt_user ON auth.login_attempt(user_id);
CREATE INDEX IF NOT EXISTS idx_login_attempt_email ON auth.login_attempt(email);
CREATE INDEX IF NOT EXISTS idx_login_attempt_ip ON auth.login_attempt(ip_address);
CREATE INDEX IF NOT EXISTS idx_login_attempt_time ON auth.login_attempt(attempt_time);
