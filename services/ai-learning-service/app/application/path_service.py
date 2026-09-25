"""Goal-bound DeepTutor path bootstrap and safe read operations."""

from typing import Any
from uuid import UUID, uuid4

from psycopg2.errors import UniqueViolation
from starlette.concurrency import run_in_threadpool

from deeptutor.learning.policy import map_summary, next_objective
from deeptutor.learning.service import LearningService

from app.adapters.curriculum_adapter import CurriculumAdapter
from app.adapters.curriculum_scope import CurriculumScope, KnowledgePointBand, target_band_of
from app.application.formal_result_applier import FormalResultApplier
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
        applier: FormalResultApplier | None = None,
    ) -> None:
        # The Assessment consumer only uses ensure_path on existing paths, so it
        # builds this service without the learner-facing HTTP clients.
        self._store = store
        self._learning = LearningService(store)
        self._users = user_client
        self._content = content_client
        # Every path creation drains the parked results of its goal.
        self._applier = applier or FormalResultApplier(store)

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
        target_band = target_band_of(goal)
        topics, content_points = await self._content.get_curriculum(bearer_token)
        scoped = CurriculumScope.select(topics, content_points, target_band)
        modules = CurriculumAdapter.to_modules(scoped.topics, scoped.knowledge_points)
        scope = {
            "target_band": str(target_band),
            "included": len(scoped.knowledge_points),
            "excluded": scoped.excluded_count,
        }
        return await run_in_threadpool(
            lambda: self.ensure_path(user_id, goal_id, modules, bands=scoped.bands, scope=scope)
        )

    def ensure_path(
        self,
        user_id: UUID | str,
        learning_goal_id: UUID | str,
        modules: list[Any] | None = None,
        *,
        bands: dict[str, KnowledgePointBand] | None = None,
        scope: dict[str, Any] | None = None,
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
            return path_id, self._create_path(
                path_id, user_id, str(learning_goal_id), modules, bands=bands, scope=scope
            )
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

    def _create_path(
        self,
        path_id: str,
        user_id: UUID,
        goal_id: str,
        modules: list[Any],
        *,
        bands: dict[str, KnowledgePointBand] | None = None,
        scope: dict[str, Any] | None = None,
    ) -> Any:
        with self._store.transaction(
            path_id,
            create=True,
            user_id=user_id,
            learning_goal_id=goal_id,
        ) as tx:
            self._learning.get_or_create(path_id)
            self._learning.replace_modules_for_path(path_id, modules, append=False)
            if bands is not None:
                # Before parked results: placement test-out reads these bands.
                self._store.replace_knowledge_point_bands(path_id, bands)
            if scope is not None:
                tx.emit("path.scope_applied", scope)
            # Joins this transaction: path, curriculum and parked results commit as one revision.
            self._applier.apply_pending(path_id, str(user_id), goal_id)
            return tx.progress

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
