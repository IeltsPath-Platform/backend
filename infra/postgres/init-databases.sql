-- Logical databases for the runnable IELTSPath services. user_db is created by POSTGRES_DB above.
-- Service-owned Flyway migrations create schema and seed demo content. User/access callbacks add demo accounts and points
-- only when DEMO_DATA_ENABLED=true. This script runs only when postgres-data is first initialized.
CREATE DATABASE content_db;
CREATE DATABASE assessment_db;
CREATE DATABASE access_db;
CREATE DATABASE learning_db;
CREATE DATABASE library_db;
CREATE DATABASE game_db;
CREATE DATABASE community_db;
