"""Best-effort ordering proposals through DeepTutor's catalog-backed LLM API."""

import asyncio
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
import logging
import time
from typing import Any

logger = logging.getLogger(__name__)
# DeepTutor's JSON parser can debug-log raw responses. Keep parsing silent and
# report only the bounded failure metadata below, including when DEBUG is enabled.
_parser_logger = logging.Logger(__name__ + ".parser")
_parser_logger.disabled = True


@dataclass(frozen=True)
class LlmProposal:
    payload: dict[str, Any] | None
    reason: str | None
    model: str | None


class DeepTutorOrderingLlm:
    def __init__(self, *, timeout_seconds: float = 20.0,
                 complete: Callable[..., Awaitable[str]] | None = None) -> None:
        self._timeout = timeout_seconds
        self._complete = complete

    async def propose(self, system_prompt: str, payload: str) -> LlmProposal:
        started = time.monotonic()
        model = None
        error_type = "None"
        reason = "llm_error"
        try:
            # Import only on the API's creation path: the consumer must not load
            # catalog settings or initialize the provider/usage ledger.
            from deeptutor.services.llm import complete
            from deeptutor.services.llm.config import get_llm_config
            from deeptutor.services.llm.exceptions import LLMConfigError
            from deeptutor.services.llm.structured_retry import json_with_reasoning_retry

            try:
                model = get_llm_config().model

                async def run(reasoning_effort):
                    return await (self._complete or complete)(
                        prompt=payload, system_prompt=system_prompt,
                        response_format={"type": "json_object"}, temperature=0.2,
                        max_tokens=8192, max_retries=0, reasoning_effort=reasoning_effort,
                    )

                data = await asyncio.wait_for(
                    json_with_reasoning_retry(run, expected_key="modules", logger_instance=_parser_logger),
                    timeout=self._timeout,
                )
                if data:
                    return LlmProposal(data, None, model)
                reason = "llm_unusable_response"
            except LLMConfigError as exc:
                reason, error_type = "llm_not_configured", type(exc).__name__
        except asyncio.TimeoutError as exc:
            reason, error_type = "llm_timeout", type(exc).__name__
        except Exception as exc:
            # Includes provider errors and local catalog/usage-ledger I/O errors.
            # Never log the exception message, which may contain provider data.
            error_type = type(exc).__name__
        logger.warning("Path ordering fallback reason=%s model=%s elapsed=%.3fs error_type=%s",
                       reason, model, time.monotonic() - started, error_type)
        return LlmProposal(None, reason, model)
