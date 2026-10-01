"""Content metadata copied into a path for each knowledge point, and IELTS band parsing.

The path keeps a snapshot of each point's skill, description and whether Content has a practice set for it, so
tutor tools and the review rule never need a Content token or an HTTP call.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from typing import Any

_HIGHEST_BAND = Decimal("9.0")
_MAX_DESCRIPTION = 1000


@dataclass(frozen=True)
class KnowledgePointDetails:
    skill: str | None
    description: str
    # Content has at least one practice set eligible as review material for this point.
    has_practice_set: bool = False


def details_from_content(point: dict[str, Any]) -> KnowledgePointDetails:
    """Snapshot one Content knowledge point; missing optional fields take their empty value."""
    return KnowledgePointDetails(
        skill=point.get("skill") or None,
        description=(point.get("description") or "")[:_MAX_DESCRIPTION],
        has_practice_set=point.get("hasPracticeSet") is True,
    )


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
