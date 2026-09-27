"""Path and profile tutor tools against a transactional in-memory store."""

from contextlib import contextmanager
import json
import unittest
from uuid import uuid4

from app.mastery.models import (
    KnowledgePoint, KnowledgeType, LearningEvidence, LearningModule, PendingQuestion, QuizAttempt,
)
from app.mastery.policy import next_objective, objective_status
from app.mastery.service import LearningService
from app.tutor.tools import TutorTools
from tests.formal_assessment_support import InMemoryLearningStore


MODULE_A = "11111111-1111-4111-8111-111111111101"
MODULE_B = "11111111-1111-4111-8111-111111111102"
KP_A1 = "22222222-2222-4222-8222-222222222201"
KP_A2 = "22222222-2222-4222-8222-222222222202"
KP_B1 = "22222222-2222-4222-8222-222222222203"
KP_B2 = "22222222-2222-4222-8222-222222222204"


class EventCaptureStore(InMemoryLearningStore):
    """Keep transaction event metadata that the shared fake normally discards."""

    def __init__(self):
        super().__init__()
        self.event_details = []

    @contextmanager
    def transaction(self, *args, **kwargs):
        outer = getattr(self._local, "active", None) is None
        with super().transaction(*args, **kwargs) as tx:
            yield tx
            if outer:
                self.event_details.extend(tx.events)


class TutorPathToolsTest(unittest.TestCase):
    def setUp(self):
        self.store = EventCaptureStore()
        self.service = LearningService(self.store)
        self.path_id = str(uuid4())
        self.tools = TutorTools(self.service, self.path_id, session_id="session-1", turn_id="turn-1")
        modules = [
            LearningModule(id=MODULE_A, name="A" * 130, order=0, knowledge_points=[
                KnowledgePoint(id=KP_A1, name="First A", type=KnowledgeType.PROCEDURE, module_id=MODULE_A),
                KnowledgePoint(id=KP_A2, name="Second A", type=KnowledgeType.PROCEDURE, module_id=MODULE_A),
            ]),
            LearningModule(id=MODULE_B, name="Module B", order=1, knowledge_points=[
                KnowledgePoint(id=KP_B1, name="First B", type=KnowledgeType.PROCEDURE, module_id=MODULE_B),
                KnowledgePoint(id=KP_B2, name="Second B", type=KnowledgeType.PROCEDURE, module_id=MODULE_B),
            ]),
        ]
        self.service.replace_modules_for_path(self.path_id, modules)

    def progress(self):
        return self.store.load(self.path_id)

    def test_outline_has_ordered_public_fields_and_current_objective(self):
        outline = self.tools.execute("path_outline", {}).result
        self.assertEqual([module["id"] for module in outline["modules"]], [MODULE_A, MODULE_B])
        self.assertEqual([point["id"] for point in outline["modules"][0]["knowledge_points"]], [KP_A1, KP_A2])
        self.assertEqual([module["order"] for module in outline["modules"]], [0, 1])
        self.assertEqual(len(outline["modules"][0]["name"]), 120)
        self.assertEqual(set(outline["modules"][0]), {"id", "name", "order", "knowledge_points"})
        self.assertEqual(set(outline["modules"][0]["knowledge_points"][0]),
                         {"id", "name", "type", "status", "mastered"})
        self.assertEqual(outline["modules"][0]["knowledge_points"][0]["status"], "new")
        self.assertFalse(outline["modules"][0]["knowledge_points"][0]["mastered"])
        self.assertEqual(outline["objective_id"], next_objective(self.progress()).knowledge_point_id)
        self.assertNotIn("expected_answer", json.dumps(outline))

        def seed(tx):
            tx.progress.quiz_attempts.append(QuizAttempt(
                question_id=str(uuid4()), knowledge_point_id=KP_A1, is_correct=False))
            tx.progress.mastery_levels[KP_A2] = 0.95
            tx.touch()
        self.store.mutate(self.path_id, seed)
        changed = self.tools.execute("path_outline", {}).result
        progress = self.progress()
        for module, public in zip(progress.modules, changed["modules"]):
            for point, row in zip(module.knowledge_points, public["knowledge_points"]):
                self.assertEqual(row["status"], objective_status(progress, point))

    def test_module_reorder_preserves_kp_order_and_emits_one_event(self):
        before = self.progress().version
        events_before = len(self.store.event_details)

        outcome = self.tools.execute("path_reorder", {"module_ids": [MODULE_B, MODULE_A]})

        progress = self.progress()
        self.assertEqual(outcome.result["status"], "reordered")
        self.assertEqual([module.id for module in progress.modules], [MODULE_B, MODULE_A])
        self.assertEqual([[kp.id for kp in module.knowledge_points] for module in progress.modules],
                         [[KP_B1, KP_B2], [KP_A1, KP_A2]])
        self.assertEqual(progress.version, before + 1)
        self.assertEqual(outcome.result["objective_id"], KP_B1)
        self.assertEqual(next_objective(progress).knowledge_point_id, KP_B1)
        self.assertEqual(outcome.events, [("path.reordered", {"module_count": 2, "knowledge_point_count": 4})])
        self.assertEqual(self.store.event_details[events_before:], [
            ("path.reordered_by_learner", {"mode": "replace", "module_count": 2,
                                           "knowledge_point_count": 4}, "session-1", "turn-1")])
        self.assertNotIn("expected_answer", json.dumps(outcome.result) + json.dumps(outcome.events))

    def test_one_module_kp_reorder_preserves_other_module_and_module_order(self):
        outcome = self.tools.execute("path_reorder", {"knowledge_points": [
            {"module_id": MODULE_A, "knowledge_point_ids": [KP_A2, KP_A1]}]})

        self.assertEqual(outcome.result["status"], "reordered")
        self.assertEqual([module.id for module in self.progress().modules], [MODULE_A, MODULE_B])
        self.assertEqual([[kp.id for kp in module.knowledge_points] for module in self.progress().modules],
                         [[KP_A2, KP_A1], [KP_B1, KP_B2]])

    def test_invalid_reorders_do_not_write_or_emit(self):
        cases = [
            ({}, "empty"),
            ({"module_ids": [MODULE_A]}, "missing_module"),
            ({"module_ids": [MODULE_A, MODULE_A]}, "duplicate_module"),
            ({"module_ids": [MODULE_A, str(uuid4())]}, "unknown_module"),
            ({"knowledge_points": [{"module_id": str(uuid4()), "knowledge_point_ids": []}]},
             "unknown_module"),
            ({"knowledge_points": [{"module_id": MODULE_A, "knowledge_point_ids": [KP_A1, str(uuid4())]}]},
             "unknown_knowledge_point"),
            ({"knowledge_points": [{"module_id": MODULE_A, "knowledge_point_ids": [KP_A1, KP_B1]}]},
             "moved_knowledge_point"),
            ({"knowledge_points": [{"module_id": MODULE_A, "knowledge_point_ids": [KP_A1]}]},
             "missing_knowledge_point"),
            ({"knowledge_points": [{"module_id": MODULE_A, "knowledge_point_ids": [KP_A1, KP_A1]}]},
             "duplicate_knowledge_point"),
            ({"knowledge_points": [
                {"module_id": MODULE_A, "knowledge_point_ids": [KP_A1, KP_A2]},
                {"module_id": MODULE_A, "knowledge_point_ids": [KP_A2, KP_A1]}]}, "duplicate_module"),
            ({"knowledge_points": "wrong type"}, "malformed"),
            ({"module_ids": "wrong type"}, "malformed"),
        ]
        for arguments, reason in cases:
            with self.subTest(reason=reason, arguments=arguments):
                revision = self.progress().version
                count = len(self.store.event_details)
                outcome = self.tools.execute("path_reorder", arguments)
                self.assertEqual(outcome.result["reason"], reason)
                self.assertTrue(outcome.result["error"])
                self.assertEqual(outcome.events, [])
                self.assertEqual(self.progress().version, revision)
                self.assertEqual(len(self.store.event_details), count)

    def test_unchanged_order_does_not_write_or_emit(self):
        revision = self.progress().version
        count = len(self.store.event_details)

        outcome = self.tools.execute("path_reorder", {"module_ids": [MODULE_A, MODULE_B],
            "knowledge_points": [{"module_id": MODULE_A, "knowledge_point_ids": [KP_A1, KP_A2]}]})

        self.assertEqual(outcome.result, {"status": "unchanged"})
        self.assertEqual(outcome.events, [])
        self.assertEqual(self.progress().version, revision)
        self.assertEqual(len(self.store.event_details), count)

    def test_reorder_keeps_mastery_attempts_and_open_question(self):
        def seed(tx):
            tx.progress.mastery_levels[KP_A1] = 0.45
            tx.progress.quiz_attempts.append(QuizAttempt(
                question_id=str(uuid4()), knowledge_point_id=KP_A1, module_id=MODULE_A,
                is_correct=True, user_answer="answer"))
            tx.progress.learning_evidence.append(LearningEvidence(
                knowledge_point_id=KP_A1, result="correct", quality=0.7))
            tx.touch()
        self.store.mutate(self.path_id, seed)
        question = PendingQuestion(question_id=str(uuid4()), knowledge_point_id=KP_A1,
                                   module_id=MODULE_A, prompt="Question?", expected_answer="private")
        self.service.register_question(self.path_id, question, session_id="session-1", turn_id="turn-1")
        before = self.progress()

        outcome = self.tools.execute("path_reorder", {"module_ids": [MODULE_B, MODULE_A]})

        after = self.progress()
        self.assertEqual(outcome.result["status"], "reordered")
        self.assertEqual(after.mastery_levels, before.mastery_levels)
        self.assertEqual(after.quiz_attempts, before.quiz_attempts)
        self.assertEqual(after.learning_evidence, before.learning_evidence)
        with self.store.transaction(self.path_id) as tx:
            active = tx.active_interaction()
        self.assertIsNotNone(active)
        self.assertEqual(active.interaction_id, question.question_id)
        self.assertEqual(active.question.module_id, MODULE_A)
        self.assertEqual(after.pending_question.module_id, MODULE_A)
        self.assertNotIn("expected_answer", json.dumps(outcome.result) + json.dumps(outcome.events))

    def test_profile_records_only_changed_nonempty_known_fields(self):
        outcome = self.tools.execute("learner_profile", {
            "prior_knowledge": "  intermediate  ", "time_budget": "20 minutes", "unknown": "ignore",
            "target_level": 6, "notes": " "})
        self.assertEqual(outcome.result["recorded"], ["prior_knowledge", "time_budget"])
        self.assertEqual(outcome.result["learner_profile"]["prior_knowledge"], "intermediate")
        self.assertEqual(outcome.events, [("profile.updated", {"fields": ["prior_knowledge", "time_budget"]})])
        self.assertNotIn("unknown", outcome.result["learner_profile"])
        self.assertEqual(outcome.result["learner_profile"]["target_level"], "")

        revision = self.progress().version
        same = self.tools.execute("learner_profile", {"time_budget": "20 minutes"})
        self.assertEqual(same.result["recorded"], [])
        self.assertEqual(same.events, [])
        self.assertEqual(self.progress().version, revision)

        empty = self.tools.execute("learner_profile", {"notes": "", "unknown": "value"})
        self.assertIn("error", empty.result)
        self.assertEqual(self.progress().version, revision)

        long_value = self.tools.execute("learner_profile", {"preferences": "x" * 601})
        self.assertEqual(long_value.result["recorded"], ["preferences"])
        self.assertEqual(len(long_value.result["learner_profile"]["preferences"]), 600)


if __name__ == "__main__":
    unittest.main()
