-- Databases for the MVP main-flow stack (docker-compose.mvp.yml). POSTGRES_DB creates user_db; each service
-- owns one database and Flyway creates its tables on start. Runs only when the data volume is first created.
CREATE DATABASE content_db;
CREATE DATABASE assessment_db;
CREATE DATABASE access_db;
CREATE DATABASE learning_db;
