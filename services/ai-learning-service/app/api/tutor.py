"""Tutor study/review routes: sessions on the learner's active path and streamed turns.

The path is always the learner's active path, resolved by the server; no route accepts a path id.
"""

from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator
from contextlib import aclosing
from datetime import date, datetime, timezone
import logging
import math
from uuid import UUID

import httpx

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from fastapi.responses import JSONResponse, StreamingResponse
from fastapi.security import HTTPAuthorizationCredentials

from app.api.dependencies import (
    get_content_client, get_learner_memory_store, get_memory_complete, get_path_service, get_quota_store,
    get_tutor_chat, get_tutor_engine, get_tutor_sessions,
)
from app.api.dto.tutor import (
    CreateSessionRequest, LearnerMemoryResponse, QuotaExceededResponse, TurnRequest, TutorSessionDetailResponse,
    TutorSessionResponse, TutorUsageResponse, session_payload,
)
from app.api.tutor_sse import SSE_HEADERS, stream_events
from app.application.path_service import PathService
from app.clients.content_service import ContentServiceClient
from app.config import Settings, get_settings
from app.security.internal_jwt import AuthenticatedUser, bearer_scheme, require_current_user
from app.tutor.engine import CardAnswer, Chat, OpenTurn, SessionNotFound, TutorEngine, TutorEvent
from app.tutor.memory import Complete, LearnerMemoryStore
from app.tutor.reading import ReadingMaterialNotFound, material_from_content
from app.tutor.session_store import ActiveTurnConflict, TutorSessionStore
from app.usage.quota import MEMORY_SUMMARY, TUTOR_TURN, DailyQuotaStore, QuotaResult

router = APIRouter(prefix="/api/ai-learning/tutor", tags=["tutor"])

_SESSION_NOT_FOUND = HTTPException(status.HTTP_404_NOT_FOUND, detail="Tutor session not found")
# Failures of the provider or of configuration, not of the learner, give the turn back.
_REFUNDED_FAILURES = frozenset({"llm_not_configured", "llm_error"})
# A later round only runs after the model answered with tools, so either event means a model call was already billed.
_MODEL_OUTPUT_EVENTS = frozenset({"tool.called", "assistant.message"})
logger = logging.getLogger(__name__)


def _quota_exceeded(allowance: QuotaResult) -> JSONResponse:
    retry_after = max(1, math.ceil((allowance.resets_at - datetime.now(timezone.utc)).total_seconds()))
    body = QuotaExceededResponse(detail="Daily tutor turn limit reached", limit=allowance.limit,
                                 resets_at=allowance.resets_at)
    return JSONResponse(status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                        content=body.model_dump(mode="json", by_alias=True),
                        headers={"Retry-After": str(retry_after)})


async def _refund_system_failures(events: AsyncIterator[TutorEvent], quota: DailyQuotaStore, turn: OpenTurn,
                                  usage_date: date) -> AsyncIterator[TutorEvent]:
    """Pass the turn's events through, refunding a turn the model never answered before its failure is sent."""
    model_answered = False
    async with aclosing(events):
        async for event in events:
            model_answered = model_answered or event.type in _MODEL_OUTPUT_EVENTS
            failure_code = event.data.get("failureCode")
            if event.type == "turn.failed" and failure_code in _REFUNDED_FAILURES and not model_answered:
                try:
                    await asyncio.to_thread(quota.refund, turn.user_id, TUTOR_TURN, usage_date)
                    logger.info("Tutor turn refunded session=%s turn=%s code=%s",
                                turn.session.id, turn.turn_id, failure_code)
                except Exception as error:  # noqa: BLE001 - a lost refund must not hide the turn's outcome
                    logger.error("Tutor turn refund failed error_type=%s", type(error).__name__)
            yield event


@router.get("/usage", response_model=TutorUsageResponse)
async def get_usage(
    user: AuthenticatedUser = Depends(require_current_user),
    settings: Settings = Depends(get_settings),
    quota: DailyQuotaStore = Depends(get_quota_store),
):
    turns = await asyncio.to_thread(quota.usage, user.user_id, TUTOR_TURN, settings.tutor_turns_per_day)
    summaries = await asyncio.to_thread(quota.usage, user.user_id, MEMORY_SUMMARY, settings.memory_summaries_per_day)
    return {
        "timezone": settings.quota_timezone,
        "resetsAt": turns.resets_at,
        "tutorTurns": {"used": turns.used, "limit": turns.limit},
        "memorySummaries": {"used": summaries.used, "limit": summaries.limit},
    }


@router.get("/memory", response_model=LearnerMemoryResponse)
async def get_learner_memory(
    user: AuthenticatedUser = Depends(require_current_user),
    store: LearnerMemoryStore = Depends(get_learner_memory_store),
):
    record = await asyncio.to_thread(store.get, user.user_id)
    return {"content": record.content, "updatedAt": record.updated_at}


@router.delete("/memory", status_code=status.HTTP_204_NO_CONTENT)
async def delete_learner_memory(
    user: AuthenticatedUser = Depends(require_current_user),
    store: LearnerMemoryStore = Depends(get_learner_memory_store),
):
    await asyncio.to_thread(store.clear, user.user_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/sessions", response_model=TutorSessionResponse, status_code=status.HTTP_201_CREATED)
async def create_session(
    request: CreateSessionRequest | None = None,
    user: AuthenticatedUser = Depends(require_current_user),
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    paths: PathService = Depends(get_path_service),
    sessions: TutorSessionStore = Depends(get_tutor_sessions),
    content: ContentServiceClient = Depends(get_content_client),
):
    path_id, _progress = await paths.ensure_active_path(user.user_id, credentials.credentials)
    title = request.title if request else None
    material = None
    if request is not None and request.reading_section_id is not None:
        # Copy the passage now, while this request's token is valid; tutor turns never call Content.
        try:
            material = material_from_content(
                await content.get_reading_passage(credentials.credentials, request.reading_section_id))
        except ReadingMaterialNotFound:
            raise HTTPException(status.HTTP_404_NOT_FOUND, detail="Reading material not found") from None
        except (httpx.HTTPError, ValueError) as error:
            status_code = error.response.status_code if isinstance(error, httpx.HTTPStatusError) else None
            logger.warning("Reading material fetch failed error_type=%s status=%s", type(error).__name__, status_code)
            raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE,
                                detail="Content Service is unavailable") from None
        title = title or material.title
    session = await asyncio.to_thread(sessions.create_session, user.user_id, path_id, title, material)
    if session is None:
        raise _SESSION_NOT_FOUND
    return session_payload(session)


@router.get("/sessions", response_model=list[TutorSessionResponse])
async def list_sessions(
    limit: int = Query(default=50, ge=1, le=100),
    user: AuthenticatedUser = Depends(require_current_user),
    sessions: TutorSessionStore = Depends(get_tutor_sessions),
):
    return [session_payload(session) for session in await asyncio.to_thread(sessions.list_sessions, user.user_id, limit)]


@router.get("/sessions/{session_id}", response_model=TutorSessionDetailResponse)
async def get_session(
    session_id: UUID,
    user: AuthenticatedUser = Depends(require_current_user),
    sessions: TutorSessionStore = Depends(get_tutor_sessions),
    engine: TutorEngine = Depends(get_tutor_engine),
):
    session = await asyncio.to_thread(sessions.get_session, user.user_id, session_id)
    if session is None:
        raise _SESSION_NOT_FOUND
    messages = await asyncio.to_thread(sessions.recent_messages, user.user_id, session_id, 200)
    pending = await engine.pending_question(session)
    return {
        **session_payload(session, with_passage=True),
        "messages": [{"id": message.id, "role": message.role, "content": message.content,
                      "createdAt": message.created_at, "questionId": message.metadata.get("question_id")}
                     for message in messages],
        "pendingQuestion": None if pending is None else {
            "questionId": pending["question_id"], "knowledgePointId": pending["knowledge_point_id"],
            "prompt": pending["prompt"], "questionType": pending["question_type"],
            "options": [{"label": option["label"], "body": option["body"]} for option in pending["options"]],
            "status": pending["status"],
        },
    }


@router.delete("/sessions/{session_id}", status_code=status.HTTP_204_NO_CONTENT)
async def archive_session(
    session_id: UUID,
    user: AuthenticatedUser = Depends(require_current_user),
    sessions: TutorSessionStore = Depends(get_tutor_sessions),
):
    try:
        archived = await asyncio.to_thread(sessions.archive_session, user.user_id, session_id)
    except ActiveTurnConflict:
        raise HTTPException(status.HTTP_409_CONFLICT, detail="Finish the running turn before archiving") from None
    if not archived:
        raise _SESSION_NOT_FOUND
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/sessions/{session_id}/turns",
             responses={200: {"content": {"text/event-stream": {}}}, 429: {"model": QuotaExceededResponse}})
async def run_turn(
    session_id: UUID,
    request: TurnRequest,
    user: AuthenticatedUser = Depends(require_current_user),
    settings: Settings = Depends(get_settings),
    engine: TutorEngine = Depends(get_tutor_engine),
    sessions: TutorSessionStore = Depends(get_tutor_sessions),
    quota: DailyQuotaStore = Depends(get_quota_store),
    chat: Chat | None = Depends(get_tutor_chat),
    complete: Complete | None = Depends(get_memory_complete),
):
    try:
        turn = await engine.open_turn(user.user_id, session_id)
    except SessionNotFound:
        raise _SESSION_NOT_FOUND from None
    except ActiveTurnConflict:
        raise HTTPException(status.HTTP_409_CONFLICT, detail="This session already has a running turn") from None
    # Counted only once the turn is open, so an unknown or busy session costs the learner nothing.
    try:
        allowance = await asyncio.to_thread(quota.consume, user.user_id, TUTOR_TURN, settings.tutor_turns_per_day)
    except Exception:
        # Never leave the session's running-turn slot taken.
        await asyncio.to_thread(sessions.finish_turn, turn.turn_id, "failed", "internal_error")
        raise
    if not allowance.allowed:
        await asyncio.to_thread(sessions.finish_turn, turn.turn_id, "failed", "quota_exceeded")
        logger.info("Tutor turn refused session=%s turn=%s reason=quota", turn.session.id, turn.turn_id)
        return _quota_exceeded(allowance)
    answer = CardAnswer(str(request.answer.question_id), request.answer.text) if request.answer else None
    events = engine.run(turn, chat, message=request.message, answer=answer, complete=complete)
    events = _refund_system_failures(events, quota, turn, allowance.usage_date)
    return StreamingResponse(stream_events(events), media_type="text/event-stream", headers=SSE_HEADERS)
