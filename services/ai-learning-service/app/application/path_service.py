"""Goal-bound DeepTutor path bootstrap and safe read operations."""

from typing import Any
from uuid import UUID, uuid4

from psycopg2.errors import UniqueViolation
from starlette.concurrency import run_in_threadpool

from deeptutor.learning.policy import map_summary, next_objective
from deeptutor.learning.service import LearningService

from app.adapters.curriculum_adapter import CurriculumAdapter
from app.clients.content_service import ContentServiceClient
from app.clients.user_service import UserServiceClient
from app.persistence.postgres_learning_store import PostgresLearningStore


class ActiveGoalRequired(Exception):
    """The learner has no active goal to anchor a default path."""


class PathNotFound(Exception):
    """The path does not exist or is not owned by the caller."""


class PathNotBootstrapped(Exception):
    """No path exists for the goal yet and no curriculum was supplied to create one."""


class PathService:
    def __init__(
        self,
        store: PostgresLearningStore,
        user_client: UserServiceClient | None = None,
        content_client: ContentServiceClient | None = None,
    ) -> None:
        # The Assessment consumer only uses ensure_path on existing paths, so it
        # builds this service without the learner-facing HTTP clients.
        self._store = store
        self._learning = LearningService(store)
        self._users = user_client
        self._content = content_client

    async def _active_goal(self, bearer_token: str) -> dict[str, Any]:
        try:
            goal = await self._users.get_active_goal(bearer_token)
        except Exception as exc:
            # User Service's 404 means there is no active goal. Other dependency
            # failures remain dependency failures and are handled by the API layer.
            import httpx

            if isinstance(exc, httpx.HTTPStatusError) and exc.response.status_code == 404:
                raise ActiveGoalRequired from exc
            raise
        if str(goal.get("status", "")).upper() != "ACTIVE":
            raise ActiveGoalRequired
        try:
            goal["id"] = str(UUID(str(goal["id"])))
            goal["userId"] = str(UUID(str(goal["userId"])))
        except (KeyError, TypeError, ValueError) as exc:
            raise ValueError("User Service returned invalid active-goal identity") from exc
        return goal

    async def ensure_active_path(self, user_id: UUID, bearer_token: str) -> tuple[str, Any]:
        goal = await self._active_goal(bearer_token)
        if goal["userId"] != str(user_id):
            raise ValueError("Active goal owner does not match authenticated learner")
        goal_id = goal["id"]
        try:
            return await run_in_threadpool(self.ensure_path, user_id, goal_id)
        except PathNotBootstrapped:
            pass
        topics, content_points = await self._content.get_curriculum(bearer_token)
        modules = CurriculumAdapter.to_modules(topics, content_points)
        return await run_in_threadpool(self.ensure_path, user_id, goal_id, modules)

    def ensure_path(
        self, user_id: UUID | str, learning_goal_id: UUID | str, modules: list[Any] | None = None
    ) -> tuple[str, Any]:
        """Return the single path of ``(user_id, learning_goal_id)``, creating it at most once.

        Shared by the learner API and the Assessment consumer. Creation needs the
        canonical curriculum; without ``modules`` a missing path raises
        ``PathNotBootstrapped`` instead of inventing an empty path.
        """
        existing_path_id = self._store.find_path(user_id, learning_goal_id)
        if existing_path_id:
            return existing_path_id, self._owned(existing_path_id, user_id)
        if modules is None:
            raise PathNotBootstrapped
        path_id = str(uuid4())
        try:
            return path_id, self._create_path(path_id, user_id, str(learning_goal_id), modules)
        except UniqueViolation as exc:
            # Two callers can pass the lookup together. The partial unique index
            # chooses the winner; the loser returns that same path.
            if exc.diag.constraint_name != "uq_mastery_paths_user_learning_goal":
                raise
            winner_path_id = self._store.find_path(user_id, learning_goal_id)
            if winner_path_id is None:
                raise
            return winner_path_id, self._owned(winner_path_id, user_id)

    def _owned(self, path_id: str, user_id: UUID | str) -> Any:
        progress = self._store.get_owned_progress(path_id, user_id)
        if progress is None:
            raise PathNotFound
        return progress

    def _create_path(self, path_id: str, user_id: UUID, goal_id: str, modules: list[Any]) -> Any:
        with self._store.transaction(
            path_id,
            create=True,
            user_id=user_id,
            learning_goal_id=goal_id,
        ):
            self._learning.get_or_create(path_id)
            return self._learning.replace_modules_for_path(path_id, modules, append=False)

    async def active_progress(self, user_id: UUID, bearer_token: str) -> tuple[str, dict[str, Any]]:
        path_id, progress = await self.ensure_active_path(user_id, bearer_token)
        summary = map_summary(progress)
        return path_id, {
            "pathId": path_id,
            "revision": progress.version,
            "moduleCount": len(progress.modules),
            "knowledgePointCount": sum(len(module.knowledge_points) for module in progress.modules),
            "mastery": summary,
        }

    async def active_status(self, user_id: UUID, bearer_token: str) -> tuple[str, dict[str, Any]]:
        path_id, progress = await self.ensure_active_path(user_id, bearer_token)
        step = next_objective(progress)
        return path_id, {
            "pathId": path_id,
            "revision": progress.version,
            "action": step.action,
            "moduleId": step.module_id,
            "moduleName": step.module_name,
            "knowledgePointId": step.knowledge_point_id,
            "knowledgePointName": step.knowledge_point_name,
            "knowledgePointType": step.knowledge_point_type,
            "status": step.status,
            "mastery": step.mastery,
            "reason": step.reason,
        }

    async def path_map(self, path_id: UUID, user_id: UUID) -> dict[str, Any]:
        progress = await run_in_threadpool(self._store.get_owned_progress, path_id, user_id)
        if progress is None:
            raise PathNotFound
        return {
            "pathId": str(path_id),
            "revision": progress.version,
            "map": map_summary(progress),
        }
