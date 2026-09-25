"""A new path contains only the knowledge points in scope for the goal's target band."""

import asyncio
import unittest
from decimal import Decimal
from uuid import uuid4

from fastapi.testclient import TestClient

from app.adapters.curriculum_scope import KnowledgePointBand, NoCurriculumInScope
from app.application.path_service import PathService

from tests.formal_assessment_support import InMemoryLearningStore

TOPIC_BASIC = "4ed3d7e1-7529-4572-921d-2e54403f7d01"
TOPIC_ADVANCED = "4ed3d7e1-7529-4572-921d-2e54403f7d02"
KP_BASIC = "c5b2641f-28c8-467d-9d64-f52c8bdc1501"
KP_ADVANCED = "c5b2641f-28c8-467d-9d64-f52c8bdc1502"


class GoalClient:
    def __init__(self, goal):
        self._goal = goal

    async def get_active_goal(self, _bearer_token):
        return dict(self._goal)


class BandedContentClient:
    """Content Service stub returning the flattened shape of ContentServiceClient, with effective bands."""

    def __init__(self):
        self.calls = 0

    async def get_curriculum(self, _bearer_token):
        self.calls += 1
        topics = [
            {"id": TOPIC_BASIC, "name": "Basic", "sortOrder": 0, "status": "ACTIVE"},
            {"id": TOPIC_ADVANCED, "name": "Advanced", "sortOrder": 1, "status": "ACTIVE"},
        ]
        points = [
            {"id": KP_BASIC, "topicId": TOPIC_BASIC, "name": "Basic KP", "learningType": "PROCEDURE",
             "status": "ACTIVE", "effectiveBandMin": 4.0, "effectiveBandMax": 5.0},
            {"id": KP_ADVANCED, "topicId": TOPIC_ADVANCED, "name": "Advanced KP", "learningType": "PROCEDURE",
             "status": "ACTIVE", "effectiveBandMin": 7.0, "effectiveBandMax": 8.0},
        ]
        return topics, points


def kp_ids(progress):
    return [kp.id for module in progress.modules for kp in module.knowledge_points]


class GoalScopedPathTest(unittest.TestCase):
    def setUp(self):
        self.store = InMemoryLearningStore()
        self.user_id = str(uuid4())

    def create(self, target_band):
        goal = {"id": str(uuid4()), "userId": self.user_id, "status": "ACTIVE", "targetBand": target_band}
        paths = PathService(self.store, GoalClient(goal), BandedContentClient())
        return asyncio.run(paths.ensure_active_path(self.user_id, "internal-token"))

    def test_the_path_keeps_only_points_within_the_target_band(self):
        path_id, progress = self.create(5.5)

        self.assertEqual(kp_ids(progress), [KP_BASIC])
        self.assertEqual([module.id for module in progress.modules], [TOPIC_BASIC])

    def test_a_higher_target_band_keeps_the_harder_points(self):
        _, progress = self.create(8.0)

        self.assertEqual(kp_ids(progress), [KP_BASIC, KP_ADVANCED])

    def test_the_band_of_every_point_in_the_path_is_recorded_with_the_path(self):
        path_id, _ = self.create(8.0)

        self.assertEqual(self.store.bands[path_id], {
            KP_BASIC: KnowledgePointBand(Decimal("4.0"), Decimal("5.0")),
            KP_ADVANCED: KnowledgePointBand(Decimal("7.0"), Decimal("8.0")),
        })

    def test_the_applied_scope_is_recorded_as_a_path_event(self):
        path_id, _ = self.create(5.5)

        scope_events = [payload for pid, _, name, payload in self.store.committed_events
                        if pid == path_id and name == "path.scope_applied"]
        self.assertEqual(scope_events, [{"target_band": "5.5", "included": 1, "excluded": 1}])

    def test_an_existing_path_is_returned_without_reading_the_curriculum_again(self):
        goal = {"id": str(uuid4()), "userId": self.user_id, "status": "ACTIVE", "targetBand": 5.5}
        content = BandedContentClient()
        paths = PathService(self.store, GoalClient(goal), content)
        first_id, _ = asyncio.run(paths.ensure_active_path(self.user_id, "internal-token"))

        second_id, _ = asyncio.run(paths.ensure_active_path(self.user_id, "internal-token"))

        self.assertEqual((second_id, content.calls), (first_id, 1))

    def test_nothing_in_scope_creates_no_path(self):
        with self.assertRaises(NoCurriculumInScope):
            self.create(3.5)

        self.assertEqual(self.store.paths, {})

    def test_a_goal_without_a_target_band_is_a_contract_error(self):
        goal = {"id": str(uuid4()), "userId": self.user_id, "status": "ACTIVE"}
        paths = PathService(self.store, GoalClient(goal), BandedContentClient())

        with self.assertRaises(ValueError):
            asyncio.run(paths.ensure_active_path(self.user_id, "internal-token"))


class NoCurriculumInScopeApiTest(unittest.TestCase):
    def test_nothing_in_scope_is_a_conflict_not_a_dependency_failure(self):
        import main
        from app.security.internal_jwt import AuthenticatedUser, bearer_scheme, require_current_user
        from fastapi.security import HTTPAuthorizationCredentials

        class Paths:
            async def active_status(self, *_args):
                raise NoCurriculumInScope

        main.app.dependency_overrides = {
            require_current_user: lambda: AuthenticatedUser(user_id=uuid4(), roles=frozenset({"CUSTOMER"})),
            bearer_scheme: lambda: HTTPAuthorizationCredentials(scheme="Bearer", credentials="internal-token"),
            main.get_path_service: lambda: Paths(),
        }
        try:
            response = TestClient(main.app).get("/api/ai-learning/status")
        finally:
            main.app.dependency_overrides = {}

        self.assertEqual(response.status_code, 409)
        self.assertEqual(response.json(), {"detail": "No learning content matches the goal's target band"})


if __name__ == "__main__":
    unittest.main()
