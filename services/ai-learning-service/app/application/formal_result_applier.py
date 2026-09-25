"""Apply finalized Assessment result versions to an existing DeepTutor path.

Orchestration only: version/idempotency decisions, dispatch to DeepTutor
operations and the transaction boundary. Every adaptive consequence is computed
by DeepTutor through ``ExternalAssessmentLearningService``.
"""

from __future__ import annotations

from dataclasses import dataclass, field
import logging
from typing import Any, Literal

from deeptutor.learning.policy import QUALITATIVE_TYPES, find_knowledge_point
from deeptutor.learning.scheduler import SpacedRepetitionScheduler

from app.adapters.formal_evidence_adapter import FormalAssessmentCommand, FormalEvidenceAdapter
from app.learning.external_assessment import ExternalAssessmentLearningService
from app.learning.placement_test_out import PLACEMENT, PlacementTestOut

logger = logging.getLogger(__name__)

EXPLICIT_QUALITATIVE_JUDGMENTS = {"PASS": True, "FAIL": False}


@dataclass(frozen=True)
class IngestionOutcome:
    status: Literal["applied", "duplicate", "stale", "pending"]
    # None while the result is pending: no path exists for its goal yet.
    path_id: str | None
    result_version: int
    recorded_evidence: tuple[str, ...] = ()
    superseded_knowledge_points: tuple[str, ...] = ()
    unknown_knowledge_points: tuple[str, ...] = field(default=())


class FormalResultApplier:
    def __init__(
        self,
        store: Any,
        learning: ExternalAssessmentLearningService | None = None,
        scheduler: SpacedRepetitionScheduler | None = None,
    ) -> None:
        self._store = store
        self._learning = learning or ExternalAssessmentLearningService(store)
        self._scheduler = scheduler or SpacedRepetitionScheduler()
        self._placement = PlacementTestOut(self._learning)

    def apply_pending(self, path_id: str, user_id: str, learning_goal_id: str) -> list[IngestionOutcome]:
        """Apply the parked results of ``(user_id, learning_goal_id)`` inside the open path transaction.

        Called while the path is being created, so the path, its curriculum and
        these results commit as one revision. Rows come back ordered by
        ``(attempt_id, result_version)``; the applied-version ledger keeps only
        the latest version of each attempt. A payload that no longer parses stays
        parked and does not block the path.
        """
        outcomes: list[IngestionOutcome] = []
        processed: list[str] = []
        for event_id, payload in self._store.pending_formal_results(path_id, user_id, learning_goal_id):
            try:
                command = FormalEvidenceAdapter.to_command(payload)
            except ValueError as exc:
                logger.warning("Pending AssessmentCompleted.v2 %s cannot be read; leaving it parked: %s",
                               event_id, exc)
                continue
            outcomes.append(self.apply_to_path(path_id, command))
            processed.append(event_id)
        if processed:
            self._store.delete_pending_formal_results(path_id, processed)
        return outcomes

    def apply_to_path(self, path_id: str, command: FormalAssessmentCommand) -> IngestionOutcome:
        # One PostgreSQL transaction under the mastery_paths row lock covers the
        # version decision, the aggregate mutation, one revision increment, the
        # evidence projection, mastery_events and the applied-version record.
        with self._store.transaction(path_id) as tx:
            applied = self._store.applied_result_version(path_id, command.attempt_id)
            if applied is not None and command.result_version <= applied:
                status = "duplicate" if command.result_version == applied else "stale"
                logger.info(
                    "Ignoring %s AssessmentCompleted.v2 %s: attempt %s already at version %s",
                    status, command.event_id, command.attempt_id, applied,
                )
                return IngestionOutcome(status, path_id, command.result_version)

            superseded: set[str] = set()
            if applied is not None:
                superseded = self._learning.supersede_external_assessment(
                    tx.progress, attempt_id=command.attempt_id, scheduler=self._scheduler
                )
            recorded, unknown = self._apply(tx, command)
            if command.assessment_type == PLACEMENT:
                # After the evidence: a point the evidence already masters needs no test-out.
                self._placement.apply(tx, path_id, command, self._store.knowledge_point_bands(path_id))
            self._store.record_applied_result(
                path_id,
                attempt_id=command.attempt_id,
                result_id=command.result_id,
                result_version=command.result_version,
                event_id=command.event_id,
            )
            tx.touch()
            tx.emit(
                "assessment.result_applied",
                {
                    "event_id": command.event_id,
                    "attempt_id": command.attempt_id,
                    "result_id": command.result_id,
                    "result_version": command.result_version,
                    "superseded_version": applied,
                    "recorded_evidence": len(recorded),
                    "unknown_knowledge_points": sorted(unknown),
                },
            )
        if unknown:
            logger.warning(
                "AssessmentCompleted.v2 %s referenced knowledge points outside path %s: %s",
                command.event_id, path_id, sorted(unknown),
            )
        return IngestionOutcome(
            "applied",
            path_id,
            command.result_version,
            recorded_evidence=tuple(recorded),
            superseded_knowledge_points=tuple(sorted(superseded)),
            unknown_knowledge_points=tuple(sorted(unknown)),
        )

    def _apply(self, tx: Any, command: FormalAssessmentCommand) -> tuple[list[str], set[str]]:
        recorded: list[str] = []
        unknown: set[str] = set()
        for item in command.items:
            provenance = command.provenance(item)
            for observation in item.knowledge_points:
                kp, module_id, _ = find_knowledge_point(tx.progress, observation.knowledge_point_id)
                if kp is None:
                    unknown.add(observation.knowledge_point_id)
                    continue
                passed = EXPLICIT_QUALITATIVE_JUDGMENTS.get(observation.qualitative_judgment or "")
                if kp.type in QUALITATIVE_TYPES and passed is not None:
                    evidence = self._learning.record_external_qualitative_outcome(
                        tx.progress,
                        knowledge_point_id=kp.id,
                        passed=passed,
                        provenance=provenance,
                        source_reference_id=observation.source_reference_id,
                        scheduler=self._scheduler,
                    )
                    tx.emit("mastery.assessed", {"knowledge_point_id": kp.id, "passed": passed})
                elif item.is_correct is not None:
                    # Quantitative gate for MEMORY/PROCEDURE; for CONCEPT/DESIGN DeepTutor
                    # treats an attempt as supporting evidence that never moves the gate.
                    evidence = self._learning.record_external_quiz_outcome(
                        tx.progress,
                        knowledge_point_id=kp.id,
                        module_id=module_id,
                        is_correct=item.is_correct,
                        error_type=observation.error_type,
                        provenance=provenance,
                        source_reference_id=observation.source_reference_id,
                        scheduler=self._scheduler,
                    )
                    tx.emit(
                        "attempt.recorded",
                        {
                            "knowledge_point_id": kp.id,
                            "is_correct": item.is_correct,
                            "source_reference_id": observation.source_reference_id,
                        },
                    )
                else:
                    # Neither explicit correctness nor an explicit judgment: an overall
                    # score or band is not evidence for this knowledge point.
                    continue
                tx.emit(
                    "evidence.recorded",
                    {
                        "knowledge_point_id": evidence.knowledge_point_id,
                        "assessment_type": evidence.assessment_type,
                        "result": evidence.result,
                        "quality": evidence.quality,
                        "source_reference_id": observation.source_reference_id,
                        "weight": str(observation.weight),
                    },
                )
                recorded.append(observation.source_reference_id)
        return recorded, unknown
