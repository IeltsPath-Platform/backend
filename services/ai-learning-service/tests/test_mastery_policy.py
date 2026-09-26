# Adapted from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/tests/test_policy.py @ da856ad.
# Cases and expected values are kept verbatim; only imports changed.
# Omitted: the three objective_report cases (the service has no per-objective report).
"""Tests for the Mastery Path policy — the per-type gate and the gate-driven
"what's next" decision that replaced the old linear stage march.

These assert the two Alpha-style principles the old engine violated:

* a HARD gate — an objective is not mastered (and never advanced past) until
  its evidence clears the threshold;
* compression — an already-proven objective is skipped, never re-taught.
"""

from __future__ import annotations

import time

from app.mastery import policy
from app.mastery.models import (
    KnowledgePoint,
    KnowledgeType,
    LearnerMasteryOverride,
    LearningModule,
    LearningProgress,
    PendingQuestion,
    RepetitionState,
    ReviewTask,
)


def _progress(*kps: KnowledgePoint) -> LearningProgress:
    progress = LearningProgress(book_id="b1")
    progress.modules = [LearningModule(id="m1", name="M1", order=0, knowledge_points=list(kps))]
    progress.current_module_id = "m1"
    for kp in kps:
        progress.knowledge_types[kp.id] = kp.type
    return progress


def _kp(kp_id: str, kp_type: KnowledgeType, name: str = "") -> KnowledgePoint:
    return KnowledgePoint(id=kp_id, name=name or kp_id, type=kp_type, module_id="m1")


# ── per-type gate ──────────────────────────────────────────────────────────


def test_memory_gate_requires_high_quantitative_mastery():
    kp = _kp("kp1", KnowledgeType.MEMORY)
    progress = _progress(kp)
    progress.mastery_levels["kp1"] = 0.8
    assert policy.is_mastered(progress, kp) is False
    progress.mastery_levels["kp1"] = 0.9
    assert policy.is_mastered(progress, kp) is True


def test_procedure_gate_uses_same_quantitative_bar():
    kp = _kp("kp1", KnowledgeType.PROCEDURE)
    progress = _progress(kp)
    progress.mastery_levels["kp1"] = 0.89
    assert policy.is_mastered(progress, kp) is False


def test_concept_gate_is_qualitative_not_quantitative():
    """A high accuracy score must NOT unlock a concept — only the qualitative
    flag does (a concept is gated by an explanation, not string matching)."""
    kp = _kp("kp1", KnowledgeType.CONCEPT)
    progress = _progress(kp)
    progress.mastery_levels["kp1"] = 1.0  # accuracy is high…
    assert policy.is_mastered(progress, kp) is False  # …but the gate is qualitative
    progress.qualitative_mastery["kp1"] = True
    assert policy.is_mastered(progress, kp) is True


def test_objective_status_new_learning_mastered():
    kp = _kp("kp1", KnowledgeType.MEMORY)
    progress = _progress(kp)
    assert policy.objective_status(progress, kp) == "new"
    from app.mastery.models import QuizAttempt

    progress.quiz_attempts.append(
        QuizAttempt(question_id="q", knowledge_point_id="kp1", is_correct=False)
    )
    assert policy.objective_status(progress, kp) == "learning"
    progress.mastery_levels["kp1"] = 0.95
    assert policy.objective_status(progress, kp) == "mastered"


def test_learner_override_advances_without_faking_assessed_mastery():
    kp1, kp2 = _kp("kp1", KnowledgeType.MEMORY), _kp("kp2", KnowledgeType.MEMORY)
    progress = _progress(kp1, kp2)
    progress.mastery_levels["kp1"] = 0.2
    progress.learner_mastery_overrides["kp1"] = LearnerMasteryOverride(
        knowledge_point_id="kp1",
        note="Covered this in class",
    )

    assert policy.is_assessed_mastered(progress, kp1) is False
    assert policy.is_mastered(progress, kp1) is True
    assert policy.mastery_source(progress, kp1) == "learner"
    assert policy.next_objective(progress).knowledge_point_id == "kp2"

    summary = policy.map_summary(progress)
    first = summary["modules"][0]["knowledge_points"][0]
    assert first["status"] == "mastered"
    assert first["mastery_source"] == "learner"
    assert first["mastery"] == 0.2
    assert first["override_note"] == "Covered this in class"


# ── next_objective: gate is the cursor, mastered objectives are skipped ─────


def test_next_objective_skips_mastered_and_returns_first_open():
    kp1, kp2 = _kp("kp1", KnowledgeType.MEMORY), _kp("kp2", KnowledgeType.MEMORY)
    progress = _progress(kp1, kp2)
    progress.mastery_levels["kp1"] = 0.95  # already proven -> compression
    step = policy.next_objective(progress)
    assert step.knowledge_point_id == "kp2"
    assert step.action == "probe"


def test_next_objective_new_is_probe_then_practice_when_seen():
    kp = _kp("kp1", KnowledgeType.PROCEDURE)
    progress = _progress(kp)
    assert policy.next_objective(progress).action == "probe"
    from app.mastery.models import QuizAttempt

    progress.quiz_attempts.append(
        QuizAttempt(question_id="q", knowledge_point_id="kp1", is_correct=False)
    )
    assert policy.next_objective(progress).action == "practice"


def test_next_objective_qualitative_type_recommends_assess():
    kp = _kp("kp1", KnowledgeType.DESIGN)
    progress = _progress(kp)
    progress.qualitative_mastery["kp1"] = False  # seen but not passed
    assert policy.next_objective(progress).action == "assess"


def test_next_objective_pending_question_takes_precedence():
    kp = _kp("kp1", KnowledgeType.MEMORY)
    progress = _progress(kp)
    progress.pending_question = PendingQuestion(
        question_id="q1", knowledge_point_id="kp1", prompt="?", expected_answer="x"
    )
    step = policy.next_objective(progress)
    assert step.action == "answer_pending"
    assert step.pending_prompt == "?"


def test_pending_choice_context_is_stable_and_does_not_expose_answer():
    kp = _kp("kp1", KnowledgeType.MEMORY)
    progress = _progress(kp)
    progress.pending_question = PendingQuestion(
        question_id="question-stable-1",
        knowledge_point_id="kp1",
        prompt="Pick the blue option",
        question_type="choice",
        expected_answer="B",
        options=["A: red", "B: blue"],
    )

    payload = policy.next_objective(progress).to_dict()

    assert payload["pending_prompt"] == "Pick the blue option"  # legacy field
    assert payload["pending_question"] == {
        "question_id": "question-stable-1",
        "prompt": "Pick the blue option",
        "question_type": "choice",
        "options": [
            {"id": "A", "label": "A", "body": "red"},
            {"id": "B", "label": "B", "body": "blue"},
        ],
    }
    assert "expected_answer" not in payload["pending_question"]


def test_pending_non_choice_context_keeps_type_without_options_or_answer():
    kp = _kp("kp1", KnowledgeType.PROCEDURE)
    progress = _progress(kp)
    progress.pending_question = PendingQuestion(
        question_id="short-1",
        knowledge_point_id="kp1",
        prompt="Name the invariant",
        question_type="short",
        expected_answer="server secret",
    )

    pending = policy.next_objective(progress).to_dict()["pending_question"]

    assert pending == {
        "question_id": "short-1",
        "prompt": "Name the invariant",
        "question_type": "short",
        "options": [],
    }


def test_next_objective_due_review_beats_new_ground():
    kp1, kp2 = _kp("kp1", KnowledgeType.MEMORY), _kp("kp2", KnowledgeType.MEMORY)
    progress = _progress(kp1, kp2)
    progress.mastery_levels["kp1"] = 0.95  # mastered, but due for review
    progress.review_queue = [
        ReviewTask(
            id="r1",
            knowledge_point_id="kp1",
            knowledge_type=KnowledgeType.MEMORY,
            due_at=time.time() - 10,
            priority=1,
            state=RepetitionState(next_review_at=time.time() - 10),
        )
    ]
    step = policy.next_objective(progress)
    assert step.action == "review"
    assert step.knowledge_point_id == "kp1"
    assert step.forgetting_risk == 0.0


def test_due_reviews_order_by_forgetting_risk_before_priority():
    kp1, kp2 = _kp("kp_high", KnowledgeType.MEMORY), _kp("kp_low", KnowledgeType.MEMORY)
    progress = _progress(kp1, kp2)
    now = time.time()
    progress.review_queue = [
        ReviewTask(
            id="r_low",
            knowledge_point_id="kp_low",
            knowledge_type=KnowledgeType.MEMORY,
            due_at=now - 10,
            priority=1,
            state=RepetitionState(next_review_at=now - 10),
            forgetting_risk=0.2,
            reason="due now; retrievability 90%.",
        ),
        ReviewTask(
            id="r_high",
            knowledge_point_id="kp_high",
            knowledge_type=KnowledgeType.MEMORY,
            due_at=now - 10,
            priority=5,
            state=RepetitionState(next_review_at=now - 10),
            forgetting_risk=0.85,
            reason="due now; retrievability 40% below 90% target; 1 lapse.",
        ),
    ]
    due = policy.due_reviews(progress, now=now)
    assert [task.knowledge_point_id for task in due] == ["kp_high", "kp_low"]
    step = policy.next_objective(progress, now=now)
    assert step.knowledge_point_id == "kp_high"
    assert step.forgetting_risk == 0.85
    assert "retrievability" in step.reason


def test_next_objective_complete_when_all_mastered():
    kp = _kp("kp1", KnowledgeType.MEMORY)
    progress = _progress(kp)
    progress.mastery_levels["kp1"] = 0.95
    assert policy.next_objective(progress).action == "complete"


# ── map_summary ─────────────────────────────────────────────────────────────


def test_map_summary_counts_and_completion():
    kp1, kp2 = _kp("kp1", KnowledgeType.MEMORY), _kp("kp2", KnowledgeType.CONCEPT)
    progress = _progress(kp1, kp2)
    progress.mastery_levels["kp1"] = 0.95
    summary = policy.map_summary(progress)
    assert summary["counts"] == {"mastered": 1, "learning": 0, "new": 1, "total": 2}
    assert summary["complete"] is False
    progress.qualitative_mastery["kp2"] = True
    assert policy.map_summary(progress)["complete"] is True


# ── path_display_name ──────────────────────────────────────────────────────


def test_path_display_name_prefers_the_paths_own_name():
    progress = _progress(_kp("kp1", KnowledgeType.MEMORY))
    progress.name = "一元二次方程基础"
    assert policy.path_display_name(progress) == "一元二次方程基础"


def test_path_display_name_falls_back_to_the_first_module():
    """How every unnamed path was named before paths had names."""
    progress = _progress(_kp("kp1", KnowledgeType.MEMORY))
    assert policy.path_display_name(progress) == "M1"


def test_path_display_name_falls_back_to_the_id_with_no_modules():
    assert policy.path_display_name(LearningProgress(book_id="b1")) == "b1"


def test_path_display_name_ignores_a_blank_name():
    progress = _progress(_kp("kp1", KnowledgeType.MEMORY))
    progress.name = "   "
    assert policy.path_display_name(progress) == "M1"


def test_map_summary_carries_the_display_name():
    """So the dashboard and the composer strip read it instead of deriving it."""
    progress = _progress(_kp("kp1", KnowledgeType.MEMORY))
    progress.name = "Quadratics"
    assert policy.map_summary(progress)["name"] == "Quadratics"
