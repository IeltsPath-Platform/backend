"""Learner memory storage and batching against the disposable PostgreSQL schema."""

import asyncio
import unittest
from uuid import uuid4

from app.tutor.memory import BATCH_MESSAGES, LearnerMemoryService, LearnerMemoryStore
from app.tutor.session_store import TutorSessionStore
from app.usage.quota import DailyQuotaStore
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip


class LearnerMemoryPostgresTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()
        cls.sessions = TutorSessionStore(cls.schema.url)
        cls.store = LearnerMemoryStore(cls.schema.url)
        cls.service = LearnerMemoryService(cls.store)

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def learner(self):
        user_id, path_id = str(uuid4()), str(uuid4())
        self.schema.execute(
            """INSERT INTO mastery_paths (path_id, user_id, learning_goal_id, state_json, revision,
                                          created_at, updated_at)
               VALUES (%s, %s, %s, '{}'::jsonb, 0, now(), now())""",
            (path_id, user_id, str(uuid4())),
        )
        session = self.sessions.create_session(user_id, path_id, "Memory test")
        self.assertIsNotNone(session)
        return user_id, session

    def add_messages(self, user_id, session, count, prefix):
        ids = []
        for index in range(count):
            role = "user" if index % 2 == 0 else "assistant"
            message_id = self.sessions.add_message(user_id, session.id, None, role, f"{prefix}-{index}")
            self.assertIsNotNone(message_id)
            ids.append(message_id)
        return ids

    @staticmethod
    def completion(result="- Prefers examples first.", error=None):
        calls = []

        async def complete(**kwargs):
            calls.append(kwargs)
            if error is not None:
                raise error
            return result

        return complete, calls

    def test_fewer_than_eight_unprocessed_messages_skips_completion(self):
        user_id, session = self.learner()
        self.add_messages(user_id, session, 7, "small-batch")
        complete, calls = self.completion()

        result = asyncio.run(self.service.update(user_id, complete))

        self.assertEqual(result, "skipped")
        self.assertEqual(calls, [])

    def test_batches_messages_across_sessions_and_advances_one_learner_cursor(self):
        user_id, first = self.learner()
        second = self.sessions.create_session(user_id, first.path_id, "Another session")
        first_ids = self.add_messages(user_id, first, 4, "first-session")
        second_ids = self.add_messages(user_id, second, 4, "second-session")
        complete, calls = self.completion()

        result = asyncio.run(self.service.update(user_id, complete))
        record = self.store.get(user_id)
        skipped = asyncio.run(self.service.update(user_id, complete))

        self.assertEqual(result, "updated")
        self.assertEqual(skipped, "skipped")
        self.assertEqual(record.content, "- Prefers examples first.")
        self.assertEqual(record.last_message_id, second_ids[-1])
        self.assertGreater(second_ids[0], first_ids[-1])
        self.assertIn("first-session-0", calls[0]["prompt"])
        self.assertIn("second-session-3", calls[0]["prompt"])
        self.assertLess(calls[0]["prompt"].index("first-session-0"), calls[0]["prompt"].index("second-session-0"))

    def test_uses_oldest_forty_then_processes_remaining_messages_in_a_later_batch(self):
        user_id, session = self.learner()
        message_ids = self.add_messages(user_id, session, BATCH_MESSAGES + 8, "batch")
        responses = iter(("- First batch.", "- Second batch."))
        complete, calls = self.completion()

        async def successive_complete(**kwargs):
            calls.append(kwargs)
            return next(responses)

        first_result = asyncio.run(self.service.update(user_id, successive_complete))
        first_record = self.store.get(user_id)
        second_result = asyncio.run(self.service.update(user_id, successive_complete))
        second_record = self.store.get(user_id)

        self.assertEqual((first_result, second_result), ("updated", "updated"))
        self.assertEqual(first_record.last_message_id, message_ids[BATCH_MESSAGES - 1])
        self.assertEqual(second_record.last_message_id, message_ids[-1])
        self.assertEqual(calls[0]["prompt"].count("[learner]"), 20)
        self.assertEqual(calls[0]["prompt"].count("[tutor]"), 20)
        self.assertIn("batch-0", calls[0]["prompt"])
        self.assertNotIn("batch-40", calls[0]["prompt"])
        self.assertIn("batch-40", calls[1]["prompt"])
        self.assertNotIn("batch-0", calls[1]["prompt"])

    def test_completion_exception_and_blank_output_preserve_existing_memory_and_cursor(self):
        user_id, session = self.learner()
        self.add_messages(user_id, session, 8, "retryable")
        self.assertTrue(self.store.save(user_id, "- Existing note.", 0, 0))
        before = self.store.get(user_id)
        broken, broken_calls = self.completion(error=RuntimeError("provider detail must not be logged"))
        blank, blank_calls = self.completion(" \n\t ")

        self.assertEqual(asyncio.run(self.service.update(user_id, broken)), "failed")
        self.assertEqual(asyncio.run(self.service.update(user_id, blank)), "failed")

        after = self.store.get(user_id)
        self.assertEqual((after.content, after.last_message_id, after.version),
                         (before.content, before.last_message_id, before.version))
        self.assertEqual(len(broken_calls), 1)
        self.assertEqual(len(blank_calls), 1)

    def test_empty_reply_without_memory_moves_past_the_batch_instead_of_retrying_it(self):
        user_id, session = self.learner()
        ids = self.add_messages(user_id, session, 8, "nothing-durable")
        blank, blank_calls = self.completion("")

        self.assertEqual(asyncio.run(self.service.update(user_id, blank)), "updated")
        self.assertEqual(asyncio.run(self.service.update(user_id, blank)), "skipped")

        record = self.store.get(user_id)
        self.assertEqual((record.content, record.last_message_id), ("", ids[-1]))
        self.assertEqual(len(blank_calls), 1)

    def test_optimistic_save_loses_to_a_concurrent_clear(self):
        user_id, session = self.learner()
        self.add_messages(user_id, session, 8, "concurrent")
        self.assertTrue(self.store.save(user_id, "- Existing note.", 0, 0))
        stale = self.store.get(user_id)

        self.store.clear(user_id)
        saved = self.store.save(user_id, "- Stale result.", 8, stale.version)

        current = self.store.get(user_id)
        self.assertFalse(saved)
        self.assertEqual(current.content, "")
        self.assertGreater(current.last_message_id, stale.last_message_id)

    def test_clear_skips_old_messages_and_only_summarizes_new_messages(self):
        user_id, session = self.learner()
        self.add_messages(user_id, session, 8, "before-clear-secret")
        self.store.clear(user_id)
        self.add_messages(user_id, session, 6, "after-clear")
        complete, calls = self.completion("- New durable observation.")

        skipped = asyncio.run(self.service.update(user_id, complete))
        self.assertEqual(skipped, "skipped")
        self.assertEqual(calls, [])

        self.add_messages(user_id, session, 2, "after-clear-later")
        updated = asyncio.run(self.service.update(user_id, complete))

        self.assertEqual(updated, "updated")
        self.assertNotIn("before-clear-secret", calls[0]["prompt"])
        self.assertIn("after-clear-0", calls[0]["prompt"])
        self.assertIn("after-clear-later-1", calls[0]["prompt"])

    def test_summaries_over_the_daily_limit_are_skipped_without_calling_the_model(self):
        user_id, session = self.learner()
        service = LearnerMemoryService(self.store, quota=DailyQuotaStore(self.schema.url, "Asia/Ho_Chi_Minh"),
                                       summaries_per_day=1)
        self.add_messages(user_id, session, 8, "first-batch")
        complete, calls = self.completion()

        self.assertEqual(asyncio.run(service.update(user_id, complete)), "updated")
        after_first = self.store.get(user_id)
        self.add_messages(user_id, session, 8, "second-batch")
        with self.assertLogs("app.tutor.memory", level="INFO") as logs:
            skipped = asyncio.run(service.update(user_id, complete))

        self.assertEqual(skipped, "skipped")
        self.assertEqual(len(calls), 1)
        self.assertEqual(self.store.get(user_id), after_first)
        self.assertIn("Learner memory summary skipped reason=quota", "\n".join(logs.output))

    def test_memory_and_pending_messages_are_scoped_to_the_learner(self):
        first_user, first_session = self.learner()
        second_user, second_session = self.learner()
        self.add_messages(first_user, first_session, 8, "first-learner-secret")
        self.add_messages(second_user, second_session, 8, "second-learner")
        self.assertTrue(self.store.save(first_user, "- First learner only.", 0, 0))

        first_record = self.store.get(first_user)
        second_record = self.store.get(second_user)
        second_messages = self.store.pending_messages(second_user, 0, BATCH_MESSAGES)

        self.assertEqual(first_record.content, "- First learner only.")
        self.assertEqual(second_record.content, "")
        self.assertNotIn("first-learner-secret", " ".join(message[2] for message in second_messages))
        self.assertEqual(len(second_messages), 8)
