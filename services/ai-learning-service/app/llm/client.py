"""Minimal OpenAI-compatible chat completions client.

The reasoning defaults, the temperature rule and the single structured-output retry follow
DeepTutor v1.6.9 (``deeptutor/services/llm/reasoning_params.py``, ``structured_retry.py`` and
``provider_core/openai_compat_provider.py``, Apache-2.0). Differences: the Gemini thinking defaults
are chosen by model name alone (DeepTutor also requires the ``gemini`` provider binding, which is
the same for the default endpoint), and DeepTutor's DeepSeek/Qwen ``extra_body`` thinking controls
are not ported. No request or response text is ever logged.
"""

from __future__ import annotations

from collections.abc import Awaitable, Callable
from dataclasses import dataclass
import json
from typing import Any

import httpx

from app.config import LlmSettings
from app.llm.json_payload import parse_json_payload

# The one retry for a structured call a reasoning model failed to answer: ask again with thinking
# turned down so the output budget goes to the payload.
RETRY_REASONING_EFFORT = "low"

# Model families that think by default and can spend the whole token budget on it unless told not
# to, and the subset that rejects "none" and accepts "minimal" as the lowest level.
_THINKING_DEFAULT_OFF = ("gemini-2.5", "gemini-3")
_MINIMAL_NOT_OFF = ("gemini-3", "gemini-2.5-pro")


class LlmConfigError(RuntimeError):
    """No usable model or API key is configured."""


class LlmApiError(RuntimeError):
    """The provider call failed. Carries only the HTTP status, never provider text."""

    def __init__(self, status_code: int | None) -> None:
        self.status_code = status_code
        super().__init__(f"LLM provider request failed (status {status_code})")


def _matches(model: str, patterns: tuple[str, ...]) -> bool:
    lowered = model.lower()
    return any(pattern in lowered for pattern in patterns)


def resolve_reasoning_effort(model: str, requested: str | None) -> str | None:
    """The ``reasoning_effort`` to send, or ``None`` to leave the field out."""
    effort = (requested or "").strip().lower()
    if not effort:
        if _matches(model, _THINKING_DEFAULT_OFF):
            return "minimal" if _matches(model, _MINIMAL_NOT_OFF) else "none"
        return None
    if effort == "none":
        return "minimal" if _matches(model, _MINIMAL_NOT_OFF) else "none"
    return effort


def supports_temperature(model: str, requested_effort: str | None) -> bool:
    """Whether to send ``temperature``: not with an explicit reasoning effort, not to o-series/gpt-5.

    Checked against the effort the caller or configuration asked for, before any model-family
    default is applied, as DeepTutor's OpenAI-compatible provider does.
    """
    effort = (requested_effort or "").strip().lower()
    if effort and effort != "none":
        return False
    return not _matches(model, ("gpt-5", "o1", "o3", "o4"))


class ChatCompletionsClient:
    def __init__(self, settings: LlmSettings, *, transport: httpx.AsyncBaseTransport | None = None) -> None:
        if not settings.configured:
            raise LlmConfigError("AI_LEARNING_LLM_MODEL and AI_LEARNING_LLM_API_KEY must be set")
        self._settings = settings
        self._transport = transport

    @property
    def model(self) -> str:
        return self._settings.model

    async def complete(
        self,
        *,
        prompt: str,
        system_prompt: str,
        response_format: dict[str, Any] | None = None,
        temperature: float = 0.2,
        max_tokens: int = 8192,
        reasoning_effort: str | None = None,
    ) -> str:
        message = await self._post(
            [{"role": "system", "content": system_prompt}, {"role": "user", "content": prompt}],
            response_format=response_format, temperature=temperature, max_tokens=max_tokens,
            reasoning_effort=reasoning_effort,
        )
        return message.get("content") or ""

    async def chat(
        self,
        messages: list[dict[str, Any]],
        *,
        tools: list[dict[str, Any]] | None = None,
        temperature: float = 0.3,
        max_tokens: int = 4096,
        reasoning_effort: str | None = None,
    ) -> "ChatReply":
        """One chat round with optional function tools; returns the text and any tool calls."""
        message = await self._post(messages, tools=tools, temperature=temperature, max_tokens=max_tokens,
                                   reasoning_effort=reasoning_effort)
        calls = []
        for raw in message.get("tool_calls") or []:
            function = raw.get("function") or {}
            try:
                arguments = json.loads(function.get("arguments") or "{}")
            except (TypeError, ValueError):
                arguments = None
            calls.append(ToolCall(str(raw.get("id") or ""), str(function.get("name") or ""),
                                  arguments if isinstance(arguments, dict) else None, raw))
        return ChatReply(message.get("content") or "", tuple(calls))

    async def _post(self, messages: list[dict[str, Any]], *, tools: list[dict[str, Any]] | None = None,
                    response_format: dict[str, Any] | None = None, temperature: float,
                    max_tokens: int, reasoning_effort: str | None) -> dict[str, Any]:
        requested_effort = reasoning_effort or self._settings.reasoning_effort
        body: dict[str, Any] = {"model": self._settings.model, "messages": messages, "max_tokens": max_tokens}
        if supports_temperature(self._settings.model, requested_effort):
            body["temperature"] = temperature
        if response_format is not None:
            body["response_format"] = response_format
        if tools:
            body["tools"] = tools
        effort = resolve_reasoning_effort(self._settings.model, requested_effort)
        if effort is not None:
            body["reasoning_effort"] = effort
        url = self._settings.base_url.rstrip("/") + "/chat/completions"
        headers = {"Authorization": f"Bearer {self._settings.api_key.get_secret_value()}"}
        async with httpx.AsyncClient(timeout=self._settings.timeout_seconds, transport=self._transport) as client:
            try:
                response = await client.post(url, json=body, headers=headers)
            except httpx.HTTPError:
                # Network failures carry no status; the original error text is dropped on purpose.
                raise LlmApiError(None) from None
        if response.status_code >= 400:
            raise LlmApiError(response.status_code)
        try:
            message = response.json()["choices"][0]["message"]
        except (ValueError, KeyError, IndexError, TypeError, AttributeError):
            raise LlmApiError(response.status_code) from None
        if not isinstance(message, dict):
            raise LlmApiError(response.status_code)
        return message


@dataclass(frozen=True)
class ToolCall:
    """A function call the model asked for. ``arguments`` is ``None`` when they were not a JSON object.

    ``raw`` is the provider's own tool-call object; it is echoed back unchanged on the next round because some
    providers attach fields (such as Gemini's thought signatures) that must round-trip.
    """

    id: str
    name: str
    arguments: dict[str, Any] | None
    raw: dict[str, Any]


@dataclass(frozen=True)
class ChatReply:
    content: str
    tool_calls: tuple[ToolCall, ...] = ()


def _usable(payload: Any, expected_key: str | None) -> bool:
    if not isinstance(payload, dict) or not payload:
        return False
    return expected_key is None or bool(payload.get(expected_key))


async def json_with_reasoning_retry(
    run: Callable[[str | None], Awaitable[str]], *, expected_key: str | None = None
) -> dict[str, Any]:
    """Run ``run`` for a JSON object, retrying once at low reasoning effort.

    ``run`` receives the effort to use (``None`` first, meaning the configured one) and returns the
    raw response text. Returns the first usable object, else the first non-empty one, else ``{}``.
    """
    first = parse_json_payload(await run(None))
    if _usable(first, expected_key):
        return first
    retried = parse_json_payload(await run(RETRY_REASONING_EFFORT))
    if _usable(retried, expected_key):
        return retried
    for candidate in (first, retried):
        if candidate:
            return candidate if isinstance(candidate, dict) else {}
    return {}
