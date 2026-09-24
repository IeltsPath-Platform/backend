"""PostgreSQL integration and end-to-end checks for formal assessment ingestion.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run them.
"""

import asyncio
import json
import threading
import unittest
from types import SimpleNamespace
from unittest import mock
from uuid import uuid4

from deeptutor.learning.policy import next_objective

from app.adapters.formal_evidence_adapter import FormalEvidenceAdapter
from app.application.formal_assessment_ingestion import FormalAssessmentIngestionService
from app.application.path_service import PathService
from app.messaging.assessment_consumer import AssessmentCompletedConsumer
from app.messaging.topology import AssessmentCompletedTopology
from app.persistence.postgres_learning_store import PostgresLearningStore

from tests.formal_assessment_support import GRAMMAR_KP, VOCABULARY_KP, curriculum, event, item, mapping
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip


class _AckingChannel:
    def __init__(self):
        self.acks, self.nacks = [], []

    def basic_ack(self, delivery_tag):
        self.acks.append(delivery_tag)

    def basic_nack(self, delivery_tag, requeue):
        self.nacks.append(delivery_tag)


class _ActiveGoalClient:
    def __init__(self, goal):
        self._goal = goal

    async def get_active_goal(self, _bearer_token):
        return dict(self._goal)


class FormalAssessmentPostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def setUp(self):
        self.store = PostgresLearningStore(self.schema.url)
        self.paths = PathService(self.store)
        self.ingestion = FormalAssessmentIngestionService(self.store, self.paths)
        self.user_id, self.goal_id = str(uuid4()), str(uuid4())
        self.path_id, _ = self.paths.ensure_path(self.user_id, self.goal_id, curriculum())

    def payload(self, *, is_correct=True, attempt_id=None, version=1):
        return event(user_id=self.user_id, goal_id=self.goal_id, attempt_id=attempt_id or str(uuid4()),
                     items=[item([mapping(VOCABULARY_KP)], is_correct=is_correct)], result_version=version)

    def reload(self):
        # A fresh store instance proves the state came back from PostgreSQL.
        return PostgresLearningStore(self.schema.url).get_owned_progress(self.path_id, self.user_id)

    def test_assessment_result_changes_status_through_real_deeptutor_after_postgres_reload(self):
        status_client = _ActiveGoalClient({"id": self.goal_id, "userId": self.user_id, "status": "ACTIVE"})
        api_paths = PathService(self.store, status_client, content_client=None)
        before = asyncio.run(api_paths.active_status(self.user_id, "internal-token"))[1]
        self.assertEqual((before["action"], before["knowledgePointId"]), ("probe", VOCABULARY_KP))

        consumer = AssessmentCompletedConsumer(self.ingestion, AssessmentCompletedTopology(), max_delivery_attempts=3)
        channel = _AckingChannel()
        for delivery_tag in range(1, 7):
            consumer.on_message(channel, SimpleNamespace(delivery_tag=delivery_tag),
                                SimpleNamespace(message_id=str(uuid4()), headers={}, content_type="application/json",
                                                type="AssessmentCompleted.v2"),
                                json.dumps(self.payload()).encode())

        self.assertEqual(channel.acks, [1, 2, 3, 4, 5, 6])
        reloaded = self.reload()
        self.assertEqual(next_objective(reloaded).knowledge_point_id, GRAMMAR_KP)
        after = asyncio.run(api_paths.active_status(self.user_id, "internal-token"))[1]
        self.assertEqual(after["knowledgePointId"], GRAMMAR_KP)
        self.assertEqual(after["revision"], reloaded.version)
        projected = self.schema.query(
            "SELECT count(*) FROM mastery_learning_evidence WHERE path_id = %s AND source_reference_id IS NOT NULL",
            (self.path_id,))
        self.assertEqual(projected[0][0], 6)
        applied_events = self.schema.query(
            "SELECT count(*) FROM mastery_events WHERE path_id = %s AND event_type = 'assessment.result_applied'",
            (self.path_id,))
        self.assertEqual(applied_events[0][0], 6)

    def test_concurrent_duplicate_deliveries_commit_one_effect(self):
        command = FormalEvidenceAdapter.to_command(self.payload())
        start = threading.Barrier(3)
        outcomes, errors = [], []

        def deliver():
            try:
                start.wait(timeout=10)
                ingestion = FormalAssessmentIngestionService(PostgresLearningStore(self.schema.url), self.paths)
                outcomes.append(ingestion.ingest(command).status)
            except Exception as exc:  # noqa: BLE001 - surfaced by the assertion below
                errors.append(exc)

        workers = [threading.Thread(target=deliver) for _ in range(3)]
        for worker in workers:
            worker.start()
        for worker in workers:
            worker.join(timeout=30)

        self.assertEqual(errors, [])
        self.assertCountEqual(outcomes, ["applied", "duplicate", "duplicate"])
        self.assertEqual(len(self.reload().quiz_attempts), 1)

    def test_regrade_keeps_only_the_latest_version_in_state_projection_and_ledger(self):
        attempt_id = str(uuid4())
        self.ingestion.ingest(FormalEvidenceAdapter.to_command(self.payload(is_correct=False, attempt_id=attempt_id)))
        regrade = FormalEvidenceAdapter.to_command(self.payload(is_correct=True, attempt_id=attempt_id, version=2))

        self.ingestion.ingest(regrade)

        progress = self.reload()
        self.assertEqual([a.is_correct for a in progress.quiz_attempts], [True])
        references = self.schema.query(
            "SELECT source_reference_id::text FROM mastery_learning_evidence WHERE path_id = %s", (self.path_id,))
        self.assertEqual([row[0] for row in references],
                         [regrade.items[0].knowledge_points[0].source_reference_id])
        ledger = self.schema.query(
            "SELECT result_version FROM formal_assessment_result_versions WHERE path_id = %s AND attempt_id = %s",
            (self.path_id, attempt_id))
        self.assertEqual(ledger, [(2,)])

    def test_failed_mutation_commits_nothing(self):
        revision_before = self.reload().version
        command = FormalEvidenceAdapter.to_command(self.payload())

        with mock.patch.object(self.store, "record_applied_result", side_effect=RuntimeError("ledger write failed")):
            with self.assertRaises(RuntimeError):
                self.ingestion.ingest(command)

        progress = self.reload()
        self.assertEqual(progress.version, revision_before)
        self.assertEqual(progress.quiz_attempts, [])
        self.assertEqual(self.schema.query(
            "SELECT count(*) FROM mastery_learning_evidence WHERE path_id = %s", (self.path_id,))[0][0], 0)

    def test_projection_rejects_a_duplicated_formal_outcome(self):
        self.ingestion.ingest(FormalEvidenceAdapter.to_command(self.payload()))
        revision = self.reload().version
        import psycopg2

        with self.assertRaises(psycopg2.errors.UniqueViolation):
            with self.store.transaction(self.path_id) as tx:
                tx.progress.learning_evidence.append(tx.progress.learning_evidence[-1].model_copy())
                tx.touch()

        self.assertEqual(self.reload().version, revision)


class _CurriculumClient:
    """Content Service stub returning the flattened topic/KP shape ``ContentServiceClient`` produces."""

    async def get_curriculum(self, _bearer_token):
        modules = curriculum()
        topics = [{"id": module.id, "name": module.name, "sortOrder": module.order, "status": "ACTIVE"}
                  for module in modules]
        points = [
            {"id": kp.id, "topicId": module.id, "name": kp.name, "learningType": kp.type.value.upper(),
             "status": "ACTIVE", "createdAt": f"2026-09-24T10:00:0{index}Z"}
            for module in modules
            for index, kp in enumerate(module.knowledge_points)
        ]
        return topics, points


class PendingFormalResultPostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def setUp(self):
        self.store = PostgresLearningStore(self.schema.url)
        self.paths = PathService(self.store)
        self.ingestion = FormalAssessmentIngestionService(self.store, self.paths)

    def command(self, user_id, goal_id):
        return FormalEvidenceAdapter.to_command(event(
            user_id=user_id, goal_id=goal_id, attempt_id=str(uuid4()),
            items=[item([mapping(VOCABULARY_KP)], is_correct=True)]))

    def pending_rows(self, user_id):
        return self.schema.query(
            "SELECT count(*) FROM pending_formal_assessment_results WHERE user_id = %s", (user_id,))[0][0]

    def test_parking_and_path_creation_racing_apply_the_result_exactly_once(self):
        for _ in range(10):
            user_id, goal_id = str(uuid4()), str(uuid4())
            command = self.command(user_id, goal_id)
            start = threading.Barrier(2)
            errors = []

            def park():
                try:
                    start.wait(timeout=10)
                    store = PostgresLearningStore(self.schema.url)
                    FormalAssessmentIngestionService(store, PathService(store)).ingest(command)
                except Exception as exc:  # noqa: BLE001 - surfaced by the assertion below
                    errors.append(exc)

            def create():
                try:
                    start.wait(timeout=10)
                    PathService(PostgresLearningStore(self.schema.url)).ensure_path(user_id, goal_id, curriculum())
                except Exception as exc:  # noqa: BLE001
                    errors.append(exc)

            workers = [threading.Thread(target=park), threading.Thread(target=create)]
            for worker in workers:
                worker.start()
            for worker in workers:
                worker.join(timeout=30)

            self.assertEqual(errors, [])
            path_id = self.store.find_path(user_id, goal_id)
            progress = self.store.get_owned_progress(path_id, user_id)
            self.assertEqual(len(progress.quiz_attempts), 1)
            self.assertEqual(self.pending_rows(user_id), 0)
            ledger = self.schema.query(
                "SELECT count(*) FROM formal_assessment_result_versions WHERE path_id = %s", (path_id,))
            self.assertEqual(ledger[0][0], 1)

    def test_first_status_call_creates_the_path_with_the_pending_result_applied(self):
        user_id, goal_id = str(uuid4()), str(uuid4())
        consumer = AssessmentCompletedConsumer(self.ingestion, AssessmentCompletedTopology(), max_delivery_attempts=3)
        channel = _AckingChannel()
        for delivery_tag in range(1, 7):
            consumer.on_message(channel, SimpleNamespace(delivery_tag=delivery_tag),
                                SimpleNamespace(message_id=str(uuid4()), headers={}, content_type="application/json",
                                                type="AssessmentCompleted.v2"),
                                json.dumps(event(user_id=user_id, goal_id=goal_id, attempt_id=str(uuid4()),
                                                 items=[item([mapping(VOCABULARY_KP)], is_correct=True)])).encode())
        self.assertEqual(channel.acks, [1, 2, 3, 4, 5, 6])
        self.assertEqual(self.pending_rows(user_id), 6)
        self.assertIsNone(self.store.find_path(user_id, goal_id))

        api_paths = PathService(
            self.store,
            _ActiveGoalClient({"id": goal_id, "userId": user_id, "status": "ACTIVE"}),
            _CurriculumClient(),
        )
        path_id, status = asyncio.run(api_paths.active_status(user_id, "internal-token"))

        self.assertEqual(status["revision"], 1)
        self.assertEqual(status["knowledgePointId"], GRAMMAR_KP)
        self.assertEqual(self.pending_rows(user_id), 0)
        reloaded = PostgresLearningStore(self.schema.url).get_owned_progress(path_id, user_id)
        self.assertEqual(next_objective(reloaded).knowledge_point_id, GRAMMAR_KP)
        ledger = self.schema.query(
            "SELECT count(*) FROM formal_assessment_result_versions WHERE path_id = %s", (path_id,))
        self.assertEqual(ledger[0][0], 6)


if __name__ == "__main__":
    unittest.main()
