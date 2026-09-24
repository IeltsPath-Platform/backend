"""Shared builders and an in-memory LearningStore for formal-assessment tests.

The store mirrors the PostgreSQL adapter's contract (row-lock serialization,
nested transactions joining the outer unit of work, commit-or-discard, and the
monotonic applied-version guard) so ingestion logic can be tested without a
database. The PostgreSQL behaviour itself is covered by the *_postgres tests.
"""

from __future__ import annotations

from contextlib import contextmanager
import json
import sqlite3
import threading
from uuid import UUID, uuid4

from deeptutor.learning.models import KnowledgePoint, KnowledgeType, LearningModule, LearningProgress
from deeptutor.learning.storage import LearningStoreError, LearningTransaction

MODULE_ID = "11111111-1111-4111-8111-111111111111"
VOCABULARY_KP = "22222222-2222-4222-8222-222222222201"  # MEMORY
GRAMMAR_KP = "22222222-2222-4222-8222-222222222202"  # PROCEDURE
COHERENCE_KP = "22222222-2222-4222-8222-222222222203"  # CONCEPT


def curriculum() -> list[LearningModule]:
    points = [
        KnowledgePoint(id=VOCABULARY_KP, name="Academic vocabulary", type=KnowledgeType.MEMORY, module_id=MODULE_ID),
        KnowledgePoint(id=GRAMMAR_KP, name="Complex sentences", type=KnowledgeType.PROCEDURE, module_id=MODULE_ID),
        KnowledgePoint(id=COHERENCE_KP, name="Coherence and cohesion", type=KnowledgeType.CONCEPT, module_id=MODULE_ID),
    ]
    return [LearningModule(id=MODULE_ID, name="Writing foundations", order=0, knowledge_points=points)]


def mapping(knowledge_point_id: str, *, weight: float = 1.0, judgment: str | None = None,
            error_type: str | None = None) -> dict:
    return {
        "knowledge_point_id": knowledge_point_id,
        "weight": weight,
        "qualitative_judgment": judgment,
        "error_type": error_type,
    }


def item(mappings: list[dict], *, is_correct: bool | None, score: float | None = None,
         max_score: float = 1.0, item_result_id: str | None = None) -> dict:
    if score is None:
        score = max_score if is_correct else 0.0
    return {
        "item_result_id": item_result_id or str(uuid4()),
        "question_version_id": str(uuid4()),
        "is_correct": is_correct,
        "score": score,
        "max_score": max_score,
        "knowledge_point_mappings": mappings,
    }


def event(*, user_id: str, goal_id: str, attempt_id: str, items: list[dict], result_id: str | None = None,
          result_version: int = 1, event_id: str | None = None, assessment_type: str = "QUIZ") -> dict:
    return {
        "event_id": event_id or str(uuid4()),
        "event_type": "AssessmentCompleted.v2",
        "occurred_at": "2026-09-24T10:00:00.123456789Z",
        "source": "assessment-service",
        "data": {
            "user_id": user_id,
            "learning_goal_id": goal_id,
            "attempt_id": attempt_id,
            "result_id": result_id or str(uuid4()),
            "result_version": result_version,
            "assessment_type": assessment_type,
            "status": "COMPLETED",
            "completed_at": "2026-09-24T10:00:00Z",
            "item_results": items,
        },
    }


class InMemoryLearningStore:
    def __init__(self) -> None:
        self.paths: dict[str, dict] = {}
        self.result_versions: dict[tuple[str, str], int] = {}
        # Parked results by event_id: {user_id, learning_goal_id, attempt_id, result_version, payload}.
        self.pending: dict[str, dict] = {}
        self.committed_events: list[tuple[str, int, str, dict]] = []
        self.commits = 0
        self._lock = threading.RLock()
        self._local = threading.local()
        # DeepTutor transactions consult mastery_interactions (qmark SQL, as in its SQLite store).
        self._interactions = sqlite3.connect(":memory:", check_same_thread=False, isolation_level=None)
        self._interactions.row_factory = sqlite3.Row
        self._interactions.execute(
            """CREATE TABLE mastery_interactions (
                   interaction_id TEXT PRIMARY KEY, path_id TEXT NOT NULL, status TEXT NOT NULL,
                   question_json TEXT NOT NULL, session_id TEXT NOT NULL DEFAULT '',
                   turn_id TEXT NOT NULL DEFAULT '', user_answer TEXT NOT NULL DEFAULT '',
                   result_json TEXT NOT NULL DEFAULT '{}', created_at REAL NOT NULL, updated_at REAL NOT NULL)"""
        )

    @contextmanager
    def transaction(self, book_id, *, create=False, user_id=None, learning_goal_id=None):
        path_id = str(UUID(str(book_id)))
        active = getattr(self._local, "active", None)
        if active is not None:
            if active[0] != path_id:
                raise LearningStoreError("Nested transactions must use the same path")
            yield active[1]
            return
        with self._lock:
            row = self.paths.get(path_id)
            created = row is None
            if created:
                if not create:
                    raise KeyError(path_id)
                progress = LearningProgress(book_id=path_id)
                owner = {"user_id": str(user_id), "learning_goal_id": str(learning_goal_id)}
            else:
                progress = LearningProgress.model_validate_json(row["state_json"])
                progress.version = row["revision"]
                owner = {"user_id": row["user_id"], "learning_goal_id": row["learning_goal_id"]}
            tx = LearningTransaction(self._interactions, progress, created=created)
            pending_versions: dict[tuple[str, str], int] = {}
            consumed_pending: set[str] = set()
            self._local.active = (path_id, tx, pending_versions, consumed_pending)
            try:
                yield tx
                for event_id in consumed_pending:
                    self.pending.pop(event_id, None)
                if tx.changed:
                    revision = tx.base_revision + 1
                    tx.progress.version = revision
                    self.paths[path_id] = {**owner, "state_json": tx.progress.model_dump_json(), "revision": revision}
                    self.result_versions.update(pending_versions)
                    self.committed_events.extend((path_id, revision, name, payload) for name, payload, _, _ in tx.events)
                    self.commits += 1
            finally:
                self._local.active = None

    def mutate(self, book_id, mutation, *, create=False):
        with self.transaction(book_id, create=create) as tx:
            result = mutation(tx)
        return tx.progress, result

    def find_path(self, user_id, learning_goal_id):
        for path_id, row in self.paths.items():
            if row["user_id"] == str(user_id) and row["learning_goal_id"] == str(learning_goal_id):
                return path_id
        return None

    def get_owned_progress(self, path_id, user_id):
        row = self.paths.get(str(path_id))
        if row is None or row["user_id"] != str(user_id):
            return None
        progress = LearningProgress.model_validate_json(row["state_json"])
        progress.version = row["revision"]
        return progress

    def load(self, path_id) -> LearningProgress:
        row = self.paths[str(path_id)]
        progress = LearningProgress.model_validate_json(row["state_json"])
        progress.version = row["revision"]
        return progress

    def _active(self, path_id):
        active = getattr(self._local, "active", None)
        if active is None or active[0] != str(UUID(str(path_id))):
            raise LearningStoreError("Formal results must be read and written inside the path transaction")
        return active

    def _pending(self, path_id):
        return self._active(path_id)[2]

    def park_formal_result(self, command):
        # The store lock stands in for the PostgreSQL (user, goal) advisory lock.
        with self._lock:
            if self.find_path(command.user_id, command.learning_goal_id) is not None:
                return False
            self.pending.setdefault(str(command.event_id), {
                "user_id": str(command.user_id),
                "learning_goal_id": str(command.learning_goal_id),
                "attempt_id": str(command.attempt_id),
                "result_version": command.result_version,
                "payload": json.loads(json.dumps(command.raw_event)),
            })
            return True

    def pending_formal_results(self, path_id, user_id, learning_goal_id):
        self._active(path_id)
        rows = [(event_id, row) for event_id, row in self.pending.items()
                if row["user_id"] == str(user_id) and row["learning_goal_id"] == str(learning_goal_id)]
        rows.sort(key=lambda entry: (entry[1]["attempt_id"], entry[1]["result_version"]))
        return [(event_id, row["payload"]) for event_id, row in rows]

    def delete_pending_formal_results(self, path_id, event_ids):
        self._active(path_id)[3].update(event_ids)

    def applied_result_version(self, path_id, attempt_id):
        key = (str(path_id), str(attempt_id))
        return self._pending(path_id).get(key, self.result_versions.get(key))

    def record_applied_result(self, path_id, *, attempt_id, result_id, result_version, event_id):
        key = (str(path_id), str(attempt_id))
        current = self.applied_result_version(path_id, attempt_id)
        if current is not None and result_version <= current:
            raise LearningStoreError("Result version is not newer than the applied version")
        self._pending(path_id)[key] = result_version
