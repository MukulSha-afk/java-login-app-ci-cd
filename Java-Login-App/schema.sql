-- Schema for the Java Login App (MySQL 8).
--
-- Initialization (safe and repeatable - nothing runs automatically):
--   * Fresh Docker installs: docker-compose mounts this file into
--     /docker-entrypoint-initdb.d so MySQL runs it once on first start.
--   * Existing databases: this file is never executed by the application
--     (spring.sql.init.mode=never). CREATE TABLE IF NOT EXISTS never alters an
--     existing table, so re-running it is a no-op for current data.
--
-- Passwords are BCrypt hashes (never plaintext).

CREATE DATABASE IF NOT EXISTS UserDB;
USE UserDB;

CREATE TABLE IF NOT EXISTS Employee (
  id INT UNSIGNED AUTO_INCREMENT NOT NULL,
  first_name VARCHAR(250) NOT NULL,
  last_name VARCHAR(250),
  email VARCHAR(250),
  username VARCHAR(250) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt hash from register flow',
  regdate DATE,
  PRIMARY KEY (id)
);

-- Unique username + email are enforced at the database level (username above,
-- email below) so concurrent duplicate registrations cannot slip through.
-- NOTE: for an EXISTING database created before this constraint, MySQL's
-- CREATE TABLE IF NOT EXISTS will not add it. Run once manually (after
-- de-duplicating addresses), it is deliberately not automatic:
--   ALTER TABLE Employee ADD UNIQUE KEY uq_employee_email (email);
ALTER TABLE Employee ADD UNIQUE KEY uq_employee_email (email);
