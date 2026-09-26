"""PostgreSQL implementation of the DeepTutor synchronous LearningStore contract."""

from __future__ import annotations

from contextlib import closing, contextmanager
from contextvars import ContextVar
import json
import time
from datetime import datetime, timezone
from typing import Any, Iterator, Mapping
from uuid import UUID

import psycopg2
from psycopg2.extras import RealDictCursor

from deeptutor.learning.models import LearningProgress
from deeptutor.learning.storage import (
    LearningConflictError,
    LearningStoreError,
    LearningTransaction,
)

from app.learning.formal_provenance import formal_source_reference


def _uuid_or_none(value: str) -> str | None:
    try:
        return str(UUID(str(value))) if value else None
    except ValueError:
        return None


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
                if create and user_id is not None and learning_goal_id:
                    # Serializes path creation with park_formal_result for the same goal.
                    self._lock_goal(cursor, user_id, learning_goal_id)
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
                    self._sync_evidence_projection(cursor, path_id, tx.progress)
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

    @staticmethod
    def _sync_evidence_projection(cursor: Any, path_id: str, progress: LearningProgress) -> None:
        """Mirror aggregate evidence into ``mastery_learning_evidence`` inside the commit.

        Same contract as DeepTutor's SQLite store: the projection is rebuilt from
        the aggregate in the write transaction, so it can never commit ahead of or
        disagree with ``state_json``. Formal evidence exposes its deterministic
        ``source_reference_id``; the unique index on
        ``(path_id, source, source_reference_id)`` rejects a duplicated outcome.
        """
        cursor.execute("DELETE FROM mastery_learning_evidence WHERE path_id = %s", (path_id,))
        for ordinal, evidence in enumerate(progress.learning_evidence):
            reference = formal_source_reference(evidence)
            cursor.execute(
                """INSERT INTO mastery_learning_evidence
                   (path_id, ordinal, knowledge_point_id, occurred_at, source, source_reference_id,
                    assessment_type, result, quality, hints_used, attempt_count, confidence,
                    response_time_seconds, session_id, turn_id, evidence_json)
                   VALUES (%s, %s, %s, to_timestamp(%s), %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s::jsonb)""",
                (
                    path_id,
                    ordinal,
                    str(UUID(evidence.knowledge_point_id)),
                    evidence.timestamp,
                    evidence.source,
                    reference,
                    evidence.assessment_type,
                    evidence.result,
                    evidence.quality,
                    evidence.hints_used,
                    evidence.attempt_count,
                    evidence.confidence,
                    evidence.response_time,
                    # Formal evidence carries provenance in these fields, not tutor ids.
                    None if reference else _uuid_or_none(evidence.session_id),
                    None if reference else _uuid_or_none(evidence.turn_id),
                    json.dumps(evidence.model_dump(mode="json"), ensure_ascii=False),
                ),
            )

    def _active_connection(self, path_id: str) -> Any:
        active = self._active.get()
        if active is None or active[0] != self._validate_id(path_id):
            raise LearningStoreError("Formal results must be read and written inside the path transaction")
        return active[1]

    def applied_result_version(self, path_id: str, attempt_id: str) -> int | None:
        """Latest result version already applied for an attempt, read under the path lock."""
        with self._active_connection(path_id).cursor() as cursor:
            cursor.execute(
                "SELECT result_version FROM formal_assessment_result_versions "
                "WHERE path_id = %s AND attempt_id = %s",
                (self._validate_id(path_id), str(attempt_id)),
            )
            row = cursor.fetchone()
            return int(row[0]) if row else None

    def record_applied_result(
        self,
        path_id: str,
        *,
        attempt_id: str,
        result_id: str,
        result_version: int,
        event_id: str,
    ) -> None:
        """Advance the applied version; the SQL predicate refuses any non-increasing version."""
        with self._active_connection(path_id).cursor() as cursor:
            cursor.execute(
                """INSERT INTO formal_assessment_result_versions
                   (path_id, attempt_id, result_id, result_version, event_id, applied_at)
                   VALUES (%s, %s, %s, %s, %s, now())
                   ON CONFLICT (path_id, attempt_id) DO UPDATE SET
                       result_id = EXCLUDED.result_id,
                       result_version = EXCLUDED.result_version,
                       event_id = EXCLUDED.event_id,
                       applied_at = EXCLUDED.applied_at
                   WHERE formal_assessment_result_versions.result_version < EXCLUDED.result_version""",
                (self._validate_id(path_id), str(attempt_id), str(result_id), int(result_version), str(event_id)),
            )
            if cursor.rowcount != 1:
                raise LearningStoreError(
                    f"Result version {result_version} for attempt {attempt_id} is not newer than the applied version"
                )

    @staticmethod
    def _lock_goal(cursor: Any, user_id: UUID | str, learning_goal_id: UUID | str) -> None:
        """Transaction-scoped advisory lock on ``(user_id, learning_goal_id)``."""
        cursor.execute(
            "SELECT pg_advisory_xact_lock(hashtextextended(%s, 0))",
            (f"{UUID(str(user_id))}:{UUID(str(learning_goal_id))}",),
        )

    def park_formal_result(self, command: Any) -> bool:
        """Park a result whose goal has no path yet; ``False`` if the path exists after all.

        Runs in its own transaction under the same advisory lock as path creation,
        so a result is either parked before the path commits (and that creation
        applies it) or finds the committed path here. A redelivered event keeps one row.
        """
        if command.raw_event is None:
            raise ValueError("Only an event received from the broker can be parked")
        with closing(psycopg2.connect(self._database_url)) as connection:
            with connection:
                with connection.cursor() as cursor:
                    self._lock_goal(cursor, command.user_id, command.learning_goal_id)
                    cursor.execute(
                        "SELECT 1 FROM mastery_paths WHERE user_id = %s AND learning_goal_id = %s",
                        (str(command.user_id), str(command.learning_goal_id)),
                    )
                    if cursor.fetchone() is not None:
                        return False
                    cursor.execute(
                        """INSERT INTO pending_formal_assessment_results
                           (event_id, user_id, learning_goal_id, attempt_id, result_version, payload)
                           VALUES (%s, %s, %s, %s, %s, %s::jsonb)
                           ON CONFLICT (event_id) DO NOTHING""",
                        (
                            str(command.event_id),
                            str(command.user_id),
                            str(command.learning_goal_id),
                            str(command.attempt_id),
                            int(command.result_version),
                            json.dumps(command.raw_event, ensure_ascii=False),
                        ),
                    )
                    return True

    def pending_formal_payloads(self, user_id: UUID | str, goal_id: UUID | str) -> list[dict]:
        """Read a goal's parked results without retaining a connection or locking its rows."""
        with closing(psycopg2.connect(self._database_url)) as connection:
            with connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        """SELECT payload FROM pending_formal_assessment_results
                           WHERE user_id = %s AND learning_goal_id = %s
                           ORDER BY attempt_id, result_version""",
                        (str(user_id), str(goal_id)),
                    )
                    return [row[0] for row in cursor.fetchall()]

    def pending_formal_results(
        self, path_id: str, user_id: UUID | str, learning_goal_id: UUID | str
    ) -> list[tuple[str, Any]]:
        """Parked ``(event_id, payload)`` rows of the goal, locked inside the path transaction."""
        with self._active_connection(path_id).cursor() as cursor:
            cursor.execute(
                """SELECT event_id::text, payload FROM pending_formal_assessment_results
                   WHERE user_id = %s AND learning_goal_id = %s
                   ORDER BY attempt_id, result_version
                   FOR UPDATE""",
                (str(user_id), str(learning_goal_id)),
            )
            return [(row[0], row[1]) for row in cursor.fetchall()]

    def delete_pending_formal_results(self, path_id: str, event_ids: list[str]) -> None:
        with self._active_connection(path_id).cursor() as cursor:
            cursor.execute(
                "DELETE FROM pending_formal_assessment_results WHERE event_id = ANY(%s::uuid[])",
                (list(event_ids),),
            )

    def replace_knowledge_point_bands(self, path_id: str, bands: Mapping[str, Any]) -> None:
        """Replace the path's band snapshot inside the open path transaction."""
        path_id = self._validate_id(path_id)
        with self._active_connection(path_id).cursor() as cursor:
            cursor.execute("DELETE FROM mastery_path_knowledge_point_bands WHERE path_id = %s", (path_id,))
            for knowledge_point_id, band in bands.items():
                cursor.execute(
                    """INSERT INTO mastery_path_knowledge_point_bands
                       (path_id, knowledge_point_id, band_min, band_max) VALUES (%s, %s, %s, %s)""",
                    (path_id, str(UUID(str(knowledge_point_id))), band.min, band.max),
                )

    def knowledge_point_bands(self, path_id: str) -> dict[str, Any]:
        """The path's band snapshot, read inside the open path transaction."""
        from app.adapters.curriculum_scope import KnowledgePointBand

        path_id = self._validate_id(path_id)
        with self._active_connection(path_id).cursor() as cursor:
            cursor.execute(
                "SELECT knowledge_point_id::text, band_min, band_max FROM mastery_path_knowledge_point_bands "
                "WHERE path_id = %s",
                (path_id,),
            )
            return {row[0]: KnowledgePointBand(row[1], row[2]) for row in cursor.fetchall()}

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
