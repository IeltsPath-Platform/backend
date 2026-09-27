# Scenarios adapted from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/tests/test_guided_mastery_updates.py
# and test_storage.py @ da856ad, run through the tutor's path: register_question -> record_question_answer ->
# grade_interaction, and record_qualitative_for_path. Expected values are DeepTutor's.
"""Question lifecycle of the ported mastery engine."""

import pytest

from app.mastery.models import (
    InteractionStatus, KnowledgePoint, KnowledgeType, LearningModule, PendingQuestion,
)
from app.mastery.scheduler import SpacedRepetitionScheduler
from app.mastery.service import (
    LearningService, MasteryInteractionError, NoPendingInteractionError, StaleInteractionError,
)
from tests.mastery_memory_store import MemoryLearningStore


def _service() -> tuple[MemoryLearningStore, LearningService]:
    store = MemoryLearningStore()
    service = LearningService(store)
    service.replace_modules_for_path("book1", [LearningModule(
        id="m1", name="Module 1", order=0,
        knowledge_points=[
            KnowledgePoint(id="kp1", name="Capitals", type=KnowledgeType.MEMORY, module_id="m1"),
            KnowledgePoint(id="kp2", name="Why capitals", type=KnowledgeType.CONCEPT, module_id="m1"),
        ],
    )])
    return store, service


def _question(question_id="q1", kp="kp1", expected="paris") -> PendingQuestion:
    return PendingQuestion(question_id=question_id, knowledge_point_id=kp, module_id="m1",
                           prompt="Capital of France?", expected_answer=expected)


def test_grade_interaction_persists_evidence_and_emits_event():
    store, service = _service()
    service.register_question("book1", _question(), session_id="sess-1", turn_id="turn-1")
    progress, _interaction, replayed = service.grade_interaction(
        "book1", answer="paris", question_id="q1", scheduler=SpacedRepetitionScheduler(),
        session_id="sess-1", turn_id="turn-1",
    )
    assert replayed is False
    assert progress.learning_evidence[-1].session_id == "sess-1"
    assert progress.learning_evidence[-1].turn_id == "turn-1"
    assert progress.learning_evidence[-1].quality == 1.0
    assert any(name == "evidence.recorded" for _path, name, _payload in store.events)


def test_registering_while_a_question_is_active_returns_the_existing_one():
    _store, service = _service()
    _progress, first, created = service.register_question("book1", _question("q1"))
    _progress, second, created_again = service.register_question("book1", _question("q2", expected="rome"))
    assert created is True and created_again is False
    assert second.interaction_id == first.interaction_id == "q1"
    assert second.question.expected_answer == "paris"


def test_unknown_objective_cannot_be_registered():
    _store, service = _service()
    with pytest.raises(MasteryInteractionError):
        service.register_question("book1", _question(kp="nope"))


def test_answer_is_recorded_before_grading_and_grading_uses_it():
    _store, service = _service()
    service.register_question("book1", _question())
    answered = service.record_question_answer("book1", "paris", interaction_id="q1")
    assert answered.status == InteractionStatus.ANSWERED
    progress, graded, _replayed = service.grade_interaction("book1", answer="ignored", question_id="q1")
    assert graded.result["is_correct"] is True
    assert progress.pending_question is None
    assert progress.quiz_attempts[-1].user_answer == "paris"


def test_answer_for_a_superseded_question_is_stale():
    _store, service = _service()
    service.register_question("book1", _question("q1"))
    with pytest.raises(StaleInteractionError):
        service.record_question_answer("book1", "paris", interaction_id="q-old")


def test_regrading_the_same_question_is_idempotent():
    _store, service = _service()
    service.register_question("book1", _question())
    service.grade_interaction("book1", answer="paris", question_id="q1")
    progress, _interaction, replayed = service.grade_interaction("book1", answer="paris", question_id="q1")
    assert replayed is True
    assert len(progress.quiz_attempts) == 1


def test_grading_without_a_question_fails():
    _store, service = _service()
    with pytest.raises(NoPendingInteractionError):
        service.grade_interaction("book1", answer="paris")


def test_an_open_error_on_another_question_does_not_weaken_a_correct_answer():
    _store, service = _service()
    service.register_question("book1", _question("q1"))
    service.grade_interaction("book1", answer="london", question_id="q1")
    service.register_question("book1", _question("q1-retry"))
    progress, _interaction, _replayed = service.grade_interaction("book1", answer="paris", question_id="q1-retry")
    assert [attempt.is_correct for attempt in progress.quiz_attempts] == [False, True]
    assert progress.error_records[0].status == "active"
    assert [event.quality for event in progress.learning_evidence] == [0.0, 1.0]


def test_qualitative_gate_is_only_for_concept_and_design():
    _store, service = _service()
    with pytest.raises(MasteryInteractionError):
        service.record_qualitative_for_path("book1", "kp1", passed=True)
    progress = service.record_qualitative_for_path("book1", "kp2", passed=True, evidence="Because it governs.")
    assert progress.qualitative_mastery["kp2"] is True
    assert progress.mastery_levels["kp2"] == 1.0
    assert progress.feynman_explanations["kp2"] == "Because it governs."
