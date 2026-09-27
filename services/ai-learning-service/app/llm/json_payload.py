# Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/utils/json_parser.py @ da856ad.
# Modified for IELTSPath: no json-repair step and no logging, so a malformed payload is simply
# unusable (the caller retries once) and response text never reaches a log.
"""Extract the JSON payload from a model response."""

from __future__ import annotations

import json
import re
from typing import Any

_FENCE = re.compile(r"```(?:json)?\s*\n?(.*?)```", re.DOTALL)
_THINK_BLOCK = re.compile(r"<think\b[^>]*>.*?</think>", re.DOTALL | re.IGNORECASE)
_THINK_PRELUDE = re.compile(r"^\s*<think\b[^>]*>.*?(?=[{\[])", re.DOTALL | re.IGNORECASE)


def _longest_json_value(text: str) -> Any:
    """The longest top-level JSON value decodable from ``text``, or ``None``.

    Prose around the payload can hold small valid JSON fragments (a schema
    example in a reasoning prelude); keeping the longest value picks the payload.
    """
    decoder = json.JSONDecoder()
    best: Any = None
    best_length = 0
    position = 0
    while True:
        starts = [index for index in (text.find("{", position), text.find("[", position)) if index != -1]
        if not starts:
            return best
        start = min(starts)
        try:
            parsed, consumed = decoder.raw_decode(text[start:])
        except json.JSONDecodeError as error:
            position = start + max(1, error.pos)
            continue
        except RecursionError:
            return best
        if consumed > best_length:
            best, best_length = parsed, consumed
        position = start + consumed


def parse_json_payload(response: str | None) -> Any:
    """Parse a model response into JSON, or return ``None`` when it holds none."""
    if not response or not response.strip():
        return None
    try:
        return json.loads(response)
    except (json.JSONDecodeError, TypeError):
        pass

    candidate = response
    fenced = _FENCE.search(response) if "```" in response else None
    if fenced:
        candidate = fenced.group(1).strip()
    try:
        return json.loads(candidate)
    except (json.JSONDecodeError, TypeError):
        pass

    if "<think" in candidate.lower():
        cleaned = _THINK_PRELUDE.sub("", _THINK_BLOCK.sub("", candidate), count=1).strip()
        if cleaned != candidate.strip():
            if not cleaned:
                return None
            try:
                return json.loads(cleaned)
            except (json.JSONDecodeError, TypeError):
                candidate = cleaned

    return _longest_json_value(candidate)
