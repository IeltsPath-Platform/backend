"""External (already graded) assessment evidence at the DeepTutor LearningService boundary.

DeepTutor v1.6.9 grades every answer itself (``grade_and_record`` ->
``_apply_grade`` -> ``grade_answer``); it has no entry point for an outcome that
another authority already graded. This subclass adds that entry point and only
sequences the pinned DeepTutor operations — attempt recording, error records,
evidence quality, mastery scoring, spaced repetition, review queue and the
qualitative gate all stay inside DeepTutor code. No threshold, formula or
schedule is implemented here.
"""

from __future__ import annotations

from deeptutor.learning.models import (
    ErrorType,
    LearningEvidence,
    LearningProgress,
    QuizAttempt,
)
from deeptutor.learning.scheduler import SpacedRepetitionScheduler
from deeptutor.learning.service import LearningService

from app.learning.formal_provenance import (
    FORMAL_EVIDENCE_SOURCE,
    formal_provenance,
    formal_source_reference,
)


class ExternalAssessmentLearningService(LearningService):
    def record_external_quiz_outcome(
        self,
        progress: LearningProgress,
        *,
        knowledge_point_id: str,
        module_id: str,
        is_correct: bool,
        error_type: ErrorType | None,
        provenance: str,
        source_reference_id: str,
        scheduler: SpacedRepetitionScheduler,
    ) -> LearningEvidence:
        """Fold one pre-graded outcome through DeepTutor's post-grade pipeline.

        Same sequence as ``LearningService._apply_grade`` after its grading step:
        record attempt -> record evidence -> recompute mastery -> advance the
        repetition state -> rebuild the review queue.
        """
        # ``_apply_grade`` marks evidence as "review" once the point is scheduled.
        already_scheduled = knowledge_point_id in progress.repetition_states
        self.record_quiz_attempt(
            progress,
            QuizAttempt(
                question_id=source_reference_id,
                knowledge_point_id=knowledge_point_id,
                module_id=module_id,
                is_correct=is_correct,
                user_answer=None,
                error_type=None if is_correct else error_type,
            ),
        )
        evidence = self._record_quiz_evidence(
            progress,
            knowledge_point_id,
            is_correct=is_correct,
            # ``retrying`` means re-answering a question with an open error record.
            # Each formal outcome has its own question identity, so none can be open.
            retrying=False,
            session_id=provenance,
            turn_id=source_reference_id,
            assessment_type="review" if already_scheduled else "quiz",
        )
        evidence.source = FORMAL_EVIDENCE_SOURCE
        self.update_mastery(
            progress, knowledge_point_id, self.calculate_mastery(progress, knowledge_point_id)
        )
        kp_type = progress.knowledge_types.get(knowledge_point_id)
        if kp_type is not None:
            state = progress.repetition_states.get(
                knowledge_point_id
            ) or scheduler.get_initial_state(kp_type)
            progress.repetition_states[knowledge_point_id] = state
            scheduler.schedule_review(state, kp_type, evidence)
            progress.review_queue = scheduler.build_review_queue(progress)
        return evidence

    def record_external_qualitative_outcome(
        self,
        progress: LearningProgress,
        *,
        knowledge_point_id: str,
        passed: bool,
        provenance: str,
        source_reference_id: str,
        scheduler: SpacedRepetitionScheduler,
    ) -> LearningEvidence:
        """Record an explicit qualitative judgment through DeepTutor's qualitative gate."""
        self.record_qualitative_in_memory(
            progress,
            knowledge_point_id,
            passed=passed,
            scheduler=scheduler,
            session_id=provenance,
            turn_id=source_reference_id,
        )
        evidence = progress.learning_evidence[-1]
        evidence.source = FORMAL_EVIDENCE_SOURCE
        return evidence

    def supersede_external_assessment(
        self,
        progress: LearningProgress,
        *,
        attempt_id: str,
        scheduler: SpacedRepetitionScheduler,
    ) -> set[str]:
        """Remove every formal outcome of an attempt so its newer result version replaces it.

        Returns the knowledge points whose derived state was rebuilt from the
        evidence that remains. Rebuilding uses DeepTutor's own recomputation seams:
        ``calculate_mastery`` over the remaining attempts and ``scheduler.replay``
        over the remaining evidence.
        """
        superseded_refs: set[str] = set()
        affected: set[str] = set()
        kept: list[LearningEvidence] = []
        for evidence in progress.learning_evidence:
            provenance = formal_provenance(evidence)
            reference = formal_source_reference(evidence)
            if provenance is not None and reference and provenance.attempt_id == attempt_id:
                superseded_refs.add(reference)
                affected.add(evidence.knowledge_point_id)
            else:
                kept.append(evidence)
        if not superseded_refs:
            return set()

        progress.learning_evidence = kept
        progress.quiz_attempts = [
            attempt for attempt in progress.quiz_attempts if attempt.question_id not in superseded_refs
        ]
        progress.error_records = [
            record for record in progress.error_records if record.question_id not in superseded_refs
        ]
        for knowledge_point_id in affected:
            self._rebuild_knowledge_point(progress, knowledge_point_id, scheduler)
        progress.review_queue = scheduler.build_review_queue(progress)
        return affected

    def _rebuild_knowledge_point(
        self,
        progress: LearningProgress,
        knowledge_point_id: str,
        scheduler: SpacedRepetitionScheduler,
    ) -> None:
        attempts = [a for a in progress.quiz_attempts if a.knowledge_point_id == knowledge_point_id]
        evidence = [
            e for e in progress.learning_evidence if e.knowledge_point_id == knowledge_point_id
        ]
        qualitative = [e for e in evidence if e.assessment_type == "qualitative"]

        if attempts:
            self.update_mastery(
                progress, knowledge_point_id, self.calculate_mastery(progress, knowledge_point_id)
            )
        else:
            progress.mastery_levels.pop(knowledge_point_id, None)

        # The qualitative gate of record is the latest recorded judgment
        # (``record_qualitative_in_memory`` writes result "correct" for a pass).
        if qualitative:
            progress.qualitative_mastery[knowledge_point_id] = qualitative[-1].result == "correct"
        else:
            progress.qualitative_mastery.pop(knowledge_point_id, None)

        kp_type = progress.knowledge_types.get(knowledge_point_id)
        if evidence and kp_type is not None:
            progress.repetition_states[knowledge_point_id] = scheduler.replay(kp_type, evidence)
        else:
            progress.repetition_states.pop(knowledge_point_id, None)
