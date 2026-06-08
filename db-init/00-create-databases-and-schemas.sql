-- Create isolated databases per service/bounded context
CREATE DATABASE sdo_auth_db;
CREATE DATABASE sdo_users_db;
CREATE DATABASE sdo_courses_db;
CREATE DATABASE sdo_communication_db;
CREATE DATABASE sdo_analytics_db;
CREATE DATABASE sdo_notifications_db;

\connect sdo_auth_db;
CREATE SCHEMA IF NOT EXISTS auth;

\connect sdo_users_db;
CREATE SCHEMA IF NOT EXISTS users;

\connect sdo_courses_db;
CREATE SCHEMA IF NOT EXISTS courses;

\connect sdo_communication_db;
CREATE SCHEMA IF NOT EXISTS communication;

\connect sdo_analytics_db;
CREATE SCHEMA IF NOT EXISTS analytics;

\connect sdo_notifications_db;
CREATE SCHEMA IF NOT EXISTS notifications;
