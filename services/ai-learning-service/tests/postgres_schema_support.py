"""Disposable PostgreSQL schema built only from this service's ``migrations/V*.sql``.

Tests apply the same migration chain Flyway runs, in the same version order, so
the schema under test cannot drift from the deployed one.
"""

from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Iterable
from urllib.parse import quote
from uuid import uuid4

MIGRATIONS_DIR = Path(__file__).parents[1] / "migrations"

_VERSIONED = re.compile(r"^V(\d+(?:_\d+)*)__")


def migration_version(path: Path) -> str:
    """Flyway version of a ``V<version>__<name>.sql`` file, with ``_`` read as ``.``."""
    match = _VERSIONED.match(path.name)
    if match is None:
        raise ValueError(f"Not a versioned migration: {path.name}")
    return match.group(1).replace("_", ".")


def ordered_migrations(paths: Iterable[Path]) -> list[Path]:
    """Order migrations the way Flyway does: by numeric version parts, never by file name."""
    return sorted(paths, key=lambda path: tuple(int(part) for part in migration_version(path).split(".")))


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
                for migration in ordered_migrations(MIGRATIONS_DIR.glob("V*.sql")):
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

    def execute(self, sql: str, params: tuple = ()) -> None:
        connection = self._psycopg2.connect(self.url)
        try:
            with connection:
                with connection.cursor() as cursor:
                    cursor.execute(sql, params)
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
