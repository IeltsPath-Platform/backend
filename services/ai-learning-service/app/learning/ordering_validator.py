"""Strict validation of a proposed path order: a permutation of the existing modules and points, nothing else."""

from __future__ import annotations

from typing import Any

from app.mastery.models import LearningModule


class InvalidOrdering(ValueError):
    def __init__(self, reason: str) -> None:
        self.reason = reason
        super().__init__(reason)


class OrderingValidator:
    @staticmethod
    def apply(modules: list[LearningModule], proposal: Any) -> list[LearningModule]:
        if not isinstance(proposal, dict) or not isinstance(proposal.get("modules"), list):
            raise InvalidOrdering("malformed")
        by_id = {module.id: module for module in modules}
        owners = {point.id: module.id for module in modules for point in module.knowledge_points}
        seen_modules: set[str] = set()
        ordered = []
        for entry in proposal["modules"]:
            if not isinstance(entry, dict) or not isinstance(entry.get("id"), str):
                raise InvalidOrdering("malformed")
            module_id = entry["id"]
            point_ids = entry.get("knowledge_point_ids")
            if not isinstance(point_ids, list) or any(not isinstance(point_id, str) for point_id in point_ids):
                raise InvalidOrdering("malformed")
            if module_id in seen_modules:
                raise InvalidOrdering("duplicate_module")
            if module_id not in by_id:
                raise InvalidOrdering("unknown_module")
            seen_modules.add(module_id)
            original = by_id[module_id]
            points = {point.id: point for point in original.knowledge_points}
            seen_points: set[str] = set()
            for point_id in point_ids:
                if point_id in seen_points:
                    raise InvalidOrdering("duplicate_knowledge_point")
                if point_id not in owners:
                    raise InvalidOrdering("unknown_knowledge_point")
                if owners[point_id] != module_id:
                    raise InvalidOrdering("moved_knowledge_point")
                seen_points.add(point_id)
            if seen_points != points.keys():
                raise InvalidOrdering("missing_knowledge_point")
            copied = original.model_copy(deep=True)
            copied.order = len(ordered)
            copied_points = {point.id: point for point in copied.knowledge_points}
            copied.knowledge_points = [copied_points[point_id] for point_id in point_ids]
            ordered.append(copied)
        if seen_modules != by_id.keys():
            raise InvalidOrdering("missing_module")
        return ordered
