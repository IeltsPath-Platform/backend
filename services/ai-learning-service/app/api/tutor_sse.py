"""Server-sent events for a tutor turn.

The turn runs in its own task and feeds a queue; the response streams from the queue. When the client disconnects
the stream stops but the turn still runs to its end, so the session's state stays consistent and the learner reads
the result from ``GET /sessions/{id}``.
"""

from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator
import json
import re
from typing import Any

from app.tutor.engine import TutorEvent

KEEP_ALIVE_SECONDS = 15.0
SSE_HEADERS = {"Cache-Control": "no-cache", "X-Accel-Buffering": "no"}
_RUNNING: set[asyncio.Task] = set()
_SNAKE = re.compile(r"_([a-z0-9])")


def camel(value: Any) -> Any:
    """Rename snake_case keys to camelCase, recursively, to match the other learner APIs."""
    if isinstance(value, dict):
        return {_SNAKE.sub(lambda match: match.group(1).upper(), str(key)): camel(item) for key, item in value.items()}
    if isinstance(value, list):
        return [camel(item) for item in value]
    return value


def format_event(event: TutorEvent) -> str:
    data = json.dumps(camel(event.data), ensure_ascii=False, separators=(",", ":"), default=str)
    return f"event: {event.type}\ndata: {data}\n\n"


def stream_events(events: AsyncIterator[TutorEvent], *,
                  keep_alive_seconds: float = KEEP_ALIVE_SECONDS) -> AsyncIterator[str]:
    """Start the turn before the HTTP response is handed to the client."""
    queue: asyncio.Queue[TutorEvent | None] = asyncio.Queue()

    async def produce() -> None:
        try:
            async for event in events:
                await queue.put(event)
        finally:
            await queue.put(None)

    task = asyncio.create_task(produce())
    # Hold a reference so the turn finishes even after the client has gone.
    _RUNNING.add(task)
    task.add_done_callback(_RUNNING.discard)

    async def consume() -> AsyncIterator[str]:
        while True:
            try:
                event = await asyncio.wait_for(queue.get(), timeout=keep_alive_seconds)
            except asyncio.TimeoutError:
                yield ": keep-alive\n\n"
                continue
            if event is None:
                return
            yield format_event(event)

    return consume()
