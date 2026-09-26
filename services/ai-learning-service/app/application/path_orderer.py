"""Order a new path before opening its creation transaction."""

from collections.abc import Callable
from dataclasses import dataclass
from datetime import date, datetime, timezone
import logging
from typing import Any

from deeptutor.learning.models import LearningModule
from starlette.concurrency import run_in_threadpool

from app.adapters.curriculum_scope import ScopedCurriculum
from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.learning.deeptutor_llm import DeepTutorOrderingLlm
from app.learning.path_ordering import (
    InvalidOrdering, LearnerContext, OrderingRequest, OrderingValidator, PayloadTooLarge,
)

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class OrderingOutcome:
    modules: list[LearningModule]
    source: str
    reason: str | None
    detail: str | None
    model: str | None
    rationale: str
    learner_profile: dict[str, str]


def _utc_today() -> date:
    return datetime.now(timezone.utc).date()


def _learner_profile(context: LearnerContext) -> dict[str, str]:
    profile = {"target_level": f"IELTS band {context.target_band}"}
    time_parts = []
    if context.minutes_per_day is not None:
        time_parts.append(f"{context.minutes_per_day} minutes per day")
    if context.days_until_exam is not None:
        time_parts.append(f"{context.days_until_exam} days until the exam")
    if time_parts:
        profile["time_budget"] = "; ".join(time_parts)
    prior = []
    if context.placement_band is not None:
        prior.append(f"Placement band {context.placement_band}")
    if context.placement_results:
        correct = sum(context.placement_results.values())
        prior.append(f"{correct} of {len(context.placement_results)} tested knowledge points answered correctly")
    if prior:
        profile["prior_knowledge"] = "; ".join(prior)
    return profile


class PathOrderer:
    def __init__(self, store: Any, llm: DeepTutorOrderingLlm, *,
                 today: Callable[[], date] = _utc_today) -> None:
        self._store = store
        self._llm = llm
        self._today = today

    async def order(self, user_id: Any, goal: dict[str, Any], modules: list[LearningModule],
                    scoped: ScopedCurriculum) -> OrderingOutcome:
        # This read owns and closes its connection before any network call.
        payloads = await run_in_threadpool(self._store.pending_formal_payloads, user_id, goal["id"])
        latest = {}
        for payload in payloads:
            try:
                command = FormalEvidenceAdapter.to_command(payload)
            except ValueError:
                continue
            if command.assessment_type != "PLACEMENT":
                continue
            previous = latest.get(command.attempt_id)
            if previous is None or command.result_version > previous.result_version:
                latest[command.attempt_id] = command
        context = LearnerContext.from_goal(goal, list(latest.values()), self._today())
        profile = _learner_profile(context)

        def fallback(reason, *, detail=None, model=None):
            logger.info("Path ordering source=content reason=%s detail=%s model=%s", reason, detail, model)
            return OrderingOutcome(modules, "content", reason, detail, model, "", profile)

        try:
            system_prompt, payload = OrderingRequest.build(context, modules, scoped)
        except PayloadTooLarge:
            return fallback("payload_too_large")
        try:
            proposal = await self._llm.propose(system_prompt, payload)
        except Exception as exc:
            logger.warning("Path ordering proposal failed error_type=%s", type(exc).__name__)
            return fallback("llm_error")
        if proposal.reason is not None:
            return fallback(proposal.reason, model=proposal.model)
        try:
            ordered = OrderingValidator.apply(modules, proposal.payload)
        except InvalidOrdering as exc:
            return fallback("invalid_ordering", detail=exc.reason, model=proposal.model)
        rationale = proposal.payload.get("rationale")
        rationale = rationale[:500] if isinstance(rationale, str) else ""
        logger.info("Path ordering source=llm model=%s", proposal.model)
        return OrderingOutcome(ordered, "llm", None, None, proposal.model, rationale, profile)
