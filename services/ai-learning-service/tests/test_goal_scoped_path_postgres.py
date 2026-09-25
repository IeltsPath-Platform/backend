"""Goal-scoped paths against PostgreSQL. Set AI_LEARNING_TEST_DATABASE_URL to run them."""

import asyncio
import unittest
from decimal import Decimal
from uuid import uuid4

from app.application.path_service import PathService
from app.persistence.postgres_learning_store import PostgresLearningStore

from tests.postgres_schema_support import PostgresSchema, database_url_or_skip
from tests.test_goal_scoped_path import KP_ADVANCED, KP_BASIC, BandedContentClient, GoalClient


class GoalScopedPathPostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def progress_for(self, target_band):
        user_id = str(uuid4())
        goal = {"id": str(uuid4()), "userId": user_id, "status": "ACTIVE", "targetBand": target_band}
        paths = PathService(PostgresLearningStore(self.schema.url), GoalClient(goal), BandedContentClient())
        return asyncio.run(paths.active_progress(user_id, "internal-token"))

    def test_learners_with_different_target_bands_get_different_paths_from_one_curriculum(self):
        _, low = self.progress_for(5.5)
        _, high = self.progress_for(8.0)

        self.assertEqual((low["knowledgePointCount"], high["knowledgePointCount"]), (1, 2))

    def test_the_band_snapshot_is_committed_with_the_path(self):
        path_id, _ = self.progress_for(8.0)

        rows = self.schema.query(
            "SELECT knowledge_point_id::text, band_min, band_max FROM mastery_path_knowledge_point_bands "
            "WHERE path_id = %s ORDER BY band_min", (path_id,))
        self.assertEqual(rows, [(KP_BASIC, Decimal("4.0"), Decimal("5.0")),
                                (KP_ADVANCED, Decimal("7.0"), Decimal("8.0"))])


    def test_placement_test_out_survives_a_reload_from_postgres(self):
        from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
        from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
        from tests.formal_assessment_support import event, item, mapping

        user_id, goal_id = str(uuid4()), str(uuid4())
        goal = {"id": goal_id, "userId": user_id, "status": "ACTIVE", "targetBand": 8.0}
        store = PostgresLearningStore(self.schema.url)
        paths = PathService(store, GoalClient(goal), BandedContentClient())
        path_id, _ = asyncio.run(paths.active_progress(user_id, "internal-token"))
        payload = event(user_id=user_id, goal_id=goal_id, attempt_id=str(uuid4()),
                        items=[item([mapping(KP_ADVANCED)], is_correct=False)], assessment_type="PLACEMENT")
        payload["data"]["overall_band"] = 6.0

        FormalAssessmentIngestionService(store, paths).ingest(FormalEvidenceAdapter.to_command(payload))

        reloaded = PathService(PostgresLearningStore(self.schema.url), GoalClient(goal), BandedContentClient())
        _, progress = asyncio.run(reloaded.active_progress(user_id, "internal-token"))
        sources = {kp["id"]: kp["mastery_source"]
                   for module in progress["mastery"]["modules"] for kp in module["knowledge_points"]}
        self.assertEqual(sources, {KP_BASIC: "placement", KP_ADVANCED: ""})
        _, status = asyncio.run(reloaded.active_status(user_id, "internal-token"))
        self.assertEqual(status["knowledgePointId"], KP_ADVANCED)


    def test_a_refresh_racing_the_consumer_loses_neither_update(self):
        import threading

        from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
        from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
        from tests.formal_assessment_support import event, item, mapping

        new_kp = "c5b2641f-28c8-467d-9d64-f52c8bdc1509"

        class GrowingContent(BandedContentClient):
            async def get_curriculum(self, bearer_token):
                topics, points = await super().get_curriculum(bearer_token)
                points.append({"id": new_kp, "topicId": points[0]["topicId"], "name": "New KP",
                               "learningType": "PROCEDURE", "status": "ACTIVE",
                               "effectiveBandMin": None, "effectiveBandMax": None})
                return topics, points

        for _ in range(5):
            user_id, goal_id = str(uuid4()), str(uuid4())
            goal = {"id": goal_id, "userId": user_id, "status": "ACTIVE", "targetBand": 8.0}
            path_id, _ = asyncio.run(PathService(PostgresLearningStore(self.schema.url), GoalClient(goal),
                                                 BandedContentClient()).active_progress(user_id, "internal-token"))
            command = FormalEvidenceAdapter.to_command(event(
                user_id=user_id, goal_id=goal_id, attempt_id=str(uuid4()),
                items=[item([mapping(KP_BASIC)], is_correct=True)]))
            start, errors = threading.Barrier(2), []

            def refresh():
                try:
                    start.wait(timeout=10)
                    asyncio.run(PathService(PostgresLearningStore(self.schema.url), GoalClient(goal),
                                            GrowingContent()).refresh_active_path(user_id, "internal-token"))
                except Exception as exc:  # noqa: BLE001 - surfaced by the assertion below
                    errors.append(exc)

            def ingest():
                try:
                    start.wait(timeout=10)
                    store = PostgresLearningStore(self.schema.url)
                    FormalAssessmentIngestionService(store, PathService(store)).ingest(command)
                except Exception as exc:  # noqa: BLE001
                    errors.append(exc)

            workers = [threading.Thread(target=refresh), threading.Thread(target=ingest)]
            for worker in workers:
                worker.start()
            for worker in workers:
                worker.join(timeout=30)

            self.assertEqual(errors, [])
            progress = PostgresLearningStore(self.schema.url).get_owned_progress(path_id, user_id)
            self.assertIn(new_kp, [kp.id for m in progress.modules for kp in m.knowledge_points])
            self.assertEqual([a.knowledge_point_id for a in progress.quiz_attempts], [KP_BASIC])


if __name__ == "__main__":
    unittest.main()
