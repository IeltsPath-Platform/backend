import threading
import unittest
from uuid import uuid4

from deeptutor.learning.mastery import compute_mastery
from deeptutor.learning.policy import next_objective

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.formal_result_applier import FormalResultApplier
from app.application.path_service import PathService

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


class FormalAssessmentIngestionTest(unittest.TestCase):
    def setUp(self):
        self.store = InMemoryLearningStore()
        self.paths = PathService(self.store)
        self.ingestion = FormalAssessmentIngestionService(self.store, self.paths)
        self.user_id, self.goal_id, self.attempt_id = str(uuid4()), str(uuid4()), str(uuid4())
        self.path_id, _ = self.paths.ensure_path(self.user_id, self.goal_id, curriculum())

    def ingest(self, payload):
        return self.ingestion.ingest(FormalEvidenceAdapter.to_command(payload))

    def result(self, items, *, version=1, result_id=None):
        return event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=self.attempt_id, items=items,
                     result_version=version, result_id=result_id)

    def vocabulary_attempts(self):
        return [a.is_correct for a in self.store.load(self.path_id).quiz_attempts
                if a.knowledge_point_id == VOCABULARY_KP]

    def test_first_delivery_is_applied_in_one_revision(self):
        revision_before = self.store.load(self.path_id).version

        outcome = self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)]))

        self.assertEqual(outcome.status, "applied")
        self.assertEqual(outcome.path_id, self.path_id)
        self.assertEqual(self.store.load(self.path_id).version, revision_before + 1)
        self.assertEqual(self.vocabulary_attempts(), [True])

    def test_same_delivery_twice_has_one_effect(self):
        payload = self.result([item([mapping(VOCABULARY_KP)], is_correct=True)])
        self.ingest(payload)
        state_after_first = self.store.load(self.path_id).model_dump()

        second = self.ingest(payload)

        self.assertEqual(second.status, "duplicate")
        self.assertEqual(self.store.load(self.path_id).model_dump(), state_after_first)

    def test_concurrent_duplicate_deliveries_have_one_effect(self):
        payload = self.result([item([mapping(VOCABULARY_KP)], is_correct=True)])
        start = threading.Barrier(4)
        outcomes = []

        def deliver():
            start.wait(timeout=10)
            outcomes.append(self.ingest(payload).status)

        workers = [threading.Thread(target=deliver) for _ in range(4)]
        for worker in workers:
            worker.start()
        for worker in workers:
            worker.join(timeout=20)

        self.assertCountEqual(outcomes, ["applied", "duplicate", "duplicate", "duplicate"])
        self.assertEqual(self.vocabulary_attempts(), [True])

    def test_result_versions_are_monotonic_and_a_regrade_replaces_the_superseded_version(self):
        version_one = self.result([item([mapping(VOCABULARY_KP)], is_correct=False)], version=1)
        version_two = self.result([item([mapping(VOCABULARY_KP)], is_correct=True)], version=2)
        version_three = self.result([item([mapping(VOCABULARY_KP)], is_correct=True)], version=3)

        self.assertEqual(self.ingest(version_one).status, "applied")
        self.assertEqual(self.ingest(version_one).status, "duplicate")
        self.assertEqual(self.ingest(version_two).status, "applied")
        # An older version arriving after a newer one is ignored.
        self.assertEqual(self.ingest(version_one).status, "stale")
        self.assertEqual(self.ingest(version_three).status, "applied")
        self.assertEqual(self.ingest(version_two).status, "stale")

        # Only the latest version remains: superseded versions are not double-counted.
        progress = self.store.load(self.path_id)
        self.assertEqual(self.vocabulary_attempts(), [True])
        self.assertEqual(progress.mastery_levels[VOCABULARY_KP], compute_mastery([True]))
        formal = [e for e in progress.learning_evidence if e.knowledge_point_id == VOCABULARY_KP]
        self.assertEqual(len(formal), 1)
        self.assertEqual(formal[0].result, "correct")

    def test_each_mapped_knowledge_point_receives_its_own_evidence_regardless_of_weight(self):
        outcome = self.ingest(self.result([item(
            [mapping(VOCABULARY_KP, weight=0.9), mapping(GRAMMAR_KP, weight=0.1)], is_correct=True)]))

        progress = self.store.load(self.path_id)
        self.assertEqual(len(outcome.recorded_evidence), 2)
        self.assertEqual(progress.mastery_levels[VOCABULARY_KP], progress.mastery_levels[GRAMMAR_KP])
        qualities = {e.knowledge_point_id: e.quality for e in progress.learning_evidence}
        self.assertEqual(qualities[VOCABULARY_KP], qualities[GRAMMAR_KP])

    def test_overall_writing_score_without_per_kp_judgment_leaves_qualitative_mastery_unchanged(self):
        outcome = self.ingest(self.result([item([mapping(COHERENCE_KP)], is_correct=None, score=8.0, max_score=9.0)]))

        progress = self.store.load(self.path_id)
        self.assertEqual(outcome.recorded_evidence, ())
        self.assertNotIn(COHERENCE_KP, progress.qualitative_mastery)
        self.assertEqual([e for e in progress.learning_evidence if e.knowledge_point_id == COHERENCE_KP], [])

    def test_not_assessed_judgment_leaves_qualitative_mastery_unchanged(self):
        self.ingest(self.result([item([mapping(COHERENCE_KP, judgment="NOT_ASSESSED")], is_correct=None,
                                      score=6.0, max_score=9.0)]))

        self.assertNotIn(COHERENCE_KP, self.store.load(self.path_id).qualitative_mastery)

    def test_explicit_pass_and_fail_judgments_update_the_qualitative_gate(self):
        self.ingest(self.result([item([mapping(COHERENCE_KP, judgment="PASS")], is_correct=None,
                                      score=7.0, max_score=9.0)]))
        self.assertIs(self.store.load(self.path_id).qualitative_mastery[COHERENCE_KP], True)

        self.ingest(self.result([item([mapping(COHERENCE_KP, judgment="FAIL")], is_correct=None,
                                      score=4.0, max_score=9.0)], version=2))
        self.assertIs(self.store.load(self.path_id).qualitative_mastery[COHERENCE_KP], False)

    def test_knowledge_points_outside_the_path_are_reported_not_invented(self):
        unknown = str(uuid4())

        outcome = self.ingest(self.result([item([mapping(unknown), mapping(VOCABULARY_KP)], is_correct=True)]))

        self.assertEqual(outcome.unknown_knowledge_points, (unknown,))
        self.assertEqual(len(outcome.recorded_evidence), 1)

    def test_event_for_a_goal_without_a_path_is_not_applied_to_another_path(self):
        """Was ``PathNotBootstrapped``; a result for a goal without a path is now parked as pending."""
        payload = event(user_id=self.user_id, goal_id=str(uuid4()), attempt_id=str(uuid4()),
                        items=[item([mapping(VOCABULARY_KP)], is_correct=True)])

        outcome = self.ingest(payload)

        self.assertEqual(outcome.status, "pending")
        self.assertIsNone(outcome.path_id)
        self.assertEqual(self.vocabulary_attempts(), [])

    def test_next_objective_changes_only_as_deeptutor_policy_decides(self):
        before = next_objective(self.store.load(self.path_id))
        self.assertEqual((before.action, before.knowledge_point_id), ("probe", VOCABULARY_KP))

        self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)]))
        after_one = next_objective(self.store.load(self.path_id))
        self.assertEqual((after_one.action, after_one.knowledge_point_id), ("practice", VOCABULARY_KP))

        for _ in range(5):
            self.attempt_id = str(uuid4())
            self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)], version=1))
        after_many = next_objective(self.store.load(self.path_id))
        self.assertEqual(after_many.knowledge_point_id, GRAMMAR_KP)


class PendingFormalResultTest(unittest.TestCase):
    """A finalized result for a goal whose path does not exist yet waits in the inbox."""

    def setUp(self):
        self.store = InMemoryLearningStore()
        self.paths = PathService(self.store)
        self.ingestion = FormalAssessmentIngestionService(self.store, self.paths)
        self.user_id, self.goal_id, self.attempt_id = str(uuid4()), str(uuid4()), str(uuid4())

    def ingest(self, payload):
        return self.ingestion.ingest(FormalEvidenceAdapter.to_command(payload))

    def result(self, items, *, version=1, goal_id=None, attempt_id=None):
        return event(user_id=self.user_id, goal_id=goal_id or self.goal_id, attempt_id=attempt_id or self.attempt_id,
                     items=items, result_version=version)

    def create_path(self, goal_id=None):
        path_id, progress = self.paths.ensure_path(self.user_id, goal_id or self.goal_id, curriculum())
        return path_id, progress

    def test_result_before_the_path_exists_is_parked_without_creating_a_path(self):
        outcome = self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)]))

        self.assertEqual(outcome.status, "pending")
        self.assertEqual(len(self.store.pending), 1)
        self.assertEqual(self.store.paths, {})

    def test_redelivery_while_pending_keeps_one_row(self):
        payload = self.result([item([mapping(VOCABULARY_KP)], is_correct=True)])

        self.assertEqual(self.ingest(payload).status, "pending")
        self.assertEqual(self.ingest(payload).status, "pending")

        self.assertEqual(len(self.store.pending), 1)

    def test_creating_the_path_applies_pending_results_in_the_same_revision(self):
        self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)]))
        commits_before = self.store.commits

        path_id, progress = self.create_path()

        self.assertEqual(self.store.commits, commits_before + 1)
        self.assertEqual(progress.version, 1)
        self.assertEqual([a.is_correct for a in progress.quiz_attempts], [True])
        self.assertEqual(self.store.load(path_id).version, 1)
        self.assertEqual(self.store.pending, {})

    def test_redelivery_after_the_pending_result_was_applied_is_a_duplicate(self):
        payload = self.result([item([mapping(VOCABULARY_KP)], is_correct=True)])
        self.ingest(payload)
        path_id, _ = self.create_path()

        outcome = self.ingest(payload)

        self.assertEqual(outcome.status, "duplicate")
        self.assertEqual(len(self.store.load(path_id).quiz_attempts), 1)

    def test_only_the_latest_pending_version_of_an_attempt_takes_effect(self):
        # Delivered out of order: the regrade arrives before the version it replaces.
        self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)], version=2))
        self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=False)], version=1))

        path_id, progress = self.create_path()

        self.assertEqual([a.is_correct for a in progress.quiz_attempts], [True])
        self.assertEqual(progress.mastery_levels[VOCABULARY_KP], compute_mastery([True]))
        self.assertEqual(self.store.pending, {})

    def test_pending_results_of_another_goal_are_not_applied(self):
        other_goal = str(uuid4())
        self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)], goal_id=other_goal))

        _, progress = self.create_path()

        self.assertEqual(progress.quiz_attempts, [])
        self.assertEqual(len(self.store.pending), 1)

    def test_unreadable_pending_payload_stays_parked_and_does_not_block_the_path(self):
        self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)]))
        broken_event_id = str(uuid4())
        self.store.pending[broken_event_id] = {
            "user_id": self.user_id, "learning_goal_id": self.goal_id, "attempt_id": str(uuid4()),
            "result_version": 1, "payload": {"event_type": "AssessmentCompleted.v1"},
        }

        with self.assertLogs("app.application.formal_result_applier", level="WARNING") as logs:
            _, progress = self.create_path()

        self.assertEqual(len(progress.quiz_attempts), 1)
        self.assertEqual(list(self.store.pending), [broken_event_id])
        self.assertIn(broken_event_id, "\n".join(logs.output))

    def test_failure_while_applying_pending_results_creates_no_path(self):
        self.ingest(self.result([item([mapping(VOCABULARY_KP)], is_correct=True)]))
        applier = FormalResultApplier(self.store)
        paths = PathService(self.store, applier=applier)
        applier.apply_to_path = lambda *_args, **_kwargs: (_ for _ in ()).throw(RuntimeError("ledger down"))

        with self.assertRaises(RuntimeError):
            paths.ensure_path(self.user_id, self.goal_id, curriculum())

        self.assertEqual(self.store.paths, {})
        self.assertEqual(len(self.store.pending), 1)


if __name__ == "__main__":
    unittest.main()
