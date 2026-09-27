"""Tutor study/review routes: sessions on the learner's active path and streamed turns.

The path is always the learner's active path, resolved by the server; no route accepts a path id.
"""

from __future__ import annotations

import asyncio
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from fastapi.responses import StreamingResponse
from fastapi.security import HTTPAuthorizationCredentials

from app.api.dependencies import (
    get_learner_memory_store, get_memory_complete, get_path_service, get_tutor_chat, get_tutor_engine,
    get_tutor_sessions,
)
from app.api.dto.tutor import (
    CreateSessionRequest, LearnerMemoryResponse, TurnRequest, TutorSessionDetailResponse, TutorSessionResponse,
    session_payload,
)
from app.api.tutor_sse import SSE_HEADERS, stream_events
from app.application.path_service import PathService
from app.security.internal_jwt import AuthenticatedUser, bearer_scheme, require_current_user
from app.tutor.engine import CardAnswer, Chat, SessionNotFound, TutorEngine
from app.tutor.memory import Complete, LearnerMemoryStore
from app.tutor.session_store import ActiveTurnConflict, TutorSessionStore

router = APIRouter(prefix="/api/ai-learning/tutor", tags=["tutor"])

_SESSION_NOT_FOUND = HTTPException(status.HTTP_404_NOT_FOUND, detail="Tutor session not found")


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
):
    path_id, _progress = await paths.ensure_active_path(user.user_id, credentials.credentials)
    session = await asyncio.to_thread(sessions.create_session, user.user_id, path_id,
                                      request.title if request else None)
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
        **session_payload(session),
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


@router.post("/sessions/{session_id}/turns", responses={200: {"content": {"text/event-stream": {}}}})
async def run_turn(
    session_id: UUID,
    request: TurnRequest,
    user: AuthenticatedUser = Depends(require_current_user),
    engine: TutorEngine = Depends(get_tutor_engine),
    chat: Chat | None = Depends(get_tutor_chat),
    complete: Complete | None = Depends(get_memory_complete),
):
    try:
        turn = await engine.open_turn(user.user_id, session_id)
    except SessionNotFound:
        raise _SESSION_NOT_FOUND from None
    except ActiveTurnConflict:
        raise HTTPException(status.HTTP_409_CONFLICT, detail="This session already has a running turn") from None
    answer = CardAnswer(str(request.answer.question_id), request.answer.text) if request.answer else None
    events = engine.run(turn, chat, message=request.message, answer=answer, complete=complete)
    return StreamingResponse(stream_events(events), media_type="text/event-stream", headers=SSE_HEADERS)
