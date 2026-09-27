"""IELTSPath Adaptive Learning API."""

import asyncio
from contextlib import asynccontextmanager
import logging

import httpx
from fastapi import Depends, FastAPI, status
from fastapi.responses import JSONResponse
from fastapi.security import HTTPAuthorizationCredentials
from psycopg2 import OperationalError
from uuid import UUID

from app.adapters.curriculum_adapter import CurriculumContractError
from app.adapters.curriculum_scope import NoCurriculumInScope
from app.api.dependencies import get_path_service
from app.api.tutor import router as tutor_router
from app.api.practice import router as practice_router
from app.application.path_service import ActiveGoalRequired, PathNotFound, PathService
from app.api.dto.responses import (
    LearningPathMapResponse,
    LearningProgressResponse,
    LearningStatusResponse,
    PathCreatedResponse,
)
from app.config import get_settings
from app.security.internal_jwt import AuthenticatedUser, bearer_scheme, require_current_user
from app.tutor.session_store import TutorSessionStore

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # One API instance serves every tutor turn, so a turn still marked running at start-up died with the
    # previous process; failing it frees its session. Do not serve turns until recovery succeeds.
    store = TutorSessionStore(get_settings().database_url.get_secret_value())
    recovered = await asyncio.to_thread(store.recover_interrupted_turns)
    if recovered:
        logger.warning("Failed %s tutor turns interrupted by the previous shutdown", recovered)
    yield


app = FastAPI(
    title="IELTSPath AI Learning Service",
    description="Adaptive Learning, Mastery Path, and study/review tutor",
    version="1.0.0",
    lifespan=lifespan,
)
app.include_router(tutor_router)
app.include_router(practice_router)


@app.exception_handler(ActiveGoalRequired)
async def active_goal_required_handler(_request, _exc):
    return JSONResponse(
        status_code=status.HTTP_409_CONFLICT,
        content={"detail": "An active learning goal is required"},
    )


@app.exception_handler(NoCurriculumInScope)
async def no_curriculum_in_scope_handler(_request, _exc):
    return JSONResponse(
        status_code=status.HTTP_409_CONFLICT,
        content={"detail": "No learning content matches the goal's target band"},
    )


@app.exception_handler(PathNotFound)
async def path_not_found_handler(_request, _exc):
    return JSONResponse(status_code=status.HTTP_404_NOT_FOUND, content={"detail": "Learning path not found"})


@app.exception_handler(CurriculumContractError)
async def curriculum_contract_handler(_request, _exc):
    return JSONResponse(
        status_code=status.HTTP_502_BAD_GATEWAY,
        content={"detail": "Content Service returned invalid curriculum metadata"},
    )


@app.exception_handler(httpx.HTTPStatusError)
async def upstream_status_handler(_request, _exc):
    return JSONResponse(status_code=status.HTTP_502_BAD_GATEWAY, content={"detail": "A required service request failed"})


@app.exception_handler(httpx.RequestError)
async def upstream_request_handler(_request, _exc):
    return JSONResponse(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, content={"detail": "A required service is unavailable"})


@app.exception_handler(OperationalError)
async def database_unavailable_handler(_request, _exc):
    return JSONResponse(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, content={"detail": "Learning storage is unavailable"})


@app.exception_handler(ValueError)
async def contract_error_handler(_request, _exc):
    return JSONResponse(status_code=status.HTTP_502_BAD_GATEWAY, content={"detail": "A required service returned invalid data"})


@app.get("/health")
async def health_check():
    return {"status": "UP", "service": "ai-learning-service"}


@app.get("/api/ai-learning/health")
async def api_health_check():
    return {"status": "UP", "service": "ai-learning-service"}


@app.post("/api/ai-learning/paths", response_model=PathCreatedResponse)
async def ensure_learning_path(
    user: AuthenticatedUser = Depends(require_current_user),
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    service: PathService = Depends(get_path_service),
):
    path_id, progress, added = await service.refresh_active_path(user.user_id, credentials.credentials)
    return {
        "pathId": path_id,
        "revision": progress.version,
        "moduleCount": len(progress.modules),
        "knowledgePointCount": sum(len(module.knowledge_points) for module in progress.modules),
        "addedKnowledgePointCount": added,
    }


@app.get("/api/ai-learning/progress", response_model=LearningProgressResponse)
async def get_active_progress(
    user: AuthenticatedUser = Depends(require_current_user),
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    service: PathService = Depends(get_path_service),
):
    _path_id, progress = await service.active_progress(user.user_id, credentials.credentials)
    return progress


@app.get("/api/ai-learning/status", response_model=LearningStatusResponse)
async def get_active_status(
    user: AuthenticatedUser = Depends(require_current_user),
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    service: PathService = Depends(get_path_service),
):
    _path_id, result = await service.active_status(user.user_id, credentials.credentials)
    return result


@app.get("/api/ai-learning/paths/{path_id}/map", response_model=LearningPathMapResponse)
async def get_path_map(
    path_id: UUID,
    user: AuthenticatedUser = Depends(require_current_user),
    service: PathService = Depends(get_path_service),
):
    return await service.path_map(path_id, user.user_id)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
