"""HTTP API for learner-owned practice entries and mistake review."""

from __future__ import annotations

import asyncio
from datetime import datetime, timezone
from typing import Literal
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Path, Query

from app.api.dependencies import get_practice_store
from app.api.dto.practice import (
    PracticeAnswerRequest, PracticeAnswerResponse, PracticeEntryResponse, PracticeReviewRequest,
    PracticeReviewResponse,
)
from app.mastery.grading import grade_answer
from app.practice.store import PracticeConflict, PracticeNotFound, PracticeStore
from app.security.internal_jwt import AuthenticatedUser, require_current_user

router = APIRouter(prefix="/api/ai-learning/practice", tags=["practice"])


def _store_error(error: PracticeNotFound | PracticeConflict) -> HTTPException:
    if isinstance(error, PracticeNotFound):
        return HTTPException(status_code=404, detail="Practice entry not found")
    return HTTPException(status_code=409, detail=str(error))


@router.get("/notebook", response_model=list[PracticeEntryResponse], response_model_exclude_none=True)
async def list_notebook(
    knowledge_point_id: UUID | None = Query(default=None, alias="knowledgePointId"),
    session_id: UUID | None = Query(default=None, alias="sessionId"),
    entry_status: Literal["open", "correct", "incorrect"] | None = Query(default=None, alias="status"),
    limit: int = Query(default=50, ge=1, le=100),
    user: AuthenticatedUser = Depends(require_current_user),
    store: PracticeStore = Depends(get_practice_store),
):
    return await asyncio.to_thread(
        store.list_entries, user.user_id, kp_id=knowledge_point_id, session_id=session_id,
        status=entry_status, limit=limit,
    )


@router.post("/entries/{entryId}/answer", response_model=PracticeAnswerResponse, response_model_exclude_none=True)
async def answer_entry(
    request: PracticeAnswerRequest,
    entry_id: int = Path(alias="entryId", gt=0),
    user: AuthenticatedUser = Depends(require_current_user),
    store: PracticeStore = Depends(get_practice_store),
):
    try:
        return await asyncio.to_thread(
            store.answer_entry, user.user_id, entry_id, request.answer, grade_answer, datetime.now(timezone.utc),
        )
    except (PracticeNotFound, PracticeConflict) as error:
        raise _store_error(error) from None


@router.get("/due", response_model=list[PracticeEntryResponse], response_model_exclude_none=True)
async def due_entries(
    limit: int = Query(default=20, ge=1, le=100),
    user: AuthenticatedUser = Depends(require_current_user),
    store: PracticeStore = Depends(get_practice_store),
):
    return await asyncio.to_thread(store.due_entries, user.user_id, datetime.now(timezone.utc), limit)


@router.post("/reviews", response_model=PracticeReviewResponse)
async def review_entry(
    request: PracticeReviewRequest,
    user: AuthenticatedUser = Depends(require_current_user),
    store: PracticeStore = Depends(get_practice_store),
):
    try:
        return await asyncio.to_thread(
            store.review, user.user_id, request.request_id, request.entry_id, request.answer,
            request.rating, grade_answer, datetime.now(timezone.utc),
        )
    except (PracticeNotFound, PracticeConflict) as error:
        raise _store_error(error) from None


__all__ = ["router"]
