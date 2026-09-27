"""PostgreSQL storage for tutor practice cards and their per-question review state."""

from __future__ import annotations

from contextlib import closing
from datetime import datetime
import json
from typing import Any, Callable
from uuid import UUID

import psycopg2
from psycopg2.extras import Json, RealDictCursor

from app.mastery.pending import resolve_choice_submission
from app.practice.scheduler import ReviewState, first_mistake, review as schedule_review

Grade = Callable[[str, str, str], bool]


class PracticeNotFound(LookupError):
    """The entry does not exist or does not belong to the current learner."""


class PracticeConflict(RuntimeError):
    """The entry is already answered, is not reviewable, or an idempotency key conflicts."""


_ENTRY_COLUMNS = """e.id AS entry_id, e.question_id, e.session_id, e.knowledge_point_id, e.knowledge_point_name,
    e.question AS prompt, e.question_type, e.options_json AS options, e.difficulty,
    e.created_at, e.answered_at, e.user_answer, e.is_correct, e.resolved,
    e.correct_answer, e.explanation"""


def _json_value(value: Any) -> Any:
    if isinstance(value, str):
        return json.loads(value)
    return value


def _timestamp(value: datetime | None) -> str | None:
    if value is None:
        return None
    return value.isoformat()


def _entry_payload(row: dict[str, Any], *, reveal_answer: bool) -> dict[str, Any]:
    options = _json_value(row.get("options")) or []
    payload = {
        "entry_id": int(row["entry_id"]),
        "question_id": row["question_id"],
        "session_id": row["session_id"],
        "knowledge_point_id": str(row["knowledge_point_id"]),
        "knowledge_point_name": row["knowledge_point_name"],
        "prompt": row["prompt"],
        "question_type": row["question_type"],
        "options": options,
        "difficulty": row["difficulty"],
        "created_at": row["created_at"],
        "answered_at": row["answered_at"],
        "user_answer": row["user_answer"] if row["answered_at"] is not None else "",
        "is_correct": bool(row["is_correct"]),
        "resolved": bool(row["resolved"]),
    }
    if reveal_answer and row["answered_at"] is not None:
        payload["correct_answer"] = row["correct_answer"]
        payload["explanation"] = row["explanation"]
    return payload


def _is_correct(grade: Grade, answer: str, row: dict[str, Any]) -> bool:
    """Grade like the mastery card: a choice answer may name the label, a labelled option or the option body."""
    submitted = answer
    if row["question_type"] == "choice":
        options = {option["label"]: option["body"] for option in _json_value(row["options_json"]) or []}
        submitted = resolve_choice_submission(answer, options) or answer
    return bool(grade(submitted, row["correct_answer"], row["question_type"]))


def _review_state(row: dict[str, Any]) -> ReviewState:
    return ReviewState(
        interval_days=float(row["interval_days"]),
        ease=float(row["ease"]),
        streak=int(row["streak"]),
        lapses=int(row["lapses"]),
        review_count=int(row["review_count"]),
        due_at=row["due_at"],
        is_mistake=bool(row["is_mistake"]),
    )


class PracticeStore:
    def __init__(self, database_url: str) -> None:
        if not database_url.strip():
            raise ValueError("database_url must not be blank")
        self._database_url = database_url

    def _connect(self):
        return closing(psycopg2.connect(self._database_url))

    def create_entries(
        self,
        user_id: UUID | str,
        session_id: UUID | str,
        turn_id: UUID | str,
        path_id: UUID | str,
        kp_id: UUID | str,
        kp_name: str,
        questions: list[dict[str, Any]],
    ) -> list[int]:
        """Insert a validated batch atomically and return its entry ids."""
        user, session, turn, path, kp = map(lambda value: str(UUID(str(value))),
                                            (user_id, session_id, turn_id, path_id, kp_id))
        ids: list[int] = []
        with self._connect() as connection, connection, connection.cursor() as cursor:
            cursor.execute(
                """SELECT 1 FROM sessions s JOIN mastery_paths p ON p.path_id = s.path_id
                   JOIN turns t ON t.session_id = s.id AND t.id = %s
                   WHERE s.id = %s AND s.user_id = %s AND s.path_id = %s AND p.user_id = s.user_id
                     AND s.archived_at IS NULL""",
                (turn, session, user, path),
            )
            if cursor.fetchone() is None:
                raise PracticeNotFound
            for question in questions:
                cursor.execute(
                    """INSERT INTO notebook_entries
                       (user_id, session_id, turn_id, mastery_path_id, knowledge_point_id,
                        knowledge_point_name, question_id, question, question_type, options_json,
                        correct_answer, explanation, difficulty, created_at, updated_at)
                       VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, now(), now())
                       RETURNING id""",
                    (user, session, turn, path, kp, kp_name, str(UUID(str(question["question_id"]))),
                     question["question"], question["question_type"], Json(question["options"]),
                     question["correct_answer"], question["explanation"], question["difficulty"]),
                )
                ids.append(int(cursor.fetchone()[0]))
        return ids

    def answer_entry(self, user_id: UUID | str, entry_id: int, answer: str, grade: Grade,
                     now: datetime) -> dict[str, Any]:
        user = str(UUID(str(user_id)))
        with self._connect() as connection, connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                """SELECT id, question_id, question_type, options_json, correct_answer, explanation, answered_at
                   FROM notebook_entries WHERE id = %s AND user_id = %s""",
                (entry_id, user),
            )
            row = cursor.fetchone()
            if row is None:
                raise PracticeNotFound
            if row["answered_at"] is not None:
                raise PracticeConflict("Practice entry has already been answered")

            is_correct = _is_correct(grade, answer, row)
            cursor.execute(
                """UPDATE notebook_entries
                   SET user_answer = %s, result = %s, is_correct = %s, answered_at = %s, updated_at = %s
                   WHERE id = %s AND user_id = %s AND answered_at IS NULL
                   RETURNING id""",
                (answer, "correct" if is_correct else "incorrect", is_correct, now, now, entry_id, user),
            )
            updated = cursor.fetchone()
            if updated is None:
                cursor.execute("SELECT answered_at FROM notebook_entries WHERE id = %s AND user_id = %s",
                               (entry_id, user))
                current = cursor.fetchone()
                if current is None:
                    raise PracticeNotFound
                raise PracticeConflict("Practice entry has already been answered")

            due_at = None
            if not is_correct:
                state = first_mistake(now)
                due_at = state.due_at
                cursor.execute(
                    """INSERT INTO practice_review_state
                       (entry_id, first_wrong_at, due_at, interval_days, ease, streak, lapses, review_count)
                       VALUES (%s, %s, %s, %s, %s, %s, %s, %s)""",
                    (entry_id, now, state.due_at, state.interval_days, state.ease, state.streak,
                     state.lapses, state.review_count),
                )
            return {
                "entry_id": int(entry_id),
                "question_id": row["question_id"],
                "is_correct": is_correct,
                "correct_answer": row["correct_answer"],
                "explanation": row["explanation"],
                "due_at": _timestamp(due_at),
            }

    def list_entries(
        self,
        user_id: UUID | str,
        *,
        kp_id: UUID | str | None,
        session_id: UUID | str | None,
        status: str | None,
        limit: int,
    ) -> list[dict[str, Any]]:
        if not 1 <= limit <= 100:
            raise ValueError("limit must be between 1 and 100")
        user = str(UUID(str(user_id)))
        clauses = ["e.user_id = %s"]
        params: list[Any] = [user]
        if kp_id is not None:
            clauses.append("e.knowledge_point_id = %s")
            params.append(str(UUID(str(kp_id))))
        if session_id is not None:
            clauses.append("e.session_id = %s")
            params.append(str(UUID(str(session_id))))
        if status == "open":
            clauses.append("e.answered_at IS NULL")
        elif status == "correct":
            clauses.append("e.answered_at IS NOT NULL AND e.is_correct")
        elif status == "incorrect":
            clauses.append("e.answered_at IS NOT NULL AND NOT e.is_correct")
        elif status is not None:
            raise ValueError("status must be open, correct, or incorrect")
        query = (f"SELECT {_ENTRY_COLUMNS} FROM notebook_entries e WHERE "
                 + " AND ".join(clauses) + " ORDER BY e.created_at DESC, e.id DESC LIMIT %s")
        params.append(limit)
        with self._connect() as connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(query, tuple(params))
            return [_entry_payload(row, reveal_answer=True) for row in cursor.fetchall()]

    def due_entries(self, user_id: UUID | str, now: datetime, limit: int) -> list[dict[str, Any]]:
        if not 1 <= limit <= 100:
            raise ValueError("limit must be between 1 and 100")
        user = str(UUID(str(user_id)))
        with self._connect() as connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                f"""SELECT {_ENTRY_COLUMNS} FROM practice_review_state s
                    JOIN notebook_entries e ON e.id = s.entry_id
                    WHERE e.user_id = %s AND s.is_mistake AND s.due_at <= %s
                    ORDER BY s.due_at, e.id LIMIT %s""",
                (user, now, limit),
            )
            # A review card is an unanswered attempt from the learner's point of view.
            return [_entry_payload(row, reveal_answer=False) for row in cursor.fetchall()]

    def review(
        self,
        user_id: UUID | str,
        request_id: UUID | str,
        entry_id: int,
        answer: str,
        rating: str,
        grade: Grade,
        now: datetime,
    ) -> dict[str, Any]:
        user, request = str(UUID(str(user_id))), str(UUID(str(request_id)))
        if rating not in {"hard", "good", "easy"}:
            raise ValueError("rating must be hard, good, or easy")
        try:
            return self._review_transaction(user, request, entry_id, answer, rating, grade, now)
        except psycopg2.errors.UniqueViolation:
            # A concurrent call with this idempotency key may have committed while this insert waited.
            replay = self._existing_review(request)
            return self._replay_or_conflict(replay, user, entry_id)

    def _existing_review(self, request_id: str) -> dict[str, Any] | None:
        with self._connect() as connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                "SELECT entry_id, user_id::text AS user_id, outcome_json FROM practice_review_events "
                "WHERE request_id = %s",
                (request_id,),
            )
            return cursor.fetchone()

    @staticmethod
    def _replay_or_conflict(row: dict[str, Any] | None, user_id: str, entry_id: int) -> dict[str, Any]:
        if row is None or row["user_id"] != user_id or int(row["entry_id"]) != int(entry_id):
            raise PracticeConflict("requestId was already used for another learner or entry")
        outcome = _json_value(row["outcome_json"])
        return dict(outcome)

    def _review_transaction(self, user: str, request: str, entry_id: int, answer: str, rating: str,
                            grade: Grade, now: datetime) -> dict[str, Any]:
        with self._connect() as connection, connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                "SELECT entry_id, user_id::text AS user_id, outcome_json FROM practice_review_events "
                "WHERE request_id = %s",
                (request,),
            )
            existing = cursor.fetchone()
            if existing is not None:
                return self._replay_or_conflict(existing, user, entry_id)

            cursor.execute(
                """SELECT e.id, e.question_id, e.question_type, e.options_json, e.correct_answer, e.explanation,
                          s.is_mistake,
                          s.interval_days, s.ease, s.streak, s.lapses, s.review_count, s.due_at
                   FROM practice_review_state s JOIN notebook_entries e ON e.id = s.entry_id
                   WHERE e.id = %s AND e.user_id = %s FOR UPDATE OF s, e""",
                (entry_id, user),
            )
            row = cursor.fetchone()
            if row is None:
                cursor.execute("SELECT 1 FROM notebook_entries WHERE id = %s AND user_id = %s", (entry_id, user))
                if cursor.fetchone() is None:
                    raise PracticeNotFound
                raise PracticeConflict("Practice entry is not in the review schedule")
            if not row["is_mistake"]:
                raise PracticeConflict("Practice entry is not in the review schedule")

            is_correct = _is_correct(grade, answer, row)
            actual_rating = rating if is_correct else "again"
            next_state = schedule_review(_review_state(row), actual_rating, now)
            cursor.execute(
                """UPDATE practice_review_state SET is_mistake = %s, due_at = %s, interval_days = %s,
                          ease = %s, streak = %s, lapses = %s, review_count = %s, last_review_at = %s,
                          version = version + 1 WHERE entry_id = %s""",
                (next_state.is_mistake, next_state.due_at, next_state.interval_days, next_state.ease,
                 next_state.streak, next_state.lapses, next_state.review_count, now, entry_id),
            )
            # The entry keeps its first answer and result; each review lives in practice_review_events.
            cursor.execute(
                "UPDATE notebook_entries SET resolved = %s, updated_at = %s WHERE id = %s",
                (not next_state.is_mistake, now, entry_id),
            )
            outcome = {
                "entry_id": int(entry_id),
                "question_id": row["question_id"],
                "is_correct": is_correct,
                "rating": actual_rating,
                "due_at": _timestamp(next_state.due_at),
                "resolved": not next_state.is_mistake,
                "correct_answer": row["correct_answer"],
                "explanation": row["explanation"],
            }
            cursor.execute(
                """INSERT INTO practice_review_events
                   (request_id, entry_id, user_id, rating, answer, reviewed_at, outcome_json)
                   VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                (request, entry_id, user, actual_rating, answer, now, Json(outcome)),
            )
            return outcome


__all__ = ["PracticeConflict", "PracticeNotFound", "PracticeStore"]
