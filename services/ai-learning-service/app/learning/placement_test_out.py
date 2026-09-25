"""Placement test-out: skip the knowledge points a placement shows the learner already masters.

A tested-out point is recorded as a DeepTutor learner mastery override, the
mechanism DeepTutor already offers for "may advance past this point without
the gate". ``is_mastered`` honours it, so ``next_objective`` skips the point.
The placement evidence itself is still recorded normally; test-out never fakes
evidence and never changes mastery scores, gates, policy or the scheduler.

DeepTutor's override has no provenance field and the pinned submodule is the
unmodified upstream release, so provenance lives in the override note:
``placement:{attempt_id}:v{result_version}``. ``mastery_source`` reports such
an override as ``placement`` instead of ``learner``.

A point is tested out by a ``PLACEMENT`` result when either
(a) its effective band has an upper end not above the placement's
    ``overall_band``, or
(b) every placement item mapped to it was answered correctly (or judged PASS)
    and at least one was.
"""

from __future__ import annotations

from typing import Any, Mapping

from deeptutor.learning.policy import find_knowledge_point, is_assessed_mastered, mastery_source as deeptutor_source

from app.adapters.curriculum_scope import KnowledgePointBand
from app.adapters.formal_evidence_adapter import FormalAssessmentCommand

PLACEMENT = "PLACEMENT"
NOTE_PREFIX = "placement:"
PLACEMENT_SOURCE = "placement"


def placement_note(attempt_id: str, result_version: int) -> str:
    return f"{NOTE_PREFIX}{attempt_id}:v{result_version}"


def is_placement_override(override: Any) -> bool:
    return str(getattr(override, "note", "")).startswith(NOTE_PREFIX)


def mastery_source(progress: Any, kp: Any) -> str:
    """DeepTutor's ``system``/``learner`` provenance, with placement overrides reported as ``placement``."""
    source = deeptutor_source(progress, kp)
    if source == "learner" and is_placement_override(progress.learner_mastery_overrides.get(kp.id)):
        return PLACEMENT_SOURCE
    return source


def with_placement_provenance(summary: dict[str, Any], progress: Any) -> dict[str, Any]:
    """Relabel ``map_summary`` entries whose override came from a placement."""
    for module in summary.get("modules", []):
        for entry in module.get("knowledge_points", []):
            override = progress.learner_mastery_overrides.get(entry["id"])
            if entry.get("mastery_source") == "learner" and is_placement_override(override):
                entry["mastery_source"] = PLACEMENT_SOURCE
    return summary


class PlacementTestOut:
    @staticmethod
    def select(
        progress: Any, command: FormalAssessmentCommand, bands: Mapping[str, KnowledgePointBand]
    ) -> set[str]:
        """Knowledge points of the path that this placement result tests out (reads only)."""
        if command.assessment_type != PLACEMENT:
            return set()
        selected: set[str] = set()
        if command.overall_band is not None:
            for kp_id, band in bands.items():
                if band.max is not None and band.max <= command.overall_band:
                    selected.add(kp_id)

        passed: set[str] = set()
        failed: set[str] = set()
        for item in command.items:
            for observation in item.knowledge_points:
                judgment = observation.qualitative_judgment
                if item.is_correct is True or judgment == "PASS":
                    passed.add(observation.knowledge_point_id)
                if item.is_correct is False or judgment == "FAIL":
                    failed.add(observation.knowledge_point_id)
        selected |= passed - failed

        return {
            kp_id for kp_id in selected
            if find_knowledge_point(progress, kp_id)[0] is not None
        }

    def __init__(self, learning: Any) -> None:
        # A DeepTutor LearningService: overrides are written through its own API.
        self._learning = learning

    def apply(self, tx: Any, path_id: str, command: FormalAssessmentCommand,
              bands: Mapping[str, KnowledgePointBand]) -> tuple[list[str], list[str]]:
        """Replace this attempt's placement test-out inside the open path transaction.

        Returns ``(tested_out, cleared)``. Overrides from another placement
        attempt stay; a learner's own override is never replaced or cleared.
        """
        if command.assessment_type != PLACEMENT:
            return [], []
        attempt_prefix = f"{NOTE_PREFIX}{command.attempt_id}:"
        overrides = tx.progress.learner_mastery_overrides
        cleared = sorted(kp_id for kp_id, override in overrides.items()
                         if str(override.note).startswith(attempt_prefix))
        for kp_id in cleared:
            self._learning.set_learner_mastery_override(path_id, kp_id, mastered=False)

        note = placement_note(command.attempt_id, command.result_version)
        tested_out: list[str] = []
        for kp_id in sorted(self.select(tx.progress, command, bands)):
            kp, _, _ = find_knowledge_point(tx.progress, kp_id)
            if kp_id in overrides or is_assessed_mastered(tx.progress, kp):
                # A learner claim or another placement already covers it, or evidence cleared the gate.
                continue
            self._learning.set_learner_mastery_override(path_id, kp_id, mastered=True, note=note)
            tested_out.append(kp_id)
        if tested_out or cleared:
            tx.emit("placement.tested_out", {
                "attempt_id": command.attempt_id,
                "result_version": command.result_version,
                "tested_out": tested_out,
                "cleared": cleared,
            })
        return tested_out, cleared
