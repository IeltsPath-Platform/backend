"""Validate and persist learner-requested path ordering under one path lock."""

from __future__ import annotations

from dataclasses import dataclass

from app.application.curriculum_refresh import same_structure
from app.learning.ordering_validator import InvalidOrdering, OrderingValidator
from app.mastery.models import LearningProgress
from app.mastery.service import LearningService


@dataclass(frozen=True)
class ReorderResult:
    status: str
    progress: LearningProgress


class ReorderRejected(ValueError):
    def __init__(self, reason: str) -> None:
        self.reason = reason
        super().__init__(reason)


def _kp_overrides(entries: list[dict] | None) -> dict[str, list[str]]:
    if entries is None:
        return {}
    if not isinstance(entries, list):
        raise ReorderRejected("malformed")
    overrides = {}
    for entry in entries:
        if not isinstance(entry, dict) or not isinstance(entry.get("module_id"), str):
            raise ReorderRejected("malformed")
        module_id = entry["module_id"]
        point_ids = entry.get("knowledge_point_ids")
        if not isinstance(point_ids, list) or any(not isinstance(point_id, str) for point_id in point_ids):
            raise ReorderRejected("malformed")
        if module_id in overrides:
            raise ReorderRejected("duplicate_module")
        overrides[module_id] = point_ids
    return overrides


def reorder_path(
    learning: LearningService,
    path_id: str,
    *,
    module_ids: list[str] | None,
    knowledge_points: list[dict] | None,
    session_id: str = "",
    turn_id: str = "",
) -> ReorderResult:
    """Apply a partial ordering proposal without changing path membership."""
    if module_ids is None and knowledge_points is None:
        raise ReorderRejected("empty")
    if module_ids is not None and (
        not isinstance(module_ids, list) or any(not isinstance(module_id, str) for module_id in module_ids)
    ):
        raise ReorderRejected("malformed")

    with learning.store.transaction(path_id) as tx:
        current = sorted(tx.progress.modules, key=lambda module: module.order)
        current_points = {module.id: [point.id for point in module.knowledge_points] for module in current}
        overrides = _kp_overrides(knowledge_points)
        order = module_ids if module_ids is not None else [module.id for module in current]
        order_ids = set(order)
        if any(module_id not in order_ids for module_id in overrides):
            raise ReorderRejected("unknown_module")
        proposal = {"modules": [
            {"id": module_id, "knowledge_point_ids": overrides.get(module_id, current_points.get(module_id, []))}
            for module_id in order
        ]}
        try:
            ordered = OrderingValidator.apply(current, proposal)
        except InvalidOrdering as exc:
            raise ReorderRejected(exc.reason) from exc
        if same_structure(current, ordered):
            return ReorderResult("unchanged", tx.progress)
        progress = learning.replace_modules_for_path(
            path_id, ordered, event_type="path.reordered_by_learner", session_id=session_id, turn_id=turn_id)
        return ReorderResult("reordered", progress)
