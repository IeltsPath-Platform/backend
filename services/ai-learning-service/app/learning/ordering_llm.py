"""Best-effort ordering proposals from the configured OpenAI-compatible LLM."""

import asyncio
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
import logging
import time
from typing import Any

from app.config import LlmSettings
from app.llm.client import ChatCompletionsClient, LlmConfigError, json_with_reasoning_retry

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class LlmProposal:
    payload: dict[str, Any] | None
    reason: str | None
    model: str | None


class OrderingLlm:
    def __init__(self, *, timeout_seconds: float | None = None,
                 complete: Callable[..., Awaitable[str]] | None = None,
                 settings: Callable[[], LlmSettings] = LlmSettings) -> None:
        self._timeout = timeout_seconds
        self._complete = complete
        self._settings = settings

    async def propose(self, system_prompt: str, payload: str) -> LlmProposal:
        started = time.monotonic()
        model = None
        error_type = "None"
        reason = "llm_error"
        try:
            try:
                settings = self._settings()
                if not settings.configured:
                    raise LlmConfigError("LLM is not configured")
                model = settings.model
                complete = self._complete or ChatCompletionsClient(settings).complete
                timeout = self._timeout if self._timeout is not None else settings.timeout_seconds

                async def run(reasoning_effort):
                    return await complete(
                        prompt=payload, system_prompt=system_prompt,
                        response_format={"type": "json_object"}, temperature=0.2,
                        max_tokens=8192, reasoning_effort=reasoning_effort,
                    )

                data = await asyncio.wait_for(json_with_reasoning_retry(run, expected_key="modules"), timeout=timeout)
                if data:
                    return LlmProposal(data, None, model)
                reason = "llm_unusable_response"
            except LlmConfigError as exc:
                reason, error_type = "llm_not_configured", type(exc).__name__
        except asyncio.TimeoutError as exc:
            reason, error_type = "llm_timeout", type(exc).__name__
        except Exception as exc:
            # Includes provider and configuration errors. Never log the exception message,
            # which may contain provider data.
            error_type = type(exc).__name__
        logger.warning("Path ordering fallback reason=%s model=%s elapsed=%.3fs error_type=%s",
                       reason, model, time.monotonic() - started, error_type)
        return LlmProposal(None, reason, model)
