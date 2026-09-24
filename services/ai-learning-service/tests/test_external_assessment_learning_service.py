"""Formal evidence runs through the pinned DeepTutor engine, not a parallel one.

Expected values are always obtained from DeepTutor itself (``compute_mastery``,
``policy``), never from a restated formula.
"""

import unittest
from uuid import uuid4

from deeptutor.learning.mastery import compute_mastery
from deeptutor.learning.models import LearningProgress
from deeptutor.learning.policy import gate_threshold, is_assessed_mastered, next_objective
from deeptutor.learning.scheduler import SpacedRepetitionScheduler

from app.learning.external_assessment import ExternalAssessmentLearningService
from app.learning.formal_provenance import FORMAL_EVIDENCE_SOURCE, FormalProvenance

from tests.formal_assessment_support import COHERENCE_KP, GRAMMAR_KP, MODULE_ID, VOCABULARY_KP, curriculum


class _UnusedStore:
    """The in-memory operations under test never touch persistence."""


class ExternalAssessmentLearningServiceTest(unittest.TestCase):
    def setUp(self):
        self.service = ExternalAssessmentLearningService(_UnusedStore())
        self.scheduler = SpacedRepetitionScheduler()
        self.progress = LearningProgress(book_id=str(uuid4()))
        self.service.replace_modules(self.progress, curriculum())
        self.progress.current_module_id = MODULE_ID
        self.attempt_id = str(uuid4())

    def quiz(self, knowledge_point_id, is_correct, *, attempt_id=None, version=1):
        provenance = FormalProvenance(attempt_id or self.attempt_id, str(uuid4()), version, str(uuid4()))
        return self.service.record_external_quiz_outcome(
            self.progress,
            knowledge_point_id=knowledge_point_id,
            module_id=MODULE_ID,
            is_correct=is_correct,
            error_type=None,
            provenance=provenance.encode(),
            source_reference_id=str(uuid4()),
            scheduler=self.scheduler,
        )

    def judge(self, knowledge_point_id, passed):
        return self.service.record_external_qualitative_outcome(
            self.progress,
            knowledge_point_id=knowledge_point_id,
            passed=passed,
            provenance=FormalProvenance(self.attempt_id, str(uuid4()), 1, str(uuid4())).encode(),
            source_reference_id=str(uuid4()),
            scheduler=self.scheduler,
        )

    def test_one_correct_formal_answer_is_capped_by_deeptutor_and_does_not_advance(self):
        self.assertEqual(next_objective(self.progress).knowledge_point_id, VOCABULARY_KP)

        self.quiz(VOCABULARY_KP, True)

        self.assertEqual(self.progress.mastery_levels[VOCABULARY_KP], compute_mastery([True]))
        self.assertLess(self.progress.mastery_levels[VOCABULARY_KP], gate_threshold(curriculum()[0].knowledge_points[0].type))
        step = next_objective(self.progress)
        self.assertEqual(step.knowledge_point_id, VOCABULARY_KP)
        self.assertEqual(step.action, "practice")

    def test_enough_formal_evidence_moves_next_objective_as_deeptutor_policy_decides(self):
        outcomes = [True] * 5
        for outcome in outcomes:
            self.quiz(VOCABULARY_KP, outcome)

        self.assertEqual(self.progress.mastery_levels[VOCABULARY_KP], compute_mastery(outcomes))
        vocabulary = curriculum()[0].knowledge_points[0]
        self.assertTrue(is_assessed_mastered(self.progress, vocabulary))
        step = next_objective(self.progress)
        self.assertNotEqual(step.knowledge_point_id, VOCABULARY_KP)
        self.assertEqual(step.knowledge_point_id, GRAMMAR_KP)

    def test_scheduler_state_and_review_queue_are_produced_by_deeptutor(self):
        evidence = self.quiz(GRAMMAR_KP, False)

        state = self.progress.repetition_states[GRAMMAR_KP]
        self.assertEqual(state.review_count, 1)
        self.assertEqual(state.lapse_count, 1)
        self.assertEqual([task.knowledge_point_id for task in self.progress.review_queue], [GRAMMAR_KP])
        self.assertEqual(evidence.source, FORMAL_EVIDENCE_SOURCE)
        self.assertEqual(evidence.result, "incorrect")
        self.assertEqual(self.progress.quiz_attempts[-1].question_id, evidence.turn_id)

    def test_explicit_pass_goes_through_deeptutor_qualitative_gate(self):
        coherence = curriculum()[0].knowledge_points[2]
        self.assertFalse(is_assessed_mastered(self.progress, coherence))

        evidence = self.judge(COHERENCE_KP, True)

        self.assertIs(self.progress.qualitative_mastery[COHERENCE_KP], True)
        self.assertTrue(is_assessed_mastered(self.progress, coherence))
        self.assertEqual(evidence.assessment_type, "qualitative")
        self.assertEqual(evidence.source, FORMAL_EVIDENCE_SOURCE)

    def test_explicit_fail_records_a_failed_gate(self):
        self.judge(COHERENCE_KP, False)

        self.assertIs(self.progress.qualitative_mastery[COHERENCE_KP], False)

    def test_correct_answers_alone_never_pass_a_qualitative_gate(self):
        for _ in range(5):
            self.quiz(COHERENCE_KP, True)

        self.assertNotIn(COHERENCE_KP, self.progress.qualitative_mastery)
        self.assertFalse(is_assessed_mastered(self.progress, curriculum()[0].knowledge_points[2]))

    def test_superseding_an_attempt_removes_its_outcomes_before_the_regrade_is_applied(self):
        self.quiz(VOCABULARY_KP, False)
        self.quiz(VOCABULARY_KP, False)
        other_attempt = str(uuid4())
        self.quiz(VOCABULARY_KP, True, attempt_id=other_attempt)

        affected = self.service.supersede_external_assessment(
            self.progress, attempt_id=self.attempt_id, scheduler=self.scheduler
        )
        self.quiz(VOCABULARY_KP, True, version=2)

        self.assertEqual(affected, {VOCABULARY_KP})
        vocabulary_attempts = [a.is_correct for a in self.progress.quiz_attempts if a.knowledge_point_id == VOCABULARY_KP]
        self.assertEqual(vocabulary_attempts, [True, True])
        self.assertEqual(self.progress.mastery_levels[VOCABULARY_KP], compute_mastery([True, True]))
        self.assertEqual(len([e for e in self.progress.learning_evidence if e.knowledge_point_id == VOCABULARY_KP]), 2)
        self.assertEqual(self.progress.error_records, [])

    def test_superseding_restores_retention_state_from_remaining_evidence_via_deeptutor_replay(self):
        other_attempt = str(uuid4())
        self.quiz(GRAMMAR_KP, True, attempt_id=other_attempt)
        kept = [e for e in self.progress.learning_evidence if e.knowledge_point_id == GRAMMAR_KP]
        self.quiz(GRAMMAR_KP, False)

        self.service.supersede_external_assessment(self.progress, attempt_id=self.attempt_id, scheduler=self.scheduler)

        expected = self.scheduler.replay(self.progress.knowledge_types[GRAMMAR_KP], kept)
        self.assertEqual(self.progress.repetition_states[GRAMMAR_KP].model_dump(), expected.model_dump())

    def test_superseding_an_attempt_without_formal_evidence_changes_nothing(self):
        self.quiz(VOCABULARY_KP, True)
        before = self.progress.model_dump()

        affected = self.service.supersede_external_assessment(
            self.progress, attempt_id=str(uuid4()), scheduler=self.scheduler
        )

        self.assertEqual(affected, set())
        self.assertEqual(self.progress.model_dump(), before)


if __name__ == "__main__":
    unittest.main()
