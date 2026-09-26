"""Merge a freshly scoped curriculum into an existing path without losing learner state.

DeepTutor's ``replace_modules`` deletes the mastery, evidence, attempts and
overrides of every knowledge point missing from the new module set. A refresh
therefore only ever adds: every point already in the path is kept. Existing
module and point order stays fixed; new points join the end of their module,
and new modules join the end of the path. Content still owns names, types and
topic membership, so a moved point joins the end of its new module.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from deeptutor.learning.models import LearningModule


@dataclass(frozen=True)
class CurriculumMerge:
    modules: list[LearningModule]
    added: list[str]
    # Points already in the path that the fresh curriculum no longer contains.
    missing: list[str]


def _structure(modules: list[Any]) -> list[tuple]:
    return [
        (m.id, m.name, m.order, [(kp.id, kp.name, kp.type, kp.module_id) for kp in m.knowledge_points])
        for m in modules
    ]


def same_structure(left: list[Any], right: list[Any]) -> bool:
    return _structure(left) == _structure(right)


def merge_curriculum(current: list[LearningModule], fresh: list[LearningModule]) -> CurriculumMerge:
    fresh_points = {kp.id: (module.id, kp) for module in fresh for kp in module.knowledge_points}
    fresh_modules = {module.id: module for module in fresh}
    current_ids = {kp.id for module in current for kp in module.knowledge_points}
    merged = []
    by_id = {}
    placed: set[str] = set()
    missing: list[str] = []
    for module in sorted(current, key=lambda m: m.order):
        copied = fresh_modules.get(module.id, module).model_copy(deep=True)
        copied.knowledge_points = []
        for point in module.knowledge_points:
            latest = fresh_points.get(point.id)
            if latest is None:
                missing.append(point.id)
                updated = point
            elif latest[0] == module.id:
                updated = latest[1]
            else:
                # Content moved this point; append it to its destination below.
                continue
            copied.knowledge_points.append(updated.model_copy(deep=True))
            placed.add(point.id)
        merged.append(copied)
        by_id[copied.id] = copied
    for module in fresh:
        if module.id not in by_id:
            copied = module.model_copy(deep=True)
            copied.knowledge_points = []
            merged.append(copied)
            by_id[copied.id] = copied
        destination = by_id[module.id]
        for point in module.knowledge_points:
            if point.id not in placed:
                destination.knowledge_points.append(point.model_copy(deep=True))
                placed.add(point.id)
    for order, module in enumerate(merged):
        module.order = order
        for kp in module.knowledge_points:
            kp.module_id = module.id

    added = [kp.id for module in fresh for kp in module.knowledge_points if kp.id not in current_ids]
    return CurriculumMerge(merged, added, missing)
