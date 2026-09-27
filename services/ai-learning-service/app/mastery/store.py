# Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/storage.py @ da856ad.
# Modified for IELTSPath: only the errors and LearningTransaction (no SQLite store, leases or topics);
# the connection is any DB-API object whose execute() takes qmark SQL, such as the PostgreSQL shim.
"""Unit of work over one mastery path aggregate, and the store contract the service relies on."""

from __future__ import annotations

from collections.abc import Callable, Mapping
from contextlib import AbstractContextManager
import json
import time
from typing import Any, Protocol, TypeVar

from app.mastery.models import InteractionStatus, LearningProgress, MasteryInteraction

_T = TypeVar("_T")
_ACTIVE_INTERACTION_STATES = (
    InteractionStatus.REGISTERED.value,
    InteractionStatus.AWAITING_INPUT.value,
    InteractionStatus.ANSWERED.value,
)
_ALLOWED_INTERACTION_TRANSITIONS: dict[InteractionStatus, frozenset[InteractionStatus]] = {
    InteractionStatus.REGISTERED: frozenset(InteractionStatus),
    InteractionStatus.AWAITING_INPUT: frozenset(
        {
            InteractionStatus.AWAITING_INPUT,
            InteractionStatus.ANSWERED,
            InteractionStatus.GRADED,
            InteractionStatus.ABANDONED,
        }
    ),
    InteractionStatus.ANSWERED: frozenset(
        {
            InteractionStatus.ANSWERED,
            InteractionStatus.GRADED,
            InteractionStatus.ABANDONED,
        }
    ),
    InteractionStatus.GRADED: frozenset({InteractionStatus.GRADED}),
    InteractionStatus.ABANDONED: frozenset({InteractionStatus.ABANDONED}),
}


class LearningStoreError(RuntimeError):
    """Base error for durable mastery state operations."""


class LearningConflictError(LearningStoreError):
    """Raised when a stale aggregate revision attempts to overwrite a path."""

    def __init__(self, path_id: str, expected: int, actual: int) -> None:
        self.path_id = path_id
        self.expected = expected
        self.actual = actual
        super().__init__(
            f"Mastery path {path_id!r} changed concurrently "
            f"(expected revision {expected}, current revision {actual})"
        )


class LearningTransaction:
    """Unit-of-work over one locked ``LearningProgress`` aggregate.

    Domain services mutate :attr:`progress`, call :meth:`touch`, update any
    interaction rows, and enqueue public events.  The store commits all of it
    with one revision bump or rolls everything back.
    """

    def __init__(
        self,
        conn: Any,
        progress: LearningProgress,
        *,
        created: bool,
    ) -> None:
        self._conn = conn
        self.progress = progress
        self.base_revision = int(progress.version)
        self.changed = created
        self._events: list[tuple[str, dict[str, Any], str, str]] = []
        if created:
            self.emit("path.created", {})

    def touch(self) -> None:
        self.changed = True

    def emit(
        self,
        event_type: str,
        payload: dict[str, Any] | None = None,
        *,
        session_id: str = "",
        turn_id: str = "",
    ) -> None:
        event_name = str(event_type or "").strip()
        if not event_name:
            raise ValueError("event_type must not be empty")
        self.changed = True
        self._events.append(
            (event_name, dict(payload or {}), str(session_id or ""), str(turn_id or ""))
        )

    @property
    def events(self) -> list[tuple[str, dict[str, Any], str, str]]:
        return list(self._events)

    @staticmethod
    def _interaction_from_row(row: Mapping[str, Any] | None) -> MasteryInteraction | None:
        if row is None:
            return None
        return MasteryInteraction(
            interaction_id=row["interaction_id"],
            path_id=row["path_id"],
            question=json.loads(row["question_json"]),
            status=InteractionStatus(row["status"]),
            session_id=row["session_id"] or "",
            turn_id=row["turn_id"] or "",
            user_answer=row["user_answer"] or "",
            result=json.loads(row["result_json"] or "{}"),
            created_at=float(row["created_at"]),
            updated_at=float(row["updated_at"]),
        )

    def get_interaction(self, interaction_id: str) -> MasteryInteraction | None:
        row = self._conn.execute(
            "SELECT * FROM mastery_interactions WHERE interaction_id = ? AND path_id = ?",
            (str(interaction_id), self.progress.book_id),
        ).fetchone()
        return self._interaction_from_row(row)

    def active_interaction(self) -> MasteryInteraction | None:
        placeholders = ",".join("?" for _ in _ACTIVE_INTERACTION_STATES)
        row = self._conn.execute(
            f"""
            SELECT * FROM mastery_interactions
            WHERE path_id = ? AND status IN ({placeholders})
            ORDER BY created_at DESC LIMIT 1
            """,  # nosec B608 - placeholders is a generated "?,?" list; every value is bound
            (self.progress.book_id, *_ACTIVE_INTERACTION_STATES),
        ).fetchone()
        return self._interaction_from_row(row)

    def put_interaction(self, interaction: MasteryInteraction) -> None:
        if interaction.path_id != self.progress.book_id:
            raise ValueError("interaction path_id does not match transaction path")
        existing = self._conn.execute(
            "SELECT path_id, status FROM mastery_interactions WHERE interaction_id = ?",
            (interaction.interaction_id,),
        ).fetchone()
        if existing is not None and str(existing["path_id"]) != interaction.path_id:
            raise ValueError(
                f"interaction_id {interaction.interaction_id!r} already belongs to another path"
            )
        if existing is not None:
            current_status = InteractionStatus(existing["status"])
            if interaction.status not in _ALLOWED_INTERACTION_TRANSITIONS[current_status]:
                raise LearningStoreError(
                    f"Invalid mastery interaction transition: "
                    f"{current_status.value} -> {interaction.status.value}"
                )
        now = time.time()
        interaction.updated_at = now
        self._conn.execute(
            """
            INSERT INTO mastery_interactions (
                interaction_id, path_id, status, question_json, session_id,
                turn_id, user_answer, result_json, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(interaction_id) DO UPDATE SET
                status = excluded.status,
                question_json = excluded.question_json,
                session_id = excluded.session_id,
                turn_id = excluded.turn_id,
                user_answer = excluded.user_answer,
                result_json = excluded.result_json,
                updated_at = excluded.updated_at
            """,
            (
                interaction.interaction_id,
                interaction.path_id,
                interaction.status.value,
                json.dumps(interaction.question.model_dump(mode="json"), ensure_ascii=False),
                interaction.session_id,
                interaction.turn_id,
                interaction.user_answer,
                json.dumps(interaction.result, ensure_ascii=False),
                interaction.created_at,
                now,
            ),
        )
        self.touch()

    def abandon_active_interactions(self) -> int:
        placeholders = ",".join("?" for _ in _ACTIVE_INTERACTION_STATES)
        cursor = self._conn.execute(
            f"""
            UPDATE mastery_interactions
            SET status = ?, updated_at = ?
            WHERE path_id = ? AND status IN ({placeholders})
            """,  # nosec B608 - placeholders is a generated "?,?" list; every value is bound
            (
                InteractionStatus.ABANDONED.value,
                time.time(),
                self.progress.book_id,
                *_ACTIVE_INTERACTION_STATES,
            ),
        )
        if cursor.rowcount:
            self.touch()
        return int(cursor.rowcount)


class LearningStore(Protocol):
    """What LearningService needs from persistence: one locked, revisioned unit of work per path."""

    def transaction(self, book_id: str, *, create: bool = False) -> AbstractContextManager[LearningTransaction]: ...

    def mutate(
        self, book_id: str, mutation: Callable[[LearningTransaction], _T], *, create: bool = False
    ) -> tuple[LearningProgress, _T]: ...


__all__ = ["LearningConflictError", "LearningStore", "LearningStoreError", "LearningTransaction"]
