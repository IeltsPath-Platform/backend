"""PostgreSQL behavior of the tutor session store: ownership, one running turn, recovery.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run it.
"""

import threading
import unittest
from uuid import uuid4

from app.tutor.session_store import ActiveTurnConflict, TutorSessionStore
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip


class TutorSessionStoreTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()
        cls.store = TutorSessionStore(cls.schema.url)

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def path_for(self, user_id):
        path_id = uuid4()
        self.schema.execute(
            """INSERT INTO mastery_paths (path_id, user_id, learning_goal_id, state_json, revision, created_at, updated_at)
               VALUES (%s, %s, %s, '{}'::jsonb, 1, now(), now())""",
            (str(path_id), str(user_id), str(uuid4())),
        )
        return path_id

    def learner_with_session(self):
        user_id = uuid4()
        session = self.store.create_session(user_id, self.path_for(user_id), "Study")
        self.assertIsNotNone(session)
        return user_id, session

    def test_session_lifecycle_and_messages(self):
        user_id, session = self.learner_with_session()
        turn_id = self.store.begin_turn(user_id, session.id)
        self.store.add_message(user_id, session.id, turn_id, "user", "Teach me")
        self.store.add_message(user_id, session.id, turn_id, "assistant", "Question?", {"question_id": "q1"})
        self.assertTrue(self.store.finish_turn(turn_id, "completed"))

        self.assertEqual([item.id for item in self.store.list_sessions(user_id)], [session.id])
        messages = self.store.recent_messages(user_id, session.id)
        self.assertEqual([(message.role, message.content) for message in messages],
                         [("user", "Teach me"), ("assistant", "Question?")])
        self.assertEqual(messages[1].metadata, {"question_id": "q1"})
        self.assertEqual([message.content for message in self.store.recent_messages(user_id, session.id, limit=1)],
                         ["Question?"])
        self.assertFalse(self.store.finish_turn(turn_id, "failed"), "a finished turn stays finished")

    def test_another_learner_cannot_see_or_touch_a_session(self):
        owner, session = self.learner_with_session()
        other = uuid4()

        self.assertIsNone(self.store.get_session(other, session.id))
        self.assertEqual(self.store.list_sessions(other), [])
        self.assertFalse(self.store.archive_session(other, session.id))
        self.assertIsNone(self.store.begin_turn(other, session.id))
        self.assertIsNone(self.store.add_message(other, session.id, None, "user", "hi"))
        self.assertEqual(self.store.recent_messages(other, session.id), [])
        self.assertIsNone(self.store.create_session(other, session.path_id, "Stolen path"))
        self.assertIsNotNone(self.store.get_session(owner, session.id))

    def test_malformed_ids_are_not_found_rather_than_errors(self):
        user_id, _session = self.learner_with_session()
        self.assertIsNone(self.store.get_session(user_id, "not-a-uuid"))
        self.assertIsNone(self.store.begin_turn(user_id, "not-a-uuid"))

    def test_only_one_turn_runs_per_session(self):
        user_id, session = self.learner_with_session()
        start = threading.Barrier(2)
        outcomes = []
        lock = threading.Lock()

        def begin():
            start.wait()
            try:
                result = self.store.begin_turn(user_id, session.id)
            except ActiveTurnConflict:
                result = "conflict"
            with lock:
                outcomes.append(result)

        workers = [threading.Thread(target=begin) for _ in range(2)]
        for worker in workers:
            worker.start()
        for worker in workers:
            worker.join(timeout=10)

        self.assertEqual(sorted(outcome == "conflict" for outcome in outcomes), [False, True])

    def test_interrupted_turns_are_failed_on_recovery(self):
        user_id, session = self.learner_with_session()
        self.store.begin_turn(user_id, session.id)
        with self.assertRaises(ActiveTurnConflict):
            self.store.begin_turn(user_id, session.id)

        self.assertGreaterEqual(self.store.recover_interrupted_turns(), 1)
        rows = self.schema.query("SELECT status, failure_code FROM turns WHERE session_id = %s", (str(session.id),))
        self.assertEqual(rows, [("failed", "interrupted")])
        self.assertIsNotNone(self.store.begin_turn(user_id, session.id))

    def test_archived_session_disappears(self):
        user_id, session = self.learner_with_session()
        self.assertTrue(self.store.archive_session(user_id, session.id))
        self.assertIsNone(self.store.get_session(user_id, session.id))
        self.assertEqual(self.store.list_sessions(user_id), [])
        self.assertIsNone(self.store.begin_turn(user_id, session.id))

    def test_running_turn_prevents_archive_until_it_finishes(self):
        user_id, session = self.learner_with_session()
        turn_id = self.store.begin_turn(user_id, session.id)
        with self.assertRaises(ActiveTurnConflict):
            self.store.archive_session(user_id, session.id)
        self.assertIsNotNone(self.store.get_session(user_id, session.id))
        self.assertTrue(self.store.finish_turn(turn_id, "completed"))
        self.assertTrue(self.store.archive_session(user_id, session.id))


if __name__ == "__main__":
    unittest.main()
