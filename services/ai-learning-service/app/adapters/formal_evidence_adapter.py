"""Validate AssessmentCompleted.v2 and translate it into a DeepTutor-oriented command.

The adapter only checks the contract and reshapes data. It never decides mastery,
schedules reviews, picks objectives or applies thresholds.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal, InvalidOperation
from typing import Any
from uuid import UUID

from deeptutor.learning.models import ErrorType

from app.adapters.curriculum_scope import parse_band
from app.learning.formal_provenance import FormalProvenance, source_reference_id

EVENT_TYPE = "AssessmentCompleted.v2"
FINAL_STATUS = "COMPLETED"
QUALITATIVE_JUDGMENTS = frozenset({"PASS", "FAIL", "NOT_ASSESSED"})


class ContractError(ValueError):
    """The event can never be processed as sent; retrying cannot help."""


@dataclass(frozen=True)
class KnowledgePointObservation:
    knowledge_point_id: str
    # Attribution metadata only: it never scales mastery, quality or scheduling.
    weight: Decimal
    qualitative_judgment: str | None
    error_type: ErrorType | None
    source_reference_id: str


@dataclass(frozen=True)
class ItemObservation:
    item_result_id: str
    question_version_id: str
    is_correct: bool | None
    score: Decimal
    max_score: Decimal
    knowledge_points: tuple[KnowledgePointObservation, ...]


@dataclass(frozen=True)
class FormalAssessmentCommand:
    event_id: str
    user_id: str
    learning_goal_id: str
    attempt_id: str
    result_id: str
    result_version: int
    assessment_type: str
    completed_at: datetime
    items: tuple[ItemObservation, ...]
    # The grader's band for this result version; None when absent or not recorded.
    overall_band: Decimal | None = None
    # The validated event as received, kept so a parked result can be re-read later.
    raw_event: dict[str, Any] | None = field(default=None, compare=False, repr=False)

    def provenance(self, item: ItemObservation) -> str:
        return FormalProvenance(
            attempt_id=self.attempt_id,
            result_id=self.result_id,
            result_version=self.result_version,
            item_result_id=item.item_result_id,
        ).encode()


class FormalEvidenceAdapter:
    @staticmethod
    def to_command(event: Any) -> FormalAssessmentCommand:
        envelope = _object(event, "event")
        if envelope.get("event_type") != EVENT_TYPE:
            raise ContractError(f"Unsupported event type {envelope.get('event_type')!r}")
        event_id = _uuid(envelope, "event_id")
        _timestamp(envelope, "occurred_at")
        _text(envelope, "source")

        data = _object(envelope.get("data"), "data")
        if data.get("status") != FINAL_STATUS:
            raise ContractError("Only a finalized (COMPLETED) result can be ingested")
        result_id = _uuid(data, "result_id")
        result_version = data.get("result_version")
        if isinstance(result_version, bool) or not isinstance(result_version, int) or result_version < 1:
            raise ContractError("result_version must be a positive integer")

        raw_items = data.get("item_results")
        if not isinstance(raw_items, list):
            raise ContractError("item_results must be a list")
        items: list[ItemObservation] = []
        seen_items: set[str] = set()
        for raw_item in raw_items:
            item = _item(raw_item, result_id, result_version)
            if item.item_result_id in seen_items:
                raise ContractError(f"Duplicate item_result_id {item.item_result_id}")
            seen_items.add(item.item_result_id)
            items.append(item)

        return FormalAssessmentCommand(
            event_id=event_id,
            user_id=_uuid(data, "user_id"),
            # Required: a result stays attributed to the goal it was taken under.
            learning_goal_id=_uuid(data, "learning_goal_id"),
            attempt_id=_uuid(data, "attempt_id"),
            result_id=result_id,
            result_version=result_version,
            assessment_type=_text(data, "assessment_type"),
            completed_at=_timestamp(data, "completed_at"),
            items=tuple(items),
            overall_band=_band(data, "overall_band"),
            raw_event=envelope,
        )


def _item(raw: Any, result_id: str, result_version: int) -> ItemObservation:
    item = _object(raw, "item_results[]")
    item_result_id = _uuid(item, "item_result_id")
    is_correct = item.get("is_correct")
    if is_correct is not None and not isinstance(is_correct, bool):
        raise ContractError("is_correct must be a boolean or null")
    score = _decimal(item, "score")
    max_score = _decimal(item, "max_score")
    if score < 0 or max_score <= 0 or score > max_score:
        raise ContractError("score must be within 0..max_score and max_score must be positive")

    raw_mappings = item.get("knowledge_point_mappings")
    if not isinstance(raw_mappings, list):
        raise ContractError("knowledge_point_mappings must be a list")
    mappings: list[KnowledgePointObservation] = []
    seen_points: set[str] = set()
    for raw_mapping in raw_mappings:
        mapping = _object(raw_mapping, "knowledge_point_mappings[]")
        knowledge_point_id = _uuid(mapping, "knowledge_point_id")
        if knowledge_point_id in seen_points:
            raise ContractError(f"Knowledge point {knowledge_point_id} is mapped twice on one item")
        seen_points.add(knowledge_point_id)
        weight = _decimal(mapping, "weight")
        if weight < 0:
            raise ContractError("Knowledge-point weight must not be negative")
        judgment = mapping.get("qualitative_judgment")
        if judgment is not None and judgment not in QUALITATIVE_JUDGMENTS:
            raise ContractError(f"Unsupported qualitative_judgment {judgment!r}")
        mappings.append(
            KnowledgePointObservation(
                knowledge_point_id=knowledge_point_id,
                weight=weight,
                qualitative_judgment=judgment,
                error_type=_error_type(mapping.get("error_type")),
                source_reference_id=source_reference_id(
                    result_id, result_version, item_result_id, knowledge_point_id
                ),
            )
        )
    return ItemObservation(
        item_result_id=item_result_id,
        question_version_id=_uuid(item, "question_version_id"),
        is_correct=is_correct,
        score=score,
        max_score=max_score,
        knowledge_points=tuple(mappings),
    )


def _error_type(value: Any) -> ErrorType | None:
    # Assessment's error analysis is free text. Only a value naming one of DeepTutor's
    # error categories maps; anything else carries no error classification.
    if not isinstance(value, str) or not value.strip():
        return None
    try:
        return ErrorType(value.strip().lower())
    except ValueError:
        return None


def _band(container: dict[str, Any], key: str) -> Decimal | None:
    # Optional and additive in v2: events published before it existed have no such key.
    try:
        return parse_band(container.get(key), key)
    except ValueError as exc:
        raise ContractError(str(exc)) from exc


def _object(value: Any, name: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ContractError(f"{name} must be an object")
    return value


def _uuid(container: dict[str, Any], key: str) -> str:
    value = container.get(key)
    if value is None:
        raise ContractError(f"{key} is required")
    try:
        return str(UUID(str(value)))
    except (TypeError, ValueError, AttributeError) as exc:
        raise ContractError(f"{key} must be a UUID") from exc


def _text(container: dict[str, Any], key: str) -> str:
    value = container.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ContractError(f"{key} is required")
    return value.strip()


def _decimal(container: dict[str, Any], key: str) -> Decimal:
    value = container.get(key)
    if isinstance(value, bool) or not isinstance(value, (int, float, str)):
        raise ContractError(f"{key} must be a number")
    try:
        number = Decimal(str(value))
    except InvalidOperation as exc:
        raise ContractError(f"{key} must be a number") from exc
    if not number.is_finite():
        raise ContractError(f"{key} must be finite")
    return number


def _timestamp(container: dict[str, Any], key: str) -> datetime:
    value = _text(container, key)
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ContractError(f"{key} must be an ISO-8601 timestamp") from exc
