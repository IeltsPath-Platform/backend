"""Ordering is a private, deterministic request and a strict curriculum permutation."""

import copy
from datetime import date
from decimal import Decimal
import json
import unittest
from uuid import uuid4

from deeptutor.learning.models import LearningProgress
from deeptutor.learning.policy import next_objective

from app.adapters.curriculum_adapter import CurriculumAdapter
from app.adapters.curriculum_scope import CurriculumScope
from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.learning.path_ordering import (
    InvalidOrdering,
    LearnerContext,
    MAX_ORDERING_KNOWLEDGE_POINTS,
    MAX_ORDERING_PAYLOAD_CHARS,
    OrderingRequest,
    OrderingValidator,
    PayloadTooLarge,
)
from tests.formal_assessment_support import event, item, mapping

MODULE_A = "11111111-1111-4111-8111-111111111101"
MODULE_B = "11111111-1111-4111-8111-111111111102"
KP_A = "22222222-2222-4222-8222-222222222201"
KP_B = "22222222-2222-4222-8222-222222222202"
KP_C = "22222222-2222-4222-8222-222222222203"
TODAY = date(2026, 9, 26)


def goal():
    return {"id": str(uuid4()), "userId": str(uuid4()), "status": "ACTIVE", "targetBand": 6.5,
            "examDate": "2026-11-25", "availableMinutesPerDay": 45,
            "email": "private-learner@example.invalid", "name": "Private learner name",
            "token": "private-bearer-token"}


def ordering_curriculum():
    topics = [
        {"id": MODULE_A, "name": "Nền tảng", "sortOrder": 0, "parentTopicId": None},
        {"id": MODULE_B, "name": "Writing", "sortOrder": 1, "parentTopicId": MODULE_A},
    ]
    points = [
        {"id": KP_B, "topicId": MODULE_A, "name": "Vocabulary", "learningType": "MEMORY",
         "skill": "READING", "description": "b" * 250, "createdAt": "2026-09-02",
         "effectiveBandMin": 5.0, "effectiveBandMax": 6.5},
        {"id": KP_A, "topicId": MODULE_A, "name": "Grammar", "learningType": "PROCEDURE",
         "skill": "WRITING", "description": "Practice clauses", "createdAt": "2026-09-01",
         "effectiveBandMin": 4.0, "effectiveBandMax": 5.0},
        {"id": KP_C, "topicId": MODULE_B, "name": "Coherence", "learningType": "PROCEDURE",
         "skill": "WRITING", "description": None, "createdAt": "2026-09-01",
         "effectiveBandMin": None, "effectiveBandMax": None},
    ]
    scoped = CurriculumScope.select(topics, points, Decimal("6.5"))
    return CurriculumAdapter.to_modules(scoped.topics, scoped.knowledge_points), scoped


def valid_proposal():
    return {"modules": [
        {"id": MODULE_B, "knowledge_point_ids": [KP_C]},
        {"id": MODULE_A, "knowledge_point_ids": [KP_B, KP_A]},
    ], "rationale": "Start with the learner's writing gap."}


def placement(items, *, band=None, completed_at="2026-09-24T10:00:00Z", **kwargs):
    payload = event(user_id=str(uuid4()), goal_id=str(uuid4()), attempt_id=str(uuid4()),
                    items=items, assessment_type="PLACEMENT", **kwargs)
    payload["data"].update(overall_band=band, completed_at=completed_at)
    return FormalEvidenceAdapter.to_command(payload)


class OrderingValidatorTest(unittest.TestCase):
    def setUp(self):
        self.modules, self.scoped = ordering_curriculum()

    def test_reorders_and_deep_copies_content_metadata_without_trusting_llm_fields(self):
        original = copy.deepcopy(self.modules)
        proposal = valid_proposal()
        proposal["modules"][0].update(name="Invented name", order=999,
                                      knowledge_points=[{"id": KP_C, "name": "Invented KP"}])

        ordered = OrderingValidator.apply(self.modules, proposal)

        self.assertEqual([module.id for module in ordered], [MODULE_B, MODULE_A])
        self.assertEqual([module.order for module in ordered], [0, 1])
        self.assertEqual([kp.id for kp in ordered[1].knowledge_points], [KP_B, KP_A])
        self.assertEqual(ordered[0].name, original[1].name)
        self.assertEqual(ordered[0].knowledge_points[0], original[1].knowledge_points[0])
        self.assertIsNot(ordered[0], self.modules[1])
        self.assertIsNot(ordered[0].knowledge_points[0], self.modules[1].knowledge_points[0])
        ordered[0].knowledge_points[0].name = "Edited copy"
        self.assertEqual(self.modules, original)

    def test_deeptutor_next_objective_observes_both_module_and_point_order(self):
        proposal = valid_proposal()
        proposal["modules"] = [proposal["modules"][1], proposal["modules"][0]]
        ordered = OrderingValidator.apply(self.modules, proposal)
        progress = LearningProgress(book_id=str(uuid4()), modules=ordered)
        self.assertEqual(next_objective(progress).knowledge_point_id, KP_B)

        ordered = OrderingValidator.apply(self.modules, valid_proposal())
        progress = LearningProgress(book_id=str(uuid4()), modules=ordered)
        self.assertEqual(next_objective(progress).knowledge_point_id, KP_C)

    def test_child_module_may_precede_its_parent(self):
        ordered = OrderingValidator.apply(self.modules, valid_proposal())
        self.assertEqual(ordered[0].id, MODULE_B)

    def test_rejects_every_invalid_permutation_with_a_safe_reason(self):
        cases = [
            (None, "malformed"),
            ([], "malformed"),
            ({}, "malformed"),
            ({"modules": ()}, "malformed"),
            ({"modules": [None]}, "malformed"),
            ({"modules": [{}]}, "malformed"),
            ({"modules": [{"id": MODULE_A}]}, "malformed"),
            ({"modules": [{"id": 1, "knowledge_point_ids": []}]}, "malformed"),
            ({"modules": [{"id": MODULE_A, "knowledge_point_ids": (KP_A, KP_B)}]}, "malformed"),
            ({"modules": [{"id": MODULE_A, "knowledge_point_ids": [1]}]}, "malformed"),
            ({"modules": [{"id": MODULE_A, "knowledge_point_ids": [KP_A, KP_B]}]}, "missing_module"),
            ({"modules": [*valid_proposal()["modules"],
                          {"id": "private-unknown-module", "knowledge_point_ids": []}]}, "unknown_module"),
            ({"modules": [*valid_proposal()["modules"], valid_proposal()["modules"][0]]}, "duplicate_module"),
            ({"modules": [{"id": MODULE_A, "knowledge_point_ids": [KP_A]},
                          {"id": MODULE_B, "knowledge_point_ids": [KP_C]}]}, "missing_knowledge_point"),
            ({"modules": [{"id": MODULE_A, "knowledge_point_ids": [KP_A, KP_B, "private-unknown-kp"]},
                          {"id": MODULE_B, "knowledge_point_ids": [KP_C]}]}, "unknown_knowledge_point"),
            ({"modules": [{"id": MODULE_A, "knowledge_point_ids": [KP_A, KP_B, KP_A]},
                          {"id": MODULE_B, "knowledge_point_ids": [KP_C]}]}, "duplicate_knowledge_point"),
            ({"modules": [{"id": MODULE_A, "knowledge_point_ids": [KP_A, KP_C]},
                          {"id": MODULE_B, "knowledge_point_ids": [KP_B]}]}, "moved_knowledge_point"),
        ]
        for proposal, reason in cases:
            with self.subTest(reason=reason, proposal=proposal):
                with self.assertRaises(InvalidOrdering) as raised:
                    OrderingValidator.apply(self.modules, proposal)
                self.assertEqual(raised.exception.reason, reason)
                self.assertNotIn("private-unknown", str(raised.exception))


class LearnerContextTest(unittest.TestCase):
    def test_placement_failure_wins_and_latest_available_band_is_used(self):
        older = placement([
            item([mapping(KP_A)], is_correct=True),
            item([mapping(KP_B, judgment="PASS")], is_correct=None, score=1),
            item([mapping(KP_C, judgment="NOT_ASSESSED")], is_correct=None),
        ], band=4.5)
        newer = placement([
            item([mapping(KP_A)], is_correct=False),
            item([mapping(KP_B)], is_correct=True),
        ], band=5.5, completed_at="2026-09-25T10:00:00Z")
        without_band = placement([], completed_at="2026-09-26T10:00:00Z")

        context = LearnerContext.from_goal(goal(), [newer, without_band, older], TODAY)

        self.assertEqual(context.target_band, Decimal("6.5"))
        self.assertEqual(context.days_until_exam, 60)
        self.assertEqual(context.minutes_per_day, 45)
        self.assertEqual(context.placement_band, Decimal("5.5"))
        self.assertEqual(context.placement_results, {KP_A: False, KP_B: True})

    def test_conflicting_boolean_and_qualitative_judgment_is_incorrect(self):
        command = placement([
            item([mapping(KP_A, judgment="FAIL")], is_correct=True),
            item([mapping(KP_B, judgment="PASS")], is_correct=False),
            item([mapping(KP_C, judgment="FAIL")], is_correct=None),
        ])
        context = LearnerContext.from_goal(goal(), [command], TODAY)
        self.assertEqual(context.placement_results, {KP_A: False, KP_B: False, KP_C: False})

    def test_optional_goal_fields_and_absent_placement(self):
        data = goal()
        data.update(examDate=None, availableMinutesPerDay=None)
        context = LearnerContext.from_goal(data, [], TODAY)
        self.assertIsNone(context.days_until_exam)
        self.assertIsNone(context.minutes_per_day)
        self.assertIsNone(context.placement_band)
        self.assertEqual(context.placement_results, {})

    def test_past_and_current_exam_dates_are_zero_days(self):
        for exam_date in ("2026-01-01", TODAY.isoformat()):
            with self.subTest(exam_date=exam_date):
                data = goal()
                data["examDate"] = exam_date
                self.assertEqual(LearnerContext.from_goal(data, [], TODAY).days_until_exam, 0)


class OrderingRequestTest(unittest.TestCase):
    def setUp(self):
        self.modules, self.scoped = ordering_curriculum()
        self.goal = goal()
        self.context = LearnerContext.from_goal(self.goal, [], TODAY)

    def test_payload_has_only_allowlisted_data_in_content_order_and_is_repeatable(self):
        self.scoped.topics[0]["private"] = "private-topic-secret"
        self.scoped.knowledge_points[0]["private"] = "private-kp-secret"
        command = placement([item([mapping(KP_A)], is_correct=False),
                             item([mapping(KP_B)], is_correct=True)], band=5.5)
        context = LearnerContext.from_goal(self.goal, [command], TODAY)

        system, payload = OrderingRequest.build(context, self.modules, self.scoped)
        _, repeated = OrderingRequest.build(context, self.modules, self.scoped)
        data = json.loads(payload)

        self.assertEqual(payload, repeated)
        key_orders = []
        json.loads(payload, object_pairs_hook=lambda pairs: key_orders.append([key for key, _ in pairs]))
        self.assertTrue(all(keys == sorted(keys) for keys in key_orders))
        self.assertIn("Nền tảng", payload)
        self.assertEqual(set(data), {"learner", "modules"})
        self.assertEqual(data["learner"], {"target_band": "6.5", "days_until_exam": 60,
                                          "minutes_per_day": 45, "placement_band": "5.5"})
        self.assertEqual([module["id"] for module in data["modules"]], [MODULE_A, MODULE_B])
        self.assertEqual(data["modules"][1]["parent_id"], MODULE_A)
        for module in data["modules"]:
            self.assertEqual(set(module), {"id", "name", "parent_id", "knowledge_points"})
            for point in module["knowledge_points"]:
                self.assertEqual(set(point), {"id", "name", "type", "skill", "description",
                                              "band_min", "band_max", "placement"})
        points = [point for module in data["modules"] for point in module["knowledge_points"]]
        self.assertEqual([point["id"] for point in points], [KP_A, KP_B, KP_C])
        self.assertEqual([point["placement"] for point in points], ["incorrect", "correct", "not_tested"])
        self.assertEqual(points[0]["type"], "PROCEDURE")
        self.assertEqual(points[0]["skill"], "WRITING")
        self.assertEqual((points[0]["band_min"], points[0]["band_max"]), ("4.0", "5.0"))
        self.assertEqual(points[1]["description"], "b" * 200)
        self.assertIsNone(points[2]["band_min"])
        self.assertIsNone(points[2]["band_max"])
        for private in (self.goal["id"], self.goal["userId"], self.goal["email"], self.goal["name"],
                        self.goal["token"], "private-topic-secret", "private-kp-secret"):
            self.assertNotIn(private, payload + system)
        self.assertIn("knowledge_point_ids", system)
        self.assertIn("rationale", system)

    def test_no_placement_is_serialized_as_null_and_not_tested(self):
        _, payload = OrderingRequest.build(self.context, self.modules, self.scoped)
        data = json.loads(payload)
        self.assertIsNone(data["learner"]["placement_band"])
        self.assertTrue(all(point["placement"] == "not_tested" for module in data["modules"]
                            for point in module["knowledge_points"]))

    def test_knowledge_point_limit_is_inclusive(self):
        self.assertEqual(MAX_ORDERING_KNOWLEDGE_POINTS, 300)
        topics = [{"id": MODULE_A, "name": "M"}]
        points = [{"id": str(uuid4()), "topicId": MODULE_A, "name": "K", "learningType": "MEMORY"}
                  for _ in range(MAX_ORDERING_KNOWLEDGE_POINTS + 1)]
        for count in (MAX_ORDERING_KNOWLEDGE_POINTS, MAX_ORDERING_KNOWLEDGE_POINTS + 1):
            scoped = CurriculumScope.select(topics, points[:count], Decimal("6.5"))
            modules = CurriculumAdapter.to_modules(scoped.topics, scoped.knowledge_points)
            if count == MAX_ORDERING_KNOWLEDGE_POINTS:
                # Minimize optional metadata; the separate character bound may be reached first.
                from unittest.mock import patch
                with patch("app.learning.path_ordering.MAX_ORDERING_PAYLOAD_CHARS", 1_000_000):
                    OrderingRequest.build(self.context, modules, scoped)
            else:
                with self.assertRaises(PayloadTooLarge):
                    OrderingRequest.build(self.context, modules, scoped)

    def test_character_limit_is_inclusive(self):
        self.assertEqual(MAX_ORDERING_PAYLOAD_CHARS, 60_000)
        self.modules[0].name = "X"
        self.scoped.topics[0]["name"] = "X"
        _, payload = OrderingRequest.build(self.context, self.modules, self.scoped)
        name = "X" * (1 + MAX_ORDERING_PAYLOAD_CHARS - len(payload))
        self.modules[0].name = name
        self.scoped.topics[0]["name"] = name
        _, payload = OrderingRequest.build(self.context, self.modules, self.scoped)
        self.assertEqual(len(payload), MAX_ORDERING_PAYLOAD_CHARS)
        self.modules[0].name += "X"
        self.scoped.topics[0]["name"] += "X"
        with self.assertRaises(PayloadTooLarge):
            OrderingRequest.build(self.context, self.modules, self.scoped)
