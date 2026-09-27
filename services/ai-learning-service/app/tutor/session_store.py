"""PostgreSQL storage for tutor sessions, their turns and messages.

Every learner-facing method takes the learner's id and checks ownership inside its SQL, so a session that belongs to
someone else (or is archived, or does not exist) reads as absent. Message text is never logged.
"""

from __future__ import annotations

from contextlib import closing
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any
from uuid import UUID, uuid4

import psycopg2
from psycopg2.extras import Json, RealDictCursor

from app.tutor.reading import SessionMaterial

MESSAGE_ROLES = frozenset({"user", "assistant"})
TURN_OUTCOMES = frozenset({"completed", "failed"})
_OWNED_SESSION = "SELECT id FROM sessions WHERE id = %s AND user_id = %s AND archived_at IS NULL"
_SESSION_COLUMNS = """s.id, s.path_id, s.title, s.created_at, s.updated_at,
    m.section_id AS material_section_id, m.package_id AS material_package_id, m.title AS material_title,
    m.instructions AS material_instructions, m.paragraphs AS material_paragraphs"""
_SESSION_SUMMARY_COLUMNS = """s.id, s.path_id, s.title, s.created_at, s.updated_at,
    m.section_id AS material_section_id, m.package_id AS material_package_id, m.title AS material_title"""
_SESSION_FROM = "sessions s LEFT JOIN session_materials m ON m.session_id = s.id"


class ActiveTurnConflict(RuntimeError):
    """The session already has a running turn."""


@dataclass(frozen=True)
class TutorSession:
    id: UUID
    path_id: UUID
    title: str
    created_at: datetime
    updated_at: datetime
    # The Reading passage the session was opened on, if any.
    material: SessionMaterial | None = None


@dataclass(frozen=True)
class TutorMessage:
    id: int
    role: str
    content: str
    turn_id: UUID | None
    created_at: datetime
    metadata: dict[str, Any] = field(default_factory=dict)


def _uuid(value: UUID | str | None) -> UUID | None:
    if value is None or isinstance(value, UUID):
        return value
    try:
        return UUID(str(value))
    except ValueError:
        return None


def _session(row: dict[str, Any]) -> TutorSession:
    material = None
    if row.get("material_section_id") is not None:
        # A listing reads the summary only; its material carries no passage text.
        material = SessionMaterial(row["material_section_id"], row["material_package_id"], row["material_title"],
                                   row.get("material_instructions") or "", list(row.get("material_paragraphs") or []))
    return TutorSession(row["id"], row["path_id"], row["title"], row["created_at"], row["updated_at"], material)


class TutorSessionStore:
    def __init__(self, database_url: str) -> None:
        if not database_url.strip():
            raise ValueError("database_url must not be blank")
        self._database_url = database_url

    def _connect(self):
        return closing(psycopg2.connect(self._database_url))

    def create_session(self, user_id: UUID | str, path_id: UUID | str, title: str | None = None,
                       material: SessionMaterial | None = None) -> TutorSession | None:
        """Open a session on one of the learner's own paths; ``None`` when the path is not theirs.

        A Reading passage, when given, is stored in the same transaction as the session.
        """
        user, path = _uuid(user_id), _uuid(path_id)
        if user is None or path is None:
            return None
        with self._connect() as connection, connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                """INSERT INTO sessions (id, user_id, path_id, title, created_at, updated_at)
                   SELECT %s, p.user_id, p.path_id, %s, now(), now()
                   FROM mastery_paths p WHERE p.path_id = %s AND p.user_id = %s
                   RETURNING id, path_id, title, created_at, updated_at""",
                (str(uuid4()), (title or "New session").strip()[:200] or "New session", str(path), str(user)),
            )
            row = cursor.fetchone()
            if row is None:
                return None
            if material is not None:
                cursor.execute(
                    """INSERT INTO session_materials (session_id, material_type, section_id, package_id, title,
                                                      instructions, paragraphs, fetched_at)
                       VALUES (%s, 'READING', %s, %s, %s, %s, %s, now())""",
                    (str(row["id"]), str(material.section_id), str(material.package_id), material.title,
                     material.instructions, Json(material.paragraphs)),
                )
            return TutorSession(row["id"], row["path_id"], row["title"], row["created_at"], row["updated_at"],
                                material)

    def list_sessions(self, user_id: UUID | str, limit: int = 50) -> list[TutorSession]:
        user = _uuid(user_id)
        if user is None or not 1 <= limit <= 100:
            return []
        with self._connect() as connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                f"""SELECT {_SESSION_SUMMARY_COLUMNS} FROM {_SESSION_FROM}
                    WHERE s.user_id = %s AND s.archived_at IS NULL ORDER BY s.updated_at DESC, s.id LIMIT %s""",
                (str(user), limit),
            )
            return [_session(row) for row in cursor.fetchall()]

    def get_session(self, user_id: UUID | str, session_id: UUID | str) -> TutorSession | None:
        user, session = _uuid(user_id), _uuid(session_id)
        if user is None or session is None:
            return None
        with self._connect() as connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                f"""SELECT {_SESSION_COLUMNS} FROM {_SESSION_FROM}
                    WHERE s.id = %s AND s.user_id = %s AND s.archived_at IS NULL""",
                (str(session), str(user)),
            )
            row = cursor.fetchone()
            return _session(row) if row else None

    def archive_session(self, user_id: UUID | str, session_id: UUID | str) -> bool:
        user, session = _uuid(user_id), _uuid(session_id)
        if user is None or session is None:
            return False
        with self._connect() as connection, connection, connection.cursor() as cursor:
            cursor.execute(_OWNED_SESSION + " FOR UPDATE", (str(session), str(user)))
            if cursor.fetchone() is None:
                return False
            cursor.execute("SELECT 1 FROM turns WHERE session_id = %s AND status = 'running'", (str(session),))
            if cursor.fetchone() is not None:
                raise ActiveTurnConflict("Finish the running turn before archiving this session")
            cursor.execute(
                "UPDATE sessions SET archived_at = now() WHERE id = %s AND user_id = %s AND archived_at IS NULL",
                (str(session), str(user)),
            )
            return cursor.rowcount == 1

    def begin_turn(self, user_id: UUID | str, session_id: UUID | str) -> UUID | None:
        """Start a turn; ``None`` when the session is not the learner's, ``ActiveTurnConflict`` if one runs."""
        user, session = _uuid(user_id), _uuid(session_id)
        if user is None or session is None:
            return None
        turn_id = uuid4()
        try:
            with self._connect() as connection, connection, connection.cursor() as cursor:
                cursor.execute(_OWNED_SESSION + " FOR UPDATE", (str(session), str(user)))
                if cursor.fetchone() is None:
                    return None
                cursor.execute("INSERT INTO turns (id, session_id, status, created_at) VALUES (%s, %s, 'running', now())",
                               (str(turn_id), str(session)))
                return turn_id
        except psycopg2.errors.UniqueViolation:
            raise ActiveTurnConflict("This session already has a running turn") from None

    def finish_turn(self, turn_id: UUID | str, status: str, failure_code: str = "") -> bool:
        """Close a running turn; a turn that already finished keeps its outcome."""
        if status not in TURN_OUTCOMES:
            raise ValueError(f"Unsupported turn outcome: {status}")
        turn = _uuid(turn_id)
        if turn is None:
            return False
        with self._connect() as connection, connection, connection.cursor() as cursor:
            cursor.execute(
                """UPDATE turns SET status = %s, failure_code = %s, finished_at = now()
                   WHERE id = %s AND status = 'running'""",
                (status, failure_code[:100], str(turn)),
            )
            return cursor.rowcount == 1

    def add_message(self, user_id: UUID | str, session_id: UUID | str, turn_id: UUID | str | None,
                    role: str, content: str, metadata: dict[str, Any] | None = None) -> int | None:
        if role not in MESSAGE_ROLES:
            raise ValueError(f"Unsupported message role: {role}")
        user, session = _uuid(user_id), _uuid(session_id)
        if user is None or session is None:
            return None
        turn = _uuid(turn_id)
        with self._connect() as connection, connection, connection.cursor() as cursor:
            cursor.execute(
                f"""INSERT INTO messages (session_id, turn_id, role, content, metadata_json, created_at)
                    SELECT id, %s, %s, %s, %s, now() FROM ({_OWNED_SESSION}) owned
                    RETURNING id""",
                (str(turn) if turn else None, role, content, Json(metadata or {}), str(session), str(user)),
            )
            row = cursor.fetchone()
            if row is None:
                return None
            cursor.execute("UPDATE sessions SET updated_at = now() WHERE id = %s", (str(session),))
            return row[0]

    def recent_messages(self, user_id: UUID | str, session_id: UUID | str, limit: int = 20) -> list[TutorMessage]:
        """The latest ``limit`` messages of the learner's session, oldest first."""
        user, session = _uuid(user_id), _uuid(session_id)
        if user is None or session is None or limit < 1:
            return []
        with self._connect() as connection, connection.cursor(cursor_factory=RealDictCursor) as cursor:
            cursor.execute(
                f"""SELECT m.id, m.role, m.content, m.turn_id, m.created_at, m.metadata_json FROM messages m
                    WHERE m.session_id = (SELECT id FROM ({_OWNED_SESSION}) owned)
                    ORDER BY m.id DESC LIMIT %s""",
                (str(session), str(user), limit),
            )
            rows = cursor.fetchall()
        return [TutorMessage(row["id"], row["role"], row["content"], row["turn_id"], row["created_at"],
                             dict(row["metadata_json"] or {})) for row in reversed(rows)]

    def recover_interrupted_turns(self) -> int:
        """Fail every running turn. Called at API start: one instance serves turns, so none can still be alive."""
        with self._connect() as connection, connection, connection.cursor() as cursor:
            cursor.execute(
                """UPDATE turns SET status = 'failed', failure_code = 'interrupted', finished_at = now()
                   WHERE status = 'running'"""
            )
            return cursor.rowcount
