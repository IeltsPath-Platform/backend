"""PostgreSQL integration check for the goal-bound mastery path invariant.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run it.
"""

import os
import threading
import unittest
from pathlib import Path
from uuid import uuid4


class MasteryPathGoalUniquenessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        database_url = os.environ.get("AI_LEARNING_TEST_DATABASE_URL")
        if not database_url:
            raise unittest.SkipTest("AI_LEARNING_TEST_DATABASE_URL is not configured")
        try:
            import psycopg2
        except ImportError as exc:
            raise unittest.SkipTest("psycopg2 is not installed") from exc

        cls.psycopg2 = psycopg2
        cls.database_url = database_url
        cls.schema = f"ai_learning_path_constraint_{uuid4().hex}"

        connection = psycopg2.connect(database_url)
        connection.autocommit = True
        try:
            with connection.cursor() as cursor:
                cursor.execute(f'CREATE SCHEMA "{cls.schema}"')
                cursor.execute(f'SET search_path TO "{cls.schema}"')
                cursor.execute(
                    """
                    CREATE TABLE mastery_paths (
                        path_id UUID PRIMARY KEY,
                        user_id UUID NOT NULL,
                        learning_goal_id UUID,
                        state_json JSONB NOT NULL DEFAULT '{}'::jsonb,
                        revision BIGINT NOT NULL DEFAULT 0
                    )
                    """
                )
                migration = (
                    Path(__file__).parents[1]
                    / "migrations"
                    / "V1__one_mastery_path_per_learning_goal.sql"
                ).read_text(encoding="utf-8")
                cursor.execute(migration)
        finally:
            connection.close()

    @classmethod
    def tearDownClass(cls):
        if not hasattr(cls, "schema"):
            return
        connection = cls.psycopg2.connect(cls.database_url)
        connection.autocommit = True
        try:
            with connection.cursor() as cursor:
                cursor.execute(f'DROP SCHEMA IF EXISTS "{cls.schema}" CASCADE')
        finally:
            connection.close()

    def test_concurrent_goal_bound_inserts_allow_only_one_winner(self):
        user_id = uuid4()
        goal_id = uuid4()
        start = threading.Barrier(2)
        outcomes = []
        outcomes_lock = threading.Lock()

        def insert_path():
            connection = self.psycopg2.connect(self.database_url)
            try:
                with connection.cursor() as cursor:
                    cursor.execute(f'SET search_path TO "{self.schema}"')
                    start.wait(timeout=10)
                    cursor.execute(
                        "INSERT INTO mastery_paths (path_id, user_id, learning_goal_id) VALUES (%s, %s, %s)",
                        (str(uuid4()), str(user_id), str(goal_id)),
                    )
                connection.commit()
                outcome = "inserted"
            except self.psycopg2.errors.UniqueViolation:
                connection.rollback()
                outcome = "conflict"
            finally:
                connection.close()
            with outcomes_lock:
                outcomes.append(outcome)

        workers = [threading.Thread(target=insert_path) for _ in range(2)]
        for worker in workers:
            worker.start()
        for worker in workers:
            worker.join(timeout=20)

        self.assertTrue(all(not worker.is_alive() for worker in workers))
        self.assertCountEqual(outcomes, ["inserted", "conflict"])

        connection = self.psycopg2.connect(self.database_url)
        try:
            with connection.cursor() as cursor:
                cursor.execute(f'SET search_path TO "{self.schema}"')
                cursor.execute(
                    "SELECT COUNT(*) FROM mastery_paths WHERE user_id = %s AND learning_goal_id = %s",
                    (str(user_id), str(goal_id)),
                )
                self.assertEqual(cursor.fetchone()[0], 1)
        finally:
            connection.close()


if __name__ == "__main__":
    unittest.main()
