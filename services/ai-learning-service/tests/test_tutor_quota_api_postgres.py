"""Daily tutor turn quota over HTTP + SSE, with a limit of two turns per learner per day.

Kept apart from ``test_tutor_api_postgres.py`` so those tests keep the default limit.
Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run it.
"""

import asyncio
import base64
from datetime import datetime, timezone
import json
import os
import unittest
from unittest.mock import patch
from uuid import uuid4

from fastapi.testclient import TestClient
from psycopg2 import OperationalError

import main
from app.api.dependencies import get_memory_complete, get_path_service, get_quota_store, get_tutor_chat
from app.application.path_service import PathService
from app.config import get_settings
from app.llm.client import ChatReply, LlmApiError
from app.persistence.postgres_learning_store import PostgresLearningStore
from app.usage.quota import DailyQuotaStore
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip
from tests.test_goal_scoped_path import GoalClient
from tests.test_tutor_api_postgres import ISSUER, SECRET_BYTES, sse, token_for
from tests.test_tutor_engine_postgres import CurriculumWithConcept, ScriptedChat, call

TURNS_PER_DAY = 2


class FailingQuota(DailyQuotaStore):
    """Real counting, with one operation failing like a lost database connection."""

    def __init__(self, database_url, failing):
        super().__init__(database_url, "Asia/Ho_Chi_Minh")
        self.failing = failing

    def consume(self, *args):
        if self.failing == "consume":
            raise OperationalError("database unavailable")
        return super().consume(*args)

    def refund(self, *args):
        if self.failing == "refund":
            raise OperationalError("database unavailable")
        return super().refund(*args)


class TutorQuotaApiTest(unittest.TestCase):
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
            "AI_LEARNING_TUTOR_TURNS_PER_DAY": str(TURNS_PER_DAY),
            "AI_LEARNING_MEMORY_SUMMARIES_PER_DAY": "10",
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

        main.app.dependency_overrides = {
            get_path_service: paths,
            get_tutor_chat: lambda: self.chat,
            # Memory summaries never reach a model in these tests.
            get_memory_complete: lambda: None,
        }
        self.client = TestClient(main.app)

    def as_user(self):
        self.current_user = str(uuid4())
        return {"Authorization": f"Bearer {token_for(self.current_user)}"}

    def new_session(self, headers):
        response = self.client.post("/api/ai-learning/tutor/sessions", json={"title": "Study"}, headers=headers)
        self.assertEqual(response.status_code, 201, response.text)
        return response.json()["sessionId"]

    def turn(self, headers, session_id, message="Hi"):
        with self.client.stream("POST", f"/api/ai-learning/tutor/sessions/{session_id}/turns",
                                json={"message": message}, headers=headers) as response:
            return response.status_code, response.headers, response.read().decode()

    def used_turns(self, user_id):
        rows = self.schema.query(
            "SELECT COALESCE(SUM(used), 0) FROM llm_daily_usage WHERE user_id = %s AND kind = 'tutor_turn'",
            (user_id,))
        return int(rows[0][0])

    def test_the_turn_over_the_limit_is_refused_before_streaming_and_before_the_model(self):
        headers = self.as_user()
        user_id = self.current_user
        session_id = self.new_session(headers)
        self.chat = ScriptedChat(ChatReply("One."), ChatReply("Two."), ChatReply("Never sent."))

        for _ in range(TURNS_PER_DAY):
            code, response_headers, body = self.turn(headers, session_id)
            self.assertEqual(code, 200, body)
            self.assertTrue(response_headers["content-type"].startswith("text/event-stream"))
            self.assertEqual(sse(body)[-1][0], "turn.completed")

        with self.assertLogs("app.api.tutor", level="INFO") as logs:
            code, response_headers, body = self.turn(headers, session_id)

        self.assertEqual(code, 429, body)
        self.assertIn("reason=quota", "\n".join(logs.output))
        self.assertTrue(response_headers["content-type"].startswith("application/json"))
        payload = json.loads(body)
        self.assertEqual((payload["detail"], payload["limit"]), ("Daily tutor turn limit reached", TURNS_PER_DAY))
        resets_at = datetime.fromisoformat(payload["resetsAt"].replace("Z", "+00:00"))
        retry_after = int(response_headers["retry-after"])
        self.assertGreater(retry_after, 0)
        self.assertLessEqual(abs((resets_at - datetime.now(timezone.utc)).total_seconds() - retry_after), 5)
        self.assertEqual(len(self.chat.requests), TURNS_PER_DAY)
        self.assertEqual(self.used_turns(user_id), TURNS_PER_DAY)
        self.assertEqual(self.schema.query(
            "SELECT status, failure_code FROM turns WHERE session_id = %s ORDER BY created_at DESC LIMIT 1",
            (session_id,)), [("failed", "quota_exceeded")])

        self.schema.execute("UPDATE llm_daily_usage SET usage_date = usage_date - 1 WHERE user_id = %s", (user_id,))
        code, _headers, body = self.turn(headers, session_id)
        self.assertEqual(code, 200, body)
        self.assertEqual(sse(body)[-1][0], "turn.completed")

    def test_unknown_session_and_running_turn_do_not_spend_the_quota(self):
        headers = self.as_user()
        user_id = self.current_user
        session_id = self.new_session(headers)

        code, _headers, _body = self.turn(headers, str(uuid4()))
        self.assertEqual(code, 404)

        self.schema.execute("INSERT INTO turns (id, session_id, status, created_at) VALUES (%s, %s, 'running', now())",
                            (str(uuid4()), session_id))
        code, _headers, _body = self.turn(headers, session_id)
        self.assertEqual(code, 409)

        self.assertEqual(self.used_turns(user_id), 0)

    def test_usage_reports_the_learners_own_counts(self):
        first = self.as_user()
        session_id = self.new_session(first)
        self.chat = ScriptedChat(ChatReply("Hello."))
        self.assertEqual(self.turn(first, session_id)[0], 200)
        second = self.as_user()

        mine = self.client.get("/api/ai-learning/tutor/usage", headers=first)
        theirs = self.client.get("/api/ai-learning/tutor/usage", headers=second)

        self.assertEqual(mine.status_code, 200, mine.text)
        body = mine.json()
        self.assertEqual(body["timezone"], "Asia/Ho_Chi_Minh")
        self.assertEqual(body["tutorTurns"], {"used": 1, "limit": TURNS_PER_DAY})
        self.assertEqual(body["memorySummaries"], {"used": 0, "limit": 10})
        self.assertGreater(datetime.fromisoformat(body["resetsAt"].replace("Z", "+00:00")),
                           datetime.now(timezone.utc))
        self.assertEqual(theirs.json()["tutorTurns"], {"used": 0, "limit": TURNS_PER_DAY})
        self.assertEqual(self.client.get("/api/ai-learning/tutor/usage").status_code, 401)

    def test_failures_before_the_model_answered_are_refunded_and_other_failures_are_counted(self):
        cases = [
            (ScriptedChat(LlmApiError(500)), "llm_error", 0),
            (None, "llm_not_configured", 0),
            # The first round was answered, and billed, before the provider failed.
            (ScriptedChat(ChatReply("", (call("mastery_status"),)), LlmApiError(500)), "llm_error", 1),
            (ScriptedChat(*[ChatReply("", (call("mastery_status"),)) for _ in range(6)]), "too_many_rounds", 1),
        ]
        for chat, failure_code, expected_used in cases:
            with self.subTest(failure_code=failure_code, expected_used=expected_used):
                headers = self.as_user()
                user_id = self.current_user
                session_id = self.new_session(headers)
                self.chat = chat

                if expected_used:
                    with self.assertNoLogs("app.api.tutor", level="INFO"):
                        code, _headers, body = self.turn(headers, session_id)
                else:
                    with self.assertLogs("app.api.tutor", level="INFO") as logs:
                        code, _headers, body = self.turn(headers, session_id)
                    self.assertIn(f"Tutor turn refunded session={session_id}", "\n".join(logs.output))
                    self.assertIn(f"code={failure_code}", "\n".join(logs.output))

                self.assertEqual(code, 200, body)
                self.assertEqual(sse(body)[-1], ("turn.failed", {"turnId": sse(body)[0][1]["turnId"],
                                                                 "failureCode": failure_code}))
                self.assertEqual(self.used_turns(user_id), expected_used)

    def test_startup_refuses_a_time_zone_postgresql_does_not_know(self):
        async def start():
            async with main.lifespan(main.app):
                pass

        with patch.dict(os.environ, {"AI_LEARNING_QUOTA_TIMEZONE": "Mars/Olympus_Mons"}):
            get_settings.cache_clear()
            try:
                with self.assertRaises(ValueError):
                    asyncio.run(start())
            finally:
                get_settings.cache_clear()

    def test_a_counting_failure_frees_the_session_for_the_next_turn(self):
        headers = self.as_user()
        session_id = self.new_session(headers)
        main.app.dependency_overrides[get_quota_store] = lambda: FailingQuota(self.schema.url, "consume")

        code, _headers, _body = self.turn(headers, session_id)

        self.assertEqual(code, 503)
        self.assertEqual(self.schema.query("SELECT status, failure_code FROM turns WHERE session_id = %s",
                                           (session_id,)), [("failed", "internal_error")])
        del main.app.dependency_overrides[get_quota_store]
        self.chat = ScriptedChat(ChatReply("Back."))
        code, _headers, body = self.turn(headers, session_id)
        self.assertEqual(code, 200, body)
        self.assertEqual(sse(body)[-1][0], "turn.completed")

    def test_a_lost_refund_still_reports_the_failed_turn_and_logs_only_the_error_type(self):
        headers = self.as_user()
        user_id = self.current_user
        session_id = self.new_session(headers)
        main.app.dependency_overrides[get_quota_store] = lambda: FailingQuota(self.schema.url, "refund")
        self.chat = None

        with self.assertLogs("app.api.tutor", level="ERROR") as logs:
            code, _headers, body = self.turn(headers, session_id, "private message")

        self.assertEqual(code, 200, body)
        self.assertEqual(sse(body)[-1][1]["failureCode"], "llm_not_configured")
        self.assertEqual(self.used_turns(user_id), 1)
        self.assertEqual(logs.output, ["ERROR:app.api.tutor:Tutor turn refund failed error_type=OperationalError"])


if __name__ == "__main__":
    unittest.main()
