"""Bulk writes of the evidence projection and the details snapshot. Set AI_LEARNING_TEST_DATABASE_URL to run them."""

import asyncio
import time
import unittest
from uuid import uuid4

from app.mastery.models import LearningEvidence

from app.adapters.knowledge_point_details import KnowledgePointDetails
from app.application.path_service import PathService
from app.persistence.postgres_learning_store import PostgresLearningStore

from tests.postgres_schema_support import PostgresSchema, database_url_or_skip
from tests.test_goal_scoped_path import KP_ADVANCED, KP_BASIC, BandedContentClient, GoalClient


class EvidenceProjectionPostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def setUp(self):
        self.user_id = str(uuid4())
        goal = {"id": str(uuid4()), "userId": self.user_id, "status": "ACTIVE", "targetBand": 8.0}
        self.store = PostgresLearningStore(self.schema.url)
        self.path_id, _ = asyncio.run(
            PathService(self.store, GoalClient(goal), BandedContentClient()).active_progress(self.user_id, "t"))

    def test_a_thousand_evidence_rows_commit_in_order_in_one_revision(self):
        count = 1200
        started = time.monotonic()
        with self.store.transaction(self.path_id) as tx:
            for index in range(count):
                tx.progress.learning_evidence.append(LearningEvidence(
                    knowledge_point_id=KP_BASIC if index % 2 else KP_ADVANCED,
                    result="correct" if index % 3 else "incorrect",
                    session_id=str(uuid4()),
                ))
            tx.touch()
        elapsed = time.monotonic() - started

        rows = self.schema.query(
            "SELECT count(*), min(ordinal), max(ordinal), count(DISTINCT ordinal) "
            "FROM mastery_learning_evidence WHERE path_id = %s", (self.path_id,))
        self.assertEqual(rows[0], (count, 0, count - 1, count))
        self.assertEqual(self.schema.query(
            "SELECT knowledge_point_id::text, result FROM mastery_learning_evidence "
            "WHERE path_id = %s AND ordinal = 4", (self.path_id,))[0], (KP_ADVANCED, "correct"))
        # Generous bound: row-by-row inserts took far longer; this only guards against a regression to them.
        self.assertLess(elapsed, 20)

        # Rewriting with fewer rows leaves exactly the aggregate's evidence.
        with self.store.transaction(self.path_id) as tx:
            del tx.progress.learning_evidence[10:]
            tx.touch()
        self.assertEqual(self.schema.query(
            "SELECT count(*) FROM mastery_learning_evidence WHERE path_id = %s", (self.path_id,))[0][0], 10)

    def test_details_snapshot_is_replaced_as_a_whole(self):
        with self.store.transaction(self.path_id) as tx:
            self.store.replace_knowledge_point_details(self.path_id, {
                KP_BASIC: KnowledgePointDetails("READING", "basic"),
                KP_ADVANCED: KnowledgePointDetails(None, ""),
            })
            tx.touch()
        with self.store.transaction(self.path_id) as tx:
            details = self.store.knowledge_point_details(self.path_id)
            self.store.replace_knowledge_point_details(self.path_id, {})
            tx.touch()

        self.assertEqual(details, {KP_BASIC: KnowledgePointDetails("READING", "basic"),
                                   KP_ADVANCED: KnowledgePointDetails(None, "")})
        self.assertEqual(self.schema.query(
            "SELECT count(*) FROM mastery_path_knowledge_point_details WHERE path_id = %s",
            (self.path_id,))[0][0], 0)
