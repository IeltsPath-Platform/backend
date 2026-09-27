"""Practice notebook behavior against PostgreSQL; no external model or service is called."""

import asyncio
import base64
from datetime import datetime, timedelta, timezone
import json
import os
import unittest
from unittest.mock import patch
from uuid import uuid4

from fastapi.testclient import TestClient
from jose import jwt

import main
from app.api.dependencies import get_practice_store
from app.application.path_service import PathService
from app.config import get_settings
from app.mastery.service import LearningService
from app.persistence.postgres_learning_store import PostgresLearningStore
from app.practice.store import PracticeStore
from app.security.internal_jwt import AuthenticatedUser, bearer_scheme, require_current_user
from app.tutor.session_store import TutorSessionStore
from app.tutor.tools import TutorTools
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip
from tests.test_goal_scoped_path import GoalClient, KP_BASIC, KP_ADVANCED, TOPIC_BASIC, TOPIC_ADVANCED

ISSUER = "urn:code-base:api-gateway"
SECRET_BYTES = b"practice-api-test-secret-at-least-32-bytes!"


def token_for(user_id: str) -> str:
    claims = {"iss": ISSUER, "sub": user_id, "roles": ["CUSTOMER"],
              "exp": datetime.now(timezone.utc) + timedelta(minutes=5)}
    return jwt.encode(claims, SECRET_BYTES, algorithm="HS256")


class PracticeCurriculum:
    async def get_curriculum(self, _bearer_token):
        topics = [
            {"id": TOPIC_BASIC, "name": "Basic", "sortOrder": 0, "status": "ACTIVE"},
            {"id": TOPIC_ADVANCED, "name": "Advanced", "sortOrder": 1, "status": "ACTIVE"},
        ]
        points = [
            {"id": KP_BASIC, "topicId": TOPIC_BASIC, "name": "Basic KP", "learningType": "PROCEDURE",
             "status": "ACTIVE", "effectiveBandMin": 4.0, "effectiveBandMax": 5.0,
             "skill": "writing", "description": "Use the correct verb form."},
            {"id": KP_ADVANCED, "topicId": TOPIC_ADVANCED, "name": "Advanced KP", "learningType": "CONCEPT",
             "status": "ACTIVE", "effectiveBandMin": 4.0, "effectiveBandMax": 5.0,
             "skill": "reading", "description": "Identify the writer's claim."},
        ]
        return topics, points


def question(*, expected="went", question="Complete the sentence.", explanation="Use the past tense.",
             question_type="short", options=None, difficulty="easy"):
    item = {"question": question, "question_type": question_type, "expected_answer": expected,
            "explanation": explanation, "difficulty": difficulty}
    if options is not None:
        item["options"] = options
    return item


class PracticePostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()
        cls.env = patch.dict(os.environ, {
            "AI_LEARNING_DATABASE_URL": cls.schema.url,
            "AI_LEARNING_INTERNAL_JWT_SECRET": base64.b64encode(SECRET_BYTES).decode(),
            "AI_LEARNING_INTERNAL_JWT_ISSUER": ISSUER,
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
        self.learning_store = PostgresLearningStore(self.schema.url)
        self.practice = PracticeStore(self.schema.url)
        self.sessions = TutorSessionStore(self.schema.url)
        main.app.dependency_overrides = {get_practice_store: lambda: self.practice}
        self.client = TestClient(main.app)

    def learner(self, user_id=None):
        user_id, goal_id = user_id or str(uuid4()), str(uuid4())
        goal = {"id": goal_id, "userId": user_id, "status": "ACTIVE", "targetBand": 5.5}
        paths = PathService(self.learning_store, GoalClient(goal), PracticeCurriculum())
        path_id, _progress = asyncio.run(paths.ensure_active_path(user_id, "internal-token"))
        session = self.sessions.create_session(user_id, path_id, "Practice")
        turn_id = str(uuid4())
        self.schema.execute(
            "INSERT INTO turns (id, session_id, status, created_at) VALUES (%s, %s, 'running', now())",
            (turn_id, str(session.id)),
        )
        tools = TutorTools(LearningService(self.learning_store), path_id, session_id=session.id, turn_id=turn_id,
                           user_id=user_id, practice=self.practice)
        return user_id, path_id, session, turn_id, tools

    def headers(self, user_id):
        return {"Authorization": f"Bearer {token_for(user_id)}"}

    def pose(self, tools, kp_id=KP_BASIC, questions=None):
        return tools.execute("practice_questions", {
            "knowledge_point_id": kp_id,
            "questions": questions if questions is not None else [question()],
        })

    def test_knowledge_point_details_returns_the_path_snapshot_and_rejects_other_points(self):
        user_id, _path_id, _session, _turn_id, tools = self.learner()

        outcome = tools.execute("knowledge_point_details", {"knowledge_point_id": KP_BASIC})
        unknown = tools.execute("knowledge_point_details", {"knowledge_point_id": str(uuid4())})

        self.assertEqual({key: outcome.result[key] for key in (
            "id", "name", "type", "module_id", "module_name", "skill", "description")}, {
            "id": KP_BASIC, "name": "Basic KP", "type": "procedure", "module_id": TOPIC_BASIC,
            "module_name": "Basic", "skill": "writing", "description": "Use the correct verb form.",
        })
        self.assertEqual((float(outcome.result["band_min"]), float(outcome.result["band_max"])), (4.0, 5.0))
        self.assertIn("error", unknown.result)

    def test_practice_tool_persists_batch_without_leaking_answers_or_explanations(self):
        user_id, path_id, session, turn_id, tools = self.learner()
        questions = [
            question(expected="went-secret", explanation="explanation-secret"),
            question(expected="B", question="Select the claim.", question_type="choice",
                     options=[{"label": "A", "body": "A detail"}, {"label": "B", "body": "The claim"}],
                     explanation="choice-explanation-secret"),
        ]

        outcome = self.pose(tools, questions=questions)
        serialized = json.dumps({"result": outcome.result, "events": outcome.events})
        rows = self.schema.query(
            "SELECT user_id::text, session_id::text, turn_id::text, mastery_path_id::text, "
            "knowledge_point_id, knowledge_point_name, question, correct_answer, explanation, answered_at "
            "FROM notebook_entries WHERE session_id = %s ORDER BY id", (str(session.id),))

        self.assertEqual(outcome.result["status"], "posed")
        self.assertEqual(len(outcome.result["entry_ids"]), 2)
        self.assertTrue(outcome.ends_turn)
        self.assertEqual(len(outcome.events), 1)
        self.assertEqual(outcome.events[0][0], "practice.questions")
        self.assertNotIn("went-secret", serialized)
        self.assertNotIn("explanation-secret", serialized)
        self.assertNotIn("choice-explanation-secret", serialized)
        self.assertEqual(len(rows), 2)
        self.assertTrue(all(row[:5] == (user_id, session.id, turn_id, path_id, KP_BASIC) for row in rows))
        self.assertTrue(all(row[5] == "Basic KP" and row[9] is None for row in rows))
        self.assertNotIn("expected_answer", json.dumps(outcome.events))
        self.assertNotIn("correct_answer", json.dumps(outcome.events))
        self.assertNotIn("explanation", json.dumps(outcome.events))

    def test_invalid_practice_batch_is_atomic_and_enforces_path_membership(self):
        user_id, _path_id, session, _turn_id, tools = self.learner()
        invalid_batches = [
            [question(explanation="")],
            [question(question_type="open")],
            [question(question_type="choice", options=[{"label": "A", "body": "One"},
                                                         {"label": "C", "body": "Two"}])],
            [question(), question(question_type="open")],
        ]
        outcomes = [self.pose(tools, questions=batch) for batch in invalid_batches]
        unknown_point = self.pose(tools, kp_id=str(uuid4()))
        too_many = self.pose(tools, questions=[question(question=f"Q{i}") for i in range(6)])

        self.assertTrue(all("error" in outcome.result for outcome in outcomes))
        self.assertIn("error", unknown_point.result)
        self.assertIn("error", too_many.result)
        self.assertEqual(self.schema.query("SELECT count(*) FROM notebook_entries WHERE session_id = %s",
                                           (str(session.id),)), [(0,)])

    def test_practice_can_target_a_non_objective_concept_knowledge_point(self):
        _user_id, _path_id, session, _turn_id, tools = self.learner()

        outcome = self.pose(tools, kp_id=KP_ADVANCED)

        self.assertEqual(outcome.result["status"], "posed")
        self.assertEqual(self.schema.query(
            "SELECT knowledge_point_id FROM notebook_entries WHERE session_id = %s", (str(session.id),)),
            [(KP_ADVANCED,)])

    def test_answer_endpoint_filters_notebook_and_never_returns_unanswered_key(self):
        user_id, _path_id, session, _turn_id, tools = self.learner()
        outcome = self.pose(tools, questions=[question(expected="went", explanation="Past tense."),
                                               question(expected="B", question_type="choice",
                                                        options=[{"label": "A", "body": "A"},
                                                                 {"label": "B", "body": "B"}],
                                                        explanation="Choice explanation.")])
        headers = self.headers(user_id)

        notebook = self.client.get("/api/ai-learning/practice/notebook", headers=headers)
        self.assertEqual(notebook.status_code, 200, notebook.text)
        self.assertEqual(len(notebook.json()), 2)
        question_ids = {entry["entryId"]: entry["questionId"] for entry in notebook.json()}
        self.assertTrue(all("correctAnswer" not in entry and "explanation" not in entry
                            for entry in notebook.json()))
        first_id, second_id = outcome.result["entry_ids"]

        answered = self.client.post(f"/api/ai-learning/practice/entries/{first_id}/answer",
                                    json={"answer": "went"}, headers=headers)
        repeated = self.client.post(f"/api/ai-learning/practice/entries/{first_id}/answer",
                                    json={"answer": "went"}, headers=headers)
        self.assertEqual(answered.status_code, 200, answered.text)
        self.assertEqual(answered.json()["questionId"], question_ids[first_id])
        self.assertTrue(answered.json()["isCorrect"])
        self.assertEqual(answered.json()["explanation"], "Past tense.")
        self.assertEqual(repeated.status_code, 409)
        self.assertEqual(self.schema.query("SELECT count(*) FROM practice_review_state WHERE entry_id = %s",
                                           (first_id,)), [(0,)])

        wrong = self.client.post(f"/api/ai-learning/practice/entries/{second_id}/answer",
                                 json={"answer": "A"}, headers=headers)
        self.assertEqual(wrong.status_code, 200, wrong.text)
        self.assertFalse(wrong.json()["isCorrect"])
        due_at = self.schema.query("SELECT due_at FROM practice_review_state WHERE entry_id = %s", (second_id,))[0][0]
        self.assertAlmostEqual((due_at - datetime.now(timezone.utc)).total_seconds(), 600, delta=10)

        by_kp = self.client.get("/api/ai-learning/practice/notebook", params={"knowledgePointId": KP_BASIC},
                                headers=headers)
        by_session = self.client.get("/api/ai-learning/practice/notebook", params={"sessionId": session.id},
                                     headers=headers)
        open_only = self.client.get("/api/ai-learning/practice/notebook", params={"status": "open"},
                                    headers=headers)
        incorrect_only = self.client.get("/api/ai-learning/practice/notebook", params={"status": "incorrect"},
                                         headers=headers)
        self.assertEqual((len(by_kp.json()), len(by_session.json())), (2, 2))
        self.assertEqual([entry["entryId"] for entry in open_only.json()], [])
        self.assertEqual([entry["entryId"] for entry in incorrect_only.json()], [second_id])

    def test_due_review_replay_conflict_streak_resolution_and_mastery_isolation(self):
        user_id, path_id, session, _turn_id, tools = self.learner()
        outcome = self.pose(tools, questions=[question(expected="correct", explanation="Explain one."),
                                               question(expected="right", explanation="Explain two."),
                                               question(expected="yes", explanation="Explain three.")])
        first_id, second_id, third_id = outcome.result["entry_ids"]
        headers = self.headers(user_id)
        mastery_before = self.schema.query(
            "SELECT revision, state_json FROM mastery_paths WHERE path_id = %s", (path_id,))
        evidence_before = self.schema.query(
            "SELECT * FROM mastery_learning_evidence WHERE path_id = %s ORDER BY ordinal", (path_id,))

        for entry_id in (first_id, second_id, third_id):
            response = self.client.post(f"/api/ai-learning/practice/entries/{entry_id}/answer",
                                        json={"answer": "wrong"}, headers=headers)
            self.assertEqual(response.status_code, 200, response.text)
        self.schema.execute("UPDATE practice_review_state SET due_at = now() - interval '1 second' "
                            "WHERE entry_id = ANY(%s)", ([first_id, second_id, third_id],))

        due = self.client.get("/api/ai-learning/practice/due", headers=headers)
        self.assertEqual({entry["entryId"] for entry in due.json()}, {first_id, second_id, third_id})
        request_id = str(uuid4())
        review_body = {"requestId": request_id, "entryId": first_id, "answer": "wrong", "rating": "easy"}
        again = self.client.post("/api/ai-learning/practice/reviews", json=review_body, headers=headers)
        replay = self.client.post("/api/ai-learning/practice/reviews", json=review_body, headers=headers)
        self.assertEqual(again.status_code, 200, again.text)
        self.assertEqual(replay.status_code, 200, replay.text)
        first_question_id = next(entry["questionId"] for entry in self.client.get(
            "/api/ai-learning/practice/notebook", headers=headers).json() if entry["entryId"] == first_id)
        self.assertEqual(again.json()["questionId"], first_question_id)
        self.assertEqual(replay.json(), again.json())
        self.assertEqual(again.json()["rating"], "again")
        self.assertEqual(self.schema.query("SELECT review_count FROM practice_review_state WHERE entry_id = %s",
                                           (first_id,)), [(1,)])
        conflicting = self.client.post("/api/ai-learning/practice/reviews",
                                       json={**review_body, "entryId": second_id}, headers=headers)
        self.assertEqual(conflicting.status_code, 409)

        for _ in range(3):
            self.schema.execute("UPDATE practice_review_state SET due_at = %s WHERE entry_id = %s",
                                (datetime.now(timezone.utc) - timedelta(seconds=1), second_id))
            response = self.client.post("/api/ai-learning/practice/reviews", json={
                "requestId": str(uuid4()), "entryId": second_id, "answer": "right", "rating": "good",
            }, headers=headers)
            self.assertEqual(response.status_code, 200, response.text)
            self.assertTrue(response.json()["isCorrect"])
        resolved = self.schema.query("SELECT resolved FROM notebook_entries WHERE id = %s", (second_id,))
        self.assertEqual(resolved, [(True,)])
        self.assertNotIn(second_id, {entry["entryId"] for entry in self.client.get(
            "/api/ai-learning/practice/due", headers=headers).json()})
        # Do the third entry's review with an explicit hard rating to exercise the correct-answer path.
        correct_review = self.client.post("/api/ai-learning/practice/reviews", json={
            "requestId": str(uuid4()), "entryId": third_id, "answer": "yes", "rating": "hard",
        }, headers=headers)
        self.assertEqual(correct_review.status_code, 200, correct_review.text)
        self.assertEqual(correct_review.json()["rating"], "hard")
        self.assertEqual(self.schema.query("SELECT revision, state_json FROM mastery_paths WHERE path_id = %s",
                                           (path_id,)),
                         mastery_before)
        self.assertEqual(self.schema.query(
            "SELECT * FROM mastery_learning_evidence WHERE path_id = %s ORDER BY ordinal", (path_id,)), evidence_before)

    def test_choice_answers_resolve_like_mastery_cards_and_reviews_keep_the_first_answer(self):
        user_id, _path_id, _session, _turn_id, tools = self.learner()
        options = [{"label": "A", "body": "has gone"}, {"label": "B", "body": "went"}]
        choice = question(expected="B", question_type="choice", options=options, explanation="Past time.")
        body_id, prefixed_id, wrong_id = self.pose(tools, questions=[choice, choice, choice]).result["entry_ids"]
        headers = self.headers(user_id)

        answers = {body_id: "went", prefixed_id: "B. went", wrong_id: "has gone"}
        results = {entry_id: self.client.post(f"/api/ai-learning/practice/entries/{entry_id}/answer",
                                              json={"answer": answer}, headers=headers)
                   for entry_id, answer in answers.items()}
        self.assertTrue(all(response.status_code == 200 for response in results.values()))
        self.assertEqual({entry_id: response.json()["isCorrect"] for entry_id, response in results.items()},
                         {body_id: True, prefixed_id: True, wrong_id: False})

        self.schema.execute("UPDATE practice_review_state SET due_at = now() - interval '1 second' "
                            "WHERE entry_id = %s", (wrong_id,))
        reviewed = self.client.post("/api/ai-learning/practice/reviews", json={
            "requestId": str(uuid4()), "entryId": wrong_id, "answer": "went", "rating": "good",
        }, headers=headers)
        self.assertEqual(reviewed.status_code, 200, reviewed.text)
        self.assertEqual((reviewed.json()["isCorrect"], reviewed.json()["rating"]), (True, "good"))
        # A correct review is recorded as a review event; the notebook keeps the first attempt.
        self.assertEqual(self.schema.query("SELECT user_answer, result, is_correct, resolved FROM notebook_entries "
                                           "WHERE id = %s", (wrong_id,)),
                         [("has gone", "incorrect", False, False)])
        incorrect = self.client.get("/api/ai-learning/practice/notebook", params={"status": "incorrect"},
                                    headers=headers)
        self.assertEqual([entry["entryId"] for entry in incorrect.json()], [wrong_id])
        self.assertEqual(self.schema.query("SELECT answer, rating FROM practice_review_events WHERE entry_id = %s",
                                           (wrong_id,)), [("went", "good")])

    def test_an_archived_session_rejects_practice_without_failing_the_tool_call(self):
        user_id, _path_id, session, turn_id, tools = self.learner()
        # A session with a running turn cannot be archived, so close the turn first.
        self.schema.execute("UPDATE turns SET status = 'completed', finished_at = now() WHERE id = %s", (turn_id,))
        self.assertTrue(self.sessions.archive_session(user_id, session.id))

        outcome = self.pose(tools)

        self.assertIn("error", outcome.result)
        self.assertFalse(outcome.ends_turn)
        self.assertEqual(self.schema.query("SELECT count(*) FROM notebook_entries WHERE session_id = %s",
                                           (str(session.id),)), [(0,)])

    def test_another_learner_cannot_read_or_change_entries(self):
        owner, _path, _session, _turn, tools = self.learner()
        entry_id = self.pose(tools).result["entry_ids"][0]
        other = str(uuid4())
        headers = self.headers(other)

        self.assertEqual(self.client.get("/api/ai-learning/practice/notebook", headers=headers).json(), [])
        self.assertEqual(self.client.get("/api/ai-learning/practice/due", headers=headers).json(), [])
        answer = self.client.post(f"/api/ai-learning/practice/entries/{entry_id}/answer",
                                  json={"answer": "went"}, headers=headers)
        review_response = self.client.post("/api/ai-learning/practice/reviews", json={
            "requestId": str(uuid4()), "entryId": entry_id, "answer": "wrong",
        }, headers=headers)
        self.assertEqual((answer.status_code, review_response.status_code), (404, 404))
        self.assertEqual(self.schema.query("SELECT answered_at FROM notebook_entries WHERE id = %s", (entry_id,)),
                         [(None,)])
        self.assertNotEqual(owner, other)


if __name__ == "__main__":
    unittest.main()
