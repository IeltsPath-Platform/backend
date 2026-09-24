"""Route one finalized Assessment result version to the learner's DeepTutor path.

The path of the result's goal either exists, and the result is applied to it, or
does not exist yet, and the result is parked until the learner's first path
request creates it. The consumer cannot create the path itself: that needs the
learner's token to read the curriculum.
"""

from __future__ import annotations

import logging
from typing import Any

from deeptutor.learning.scheduler import SpacedRepetitionScheduler

from app.adapters.formal_evidence_adapter import FormalAssessmentCommand
from app.application.formal_result_applier import FormalResultApplier, IngestionOutcome
from app.application.path_service import PathNotBootstrapped, PathService
from app.learning.external_assessment import ExternalAssessmentLearningService

__all__ = ["FormalAssessmentIngestionService", "IngestionOutcome"]

logger = logging.getLogger(__name__)


class FormalAssessmentIngestionService:
    def __init__(
        self,
        store: Any,
        paths: PathService,
        learning: ExternalAssessmentLearningService | None = None,
        scheduler: SpacedRepetitionScheduler | None = None,
        applier: FormalResultApplier | None = None,
    ) -> None:
        self._store = store
        self._paths = paths
        self._applier = applier or FormalResultApplier(store, learning, scheduler)

    def ingest(self, command: FormalAssessmentCommand) -> IngestionOutcome:
        try:
            path_id, _ = self._paths.ensure_path(command.user_id, command.learning_goal_id)
        except PathNotBootstrapped:
            # Parking and path creation hold the same (user, goal) advisory lock, so
            # either this row is parked before the path commits (and creation applies
            # it) or the path is visible here and the result is applied normally.
            if self._store.park_formal_result(command):
                logger.info(
                    "Parked AssessmentCompleted.v2 %s: no path yet for goal %s",
                    command.event_id, command.learning_goal_id,
                )
                return IngestionOutcome("pending", None, command.result_version)
            path_id, _ = self._paths.ensure_path(command.user_id, command.learning_goal_id)
        return self._applier.apply_to_path(path_id, command)
