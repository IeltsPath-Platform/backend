"""PostgreSQL integration check for the goal-bound mastery path invariant.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run it.
"""

import threading
import unittest
from uuid import uuid4

from tests.postgres_schema_support import PostgresSchema, database_url_or_skip


class MasteryPathGoalUniquenessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        database_url = database_url_or_skip(cls)
        import psycopg2

        cls.psycopg2 = psycopg2
        cls.database_url = database_url
        cls.postgres_schema = PostgresSchema(database_url)
        cls.postgres_schema.create()
        cls.schema = cls.postgres_schema.name

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "postgres_schema"):
            cls.postgres_schema.drop()

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
                        """INSERT INTO mastery_paths
                           (path_id, user_id, learning_goal_id, state_json, revision, created_at, updated_at)
                           VALUES (%s, %s, %s, '{}'::jsonb, 0, now(), now())""",
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
