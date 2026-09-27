"""Study/review tutor turns against PostgreSQL, with a scripted model in place of the LLM.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run it.
"""

import asyncio
import json
import unittest
from uuid import uuid4

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_service import PathService
from app.llm.client import ChatReply, LlmApiError, ToolCall
from app.persistence.postgres_learning_store import PostgresLearningStore
from app.practice.store import PracticeStore
from app.api.tutor_sse import stream_events
from app.tutor.engine import CardAnswer, SessionNotFound, TutorEngine
from app.tutor.session_store import ActiveTurnConflict, TutorSessionStore
from tests.formal_assessment_support import event as formal_event, item, mapping
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip
from tests.test_goal_scoped_path import GoalClient, KP_BASIC, TOPIC_BASIC

KP_CONCEPT = "c5b2641f-28c8-467d-9d64-f52c8bdc1503"
TOPIC_SECOND = "c5b2641f-28c8-467d-9d64-f52c8bdc1504"
KP_SECOND = "c5b2641f-28c8-467d-9d64-f52c8bdc1505"


class CurriculumWithConcept:
    async def get_curriculum(self, _bearer_token):
        topics = [{"id": TOPIC_BASIC, "name": "Basic", "sortOrder": 0, "status": "ACTIVE"}]
        points = [
            {"id": KP_BASIC, "topicId": TOPIC_BASIC, "name": "Basic KP", "learningType": "PROCEDURE",
             "status": "ACTIVE", "effectiveBandMin": 4.0, "effectiveBandMax": 5.0},
            {"id": KP_CONCEPT, "topicId": TOPIC_BASIC, "name": "Concept KP", "learningType": "CONCEPT",
             "status": "ACTIVE", "effectiveBandMin": 4.0, "effectiveBandMax": 5.0},
        ]
        return topics, points


class ConceptOnlyCurriculum(CurriculumWithConcept):
    async def get_curriculum(self, bearer_token):
        topics, points = await super().get_curriculum(bearer_token)
        return topics, [point for point in points if point["id"] == KP_CONCEPT]


class TwoModuleCurriculum(CurriculumWithConcept):
    async def get_curriculum(self, bearer_token):
        topics, points = await super().get_curriculum(bearer_token)
        topics.append({"id": TOPIC_SECOND, "name": "Second", "sortOrder": 1, "status": "ACTIVE"})
        points.append({"id": KP_SECOND, "topicId": TOPIC_SECOND, "name": "Second KP",
                       "learningType": "PROCEDURE", "status": "ACTIVE",
                       "effectiveBandMin": 4.0, "effectiveBandMax": 5.0})
        return topics, points


def call(name, **arguments):
    raw = {"id": f"call-{uuid4().hex[:8]}", "type": "function",
           "function": {"name": name, "arguments": json.dumps(arguments)}}
    return ToolCall(raw["id"], name, arguments, raw)


class ScriptedChat:
    """Replies in order and records every request's messages."""

    def __init__(self, *replies):
        self.replies = list(replies)
        self.requests = []

    async def __call__(self, messages, *, tools=None):
        self.requests.append({"messages": [dict(message) for message in messages],
                              "tools": [tool["function"]["name"] for tool in tools or []]})
        reply = self.replies.pop(0)
        if isinstance(reply, Exception):
            raise reply
        return reply


class TutorEngineTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()
        cls.learning_store = PostgresLearningStore(cls.schema.url)
        cls.sessions = TutorSessionStore(cls.schema.url)
        cls.engine = TutorEngine(cls.sessions, cls.learning_store)

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def learner(self, curriculum=None):
        user_id, goal_id = str(uuid4()), str(uuid4())
        goal = {"id": goal_id, "userId": user_id, "status": "ACTIVE", "targetBand": 5.5}
        paths = PathService(self.learning_store, GoalClient(goal), curriculum or CurriculumWithConcept())
        path_id, _progress = asyncio.run(paths.ensure_active_path(user_id, "internal-token"))
        session = self.sessions.create_session(user_id, path_id, "Study")
        return user_id, goal_id, paths, session

    def turn(self, user_id, session, chat, **kwargs):
        async def run():
            opened = await self.engine.open_turn(user_id, session.id)
            return [event async for event in self.engine.run(opened, chat, **kwargs)]
        return asyncio.run(run())

    def status_of(self, request):
        context = next(message["content"] for message in request["messages"]
                       if message["role"] == "system" and message["content"].startswith("Current status"))
        return json.loads(context.split("\n", 1)[1])

    def test_quiz_then_answer_grades_the_objective(self):
        user_id, _goal_id, _paths, session = self.learner()
        quiz = ScriptedChat(ChatReply("Let's check what you know.", (call(
            "mastery_quiz", knowledge_point_id=KP_BASIC, question="Pick the correct form.", question_type="choice",
            options=[{"label": "A", "body": "has went"}, {"label": "B", "body": "has gone"}],
            expected_answer="B", explanation="Present perfect uses the past participle."),)))
        events = self.turn(user_id, session, quiz, message="Teach me")

        types = [event.type for event in events]
        self.assertEqual(types, ["turn.started", "assistant.message", "tool.called", "question", "turn.completed"])
        question = next(event.data for event in events if event.type == "question")
        self.assertEqual([option["label"] for option in question["options"]], ["A", "B"])
        self.assertNotIn("has gone\"", json.dumps([event.data for event in events if event.type != "question"]))
        self.assertNotIn("expected_answer", json.dumps([event.data for event in events]))
        self.assertEqual(quiz.requests[0]["tools"], [
            "mastery_status", "mastery_quiz", "mastery_grade", "mastery_assess",
            "path_outline", "path_reorder", "learner_profile"])
        self.assertEqual(self.status_of(quiz.requests[0])["objective"]["knowledge_point_id"], KP_BASIC)

        question_id = question["question_id"]
        grade = ScriptedChat(ChatReply("", (call("mastery_grade", answer="B"),)), ChatReply("Correct!"))
        events = self.turn(user_id, session, grade, answer=CardAnswer(question_id, "B"))

        pending = self.status_of(grade.requests[0])["pending_interaction"]
        self.assertEqual((pending["status"], pending["learner_answer"]), ("answered", "B"),
                         "the card answer is committed before the model runs")
        grading = next(event.data for event in events if event.type == "grading")
        self.assertEqual((grading["is_correct"], grading["knowledge_point_id"]), (True, KP_BASIC))
        self.assertEqual(events[-1].type, "turn.completed")
        self.assertEqual(self.schema.query(
            "SELECT status FROM mastery_interactions WHERE interaction_id = %s", (question_id,)), [("graded",)])
        self.assertEqual(self.schema.query(
            "SELECT count(*) FROM mastery_learning_evidence WHERE path_id = %s AND source = 'mastery_path'",
            (str(session.path_id),)), [(1,)])
        roles = [message.role for message in self.sessions.recent_messages(user_id, session.id)]
        self.assertEqual(roles, ["user", "assistant", "user", "assistant"])

    def test_concept_objective_is_assessed_qualitatively(self):
        user_id, _goal_id, _paths, session = self.learner(ConceptOnlyCurriculum())
        chat = ScriptedChat(ChatReply("", (call("mastery_assess", knowledge_point_id=KP_CONCEPT, passed=True,
                                                feedback="Explained it clearly."),)),
                            ChatReply("Well explained."))
        events = self.turn(user_id, session, chat, message="Here is my explanation")
        grading = next(event.data for event in events if event.type == "grading")
        self.assertEqual((grading["passed"], grading["mastered"]), (True, True))

    def test_outline_then_reorder_updates_sse_and_next_turn_objective(self):
        user_id, _goal_id, _paths, session = self.learner(TwoModuleCurriculum())
        chat = ScriptedChat(
            ChatReply("", (call("path_outline"),)),
            ChatReply("", (call("path_reorder", module_ids=[TOPIC_SECOND, TOPIC_BASIC]),)),
            ChatReply("I moved the second module first."),
        )

        events = self.turn(user_id, session, chat, message="Move the second module first")

        types = [event.type for event in events]
        self.assertEqual(types.count("tool.called"), 2)
        self.assertEqual(types.count("path.reordered"), 1)
        self.assertEqual(types[-1], "turn.completed")
        self.assertNotIn("expected_answer", json.dumps([event.data for event in events]))
        progress = self.learning_store.get_owned_progress(session.path_id, user_id)
        self.assertEqual([module.id for module in progress.modules], [TOPIC_SECOND, TOPIC_BASIC])

        later = ScriptedChat(ChatReply("Let's start there."))
        self.turn(user_id, session, later, message="What is next?")
        self.assertEqual(self.status_of(later.requests[0])["objective"]["knowledge_point_id"], KP_SECOND)

    def test_profile_update_emits_sse_and_appears_in_next_turn(self):
        user_id, _goal_id, _paths, session = self.learner()
        chat = ScriptedChat(ChatReply("", (call("learner_profile", time_budget="25 minutes daily"),)),
                            ChatReply("I will keep that in mind."))

        events = self.turn(user_id, session, chat, message="I have 25 minutes each day")

        self.assertEqual([event.data for event in events if event.type == "profile.updated"],
                         [{"fields": ["time_budget"]}])
        self.assertEqual(events[-1].type, "turn.completed")
        later = ScriptedChat(ChatReply("Ready."))
        self.turn(user_id, session, later, message="Continue")
        self.assertEqual(self.status_of(later.requests[0])["learner_profile"]["time_budget"],
                         "25 minutes daily")

    def test_a_later_objective_cannot_be_assessed_early(self):
        user_id, _goal_id, _paths, session = self.learner()
        chat = ScriptedChat(ChatReply("", (call("mastery_assess", knowledge_point_id=KP_CONCEPT,
                                                passed=True, feedback="Pass"),)),
                            ChatReply("Let's work on the current objective."))
        events = self.turn(user_id, session, chat, message="Skip ahead")
        self.assertFalse(any(event.type == "grading" for event in events))
        self.assertIn("current objective", json.loads(chat.requests[1]["messages"][-1]["content"])["error"])

    def test_quiz_reply_prose_cannot_reveal_the_expected_answer(self):
        user_id, _goal_id, _paths, session = self.learner()
        chat = ScriptedChat(
            ChatReply("The answer is secret-value", (call("mastery_status"),)),
            ChatReply("The answer is secret-value", (call(
                "mastery_quiz", knowledge_point_id=KP_BASIC, question="Q?",
                expected_answer="secret-value"),)),
        )
        events = self.turn(user_id, session, chat, message="Quiz me")
        self.assertIn("question", [event.type for event in events])
        self.assertNotIn("secret-value", json.dumps([event.data for event in events]))
        self.assertNotIn("secret-value", " ".join(message.content for message in
                                                    self.sessions.recent_messages(user_id, session.id)))

    def test_practice_card_ends_turn_and_suppresses_model_prose_and_answer(self):
        user_id, _goal_id, _paths, session = self.learner()
        practice = PracticeStore(self.schema.url)
        engine = TutorEngine(self.sessions, self.learning_store, practice=practice)
        chat = ScriptedChat(ChatReply(
            "This prose must be discarded; the answer is hidden-answer-secret.",
            (call("practice_questions", knowledge_point_id=KP_BASIC, questions=[{
                "question": "Choose the correct form.", "question_type": "short",
                "expected_answer": "hidden-answer-secret", "explanation": "hidden-explanation-secret",
            }]),)))

        async def run():
            opened = await engine.open_turn(user_id, session.id)
            return [event async for event in engine.run(opened, chat, message="Give me practice")]

        events = asyncio.run(run())
        entry_ids = self.schema.query(
            "SELECT id FROM notebook_entries WHERE session_id = %s ORDER BY id", (str(session.id),))
        assistant = self.schema.query(
            "SELECT content, metadata_json FROM messages WHERE session_id = %s AND role = 'assistant' "
            "ORDER BY id DESC LIMIT 1", (str(session.id),))[0]
        rendered = json.dumps([{"type": event.type, "data": event.data} for event in events])

        self.assertEqual([event.type for event in events], [
            "turn.started", "assistant.message", "tool.called", "practice.questions", "turn.completed",
        ])
        self.assertEqual(events[1].data["text"], "Try these practice questions.")
        self.assertNotIn("This prose must be discarded", rendered)
        self.assertNotIn("hidden-answer-secret", rendered)
        self.assertNotIn("hidden-explanation-secret", rendered)
        self.assertEqual(assistant[0], "Try these practice questions.")
        self.assertEqual(assistant[1]["practice_entry_ids"], [entry[0] for entry in entry_ids])
        self.assertNotIn("hidden-answer-secret", assistant[0])
        self.assertNotIn("hidden-explanation-secret", assistant[0])

    def test_open_question_stays_in_its_original_session(self):
        user_id, _goal_id, _paths, original = self.learner()
        second = self.sessions.create_session(user_id, original.path_id, "Other session")
        quiz = ScriptedChat(ChatReply("", (call(
            "mastery_quiz", knowledge_point_id=KP_BASIC, question="Which form?",
            expected_answer="B", question_type="choice",
            options=[{"label": "A", "body": "went"}, {"label": "B", "body": "gone"}]),)))
        question_id = next(event.data["question_id"] for event in self.turn(
            user_id, original, quiz, message="Quiz me") if event.type == "question")

        self.assertIsNone(asyncio.run(self.engine.pending_question(second)))
        self.assertEqual(asyncio.run(self.engine.pending_question(original))["question_id"], question_id)
        other_quiz = ScriptedChat(ChatReply("", (call(
            "mastery_quiz", knowledge_point_id=KP_BASIC, question="Another question",
            expected_answer="A"),)), ChatReply("Return to the first session."))
        events = self.turn(user_id, second, other_quiz, message="Quiz me here")
        self.assertFalse(any(event.type == "question" for event in events))
        self.assertIn("original session", json.loads(other_quiz.requests[1]["messages"][-1]["content"])["error"])
        second_status = self.status_of(other_quiz.requests[0])
        self.assertIsNone(second_status["pending_interaction"])
        self.assertTrue(second_status["question_in_other_session"])

        other_grade = ScriptedChat(ChatReply("", (call("mastery_grade", answer="B"),)),
                                   ChatReply("Return to the first session."))
        events = self.turn(user_id, second, other_grade, answer=CardAnswer(question_id, "B"))
        self.assertFalse(any(event.type == "grading" for event in events))
        self.assertIn("another session", json.loads(other_grade.requests[1]["messages"][-1]["content"])["error"])
        self.assertEqual(self.schema.query(
            "SELECT status, session_id FROM mastery_interactions WHERE interaction_id = %s", (question_id,)),
            [("awaiting_input", original.id)])

        original_chat = ScriptedChat(ChatReply("Ready."))
        self.turn(user_id, original, original_chat, message="Explain the question")
        pending = self.status_of(original_chat.requests[0])["pending_interaction"]
        self.assertEqual(pending["question"]["prompt"], "Which form?")
        self.assertEqual([option["body"] for option in pending["question"]["options"]], ["went", "gone"])

    def test_model_cannot_grade_before_the_card_is_answered(self):
        user_id, _goal_id, _paths, session = self.learner()
        quiz = ScriptedChat(ChatReply("", (call(
            "mastery_quiz", knowledge_point_id=KP_BASIC, question="Q?", expected_answer="yes"),)))
        question_id = next(event.data["question_id"] for event in self.turn(
            user_id, session, quiz, message="Go") if event.type == "question")
        grade = ScriptedChat(ChatReply("", (call("mastery_grade", answer="yes"),)), ChatReply("Answer first."))
        events = self.turn(user_id, session, grade, message="Can you grade this?")
        self.assertFalse(any(event.type == "grading" for event in events))
        self.assertIn("not answered", json.loads(grade.requests[1]["messages"][-1]["content"])["error"])
        self.assertEqual(self.schema.query(
            "SELECT status FROM mastery_interactions WHERE interaction_id = %s", (question_id,)),
            [("awaiting_input",)])

    def test_turn_runs_when_sse_body_is_never_read(self):
        user_id, _goal_id, _paths, session = self.learner()

        async def run_without_reading():
            opened = await self.engine.open_turn(user_id, session.id)
            stream = stream_events(self.engine.run(opened, ScriptedChat(ChatReply("Done.")), message="Hi"))
            for _ in range(60):
                state = await asyncio.to_thread(self.schema.query,
                                                "SELECT status FROM turns WHERE id = %s", (str(opened.turn_id),))
                if state == [("completed",)]:
                    break
                await asyncio.sleep(0.05)
            else:
                self.fail("Turn remained running without an SSE reader")
            await stream.aclose()

        asyncio.run(run_without_reading())
        self.assertEqual([message.content for message in self.sessions.recent_messages(user_id, session.id)],
                         ["Hi", "Done."])

    def test_a_quiz_on_a_concept_objective_is_refused_to_the_model(self):
        user_id, _goal_id, _paths, session = self.learner()
        chat = ScriptedChat(ChatReply("", (call("mastery_quiz", knowledge_point_id=KP_CONCEPT, question="Q?",
                                                expected_answer="A"),)),
                            ChatReply("Tell me in your own words instead."))
        events = self.turn(user_id, session, chat, message="Quiz me")
        tool_result = json.loads(chat.requests[1]["messages"][-1]["content"])
        self.assertIn("mastery_assess", tool_result["error"])
        self.assertEqual(events[-1].type, "turn.completed")
        self.assertFalse(any(event.type == "question" for event in events))

    def test_formal_result_between_turns_keeps_both_evidence(self):
        user_id, goal_id, paths, session = self.learner()
        quiz = ScriptedChat(ChatReply("", (call("mastery_quiz", knowledge_point_id=KP_BASIC, question="Q?",
                                                expected_answer="yes"),)))
        question_id = next(event.data["question_id"] for event in self.turn(user_id, session, quiz, message="Go")
                           if event.type == "question")
        ingestion = FormalAssessmentIngestionService(self.learning_store, paths)
        ingestion.ingest(FormalEvidenceAdapter.to_command(formal_event(
            user_id=user_id, goal_id=goal_id, attempt_id=str(uuid4()),
            items=[item([mapping(KP_BASIC)], is_correct=True)])))
        grade = ScriptedChat(ChatReply("", (call("mastery_grade", answer="yes"),)), ChatReply("Good."))
        self.turn(user_id, session, grade, answer=CardAnswer(question_id, "yes"))

        sources = sorted(row[0] for row in self.schema.query(
            "SELECT source FROM mastery_learning_evidence WHERE path_id = %s", (str(session.path_id),)))
        self.assertEqual(len(sources), 2)
        self.assertIn("mastery_path", sources)

    def test_failures_close_the_turn_with_a_code(self):
        cases = [
            (ScriptedChat(LlmApiError(500)), "llm_error"),
            (None, "llm_not_configured"),
            (ScriptedChat(*[ChatReply("", (call("mastery_status"),)) for _ in range(6)]), "too_many_rounds"),
        ]
        for chat, code in cases:
            with self.subTest(code=code):
                user_id, _goal_id, _paths, session = self.learner()
                events = self.turn(user_id, session, chat, message="Hi")
                self.assertEqual((events[-1].type, events[-1].data["failureCode"]), ("turn.failed", code))
                self.assertEqual(self.schema.query(
                    "SELECT status, failure_code FROM turns WHERE session_id = %s", (str(session.id),)),
                    [("failed", code)])
                self.turn(user_id, session, ScriptedChat(ChatReply("Back again.")), message="Hi again")

    def test_only_the_owner_opens_a_turn_and_only_one_at_a_time(self):
        user_id, _goal_id, _paths, session = self.learner()
        with self.assertRaises(SessionNotFound):
            asyncio.run(self.engine.open_turn(str(uuid4()), session.id))

        async def two_opens():
            await self.engine.open_turn(user_id, session.id)
            await self.engine.open_turn(user_id, session.id)

        with self.assertRaises(ActiveTurnConflict):
            asyncio.run(two_opens())
        self.sessions.recover_interrupted_turns()


if __name__ == "__main__":
    unittest.main()
