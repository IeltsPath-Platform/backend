# Adapted from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/tests/test_service_replace_merge.py @ da856ad.
# Cases and expected values are kept verbatim; the SQLite store is replaced by MemoryLearningStore.
# Omitted: the two init_modules cases (alias not ported) and TestGradeAndRecord (seven cases of the
# grade_and_record pipeline, which the service does not port).
"""Tests for the unified LearningService pipeline.

Covers module replacement (replace_modules / init_modules both have replace
semantics and purge stale per-KP state), the recency-weighted mastery policy
with its low-confidence cap, and the fail-closed grade_and_record pipeline that
records an attempt, recomputes mastery, advances the spaced-repetition state,
rebuilds the review queue, and persists.
"""

from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from app.mastery.models import (
    ErrorRecord,
    ErrorType,
    KnowledgePoint,
    KnowledgeType,
    LearningModule,
    LearningProgress,
    MasteryInteraction,
    PendingQuestion,
    RepetitionState,
    ReviewTask,
)
from app.mastery.service import LearningService
from tests.mastery_memory_store import MemoryLearningStore


def _make_kp(kp_id: str, module_id: str = "m1") -> KnowledgePoint:
    return KnowledgePoint(
        id=kp_id, name=f"KP {kp_id}", type=KnowledgeType.CONCEPT, module_id=module_id
    )


def _make_module(mod_id: str, kp_ids: list[str]) -> LearningModule:
    return LearningModule(
        id=mod_id,
        name=f"Module {mod_id}",
        order=0,
        knowledge_points=[_make_kp(kid, mod_id) for kid in kp_ids],
    )


# ── replace_modules / init_modules (replace semantics) ────────────────────


class TestReplaceModules:
    @staticmethod
    def _pending(question_id: str = "q1", kp_id: str = "kp2") -> PendingQuestion:
        return PendingQuestion(
            question_id=question_id,
            knowledge_point_id=kp_id,
            module_id="m1",
            prompt="Explain it",
            expected_answer="Clearly",
        )

    def test_route_reorder_keeps_current_objective_and_pending_interaction(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        service.replace_modules_for_path(
            "test", [_make_module("m1", ["kp1", "kp2"])], event_type="seed"
        )

        def prepare(tx):
            tx.progress.current_module_id = "m1"
            tx.progress.current_kp_index = 1
            tx.progress.pending_question = self._pending()
            tx.put_interaction(
                MasteryInteraction(
                    interaction_id="q1",
                    path_id="test",
                    question=self._pending(),
                    session_id="session-1",
                )
            )
            tx.touch()

        store.mutate("test", prepare)

        progress = service.replace_modules_for_path(
            "test", [_make_module("m1", ["kp2", "kp1"])], event_type="topic.map_edited"
        )

        assert progress.current_module_id == "m1"
        assert progress.current_kp_index == 0
        assert progress.pending_question is not None
        assert progress.pending_question.knowledge_point_id == "kp2"
        assert store.get_active_interaction("test") is not None

    def test_route_deletion_abandons_only_removed_pending_objective(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        service.replace_modules_for_path("test", [_make_module("m1", ["kp1", "kp2"])])

        def prepare(tx):
            tx.progress.current_module_id = "m1"
            tx.progress.current_kp_index = 1
            tx.progress.pending_question = self._pending()
            tx.put_interaction(
                MasteryInteraction(
                    interaction_id="q1",
                    path_id="test",
                    question=self._pending(),
                    session_id="session-1",
                )
            )
            tx.touch()

        store.mutate("test", prepare)

        progress = service.replace_modules_for_path(
            "test", [_make_module("m1", ["kp1"])], event_type="topic.map_edited"
        )

        assert progress.current_kp_index == 0
        assert progress.pending_question is None
        assert store.get_active_interaction("test") is None

    def test_append_to_empty_path_selects_first_module(self, tmp_path: Path):
        store = MemoryLearningStore()
        progress = LearningService(store).replace_modules_for_path(
            "test",
            [_make_module("incoming", ["incoming-kp"])],
            append=True,
        )

        assert progress.current_module_id == "test_m0"
        assert progress.current_kp_index == 0

    def test_atomic_appends_rebase_ids_without_losing_modules(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        service.replace_modules_for_path("test", [_make_module("seed", ["seed-kp"])])

        def append(index: int) -> None:
            LearningService(store).replace_modules_for_path(
                "test",
                [_make_module(f"incoming-{index}", [f"incoming-kp-{index}"])],
                append=True,
            )

        with ThreadPoolExecutor(max_workers=2) as executor:
            list(executor.map(append, [1, 2]))

        progress = store.load("test")
        assert progress is not None
        assert len(progress.modules) == 3
        assert [module.id for module in progress.modules] == [
            "seed",
            "test_m1",
            "test_m2",
        ]
        assert {kp.id for module in progress.modules[1:] for kp in module.knowledge_points} == {
            "test_m1_kp0",
            "test_m2_kp0",
        }

    def test_replace_removes_old_modules(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(
            progress, [_make_module("m1", ["kp1"]), _make_module("m2", ["kp2"])]
        )
        assert len(progress.modules) == 2

        service.replace_modules(progress, [_make_module("m3", ["kp3"])])
        assert len(progress.modules) == 1
        assert progress.modules[0].id == "m3"

    def test_replace_cleans_stale_mastery(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.mastery_levels["kp1"] = 0.8

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert "kp1" not in progress.mastery_levels

    def test_replace_cleans_stale_knowledge_types(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        assert "kp1" in progress.knowledge_types

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert "kp1" not in progress.knowledge_types
        assert "kp2" in progress.knowledge_types

    def test_replace_cleans_stale_repetition_states(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.repetition_states["kp1"] = RepetitionState(
            interval_index=0, consecutive_correct=0, consecutive_wrong=0, next_review_at=0
        )

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert "kp1" not in progress.repetition_states

    def test_replace_cleans_stale_learning_evidence(self, tmp_path: Path):
        from app.mastery.models import LearningEvidence

        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.learning_evidence.append(
            LearningEvidence(knowledge_point_id="kp1", result="correct")
        )

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert progress.learning_evidence == []

    def test_replace_cleans_stale_error_records(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.error_records.append(
            ErrorRecord(
                id="er1",
                question_id="q1",
                knowledge_point_id="kp1",
                module_id="m1",
                error_type=ErrorType.APPLICATION_ERROR,
            )
        )

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert len(progress.error_records) == 0

    def test_replace_cleans_stale_feynman_retries(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.feynman_retries["kp1"] = 2

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert "kp1" not in progress.feynman_retries

    def test_replace_cleans_stale_feynman_explanations(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.feynman_explanations["kp1"] = "user explanation text"

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert "kp1" not in progress.feynman_explanations

    def test_replace_cleans_stale_review_queue(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.review_queue.append(
            ReviewTask(
                id="rt1",
                knowledge_point_id="kp1",
                knowledge_type=KnowledgeType.CONCEPT,
                due_at=0,
                priority=1,
                state=RepetitionState(
                    interval_index=0, consecutive_correct=0, consecutive_wrong=0, next_review_at=0
                ),
            )
        )

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert len(progress.review_queue) == 0

    def test_replace_clears_stage_failure_records(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        progress.stage_failure_counts["explain"] = 4
        progress.stage_failure_notes["explain"] = "timeout"

        service.replace_modules(progress, [_make_module("m2", ["kp2"])])
        assert progress.stage_failure_counts == {}
        assert progress.stage_failure_notes == {}

    def test_replace_preserves_new_module_kps(self, tmp_path: Path):
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1"])])
        assert "kp1" in progress.knowledge_types
        assert progress.modules[0].knowledge_points[0].id == "kp1"

    def test_replace_keeps_state_for_surviving_kps(self, tmp_path: Path):
        """A KP that exists in both the old and new module set keeps its state."""
        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")

        service.replace_modules(progress, [_make_module("m1", ["kp1", "kp2"])])
        progress.mastery_levels["kp1"] = 0.8
        progress.mastery_levels["kp2"] = 0.3

        # kp1 survives into the new module set, kp2 is dropped.
        service.replace_modules(progress, [_make_module("m2", ["kp1"])])
        assert progress.mastery_levels["kp1"] == 0.8
        assert "kp2" not in progress.mastery_levels


# ── mastery policy (recency-weighted with low-confidence cap) ─────────────


class TestMasteryPolicy:
    def _service_with_attempts(self, tmp_path: Path, kp_id: str, outcomes: list[bool]):
        from app.mastery.models import QuizAttempt

        store = MemoryLearningStore()
        service = LearningService(store)
        progress = LearningProgress(book_id="test")
        for correct in outcomes:
            progress.quiz_attempts.append(
                QuizAttempt(question_id="q", knowledge_point_id=kp_id, is_correct=correct)
            )
        return service, progress

    def test_no_attempts_is_zero(self, tmp_path: Path):
        service, progress = self._service_with_attempts(tmp_path, "kp1", [])
        assert service.calculate_mastery(progress, "kp1") == 0.0

    def test_single_correct_attempt_is_capped_at_half(self, tmp_path: Path):
        """One lucky correct answer cannot declare a point mastered."""
        service, progress = self._service_with_attempts(tmp_path, "kp1", [True])
        assert service.calculate_mastery(progress, "kp1") == 0.5

    def test_single_wrong_attempt_is_zero(self, tmp_path: Path):
        service, progress = self._service_with_attempts(tmp_path, "kp1", [False])
        assert service.calculate_mastery(progress, "kp1") == 0.0

    def test_two_correct_attempts_capped_at_point_eight(self, tmp_path: Path):
        service, progress = self._service_with_attempts(tmp_path, "kp1", [True, True])
        assert service.calculate_mastery(progress, "kp1") == 0.8

    def test_three_plus_correct_can_reach_one(self, tmp_path: Path):
        service, progress = self._service_with_attempts(tmp_path, "kp1", [True, True, True])
        assert service.calculate_mastery(progress, "kp1") == 1.0

    def test_more_correct_attempts_score_higher_once_uncapped(self, tmp_path: Path):
        """With enough evidence (3+ attempts) the cap lifts, so a mostly-correct
        history scores strictly higher than a mostly-wrong one."""
        mostly_right, p_right = self._service_with_attempts(tmp_path, "kp1", [True, True, False])
        mostly_wrong, p_wrong = self._service_with_attempts(tmp_path, "kp2", [False, False, True])

        right_score = mostly_right.calculate_mastery(p_right, "kp1")
        wrong_score = mostly_wrong.calculate_mastery(p_wrong, "kp2")

        # Three attempts with two correct clears the single-attempt cap of 0.5.
        assert right_score > 0.5
        assert right_score > wrong_score
