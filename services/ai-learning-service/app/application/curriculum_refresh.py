"""Merge a freshly scoped curriculum into an existing path without losing learner state.

DeepTutor's ``replace_modules`` deletes the mastery, evidence, attempts and
overrides of every knowledge point missing from the new module set. A refresh
therefore only ever adds: every point already in the path is kept. Points of
the fresh curriculum come first, in Content order; a kept point whose topic
is still in the curriculum stays in that module after its fresh points, and a
module that left the curriculum keeps its points at the end of the path.
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
    fresh_ids = {kp.id for module in fresh for kp in module.knowledge_points}
    current_ids = {kp.id for module in current for kp in module.knowledge_points}

    merged = [module.model_copy(deep=True) for module in fresh]
    by_id = {module.id: module for module in merged}
    missing: list[str] = []
    for module in sorted(current, key=lambda m: m.order):
        kept = [kp for kp in module.knowledge_points if kp.id not in fresh_ids]
        if not kept:
            continue
        missing.extend(kp.id for kp in kept)
        if module.id in by_id:
            by_id[module.id].knowledge_points.extend(kp.model_copy(deep=True) for kp in kept)
        else:
            leftover = module.model_copy(deep=True)
            leftover.knowledge_points = [kp.model_copy(deep=True) for kp in kept]
            merged.append(leftover)
            by_id[leftover.id] = leftover
    for order, module in enumerate(merged):
        module.order = order
        for kp in module.knowledge_points:
            kp.module_id = module.id

    added = [kp.id for module in fresh for kp in module.knowledge_points if kp.id not in current_ids]
    return CurriculumMerge(merged, added, missing)
