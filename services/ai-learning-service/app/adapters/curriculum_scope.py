"""Choose which canonical knowledge points belong in a learner's path for a goal.

This is content scoping, like choosing a syllabus: it decides what the path
contains before DeepTutor sees it. It makes no adaptive decision; DeepTutor's
policy alone decides what to learn next inside the scoped path.

Rule: a knowledge point is in scope when its effective ``bandMin`` (from
Content Service) is empty or not above the goal's target band. There is no
upper bound: points easier than the target stay in the path, and placement
test-out lets a learner skip them.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal, InvalidOperation
from typing import Any

_HIGHEST_BAND = Decimal("9.0")


class NoCurriculumInScope(Exception):
    """No active knowledge point matches the goal's target band."""


@dataclass(frozen=True)
class KnowledgePointBand:
    min: Decimal | None
    max: Decimal | None


@dataclass(frozen=True)
class ScopedCurriculum:
    topics: list[dict[str, Any]]
    knowledge_points: list[dict[str, Any]]
    # Effective band of every kept point, keyed by its canonical id.
    bands: dict[str, KnowledgePointBand] = field(default_factory=dict)
    excluded_count: int = 0


def parse_band(value: Any, name: str) -> Decimal | None:
    """An IELTS band (0.0-9.0, half-band steps) or ``None``; anything else is a contract error."""
    if value is None:
        return None
    if isinstance(value, bool):
        raise ValueError(f"{name} must be a number")
    try:
        band = Decimal(str(value))
    except InvalidOperation as exc:
        raise ValueError(f"{name} must be a number") from exc
    if not band.is_finite() or band < 0 or band > _HIGHEST_BAND or (band * 2) % 1 != 0:
        raise ValueError(f"{name} must be an IELTS band between 0.0 and 9.0 in half-band steps")
    return band.quantize(Decimal("0.1"))


def target_band_of(goal: dict[str, Any]) -> Decimal:
    """The learning goal's target band, which User Service requires on every goal."""
    band = parse_band(goal.get("targetBand"), "targetBand")
    if band is None:
        raise ValueError("User Service returned an active goal without targetBand")
    return band


class CurriculumScope:
    @staticmethod
    def select(
        topics: list[dict[str, Any]], knowledge_points: list[dict[str, Any]], target_band: Decimal
    ) -> ScopedCurriculum:
        kept: list[dict[str, Any]] = []
        bands: dict[str, KnowledgePointBand] = {}
        topics_with_points: set[str] = set()
        topics_kept: set[str] = set()
        for point in knowledge_points:
            topic_id = str(point.get("topicId", ""))
            topics_with_points.add(topic_id)
            band = KnowledgePointBand(
                parse_band(point.get("effectiveBandMin"), "effectiveBandMin"),
                parse_band(point.get("effectiveBandMax"), "effectiveBandMax"),
            )
            if band.min is not None and band.min > target_band:
                continue
            kept.append(point)
            bands[str(point["id"])] = band
            topics_kept.add(topic_id)
        if knowledge_points and not kept:
            raise NoCurriculumInScope
        # Drop a topic only when scoping emptied it; a topic that never had points
        # is passed through unchanged, as before scoping existed.
        scoped_topics = [
            topic for topic in topics
            if str(topic["id"]) in topics_kept or str(topic["id"]) not in topics_with_points
        ]
        return ScopedCurriculum(scoped_topics, kept, bands, len(knowledge_points) - len(kept))
