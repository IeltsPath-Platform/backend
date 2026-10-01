"""Daily per-learner quota counting against the disposable PostgreSQL schema.

Set AI_LEARNING_TEST_DATABASE_URL to a disposable PostgreSQL database to run it.
"""

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import unittest
from urllib.parse import quote
from uuid import uuid4

from app.usage.quota import MEMORY_SUMMARY, TUTOR_TURN, DailyQuotaStore
from tests.postgres_schema_support import PostgresSchema, database_url_or_skip

QUOTA_TIMEZONE = "Asia/Ho_Chi_Minh"
# Viet Nam has no daylight saving time, so a fixed offset matches the IANA zone without tzdata on Windows.
VIETNAM = timezone(timedelta(hours=7))


class DailyQuotaStoreTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.schema = PostgresSchema(database_url_or_skip(cls))
        cls.schema.create()
        cls.store = DailyQuotaStore(cls.schema.url, QUOTA_TIMEZONE)

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "schema"):
            cls.schema.drop()

    def used(self, user_id, kind):
        rows = self.schema.query("SELECT used FROM llm_daily_usage WHERE user_id = %s AND kind = %s",
                                 (user_id, kind))
        return rows[0][0] if rows else 0

    def test_allows_exactly_the_limit_then_refuses_without_counting_the_refusal(self):
        user_id = str(uuid4())

        results = [self.store.consume(user_id, TUTOR_TURN, 3) for _ in range(5)]

        self.assertEqual([result.allowed for result in results], [True, True, True, False, False])
        self.assertEqual([result.used for result in results], [1, 2, 3, 3, 3])
        self.assertTrue(all(result.limit == 3 for result in results))
        self.assertEqual(self.used(user_id, TUTOR_TURN), 3)

    def test_kinds_and_learners_are_counted_independently(self):
        first, second = str(uuid4()), str(uuid4())

        self.store.consume(first, TUTOR_TURN, 1)
        self.assertFalse(self.store.consume(first, TUTOR_TURN, 1).allowed)

        self.assertTrue(self.store.consume(first, MEMORY_SUMMARY, 1).allowed)
        self.assertTrue(self.store.consume(second, TUTOR_TURN, 1).allowed)
        self.assertEqual((self.used(first, TUTOR_TURN), self.used(first, MEMORY_SUMMARY),
                          self.used(second, TUTOR_TURN)), (1, 1, 1))

    def test_zero_limit_never_refuses_but_still_counts(self):
        user_id = str(uuid4())

        results = [self.store.consume(user_id, TUTOR_TURN, 0) for _ in range(4)]

        self.assertTrue(all(result.allowed for result in results))
        self.assertEqual(results[-1].used, 4)
        self.assertEqual(self.store.usage(user_id, TUTOR_TURN, 0).used, 4)

    def test_concurrent_consumers_never_exceed_the_limit(self):
        user_id = str(uuid4())

        with ThreadPoolExecutor(max_workers=20) as pool:
            results = list(pool.map(lambda _index: self.store.consume(user_id, TUTOR_TURN, 10), range(20)))

        self.assertEqual(sum(result.allowed for result in results), 10)
        self.assertEqual(self.used(user_id, TUTOR_TURN), 10)

    def test_a_new_local_day_starts_counting_again(self):
        user_id = str(uuid4())
        self.store.consume(user_id, TUTOR_TURN, 1)
        self.assertFalse(self.store.consume(user_id, TUTOR_TURN, 1).allowed)

        self.schema.execute("UPDATE llm_daily_usage SET usage_date = usage_date - 1 WHERE user_id = %s", (user_id,))
        again = self.store.consume(user_id, TUTOR_TURN, 1)

        self.assertTrue(again.allowed)
        self.assertEqual(again.used, 1)
        self.assertEqual(self.store.usage(user_id, TUTOR_TURN, 1).used, 1)

    def test_resets_at_is_the_next_local_midnight(self):
        result = self.store.consume(str(uuid4()), TUTOR_TURN, 5)
        expected = self.schema.query(
            "SELECT (date_trunc('day', now() AT TIME ZONE %s) + interval '1 day') AT TIME ZONE %s",
            (QUOTA_TIMEZONE, QUOTA_TIMEZONE))[0][0]
        local = result.resets_at.astimezone(VIETNAM)

        self.assertEqual(result.resets_at, expected)
        self.assertEqual(result.resets_at.utcoffset(), timedelta(0))
        self.assertEqual((local.hour, local.minute, local.second), (0, 0, 0))
        self.assertEqual(local.date(), result.usage_date + timedelta(days=1))
        self.assertLess(timedelta(0), result.resets_at - datetime.now(timezone.utc))
        self.assertLessEqual(result.resets_at - datetime.now(timezone.utc), timedelta(days=1))

    def test_refund_returns_one_unit_on_the_consumed_day_and_never_goes_below_zero(self):
        user_id = str(uuid4())
        first = self.store.consume(user_id, TUTOR_TURN, 1)

        self.store.refund(user_id, TUTOR_TURN, first.usage_date)
        self.store.refund(user_id, TUTOR_TURN, first.usage_date)

        self.assertEqual(self.used(user_id, TUTOR_TURN), 0)
        self.assertTrue(self.store.consume(user_id, TUTOR_TURN, 1).allowed)

    def test_usage_reads_without_counting(self):
        user_id = str(uuid4())

        empty = self.store.usage(user_id, MEMORY_SUMMARY, 10)
        self.store.consume(user_id, MEMORY_SUMMARY, 10)
        after = self.store.usage(user_id, MEMORY_SUMMARY, 10)

        self.assertEqual((empty.used, empty.limit, empty.allowed), (0, 10, True))
        self.assertEqual(after.used, 1)
        self.assertEqual(self.used(user_id, MEMORY_SUMMARY), 1)

    def test_unknown_kind_and_negative_limit_are_rejected(self):
        with self.assertRaises(ValueError):
            self.store.consume(str(uuid4()), "path_ordering", 1)
        with self.assertRaises(ValueError):
            self.store.consume(str(uuid4()), TUTOR_TURN, -1)

    def test_startup_check_needs_an_iana_zone_and_the_usage_table(self):
        import psycopg2

        # "UTC+7" parses in PostgreSQL but means UTC-7, so it must be refused like an unknown name.
        for zone in ("Mars/Olympus_Mons", "UTC+7"):
            with self.subTest(zone=zone), self.assertRaises(ValueError):
                DailyQuotaStore(self.schema.url, zone).check_ready()
        DailyQuotaStore(self.schema.url, QUOTA_TIMEZONE).check_ready()

        base = database_url_or_skip(type(self))
        separator = "&" if "?" in base else "?"
        without_tables = f"{base}{separator}options={quote(f'-csearch_path=ai_learning_missing_{uuid4().hex}')}"
        with self.assertRaises(psycopg2.errors.UndefinedTable):
            DailyQuotaStore(without_tables, QUOTA_TIMEZONE).check_ready()


if __name__ == "__main__":
    unittest.main()
