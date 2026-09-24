"""The repository migrations alone must build the AI Learning schema from an empty database.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run the
PostgreSQL checks; the ordering checks always run.
"""

from __future__ import annotations

import unittest
from pathlib import Path
from uuid import uuid4

from tests.postgres_schema_support import (
    MIGRATIONS_DIR,
    PostgresSchema,
    database_url_or_skip,
    migration_version,
    ordered_migrations,
)


class MigrationOrderingTest(unittest.TestCase):
    def test_repository_migrations_start_with_the_v5_baseline(self):
        versions = [migration_version(path) for path in ordered_migrations(MIGRATIONS_DIR.glob("V*.sql"))]

        self.assertEqual(versions[:3], ["0.1", "1", "2"])

    def test_versions_are_ordered_numerically_not_lexically(self):
        names = ["V10__later.sql", "V2__second.sql", "V0_1__baseline.sql", "V1__first.sql", "V1_5__patch.sql"]

        ordered = [migration_version(path) for path in ordered_migrations(Path(name) for name in names)]

        self.assertEqual(ordered, ["0.1", "1", "1.5", "2", "10"])

    def test_non_versioned_file_names_are_rejected(self):
        with self.assertRaises(ValueError):
            migration_version(Path("R__repeatable.sql"))


class MigrationsPostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def _tables(self) -> set[str]:
        rows = self.schema.query(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = %s",
            (self.schema.name,),
        )
        return {row[0] for row in rows}

    def _indexes(self) -> set[str]:
        rows = self.schema.query("SELECT indexname FROM pg_indexes WHERE schemaname = %s", (self.schema.name,))
        return {row[0] for row in rows}

    def _insert_path(self) -> str:
        path_id = str(uuid4())
        self.schema.execute(
            """INSERT INTO mastery_paths (path_id, user_id, learning_goal_id, state_json, revision,
                                          created_at, updated_at)
               VALUES (%s, %s, %s, '{}'::jsonb, 0, now(), now())""",
            (path_id, str(uuid4()), str(uuid4())),
        )
        return path_id

    def _insert_interaction(self, path_id: str, status: str) -> None:
        self.schema.execute(
            """INSERT INTO mastery_interactions (interaction_id, path_id, status, question_json,
                                                 created_at, updated_at)
               VALUES (%s, %s, %s, '{}'::jsonb, now(), now())""",
            (str(uuid4()), path_id, status),
        )

    def test_migrations_alone_create_the_mastery_tables(self):
        self.assertTrue(
            {"mastery_paths", "mastery_interactions", "mastery_events",
             "mastery_learning_evidence", "formal_assessment_result_versions"} <= self._tables()
        )

    def test_later_migrations_find_their_baseline_objects(self):
        indexes = self._indexes()

        self.assertIn("uq_mastery_paths_user_learning_goal", indexes)
        self.assertIn("uq_mastery_evidence_source_reference", indexes)
        self.assertIn("uq_mastery_one_active_interaction", indexes)

    def test_interaction_status_uses_deeptutor_lowercase_values(self):
        import psycopg2

        path_id = self._insert_path()
        for status in ("graded", "abandoned", "registered"):
            self._insert_interaction(path_id, status)

        with self.assertRaises(psycopg2.errors.CheckViolation):
            self._insert_interaction(self._insert_path(), "REGISTERED")

    def test_a_path_has_at_most_one_active_interaction(self):
        import psycopg2

        path_id = self._insert_path()
        self._insert_interaction(path_id, "awaiting_input")

        with self.assertRaises(psycopg2.errors.UniqueViolation):
            self._insert_interaction(path_id, "answered")

    def test_path_revision_cannot_be_negative(self):
        import psycopg2

        with self.assertRaises(psycopg2.errors.CheckViolation):
            self.schema.execute(
                """INSERT INTO mastery_paths (path_id, user_id, state_json, revision, created_at, updated_at)
                   VALUES (%s, %s, '{}'::jsonb, -1, now(), now())""",
                (str(uuid4()), str(uuid4())),
            )


if __name__ == "__main__":
    unittest.main()
