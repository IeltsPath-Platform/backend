# Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/service.py @ da856ad.
# Modified for IELTSPath: only the operations the service calls; the store is always injected
# (no default SQLite store).
"""Mastery path operations over an injected, transactional store."""

from __future__ import annotations

import time
from typing import TYPE_CHECKING, Literal
import uuid

from app.mastery.mastery import compute_mastery
from app.mastery.models import (
    ErrorRecord,
    LearnerMasteryOverride,
    LearnerProfile,
    LearningEvidence,
    LearningModule,
    LearningProgress,
    QuizAttempt,
    RetryAttempt,
)
from app.mastery.store import LearningStore

if TYPE_CHECKING:
    from app.mastery.scheduler import SpacedRepetitionScheduler


# Long enough for a course title, short enough that a list row stays a row.
# Matches the cap module and objective names already use.
_MAX_PATH_NAME_LEN = 200
#: One intake answer. Free text, but a paragraph is an answer and a chapter is
#: a paste — and the whole profile is injected into every turn's status.
_MAX_PROFILE_FIELD_LEN = 600
#: The intake fields a caller may set. Named here so the tool schema, the REST
#: layer and this merge cannot drift apart.
_LEARNER_PROFILE_FIELDS: tuple[str, ...] = (
    "prior_knowledge",
    "target_level",
    "time_budget",
    "preferences",
    "notes",
)


class MasteryInteractionError(RuntimeError):
    """Base error for invalid durable question lifecycle transitions."""


class LearningService:
    def __init__(self, store: LearningStore) -> None:
        self._store = store

    @property
    def store(self) -> LearningStore:
        """Expose the persistence boundary for read-only interaction queries."""
        return self._store

    def get_or_create(self, book_id: str) -> LearningProgress:
        # The store serializes creation under its path lock, so two callers
        # cannot both manufacture revision 1 and race to overwrite one another.
        with self._store.transaction(book_id, create=True) as tx:
            return tx.progress

    def replace_modules(self, progress: LearningProgress, modules: list[LearningModule]) -> None:
        """Replace all modules and clean stale KP state."""
        new_kp_ids = {kp.id for m in modules for kp in m.knowledge_points}

        # Clean stale KP state
        for key in list(progress.mastery_levels.keys()):
            if key not in new_kp_ids:
                del progress.mastery_levels[key]
        for key in list(progress.knowledge_types.keys()):
            if key not in new_kp_ids:
                del progress.knowledge_types[key]
        for key in list(progress.qualitative_mastery.keys()):
            if key not in new_kp_ids:
                del progress.qualitative_mastery[key]
        for key in list(progress.repetition_states.keys()):
            if key not in new_kp_ids:
                del progress.repetition_states[key]
        for key in list(progress.learner_mastery_overrides.keys()):
            if key not in new_kp_ids:
                del progress.learner_mastery_overrides[key]
        progress.error_records = [
            r for r in progress.error_records if r.knowledge_point_id in new_kp_ids
        ]
        progress.quiz_attempts = [
            attempt
            for attempt in progress.quiz_attempts
            if attempt.knowledge_point_id in new_kp_ids
        ]
        progress.learning_evidence = [
            event for event in progress.learning_evidence if event.knowledge_point_id in new_kp_ids
        ]
        progress.feynman_retries = {
            k: v for k, v in progress.feynman_retries.items() if k in new_kp_ids
        }
        progress.feynman_explanations = {
            k: v for k, v in progress.feynman_explanations.items() if k in new_kp_ids
        }
        progress.review_queue = [
            t for t in progress.review_queue if t.knowledge_point_id in new_kp_ids
        ]
        # Clear global stage failure records — different modules should not share failure counts
        progress.stage_failure_counts = {}
        progress.stage_failure_notes = {}

        # Set new modules
        progress.modules = list(modules)
        for mod in modules:
            for kp in mod.knowledge_points:
                progress.knowledge_types[kp.id] = kp.type

    def record_quiz_attempt(self, progress: LearningProgress, attempt: QuizAttempt) -> None:
        if not attempt.is_correct and attempt.error_type is not None:
            # Find existing error record for this question + knowledge point.
            existing = None
            for rec in progress.error_records:
                if (
                    rec.question_id == attempt.question_id
                    and rec.knowledge_point_id == attempt.knowledge_point_id
                ):
                    existing = rec
                    break

            if existing is not None:
                existing.retry_history.append(
                    RetryAttempt(
                        timestamp=time.time(),
                        is_correct=False,
                        attempt_number=len(existing.retry_history) + 1,
                    )
                )
                existing.status = "retrying"
            else:
                record = ErrorRecord(
                    id=uuid.uuid4().hex,
                    question_id=attempt.question_id,
                    knowledge_point_id=attempt.knowledge_point_id,
                    module_id=attempt.module_id,
                    error_type=attempt.error_type,
                    self_attribution=attempt.self_attribution,
                    status="active",
                )
                progress.error_records.append(record)

        elif attempt.is_correct:
            # Graduate any active error record for this question + knowledge point.
            for rec in progress.error_records:
                if (
                    rec.question_id == attempt.question_id
                    and rec.knowledge_point_id == attempt.knowledge_point_id
                    and rec.status in ("active", "retrying")
                ):
                    rec.retry_history.append(
                        RetryAttempt(
                            timestamp=time.time(),
                            is_correct=True,
                            attempt_number=len(rec.retry_history) + 1,
                        )
                    )
                    rec.status = "graduated"
                    break

        progress.quiz_attempts.append(attempt)
        progress.updated_at = time.time()

    def calculate_mastery(self, progress: LearningProgress, kp_id: str) -> float:
        """Mastery 0..1 for *kp_id* from its attempt history (policy in mastery.py)."""
        correctness = [
            a.is_correct for a in progress.quiz_attempts if a.knowledge_point_id == kp_id
        ]
        return compute_mastery(correctness)

    def update_mastery(self, progress: LearningProgress, kp_id: str, level: float) -> None:
        progress.mastery_levels[kp_id] = level
        progress.updated_at = time.time()

    def _record_quiz_evidence(
        self,
        progress: LearningProgress,
        kp_id: str,
        *,
        is_correct: bool,
        retrying: bool = False,
        session_id: str = "",
        turn_id: str = "",
        assessment_type: Literal["quiz", "qualitative", "review"] = "quiz",
    ) -> LearningEvidence:
        attempt_count = sum(
            1 for attempt in progress.quiz_attempts if attempt.knowledge_point_id == kp_id
        )
        evidence = LearningEvidence(
            knowledge_point_id=kp_id,
            assessment_type=assessment_type,
            result="correct" if is_correct else "incorrect",
            quality=(0.6 if retrying else 1.0) if is_correct else 0.0,
            attempt_count=max(1, attempt_count),
            session_id=session_id,
            turn_id=turn_id,
        )
        progress.learning_evidence.append(evidence)
        return evidence

    def replace_modules_for_path(
        self,
        book_id: str,
        modules: list[LearningModule],
        *,
        append: bool = False,
        name: str = "",
        event_type: str = "path.modules_replaced",
        session_id: str = "",
        turn_id: str = "",
    ) -> LearningProgress:
        """Install a module set, optionally naming a path that has no name yet.

        ``name`` is applied only when the path is still unnamed, in the same
        transaction as the modules so a built path is never briefly nameless.
        Replacing the map deliberately does NOT rename: the map is what the
        path teaches, the name is which path it is — deriving one from the
        other is what made a rebuild look like a different course.
        """

        def replace(tx):
            if name.strip() and not tx.progress.name.strip():
                tx.progress.name = name.strip()[:_MAX_PATH_NAME_LEN]
            applied_modules = [module.model_copy(deep=True) for module in modules]
            if append:
                offset = len(tx.progress.modules)
                for index, module in enumerate(applied_modules, start=offset):
                    module.id = f"{book_id}_m{index}"
                    module.order = index
                    for kp_index, kp in enumerate(module.knowledge_points):
                        kp.module_id = module.id
                        kp.id = f"{module.id}_kp{kp_index}"
                        tx.progress.knowledge_types[kp.id] = kp.type
                tx.progress.modules.extend(applied_modules)
                if not tx.progress.current_module_id and applied_modules:
                    tx.progress.current_module_id = applied_modules[0].id
                    tx.progress.current_kp_index = 0
            else:
                current_kp_id = ""
                for current_module in tx.progress.modules:
                    if current_module.id != tx.progress.current_module_id:
                        continue
                    if 0 <= tx.progress.current_kp_index < len(current_module.knowledge_points):
                        current_kp_id = current_module.knowledge_points[
                            tx.progress.current_kp_index
                        ].id
                    break
                pending_kp_id = (
                    tx.progress.pending_question.knowledge_point_id
                    if tx.progress.pending_question is not None
                    else ""
                )
                active_interaction = tx.active_interaction()
                active_kp_id = (
                    active_interaction.question.knowledge_point_id
                    if active_interaction is not None
                    else ""
                )

                self.replace_modules(tx.progress, applied_modules)
                objective_locations = {
                    kp.id: (module.id, kp_index)
                    for module in applied_modules
                    for kp_index, kp in enumerate(module.knowledge_points)
                }

                if current_kp_id in objective_locations:
                    (
                        tx.progress.current_module_id,
                        tx.progress.current_kp_index,
                    ) = objective_locations[current_kp_id]
                elif applied_modules:
                    tx.progress.current_module_id = applied_modules[0].id
                    tx.progress.current_kp_index = 0
                else:
                    tx.progress.current_module_id = ""
                    tx.progress.current_kp_index = 0

                if tx.progress.pending_question is not None:
                    if pending_kp_id not in objective_locations:
                        tx.progress.pending_question = None
                    else:
                        pending_module_id, _ = objective_locations[pending_kp_id]
                        tx.progress.pending_question.module_id = pending_module_id

                if active_interaction is not None:
                    if active_kp_id not in objective_locations:
                        tx.abandon_active_interactions()
                    else:
                        active_module_id, _ = objective_locations[active_kp_id]
                        if active_interaction.question.module_id != active_module_id:
                            active_interaction.question.module_id = active_module_id
                            tx.put_interaction(active_interaction)
            tx.touch()
            tx.emit(
                event_type,
                {
                    "mode": "append" if append else "replace",
                    "module_count": len(applied_modules),
                    "knowledge_point_count": sum(
                        len(module.knowledge_points) for module in applied_modules
                    ),
                },
                session_id=session_id,
                turn_id=turn_id,
            )

        progress, _ = self._store.mutate(book_id, replace, create=True)
        return progress

    def record_learner_profile(
        self,
        book_id: str,
        *,
        fields: dict[str, str],
        session_id: str = "",
        turn_id: str = "",
    ) -> tuple[LearningProgress, list[str]]:
        """Merge intake answers into this goal's learner profile.

        Merge, never replace: intake is not a single moment. The first session
        asks four questions, and months later "我时间变少了" has to be able to
        change one of them without wiping the other three. Only fields the
        caller actually names are touched, so an omitted field keeps whatever
        the learner said about it before.

        Returns the progress and the names of the fields that really changed,
        so the caller can tell the learner what it recorded rather than
        claiming to have recorded everything it was handed.
        """
        cleaned = {
            key: str(value or "").strip()[:_MAX_PROFILE_FIELD_LEN]
            for key, value in fields.items()
            if key in _LEARNER_PROFILE_FIELDS and value is not None
        }

        def record(tx):
            profile = tx.progress.learner_profile or LearnerProfile()
            changed = [key for key, value in cleaned.items() if getattr(profile, key) != value]
            if not changed:
                return []
            updated = profile.model_copy(update={**cleaned, "updated_at": time.time()})
            tx.progress.learner_profile = updated
            tx.touch()
            tx.emit(
                "path.learner_profile_recorded",
                {"fields": changed},
                session_id=session_id,
                turn_id=turn_id,
            )
            return changed

        return self._store.mutate(book_id, record, create=True)

    def set_learner_mastery_override(
        self,
        book_id: str,
        kp_id: str,
        *,
        mastered: bool,
        note: str = "",
    ) -> LearningProgress:
        """Set or clear an explicit learner claim without changing evidence."""

        def update(tx):
            from app.mastery.policy import find_knowledge_point

            kp, _, _ = find_knowledge_point(tx.progress, kp_id)
            if kp is None:
                raise MasteryInteractionError(f"Unknown objective {kp_id!r}")
            if mastered:
                tx.progress.learner_mastery_overrides[kp_id] = LearnerMasteryOverride(
                    knowledge_point_id=kp_id,
                    note=str(note or "").strip()[:500],
                )
                event_type = "mastery.overridden"
            else:
                if kp_id not in tx.progress.learner_mastery_overrides:
                    return
                tx.progress.learner_mastery_overrides.pop(kp_id, None)
                event_type = "mastery.override_cleared"
            tx.touch()
            tx.emit(
                event_type,
                {"knowledge_point_id": kp_id, "mastered": bool(mastered)},
            )

        progress, _ = self._store.mutate(book_id, update)
        return progress

    @staticmethod
    def record_qualitative_in_memory(
        progress: LearningProgress,
        kp_id: str,
        *,
        passed: bool,
        evidence: str = "",
        scheduler: SpacedRepetitionScheduler | None = None,
        session_id: str = "",
        turn_id: str = "",
    ) -> None:
        progress.qualitative_mastery[kp_id] = bool(passed)
        current = progress.mastery_levels.get(kp_id, 0.0)
        progress.mastery_levels[kp_id] = max(current, 1.0) if passed else min(current, 0.4)
        if evidence:
            progress.feynman_explanations[kp_id] = evidence
        review_evidence = LearningEvidence(
            knowledge_point_id=kp_id,
            assessment_type="qualitative",
            result="correct" if passed else "partial",
            quality=(1.0 if evidence else 0.9) if passed else 0.2,
            attempt_count=1,
            session_id=session_id,
            turn_id=turn_id,
        )
        progress.learning_evidence.append(review_evidence)
        kp_type = progress.knowledge_types.get(kp_id)
        if kp_type is not None and scheduler is not None:
            state = progress.repetition_states.get(kp_id)
            if state is not None and state.next_review_at <= time.time():
                scheduler.schedule_review(state, kp_type, review_evidence)
            elif state is None and passed:
                progress.repetition_states[kp_id] = scheduler.get_initial_state(kp_type)
            progress.review_queue = scheduler.build_review_queue(progress)
        progress.updated_at = time.time()


__all__ = ["LearningService", "MasteryInteractionError"]
