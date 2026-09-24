"""Disposable PostgreSQL schema with the DATABASE_V5 AI Learning tables plus this service's migrations.

The base V5 tables are not created by a migration in this repository, so tests
create them here in an isolated schema and then apply ``migrations/V*.sql``.
"""

from __future__ import annotations

import os
from pathlib import Path
from urllib.parse import quote
from uuid import uuid4

BASE_V5_TABLES = """
CREATE TABLE mastery_paths (
    path_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    learning_goal_id UUID,
    state_json JSONB NOT NULL,
    revision BIGINT NOT NULL,
    owner_session_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE mastery_interactions (
    interaction_id UUID PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    question_json JSONB NOT NULL,
    session_id UUID,
    turn_id UUID,
    user_answer TEXT NOT NULL DEFAULT '',
    result_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE mastery_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES mastery_paths(path_id) ON DELETE CASCADE,
    revision BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    session_id UUID,
    turn_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);
"""


class PostgresSchema:
    def __init__(self, database_url: str) -> None:
        import psycopg2

        self._psycopg2 = psycopg2
        self._database_url = database_url
        self.name = f"ai_learning_formal_{uuid4().hex}"
        separator = "&" if "?" in database_url else "?"
        # libpq applies search_path to every connection the store opens.
        self.url = f"{database_url}{separator}options={quote(f'-csearch_path={self.name}')}"

    def create(self) -> None:
        connection = self._psycopg2.connect(self._database_url)
        connection.autocommit = True
        try:
            with connection.cursor() as cursor:
                cursor.execute(f'CREATE SCHEMA "{self.name}"')
                cursor.execute(f'SET search_path TO "{self.name}"')
                cursor.execute(BASE_V5_TABLES)
                for migration in sorted((Path(__file__).parents[1] / "migrations").glob("V*.sql")):
                    cursor.execute(migration.read_text(encoding="utf-8"))
        finally:
            connection.close()

    def drop(self) -> None:
        connection = self._psycopg2.connect(self._database_url)
        connection.autocommit = True
        try:
            with connection.cursor() as cursor:
                cursor.execute(f'DROP SCHEMA IF EXISTS "{self.name}" CASCADE')
        finally:
            connection.close()

    def query(self, sql: str, params: tuple = ()) -> list[tuple]:
        connection = self._psycopg2.connect(self.url)
        try:
            with connection.cursor() as cursor:
                cursor.execute(sql, params)
                return cursor.fetchall()
        finally:
            connection.close()


def database_url_or_skip(test_case_type) -> str:
    import unittest

    url = os.environ.get("AI_LEARNING_TEST_DATABASE_URL")
    if not url:
        raise unittest.SkipTest("AI_LEARNING_TEST_DATABASE_URL is not configured")
    return url
