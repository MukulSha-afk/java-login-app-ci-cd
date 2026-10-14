## Java Login App ##
Testing 

## Sample Java Login application uses "UserDB" database and Table schema to store the Employee Login details. ##

## How to see list of Databases ##
SHOW DATABASES;

## How to create Database ##

CREATE DATABASE UserDB;

## How to list Tables ##

USE UserDB;

SHOW TABLES;

# Database schema for UserDB (aligns with parameterized login/register code).
# Passwords must be BCrypt hashes (60 chars) - legacy plaintext rows still login once for migration.
CREATE DATABASE IF NOT EXISTS UserDB;
USE UserDB;

CREATE TABLE IF NOT EXISTS Employee (
  id int unsigned auto_increment not null,
  first_name varchar(250) NOT NULL,
  last_name varchar(250),
  email varchar(250),
  username varchar(250) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  regdate date,
  primary key (id)
);

-- Operational hardening (run once):
-- ALTER TABLE Employee MODIFY password varchar(255) NOT NULL;
-- CREATE UNIQUE INDEX ux_employee_username ON Employee(username);

## List Table data ##
SELECT * FROM Employee;

## Describe Table schema ##
DESCRIBE Employee;