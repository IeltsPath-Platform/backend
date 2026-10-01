"""A proposed order must be a strict permutation of the path's modules and their knowledge points."""

import copy
import unittest
from uuid import uuid4

from app.mastery.models import LearningProgress
from app.mastery.policy import next_objective

from app.adapters.curriculum_adapter import CurriculumAdapter
from app.learning.ordering_validator import InvalidOrdering, OrderingValidator

MODULE_A = "11111111-1111-4111-8111-111111111101"
MODULE_B = "11111111-1111-4111-8111-111111111102"
KP_A = "22222222-2222-4222-8222-222222222201"
KP_B = "22222222-2222-4222-8222-222222222202"
KP_C = "22222222-2222-4222-8222-222222222203"


def modules():
    topics = [
        {"id": MODULE_A, "name": "Nền tảng", "sortOrder": 0, "parentTopicId": None},
        {"id": MODULE_B, "name": "Writing", "sortOrder": 1, "parentTopicId": MODULE_A},
    ]
    points = [
        {"id": KP_B, "topicId": MODULE_A, "name": "Vocabulary", "learningType": "MEMORY",
         "skill": "READING", "description": "b", "createdAt": "2026-09-02"},
        {"id": KP_A, "topicId": MODULE_A, "name": "Grammar", "learningType": "PROCEDURE",
         "skill": "WRITING", "description": "Practice clauses", "createdAt": "2026-09-01"},
        {"id": KP_C, "topicId": MODULE_B, "name": "Coherence", "learningType": "PROCEDURE",
         "skill": "WRITING", "description": None, "createdAt": "2026-09-01"},
    ]
    return CurriculumAdapter.to_modules(topics, points)


def valid_proposal():
    return {"modules": [
        {"id": MODULE_B, "knowledge_point_ids": [KP_C]},
        {"id": MODULE_A, "knowledge_point_ids": [KP_B, KP_A]},
    ], "rationale": "Start with the learner's writing gap."}


class OrderingValidatorTest(unittest.TestCase):
    def setUp(self):
        self.modules = modules()

    def test_reorders_and_deep_copies_content_metadata_without_trusting_proposal_fields(self):
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
