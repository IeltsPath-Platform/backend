"""In-memory LearningStore for engine tests that need a store but not PostgreSQL.

Commits like the production store: one locked unit of work per path, one revision
bump when the transaction changed anything, rollback on error. Interaction rows
live in an in-memory SQLite table because ``LearningTransaction`` speaks qmark SQL.
"""

from __future__ import annotations

from collections.abc import Callable, Iterator
from contextlib import contextmanager
import sqlite3
import threading
import time
from typing import Any, TypeVar

from app.mastery.models import LearningProgress
from app.mastery.store import LearningTransaction

_T = TypeVar("_T")

_SCHEMA = """
CREATE TABLE mastery_interactions (
    interaction_id TEXT PRIMARY KEY,
    path_id TEXT NOT NULL,
    status TEXT NOT NULL,
    question_json TEXT NOT NULL,
    session_id TEXT NOT NULL DEFAULT '',
    turn_id TEXT NOT NULL DEFAULT '',
    user_answer TEXT NOT NULL DEFAULT '',
    result_json TEXT NOT NULL DEFAULT '{}',
    created_at REAL NOT NULL,
    updated_at REAL NOT NULL
);
CREATE UNIQUE INDEX uq_mastery_one_active_interaction
    ON mastery_interactions(path_id) WHERE status IN ('registered', 'awaiting_input', 'answered');
"""


class MemoryLearningStore:
    def __init__(self) -> None:
        self._lock = threading.RLock()
        self._paths: dict[str, str] = {}
        self.events: list[tuple[str, str, dict[str, Any]]] = []
        self._connection = sqlite3.connect(":memory:", check_same_thread=False, isolation_level=None)
        self._connection.row_factory = sqlite3.Row
        self._connection.executescript(_SCHEMA)

    @contextmanager
    def transaction(self, book_id: str, *, create: bool = False) -> Iterator[LearningTransaction]:
        with self._lock:
            stored = self._paths.get(book_id)
            if stored is None and not create:
                raise KeyError(book_id)
            progress = (
                LearningProgress.model_validate_json(stored) if stored else LearningProgress(book_id=book_id)
            )
            self._connection.execute("BEGIN")
            tx = LearningTransaction(self._connection, progress, created=stored is None)
            try:
                yield tx
            except BaseException:
                self._connection.execute("ROLLBACK")
                raise
            if tx.changed:
                tx.progress.version = tx.base_revision + 1
                tx.progress.updated_at = time.time()
                self._paths[book_id] = tx.progress.model_dump_json()
                self.events.extend((book_id, name, payload) for name, payload, _session, _turn in tx.events)
            self._connection.execute("COMMIT")

    def mutate(
        self, book_id: str, mutation: Callable[[LearningTransaction], _T], *, create: bool = False
    ) -> tuple[LearningProgress, _T]:
        with self.transaction(book_id, create=create) as tx:
            result = mutation(tx)
            return tx.progress, result

    def load(self, book_id: str) -> LearningProgress | None:
        with self._lock:
            stored = self._paths.get(book_id)
            return LearningProgress.model_validate_json(stored) if stored else None

    def get_active_interaction(self, book_id: str):
        with self.transaction(book_id) as tx:
            return tx.active_interaction()
