-- Test-only DDL for the isolated H2 database (MODE=MySQL).
-- Mirrors src/main/schema.sql for the columns the application uses, including
-- the unique constraints that guard against duplicate registrations.
-- This file is only on the test classpath; production MySQL never sees it.
CREATE TABLE IF NOT EXISTS Employee (
  id INT AUTO_INCREMENT NOT NULL,
  first_name VARCHAR(250) NOT NULL,
  last_name VARCHAR(250),
  email VARCHAR(250),
  username VARCHAR(250) NOT NULL,
  password VARCHAR(255) NOT NULL,
  regdate DATE,
  PRIMARY KEY (id),
  CONSTRAINT uq_employee_username UNIQUE (username),
  CONSTRAINT uq_employee_email UNIQUE (email)
);
