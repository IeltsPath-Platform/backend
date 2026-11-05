-- recreate_databases.sql
-- WARNING: This will terminate active connections and drop the listed databases.
-- Run with psql against the postgres superuser on the target host/port.
-- Example (PowerShell):
--   $env:PGPASSWORD='your_password'; psql -h localhost -p 5440 -U postgres -d postgres -f recreate_databases.sql

-- Step 1: Terminate active connections (PostgreSQL 13+ required)
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname IN ('access_db', 'assessment_db', 'content_db', 'learning_db', 'user_db')
  AND pid <> pg_backend_pid();

-- Step 2: Drop old databases
DROP DATABASE IF EXISTS access_db;
DROP DATABASE IF EXISTS assessment_db;
DROP DATABASE IF EXISTS content_db;
DROP DATABASE IF EXISTS learning_db;
DROP DATABASE IF EXISTS user_db;

-- Step 3: Recreate databases
CREATE DATABASE access_db;
CREATE DATABASE assessment_db;
CREATE DATABASE content_db;
CREATE DATABASE learning_db;
CREATE DATABASE user_db;
