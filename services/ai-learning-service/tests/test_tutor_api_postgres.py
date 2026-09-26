"""Tutor HTTP + SSE API against PostgreSQL, with real internal JWTs and a scripted model.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run it.
"""

import asyncio
import base64
from datetime import datetime, timedelta, timezone
import json
import logging
import os
import unittest
from unittest.mock import patch
from uuid import uuid4

from fastapi.testclient import TestClient
from jose import jwt
from psycopg2 import OperationalError

import main
from app.api.dependencies import get_path_service, get_tutor_chat
from app.application.path_service import PathService
from app.config import get_settings
from app.llm.client import ChatReply
from app.persistence.postgres_learning_store import PostgresLearningStore
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip
from tests.test_goal_scoped_path import GoalClient, KP_BASIC
from tests.test_tutor_engine_postgres import CurriculumWithConcept, ScriptedChat, call

ISSUER = "urn:code-base:api-gateway"
SECRET_BYTES = b"tutor-api-test-secret-at-least-32-bytes!"


def token_for(user_id: str) -> str:
    claims = {"iss": ISSUER, "sub": user_id, "roles": ["CUSTOMER"],
              "exp": datetime.now(timezone.utc) + timedelta(minutes=5)}
    return jwt.encode(claims, SECRET_BYTES, algorithm="HS256")


def sse(body: str) -> list[tuple[str, dict]]:
    events = []
    for block in body.strip().split("\n\n"):
        lines = [line for line in block.splitlines() if not line.startswith(":")]
        if lines:
            kind = next(line[7:] for line in lines if line.startswith("event: "))
            data = json.loads(next(line[6:] for line in lines if line.startswith("data: ")))
            events.append((kind, data))
    return events


class TutorApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()
        cls.env = patch.dict(os.environ, {
            "AI_LEARNING_DATABASE_URL": cls.schema.url,
            "AI_LEARNING_INTERNAL_JWT_SECRET": base64.b64encode(SECRET_BYTES).decode(),
            "AI_LEARNING_INTERNAL_JWT_ISSUER": ISSUER,
            "AI_LEARNING_USER_SERVICE_BASE_URL": "http://127.0.0.1:9",
            "AI_LEARNING_CONTENT_SERVICE_BASE_URL": "http://127.0.0.1:9",
        })
        cls.env.start()
        get_settings.cache_clear()

    @classmethod
    def tearDownClass(cls):
        main.app.dependency_overrides = {}
        if hasattr(cls, "env"):
            cls.env.stop()
        get_settings.cache_clear()
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def setUp(self):
        self.goals = {}
        self.chat = None

        def paths():
            user_id = self.current_user
            goal = self.goals.setdefault(user_id, {"id": str(uuid4()), "userId": user_id, "status": "ACTIVE",
                                                   "targetBand": 5.5})
            return PathService(PostgresLearningStore(self.schema.url), GoalClient(goal), CurriculumWithConcept())

        main.app.dependency_overrides = {get_path_service: paths, get_tutor_chat: lambda: self.chat}
        self.client = TestClient(main.app)

    def as_user(self, user_id=None):
        self.current_user = user_id or str(uuid4())
        return {"Authorization": f"Bearer {token_for(self.current_user)}"}

    def new_session(self, headers):
        response = self.client.post("/api/ai-learning/tutor/sessions", json={"title": "Study"}, headers=headers)
        self.assertEqual(response.status_code, 201, response.text)
        return response.json()["sessionId"]

    def turn(self, headers, session_id, body):
        with self.client.stream("POST", f"/api/ai-learning/tutor/sessions/{session_id}/turns",
                                json=body, headers=headers) as response:
            return response.status_code, response.headers.get("content-type", ""), response.read().decode()

    def test_a_study_cycle_over_http_and_sse(self):
        headers = self.as_user()
        session_id = self.new_session(headers)

        self.chat = ScriptedChat(ChatReply("Try this one.", (call(
            "mastery_quiz", knowledge_point_id=KP_BASIC, question="Which is correct?", question_type="choice",
            options=[{"label": "A", "body": "has went"}, {"label": "B", "body": "has gone"}],
            expected_answer="B", explanation="Past participle."),)))
        code, content_type, body = self.turn(headers, session_id, {"message": "Teach me"})
        self.assertEqual(code, 200)
        self.assertTrue(content_type.startswith("text/event-stream"))
        events = sse(body)
        self.assertEqual([kind for kind, _data in events],
                         ["turn.started", "assistant.message", "tool.called", "question", "turn.completed"])
        question = dict(events)["question"]
        self.assertIn("questionId", question)
        self.assertNotIn("expectedAnswer", body)

        detail = self.client.get(f"/api/ai-learning/tutor/sessions/{session_id}", headers=headers).json()
        self.assertEqual(detail["pendingQuestion"]["questionId"], question["questionId"])
        self.assertEqual(detail["pendingQuestion"]["status"], "awaiting_input")
        self.assertEqual([message["role"] for message in detail["messages"]], ["user", "assistant"])

        self.chat = ScriptedChat(ChatReply("", (call("mastery_grade", answer="B"),)), ChatReply("Correct!"))
        _code, _type, body = self.turn(headers, session_id,
                                       {"answer": {"questionId": question["questionId"], "text": "B"}})
        grading = dict(sse(body))["grading"]
        self.assertEqual((grading["isCorrect"], grading["knowledgePointId"]), (True, KP_BASIC))
        detail = self.client.get(f"/api/ai-learning/tutor/sessions/{session_id}", headers=headers).json()
        self.assertIsNone(detail["pendingQuestion"])

    def test_authentication_and_ownership(self):
        owner = self.as_user()
        session_id = self.new_session(owner)
        self.assertEqual(self.client.get("/api/ai-learning/tutor/sessions").status_code, 401)

        other = self.as_user()
        self.assertEqual(self.client.get(f"/api/ai-learning/tutor/sessions/{session_id}", headers=other).status_code, 404)
        self.assertEqual(self.client.delete(f"/api/ai-learning/tutor/sessions/{session_id}", headers=other).status_code,
                         404)
        code, _type, _body = self.turn(other, session_id, {"message": "hi"})
        self.assertEqual(code, 404)
        self.assertEqual(self.client.get("/api/ai-learning/tutor/sessions", headers=other).json(), [])

    def test_invalid_turn_bodies_are_rejected_before_streaming(self):
        headers = self.as_user()
        session_id = self.new_session(headers)
        for body in ({}, {"message": "hi", "answer": {"questionId": str(uuid4()), "text": "A"}},
                     {"message": "x" * 4001}):
            with self.subTest(body=list(body)):
                code, _type, _body = self.turn(headers, session_id, body)
                self.assertEqual(code, 422)

    def test_a_second_turn_while_one_runs_is_a_conflict(self):
        headers = self.as_user()
        session_id = self.new_session(headers)
        self.schema.execute("INSERT INTO turns (id, session_id, status, created_at) VALUES (%s, %s, 'running', now())",
                            (str(uuid4()), session_id))
        code, _type, _body = self.turn(headers, session_id, {"message": "hi"})
        self.assertEqual(code, 409)
        self.assertEqual(self.client.delete(f"/api/ai-learning/tutor/sessions/{session_id}",
                                            headers=headers).status_code, 409)

    def test_archive_and_no_active_goal(self):
        headers = self.as_user()
        session_id = self.new_session(headers)
        self.assertEqual(self.client.delete(f"/api/ai-learning/tutor/sessions/{session_id}", headers=headers).status_code,
                         204)
        self.assertEqual(self.client.get("/api/ai-learning/tutor/sessions", headers=headers).json(), [])

        headers = self.as_user()
        self.goals[self.current_user] = {"id": str(uuid4()), "userId": self.current_user, "status": "COMPLETED",
                                         "targetBand": 5.5}
        response = self.client.post("/api/ai-learning/tutor/sessions", headers=headers)
        self.assertEqual(response.status_code, 409, response.text)

    def test_unconfigured_llm_fails_the_turn_without_leaking_text(self):
        headers = self.as_user()
        session_id = self.new_session(headers)
        self.chat = None
        with self.assertLogs("app.tutor.engine", level="INFO") as logs:
            _code, _type, body = self.turn(headers, session_id, {"message": "private message"})
        self.assertEqual(sse(body)[-1], ("turn.failed", {"turnId": sse(body)[0][1]["turnId"],
                                                         "failureCode": "llm_not_configured"}))
        self.assertNotIn("private message", "\n".join(logs.output))

    def test_startup_waits_for_turn_recovery(self):
        async def start():
            async with main.lifespan(main.app):
                pass

        with patch.object(main.TutorSessionStore, "recover_interrupted_turns",
                          side_effect=OperationalError("database unavailable")):
            with self.assertRaises(OperationalError):
                asyncio.run(start())


if __name__ == "__main__":
    logging.basicConfig()
    unittest.main()
