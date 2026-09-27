"""Save-note tool checks that run without PostgreSQL or a message broker."""

import asyncio
from datetime import datetime, timezone
import json
from types import SimpleNamespace
import unittest
from unittest.mock import patch
from uuid import UUID, uuid4

from app.llm.client import ChatReply, ToolCall
from app.mastery.models import KnowledgePoint, KnowledgeType, LearningModule, PendingQuestion
from app.mastery.service import LearningService
from app.tutor.engine import OpenTurn, TutorEngine
from app.tutor.session_store import TutorSession
from app.tutor.tools import TOOL_DEFINITIONS, TutorTools
from tests.formal_assessment_support import InMemoryLearningStore


MODULE_ID = "11111111-1111-4111-8111-111111111101"
KP_ID = "22222222-2222-4222-8222-222222222201"


def call(name, **arguments):
    raw = {"id": f"call-{uuid4().hex[:8]}", "type": "function",
           "function": {"name": name, "arguments": json.dumps(arguments)}}
    return ToolCall(raw["id"], name, arguments, raw)


class ScriptedChat:
    def __init__(self, *replies):
        self.replies = list(replies)
        self.requests = []

    async def __call__(self, messages, *, tools=None):
        self.requests.append({"messages": [dict(item) for item in messages],
                              "tools": [item["function"]["name"] for item in tools or []]})
        return self.replies.pop(0)


class InMemorySessions:
    def __init__(self):
        self.messages = []
        self.outcomes = []

    def add_message(self, _user_id, _session_id, _turn_id, role, content, metadata=None):
        self.messages.append(SimpleNamespace(role=role, content=content, metadata=metadata or {}))
        return len(self.messages)

    def recent_messages(self, _user_id, _session_id, limit=20):
        return self.messages[-limit:]

    def finish_turn(self, _turn_id, status, failure_code=""):
        self.outcomes.append((status, failure_code))
        return True


class SaveNoteToolTest(unittest.TestCase):
    def setUp(self):
        self.store = InMemoryLearningStore()
        self.service = LearningService(self.store)
        self.path_id = str(uuid4())
        self.session_id = str(uuid4())
        self.tools = TutorTools(self.service, self.path_id, session_id=self.session_id,
                                turn_id=str(uuid4()))
        self.service.replace_modules_for_path(self.path_id, [LearningModule(
            id=MODULE_ID, name="Grammar", order=0, knowledge_points=[KnowledgePoint(
                id=KP_ID, name="Present perfect", type=KnowledgeType.PROCEDURE, module_id=MODULE_ID,
            )],
        )])

    def test_definition_and_kp_draft_without_path_write(self):
        self.assertIn("save_note", [item["function"]["name"] for item in TOOL_DEFINITIONS])
        revision = self.store.load(self.path_id).version
        outcome = self.tools.execute("save_note", {
            "title": "  Present perfect  ", "body": "  Use has with the past participle.  ",
            "knowledge_point_id": KP_ID,
        })
        self.assertEqual(outcome.result, {"status": "offered", "note": "The learner's app saves this note."})
        self.assertEqual(outcome.events, [("note.draft", {
            "title": "Present perfect", "body": "Use has with the past participle.",
            "source_type": "KNOWLEDGE_POINT", "source_reference_id": KP_ID,
        })])
        self.assertFalse(outcome.ends_turn)
        self.assertEqual(self.store.load(self.path_id).version, revision)

    def test_session_source_and_text_limits(self):
        outcome = self.tools.execute("save_note", {"title": " T" * 300, "body": " x" * 25000})
        draft = outcome.events[0][1]
        self.assertEqual(draft["source_type"], "TUTOR_SESSION")
        self.assertEqual(draft["source_reference_id"], self.session_id)
        self.assertEqual(len(draft["title"]), 255)
        self.assertLessEqual(len(draft["body"]), 19900 + len("\n\n[truncated]"))
        self.assertTrue(draft["body"].endswith("[truncated]"))

    def test_null_or_blank_kp_means_the_session_and_a_non_string_kp_is_refused(self):
        for kp_value in (None, "", "   "):
            with self.subTest(kp_value=kp_value):
                outcome = self.tools.execute("save_note", {"title": "T", "body": "B", "knowledge_point_id": kp_value})
                self.assertEqual(outcome.events[0][1]["source_type"], "TUTOR_SESSION")
                self.assertEqual(outcome.events[0][1]["source_reference_id"], self.session_id)
        refused = self.tools.execute("save_note", {"title": "T", "body": "B", "knowledge_point_id": 123})
        self.assertIn("error", refused.result)
        self.assertEqual(refused.events, [])

    def test_invalid_kp_and_empty_text_emit_nothing(self):
        for arguments in (
            {"title": "title", "body": "body", "knowledge_point_id": str(uuid4())},
            {"title": "  ", "body": "body"},
            {"title": "title", "body": "  "},
        ):
            with self.subTest(arguments=arguments):
                outcome = self.tools.execute("save_note", arguments)
                self.assertIn("error", outcome.result)
                self.assertEqual(outcome.events, [])

    def test_open_question_answer_is_blocked_only_in_its_session(self):
        question = PendingQuestion(question_id=str(uuid4()), knowledge_point_id=KP_ID,
                                   module_id=MODULE_ID, prompt="Fill in the blank.",
                                   expected_answer="has gone")
        _, interaction, _ = self.service.register_question(
            self.path_id, question, session_id=self.session_id, turn_id=str(uuid4()))
        self.service.mark_question_awaiting(self.path_id, interaction_id=interaction.interaction_id,
                                            session_id=self.session_id, turn_id=str(uuid4()))
        blocked = self.tools.execute("save_note", {"title": "Verb form", "body": "It is Has gone."})
        self.assertEqual(blocked.result["error"],
                         "The note would reveal the answer to the open question; save it after grading.")
        self.assertEqual(blocked.events, [])
        allowed = TutorTools(self.service, self.path_id, session_id=str(uuid4()), turn_id=str(uuid4()))
        self.assertEqual(allowed.execute("save_note", {"title": "Verb form", "body": "has gone"})
                         .events[0][0], "note.draft")

    def test_scripted_turn_streams_note_before_message_and_completion(self):
        sessions = InMemorySessions()
        engine = TutorEngine(sessions, self.store)
        now = datetime.now(timezone.utc)
        session = TutorSession(UUID(self.session_id), UUID(self.path_id), "Grammar", now, now)
        turn = OpenTurn(uuid4(), session, uuid4())
        chat = ScriptedChat(
            ChatReply("", (call("save_note", title="  Grammar  ", body="  A study note.  ",
                                knowledge_point_id=KP_ID),)),
            ChatReply("Your note is being saved."),
        )

        async def run():
            return [event async for event in engine.run(turn, chat, message="Please save this")]

        events = asyncio.run(run())

        self.assertEqual([event.type for event in events], [
            "turn.started", "tool.called", "note.draft", "assistant.message", "turn.completed",
        ])
        self.assertEqual(events[2].data["source_reference_id"], KP_ID)
        self.assertEqual(json.loads(chat.requests[1]["messages"][-1]["content"]), {
            "status": "offered", "note": "The learner's app saves this note.",
        })
        self.assertEqual(sessions.outcomes, [("completed", "")])

    def test_save_note_never_opens_a_network_connection(self):
        with patch("socket.socket.connect", side_effect=AssertionError("network call attempted")):
            outcome = self.tools.execute("save_note", {"title": "Study", "body": "Remember this."})
        self.assertEqual(outcome.events[0][0], "note.draft")


if __name__ == "__main__":
    unittest.main()
