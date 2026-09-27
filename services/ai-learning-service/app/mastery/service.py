# Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/service.py @ da856ad.
# Modified for IELTSPath: only the operations the service and the tutor call; the store is always injected
# (no default SQLite store).
"""Mastery path operations over an injected, transactional store."""

from __future__ import annotations

import time
from typing import TYPE_CHECKING, Literal
import uuid

from app.mastery.grading import classify_error, grade_answer
from app.mastery.mastery import compute_mastery
from app.mastery.models import (
    ErrorRecord,
    InteractionStatus,
    LearnerMasteryOverride,
    LearnerProfile,
    LearningEvidence,
    LearningModule,
    LearningProgress,
    MasteryInteraction,
    PendingOption,
    PendingQuestion,
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


class NoPendingInteractionError(MasteryInteractionError):
    """Raised when grading or resuming without an outstanding question."""


class StaleInteractionError(MasteryInteractionError):
    """Raised when a caller submits an answer for a superseded question."""

    def __init__(self, submitted_id: str, current_id: str) -> None:
        self.submitted_id = submitted_id
        self.current_id = current_id
        super().__init__(
            f"Question {submitted_id!r} is no longer pending; answer {current_id!r} instead"
        )


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

    def _apply_grade(
        self,
        progress: LearningProgress,
        *,
        question_id: str,
        knowledge_point_id: str,
        module_id: str,
        user_answer: str,
        expected_answer: str,
        question_type: str,
        self_attribution: str = "",
        scheduler: SpacedRepetitionScheduler | None = None,
        session_id: str = "",
        turn_id: str = "",
    ) -> bool:
        """Mutate one aggregate with a grade without performing I/O."""
        is_correct = bool(expected_answer) and grade_answer(
            user_answer, expected_answer, question_type
        )
        # Capture the active retry before recording this answer graduates it.
        # Past retries on this or another question must not weaken later reviews.
        retrying = any(
            rec.question_id == question_id
            and rec.knowledge_point_id == knowledge_point_id
            and rec.status in ("active", "retrying")
            for rec in progress.error_records
        )
        already_scheduled = knowledge_point_id in progress.repetition_states
        self.record_quiz_attempt(
            progress,
            QuizAttempt(
                question_id=question_id,
                knowledge_point_id=knowledge_point_id,
                module_id=module_id,
                is_correct=is_correct,
                user_answer=user_answer,
                self_attribution=self_attribution,
                error_type=None if is_correct else classify_error(user_answer),
            ),
        )
        evidence = None
        if knowledge_point_id:
            evidence = self._record_quiz_evidence(
                progress,
                knowledge_point_id,
                is_correct=is_correct,
                retrying=retrying,
                session_id=session_id,
                turn_id=turn_id,
                assessment_type="review" if already_scheduled else "quiz",
            )
            self.update_mastery(
                progress, knowledge_point_id, self.calculate_mastery(progress, knowledge_point_id)
            )
            kp_type = progress.knowledge_types.get(knowledge_point_id)
            if kp_type is not None and scheduler is not None:
                state = progress.repetition_states.get(
                    knowledge_point_id
                ) or scheduler.get_initial_state(kp_type)
                progress.repetition_states[knowledge_point_id] = state
                scheduler.schedule_review(state, kp_type, evidence)
                progress.review_queue = scheduler.build_review_queue(progress)
        return is_correct

    @staticmethod
    def _interaction_from_legacy_pending(
        progress: LearningProgress,
        *,
        session_id: str = "",
        turn_id: str = "",
    ) -> MasteryInteraction | None:
        pending = progress.pending_question
        if pending is None:
            return None
        return MasteryInteraction(
            interaction_id=pending.question_id,
            path_id=progress.book_id,
            question=pending,
            status=InteractionStatus.REGISTERED,
            session_id=session_id,
            turn_id=turn_id,
        )

    def register_question(
        self,
        book_id: str,
        pending: PendingQuestion,
        *,
        session_id: str = "",
        turn_id: str = "",
        require_current_objective: bool = False,
    ) -> tuple[LearningProgress, MasteryInteraction, bool]:
        """Atomically register one outstanding question.

        Retrying ``mastery_quiz`` while a question is active returns the
        existing interaction instead of overwriting its expected answer.
        """

        def register(tx):
            active = tx.active_interaction()
            if active is None:
                active = self._interaction_from_legacy_pending(
                    tx.progress, session_id=session_id, turn_id=turn_id
                )
                if active is not None:
                    persisted = tx.get_interaction(active.interaction_id)
                    if persisted is not None and persisted.status in {
                        InteractionStatus.GRADED,
                        InteractionStatus.ABANDONED,
                    }:
                        # Repair a legacy aggregate whose compatibility field
                        # survived after the durable interaction completed.
                        tx.progress.pending_question = None
                        tx.touch()
                        active = None
                    elif persisted is not None:
                        active = persisted
                    else:
                        tx.put_interaction(active)
            if active is not None:
                return active, False

            known_kp = next(
                (
                    kp
                    for module in tx.progress.modules
                    for kp in module.knowledge_points
                    if kp.id == pending.knowledge_point_id
                ),
                None,
            )
            if known_kp is None:
                raise MasteryInteractionError(
                    f"Unknown objective {pending.knowledge_point_id!r}; refresh mastery_status"
                )
            if require_current_objective:
                from app.mastery.policy import next_objective

                if next_objective(tx.progress).knowledge_point_id != pending.knowledge_point_id:
                    raise MasteryInteractionError("The requested knowledge point is not the current objective")

            interaction = MasteryInteraction(
                interaction_id=pending.question_id,
                path_id=book_id,
                question=pending,
                status=InteractionStatus.REGISTERED,
                session_id=session_id,
                turn_id=turn_id,
            )
            tx.progress.pending_question = pending
            tx.put_interaction(interaction)
            from app.mastery.pending import public_pending_question

            tx.emit(
                "interaction.registered",
                {
                    "interaction_id": interaction.interaction_id,
                    "knowledge_point_id": pending.knowledge_point_id,
                    "question": public_pending_question(pending).to_dict(),
                },
                session_id=session_id,
                turn_id=turn_id,
            )
            return interaction, True

        progress, result = self._store.mutate(book_id, register)
        interaction, created = result
        return progress, interaction, created

    def mark_question_awaiting(
        self,
        book_id: str,
        *,
        interaction_id: str = "",
        session_id: str = "",
        turn_id: str = "",
    ) -> MasteryInteraction | None:
        """Persist that an interaction card has been presented to the learner."""

        def mark(tx):
            interaction = (
                tx.get_interaction(interaction_id) if interaction_id else tx.active_interaction()
            )
            if interaction is None:
                active = tx.active_interaction()
                if active is not None and interaction_id:
                    raise StaleInteractionError(interaction_id, active.interaction_id)
                interaction = self._interaction_from_legacy_pending(
                    tx.progress, session_id=session_id, turn_id=turn_id
                )
                if interaction is None:
                    return None
                if interaction_id and interaction.interaction_id != interaction_id:
                    raise StaleInteractionError(interaction_id, interaction.interaction_id)
            if interaction.status == InteractionStatus.REGISTERED:
                interaction.status = InteractionStatus.AWAITING_INPUT
                interaction.session_id = session_id or interaction.session_id
                interaction.turn_id = turn_id or interaction.turn_id
                tx.put_interaction(interaction)
                tx.emit(
                    "interaction.awaiting_input",
                    {"interaction_id": interaction.interaction_id},
                    session_id=interaction.session_id,
                    turn_id=interaction.turn_id,
                )
            return interaction

        _, interaction = self._store.mutate(book_id, mark)
        return interaction

    def record_question_answer(
        self,
        book_id: str,
        answer: str,
        *,
        interaction_id: str = "",
        session_id: str = "",
        turn_id: str = "",
    ) -> MasteryInteraction | None:
        """Durably record a reply before the LLM gets another reasoning round."""

        def record(tx):
            interaction = (
                tx.get_interaction(interaction_id) if interaction_id else tx.active_interaction()
            )
            if interaction is None:
                active = tx.active_interaction()
                if active is not None and interaction_id:
                    raise StaleInteractionError(interaction_id, active.interaction_id)
                interaction = self._interaction_from_legacy_pending(
                    tx.progress, session_id=session_id, turn_id=turn_id
                )
            if interaction is None:
                return None
            if interaction_id and interaction.interaction_id != interaction_id:
                raise StaleInteractionError(interaction_id, interaction.interaction_id)
            if session_id and interaction.session_id and interaction.session_id != session_id:
                raise MasteryInteractionError("The question belongs to another session")
            if interaction.status in {
                InteractionStatus.REGISTERED,
                InteractionStatus.AWAITING_INPUT,
            }:
                interaction.status = InteractionStatus.ANSWERED
                interaction.user_answer = str(answer or "")
                interaction.session_id = session_id or interaction.session_id
                interaction.turn_id = turn_id or interaction.turn_id
                tx.put_interaction(interaction)
                tx.emit(
                    "interaction.answered",
                    {"interaction_id": interaction.interaction_id},
                    session_id=interaction.session_id,
                    turn_id=interaction.turn_id,
                )
            elif (
                interaction.status == InteractionStatus.ANSWERED
                and interaction.question.question_type == "choice"
            ):
                # Recover from a prior unreadable composer commit (#1004): allow
                # a later readable pick to replace the stalled user_answer.
                from app.mastery.pending import is_readable_choice_answer

                stored = str(interaction.user_answer or "")
                incoming = str(answer or "")
                option_map = interaction.question.choice_map
                if not is_readable_choice_answer(stored, option_map) and is_readable_choice_answer(
                    incoming, option_map
                ):
                    interaction.user_answer = incoming
                    interaction.session_id = session_id or interaction.session_id
                    interaction.turn_id = turn_id or interaction.turn_id
                    tx.put_interaction(interaction)
                    tx.emit(
                        "interaction.answered",
                        {"interaction_id": interaction.interaction_id},
                        session_id=interaction.session_id,
                        turn_id=interaction.turn_id,
                    )
            return interaction

        _, interaction = self._store.mutate(book_id, record)
        return interaction

    def grade_interaction(
        self,
        book_id: str,
        *,
        answer: str,
        question_id: str = "",
        answer_for_grading: str | None = None,
        expected_answer: str | None = None,
        resolved_choice_options: dict[str, str] | None = None,
        scheduler: SpacedRepetitionScheduler | None = None,
        session_id: str = "",
        turn_id: str = "",
        require_answered: bool = False,
    ) -> tuple[LearningProgress, MasteryInteraction, bool]:
        """Grade and resolve an interaction in one idempotent transaction.

        Returns ``(progress, interaction, replayed)``.  A retry carrying the
        same ``question_id`` returns the stored result and never appends a
        second attempt.
        """

        def grade(tx):
            interaction = tx.get_interaction(question_id) if question_id else None
            if interaction is None and not question_id:
                interaction = tx.active_interaction()
            if interaction is None:
                legacy = self._interaction_from_legacy_pending(
                    tx.progress, session_id=session_id, turn_id=turn_id
                )
                if legacy is not None and (not question_id or legacy.interaction_id == question_id):
                    interaction = legacy
                    tx.put_interaction(interaction)
            if interaction is None:
                active = tx.active_interaction()
                if active is not None and question_id:
                    raise StaleInteractionError(question_id, active.interaction_id)
                raise NoPendingInteractionError("No question is awaiting an answer")
            if question_id and interaction.interaction_id != question_id:
                raise StaleInteractionError(question_id, interaction.interaction_id)
            if session_id and interaction.session_id and interaction.session_id != session_id:
                raise MasteryInteractionError("The question belongs to another session")
            if interaction.status == InteractionStatus.GRADED:
                return interaction, True
            if interaction.status == InteractionStatus.ABANDONED:
                raise NoPendingInteractionError("The question was abandoned")
            if require_answered and interaction.status != InteractionStatus.ANSWERED:
                raise MasteryInteractionError("The learner has not answered this question")

            pending = interaction.question
            raw_answer = str(answer or "")
            if interaction.status == InteractionStatus.ANSWERED:
                stored = str(interaction.user_answer or "")
                if pending.question_type == "choice":
                    from app.mastery.pending import (
                        has_option_bodies,
                        is_readable_choice_answer,
                        resolve_choice_submission,
                    )

                    option_map = pending.choice_map
                    if is_readable_choice_answer(stored, option_map):
                        raw_answer = stored
                    elif is_readable_choice_answer(raw_answer, option_map):
                        # Prior commit was unreadable clarifying text (#1004) —
                        # accept the fresh readable answer and rewrite storage.
                        interaction.user_answer = raw_answer
                    else:
                        raw_answer = stored
                    if has_option_bodies(option_map):
                        graded_answer = (
                            resolve_choice_submission(raw_answer, option_map) or raw_answer
                        )
                    else:
                        # Legacy questions may need option bodies recovered by
                        # the trusted tool adapter from the original turn.
                        graded_answer = (
                            raw_answer if answer_for_grading is None else answer_for_grading
                        )
                else:
                    raw_answer = stored
                    graded_answer = raw_answer
            else:
                graded_answer = raw_answer if answer_for_grading is None else answer_for_grading
            authoritative_answer = (
                pending.expected_answer if expected_answer is None else expected_answer
            )
            if pending.question_type == "choice" and resolved_choice_options:
                # Bodies recovered for a legacy question (see the tool
                # adapter): store them in the structured form so nothing has
                # to recover them again.
                pending.options = [
                    PendingOption(label=label, body=body)
                    for label, body in resolved_choice_options.items()
                ]
                pending.expected_answer = authoritative_answer
                interaction.question = pending
            is_correct = self._apply_grade(
                tx.progress,
                question_id=pending.question_id,
                knowledge_point_id=pending.knowledge_point_id,
                module_id=pending.module_id,
                user_answer=graded_answer,
                expected_answer=authoritative_answer,
                question_type=pending.question_type,
                scheduler=scheduler,
                session_id=session_id,
                turn_id=turn_id,
            )
            if (
                tx.progress.pending_question is not None
                and tx.progress.pending_question.question_id == pending.question_id
            ):
                tx.progress.pending_question = None
            interaction.status = InteractionStatus.GRADED
            interaction.user_answer = raw_answer
            interaction.session_id = session_id or interaction.session_id
            interaction.turn_id = turn_id or interaction.turn_id
            interaction.result = {
                "is_correct": is_correct,
                "knowledge_point_id": pending.knowledge_point_id,
            }
            tx.put_interaction(interaction)
            tx.emit(
                "attempt.recorded",
                {
                    "interaction_id": interaction.interaction_id,
                    "knowledge_point_id": pending.knowledge_point_id,
                    "is_correct": is_correct,
                },
                session_id=interaction.session_id,
                turn_id=interaction.turn_id,
            )
            if tx.progress.learning_evidence:
                latest = tx.progress.learning_evidence[-1]
                if latest.knowledge_point_id == pending.knowledge_point_id:
                    tx.emit(
                        "evidence.recorded",
                        {
                            "knowledge_point_id": latest.knowledge_point_id,
                            "assessment_type": latest.assessment_type,
                            "result": latest.result,
                            "quality": latest.quality,
                        },
                        session_id=interaction.session_id,
                        turn_id=interaction.turn_id,
                    )
            tx.emit(
                "interaction.graded",
                dict(interaction.result),
                session_id=interaction.session_id,
                turn_id=interaction.turn_id,
            )
            return interaction, False

        progress, result = self._store.mutate(book_id, grade)
        interaction, replayed = result
        return progress, interaction, replayed

    def record_qualitative_for_path(
        self,
        book_id: str,
        kp_id: str,
        *,
        passed: bool,
        evidence: str = "",
        scheduler: SpacedRepetitionScheduler | None = None,
        session_id: str = "",
        turn_id: str = "",
        require_current_objective: bool = False,
    ) -> LearningProgress:
        def record(tx):
            from app.mastery.policy import QUALITATIVE_TYPES, find_knowledge_point

            kp, _, _ = find_knowledge_point(tx.progress, kp_id)
            if kp is None:
                raise MasteryInteractionError(
                    f"Unknown objective {kp_id!r}; refresh mastery_status"
                )
            if kp.type not in QUALITATIVE_TYPES:
                raise MasteryInteractionError(
                    f"Objective {kp.name!r} must be graded with mastery_quiz + mastery_grade"
                )
            if require_current_objective:
                from app.mastery.policy import next_objective

                if next_objective(tx.progress).knowledge_point_id != kp_id:
                    raise MasteryInteractionError("The requested knowledge point is not the current objective")
            self.record_qualitative_in_memory(
                tx.progress,
                kp_id,
                passed=passed,
                evidence=evidence,
                scheduler=scheduler,
                session_id=session_id,
                turn_id=turn_id,
            )
            tx.touch()
            tx.emit(
                "mastery.assessed",
                {
                    "knowledge_point_id": kp_id,
                    "passed": bool(passed),
                },
                session_id=session_id,
                turn_id=turn_id,
            )
            if tx.progress.learning_evidence:
                latest = tx.progress.learning_evidence[-1]
                if latest.knowledge_point_id == kp_id:
                    tx.emit(
                        "evidence.recorded",
                        {
                            "knowledge_point_id": latest.knowledge_point_id,
                            "assessment_type": latest.assessment_type,
                            "result": latest.result,
                            "quality": latest.quality,
                        },
                        session_id=session_id,
                        turn_id=turn_id,
                    )

        progress, _ = self._store.mutate(book_id, record)
        return progress

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


__all__ = ["LearningService", "MasteryInteractionError", "NoPendingInteractionError", "StaleInteractionError"]
