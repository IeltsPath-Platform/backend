"""PostgreSQL implementation of the DeepTutor synchronous LearningStore contract."""

from __future__ import annotations

from contextlib import closing, contextmanager
from contextvars import ContextVar
import json
import time
from datetime import datetime, timezone
from typing import Any, Iterator
from uuid import UUID

import psycopg2
from psycopg2.extras import RealDictCursor

from deeptutor.learning.models import LearningProgress
from deeptutor.learning.storage import (
    LearningConflictError,
    LearningStoreError,
    LearningTransaction,
)


class _Cursor:
    def __init__(self, cursor: Any) -> None:
        self._cursor = cursor

    @property
    def rowcount(self) -> int:
        return self._cursor.rowcount

    def fetchone(self) -> Any:
        return self._cursor.fetchone()


class _Connection:
    """Small DB-API compatibility layer for SQL used by DeepTutor transactions."""

    def __init__(self, connection: Any) -> None:
        self._connection = connection

    def execute(self, sql: str, params: tuple[Any, ...] = ()) -> _Cursor:
        # DeepTutor's transaction contract uses SQLite qmark parameters. PostgreSQL
        # accepts the same SQL once those placeholders are translated.
        values = list(params)
        if "INSERT INTO mastery_interactions" in sql:
            for index in (4, 5):
                if index < len(values) and values[index] == "":
                    values[index] = None
            for index in (8, 9):
                if index < len(values) and isinstance(values[index], (float, int)):
                    values[index] = datetime.fromtimestamp(float(values[index]), timezone.utc)
        elif "UPDATE mastery_interactions" in sql and len(values) > 1:
            if isinstance(values[1], (float, int)):
                values[1] = datetime.fromtimestamp(float(values[1]), timezone.utc)
        with self._connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(sql.replace("?", "%s"), tuple(values))
            if cursor.description:
                rows = [self._normalize_row(row) for row in cursor.fetchall()]
                return _BufferedCursor(rows, cursor.rowcount)
            return _Cursor(_RowCountCursor(cursor.rowcount))

    @staticmethod
    def _normalize_row(row: Any) -> dict[str, Any]:
        normalized = dict(row)
        for key in ("created_at", "updated_at"):
            value = normalized.get(key)
            if isinstance(value, datetime):
                if value.tzinfo is None:
                    value = value.replace(tzinfo=timezone.utc)
                normalized[key] = value.timestamp()
        for key in ("question_json", "result_json"):
            value = normalized.get(key)
            if value is not None and not isinstance(value, str):
                normalized[key] = json.dumps(value, ensure_ascii=False)
        return normalized


class _RowCountCursor:
    def __init__(self, rowcount: int) -> None:
        self.rowcount = rowcount


class _BufferedCursor(_Cursor):
    def __init__(self, rows: list[Any], rowcount: int) -> None:
        self._rows = rows
        self._rowcount = rowcount

    @property
    def rowcount(self) -> int:
        return self._rowcount

    def fetchone(self) -> Any:
        return self._rows.pop(0) if self._rows else None


class PostgresLearningStore:
    """Store DeepTutor aggregates in V5 ``mastery_paths`` rows.

    The interface remains synchronous to match DeepTutor v1.6.9. Nested
    transactions for the same path join the outer unit of work, allowing path
    creation and curriculum replacement to commit as one aggregate revision.
    """

    def __init__(self, database_url: str) -> None:
        if not database_url.strip():
            raise ValueError("database_url must not be blank")
        self._database_url = database_url
        self._active: ContextVar[tuple[str, Any, LearningTransaction] | None] = (
            ContextVar(f"learning_tx_{id(self)}", default=None)
        )

    @staticmethod
    def _validate_id(book_id: str) -> str:
        value = str(book_id or "")
        try:
            return str(UUID(value))
        except (ValueError, TypeError, AttributeError) as exc:
            raise ValueError("book_id must be a UUID") from exc

    @staticmethod
    def _progress_from_row(row: Any) -> LearningProgress:
        payload = row["state_json"]
        if not isinstance(payload, str):
            payload = json.dumps(payload)
        progress = LearningProgress.model_validate_json(payload)
        progress.book_id = str(row["path_id"])
        progress.version = int(row["revision"])
        return progress

    @contextmanager
    def transaction(
        self,
        book_id: str,
        *,
        create: bool = False,
        user_id: UUID | str | None = None,
        learning_goal_id: UUID | str | None = None,
    ) -> Iterator[LearningTransaction]:
        path_id = self._validate_id(book_id)
        active = self._active.get()
        if active is not None:
            active_path, _connection, tx = active
            if active_path != path_id:
                raise LearningStoreError("Nested transactions must use the same path")
            yield tx
            return

        connection = psycopg2.connect(self._database_url)
        token = None
        try:
            with connection.cursor(cursor_factory=RealDictCursor) as cursor:
                cursor.execute(
                    "SELECT path_id, state_json, revision FROM mastery_paths "
                    "WHERE path_id = %s FOR UPDATE",
                    (path_id,),
                )
                row = cursor.fetchone()
                created = row is None
                if created:
                    if not create:
                        raise KeyError(path_id)
                    if user_id is None:
                        raise ValueError("user_id is required when creating a path")
                    progress = LearningProgress(book_id=path_id)
                    cursor.execute(
                        """INSERT INTO mastery_paths
                           (path_id, user_id, learning_goal_id, state_json, revision,
                            created_at, updated_at)
                           VALUES (%s, %s, %s, %s::jsonb, 0, now(), now())""",
                        (
                            path_id,
                            str(user_id),
                            str(learning_goal_id) if learning_goal_id else None,
                            progress.model_dump_json(),
                        ),
                    )
                else:
                    progress = self._progress_from_row(row)

            tx = LearningTransaction(_Connection(connection), progress, created=created)
            token = self._active.set((path_id, connection, tx))
            yield tx
            if tx.changed:
                revision = tx.base_revision + 1
                now = time.time()
                tx.progress.version = revision
                tx.progress.updated_at = now
                payload = tx.progress.model_dump(mode="json")
                with connection.cursor() as cursor:
                    cursor.execute(
                        """UPDATE mastery_paths SET state_json = %s::jsonb,
                           revision = %s, updated_at = to_timestamp(%s)
                           WHERE path_id = %s AND revision = %s""",
                        (json.dumps(payload, ensure_ascii=False), revision, now, path_id, tx.base_revision),
                    )
                    if cursor.rowcount != 1:
                        cursor.execute("SELECT revision FROM mastery_paths WHERE path_id = %s", (path_id,))
                        current = cursor.fetchone()
                        raise LearningConflictError(path_id, tx.base_revision, int(current[0]) if current else 0)
                    for event_type, event_payload, session_id, turn_id in tx.events:
                        cursor.execute(
                            """INSERT INTO mastery_events
                               (path_id, revision, event_type, payload_json, session_id, turn_id, created_at)
                               VALUES (%s, %s, %s, %s::jsonb, %s, %s, now())""",
                            (path_id, revision, event_type, json.dumps(event_payload, ensure_ascii=False), session_id or None, turn_id or None),
                        )
            connection.commit()
        except Exception:
            connection.rollback()
            raise
        finally:
            if token is not None:
                self._active.reset(token)
            connection.close()

    def mutate(self, book_id: str, mutation: Any, *, create: bool = False) -> tuple[LearningProgress, Any]:
        with self.transaction(book_id, create=create) as tx:
            result = mutation(tx)
        return tx.progress, result

    def find_path(self, user_id: UUID | str, learning_goal_id: UUID | str) -> str | None:
        with closing(psycopg2.connect(self._database_url)) as connection:
            with connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        "SELECT path_id FROM mastery_paths WHERE user_id = %s AND learning_goal_id = %s",
                        (str(user_id), str(learning_goal_id)),
                    )
                    row = cursor.fetchone()
                    return str(row[0]) if row else None

    def get_owned_progress(self, path_id: UUID | str, user_id: UUID | str) -> LearningProgress | None:
        with closing(psycopg2.connect(self._database_url)) as connection:
            with connection:
                with connection.cursor(cursor_factory=RealDictCursor) as cursor:
                    cursor.execute(
                        """SELECT path_id, state_json, revision FROM mastery_paths
                           WHERE path_id = %s AND user_id = %s""",
                        (str(path_id), str(user_id)),
                    )
                    row = cursor.fetchone()
                    return self._progress_from_row(row) if row else None
