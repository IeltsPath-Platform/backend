"""Placement results mark the knowledge points a learner already masters as tested out.

Test-out goes through DeepTutor's learner mastery override; it never fakes
evidence and never touches mastery scores, gates, policy or the scheduler.
"""

import unittest
from decimal import Decimal
from uuid import uuid4

from deeptutor.learning.models import LearnerMasteryOverride
from deeptutor.learning.policy import next_objective

from app.adapters.curriculum_scope import KnowledgePointBand
from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_service import PathService
from app.learning.placement_test_out import PlacementTestOut, with_placement_provenance
from deeptutor.learning.policy import map_summary

from tests.formal_assessment_support import (
    COHERENCE_KP,
    GRAMMAR_KP,
    VOCABULARY_KP,
    InMemoryLearningStore,
    curriculum,
    event,
    item,
    mapping,
)

BANDS = {
    VOCABULARY_KP: KnowledgePointBand(Decimal("4.0"), Decimal("5.0")),
    GRAMMAR_KP: KnowledgePointBand(Decimal("6.0"), Decimal("7.0")),
    COHERENCE_KP: KnowledgePointBand(None, None),
}


def sources(progress):
    summary = with_placement_provenance(map_summary(progress), progress)
    return {kp["id"]: kp["mastery_source"] for module in summary["modules"] for kp in module["knowledge_points"]}


class PlacementTestOutTest(unittest.TestCase):
    def setUp(self):
        self.store = InMemoryLearningStore()
        self.paths = PathService(self.store)
        self.ingestion = FormalAssessmentIngestionService(self.store, self.paths)
        self.user_id, self.goal_id, self.attempt_id = str(uuid4()), str(uuid4()), str(uuid4())
        self.path_id, _ = self.paths.ensure_path(self.user_id, self.goal_id, curriculum(), bands=BANDS)

    def placement(self, items, *, band=None, version=1, assessment_type="PLACEMENT", attempt_id=None):
        payload = event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=attempt_id or self.attempt_id,
                        items=items, result_version=version, assessment_type=assessment_type)
        payload["data"]["overall_band"] = band
        return self.ingestion.ingest(FormalEvidenceAdapter.to_command(payload))

    def progress(self):
        return self.store.load(self.path_id)

    def test_points_whose_band_is_below_the_placement_band_are_tested_out(self):
        self.placement([item([mapping(GRAMMAR_KP)], is_correct=False)], band=5.5)

        progress = self.progress()
        self.assertEqual(sources(progress)[VOCABULARY_KP], "placement")
        self.assertEqual(sources(progress)[GRAMMAR_KP], "")
        self.assertEqual(next_objective(progress).knowledge_point_id, GRAMMAR_KP)

    def test_a_point_answered_correctly_on_every_placement_item_is_tested_out(self):
        self.placement([item([mapping(VOCABULARY_KP)], is_correct=True),
                        item([mapping(GRAMMAR_KP)], is_correct=False)])

        self.assertEqual(sources(self.progress())[VOCABULARY_KP], "placement")
        self.assertEqual(sources(self.progress())[GRAMMAR_KP], "")

    def test_one_wrong_placement_item_keeps_the_point_in_play(self):
        self.placement([item([mapping(VOCABULARY_KP)], is_correct=True),
                        item([mapping(VOCABULARY_KP)], is_correct=False)])

        self.assertEqual(sources(self.progress())[VOCABULARY_KP], "")

    def test_test_out_keeps_the_placement_evidence_and_does_not_fake_mastery(self):
        self.placement([item([mapping(VOCABULARY_KP)], is_correct=True)])

        progress = self.progress()
        self.assertEqual([a.is_correct for a in progress.quiz_attempts], [True])
        self.assertLess(progress.mastery_levels[VOCABULARY_KP], 0.9)

    def test_other_assessment_types_never_test_out(self):
        self.placement([item([mapping(VOCABULARY_KP)], is_correct=True)], band=9.0, assessment_type="QUIZ")

        self.assertEqual(self.progress().learner_mastery_overrides, {})

    def test_a_regraded_placement_replaces_its_earlier_test_out(self):
        self.placement([item([mapping(COHERENCE_KP, judgment="NOT_ASSESSED")], is_correct=None, score=1)], band=7.0)
        self.assertEqual({VOCABULARY_KP, GRAMMAR_KP}, set(self.progress().learner_mastery_overrides))

        self.placement([item([mapping(COHERENCE_KP, judgment="NOT_ASSESSED")], is_correct=None, score=1)],
                       band=5.0, version=2)

        self.assertEqual({VOCABULARY_KP}, set(self.progress().learner_mastery_overrides))
        self.assertTrue(self.progress().learner_mastery_overrides[VOCABULARY_KP].note.endswith(":v2"))

    def test_a_learner_override_is_never_replaced_or_cleared(self):
        with self.store.transaction(self.path_id) as tx:
            tx.progress.learner_mastery_overrides[VOCABULARY_KP] = LearnerMasteryOverride(
                knowledge_point_id=VOCABULARY_KP, note="I already know this")
            tx.touch()

        self.placement([item([mapping(VOCABULARY_KP)], is_correct=True)], band=9.0)
        self.placement([item([mapping(VOCABULARY_KP)], is_correct=False)], band=3.0, version=2)

        override = self.progress().learner_mastery_overrides[VOCABULARY_KP]
        self.assertEqual(override.note, "I already know this")
        self.assertEqual(sources(self.progress())[VOCABULARY_KP], "learner")

    def test_a_point_already_mastered_by_evidence_is_not_overridden(self):
        self.placement([item([mapping(COHERENCE_KP, judgment="PASS")], is_correct=None, score=1)])

        self.assertNotIn(COHERENCE_KP, self.progress().learner_mastery_overrides)
        self.assertEqual(sources(self.progress())[COHERENCE_KP], "system")

    def test_another_placement_attempt_keeps_the_first_attempts_test_out(self):
        self.placement([item([mapping(VOCABULARY_KP)], is_correct=True)])

        self.placement([item([mapping(GRAMMAR_KP)], is_correct=False)], attempt_id=str(uuid4()))

        self.assertIn(VOCABULARY_KP, self.progress().learner_mastery_overrides)

    def test_test_out_commits_in_the_same_revision_as_the_placement_evidence(self):
        revision_before = self.progress().version

        self.placement([item([mapping(VOCABULARY_KP)], is_correct=True)], band=5.0)

        self.assertEqual(self.progress().version, revision_before + 1)
        names = [name for pid, _, name, _ in self.store.committed_events if pid == self.path_id]
        self.assertIn("placement.tested_out", names)


class PendingPlacementTestOutTest(unittest.TestCase):
    def test_a_placement_parked_before_the_path_is_tested_out_when_the_path_is_created(self):
        store = InMemoryLearningStore()
        paths = PathService(store)
        user_id, goal_id = str(uuid4()), str(uuid4())
        payload = event(user_id=user_id, goal_id=goal_id, attempt_id=str(uuid4()),
                        items=[item([mapping(GRAMMAR_KP)], is_correct=False)], assessment_type="PLACEMENT")
        payload["data"]["overall_band"] = 5.5
        self.assertEqual(
            FormalAssessmentIngestionService(store, paths).ingest(FormalEvidenceAdapter.to_command(payload)).status,
            "pending")

        _, progress = paths.ensure_path(user_id, goal_id, curriculum(), bands=BANDS)

        self.assertEqual(sources(progress)[VOCABULARY_KP], "placement")
        self.assertEqual(next_objective(progress).knowledge_point_id, GRAMMAR_KP)


class SelectionTest(unittest.TestCase):
    def test_a_point_without_an_upper_band_is_never_tested_out_by_band(self):
        store = InMemoryLearningStore()
        path_id, progress = PathService(store).ensure_path(str(uuid4()), str(uuid4()), curriculum(), bands=BANDS)
        payload = event(user_id=str(uuid4()), goal_id=str(uuid4()), attempt_id=str(uuid4()), items=[],
                        assessment_type="PLACEMENT")
        payload["data"]["overall_band"] = 9.0

        selected = PlacementTestOut.select(progress, FormalEvidenceAdapter.to_command(payload), BANDS)

        self.assertEqual(selected, {VOCABULARY_KP, GRAMMAR_KP})


if __name__ == "__main__":
    unittest.main()
