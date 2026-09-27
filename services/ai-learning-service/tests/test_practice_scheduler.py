"""Pure unit tests for per-question practice review scheduling."""

from datetime import datetime, timedelta, timezone
import unittest

from app.practice.scheduler import AGAIN_INTERVAL, DAY, ReviewState, first_mistake, review


NOW = datetime(2026, 9, 27, 12, 0, tzinfo=timezone.utc)


class PracticeSchedulerTest(unittest.TestCase):
    def test_first_mistake_is_due_in_ten_minutes(self):
        state = first_mistake(NOW)

        self.assertEqual(state.interval_days, 1)
        self.assertEqual(state.ease, 2.5)
        self.assertEqual((state.streak, state.lapses, state.review_count), (0, 0, 0))
        self.assertTrue(state.is_mistake)
        self.assertEqual(state.due_at, NOW + timedelta(minutes=10))
        self.assertAlmostEqual((state.due_at - NOW).total_seconds() / DAY, AGAIN_INTERVAL)

    def test_ratings_use_their_distinct_intervals_and_reset_streaks(self):
        initial = first_mistake(NOW)

        again = review(initial, "again", NOW)
        hard = review(initial, "hard", NOW)
        good = review(initial, "good", NOW)
        easy = review(initial, "easy", NOW)

        self.assertEqual(again.interval_days, 0.007)
        self.assertEqual((again.streak, again.lapses, again.review_count, again.ease), (0, 1, 1, 2.3))
        self.assertTrue(again.is_mistake)
        self.assertEqual(again.due_at, NOW + timedelta(days=0.007))
        self.assertEqual((hard.interval_days, hard.streak, hard.lapses, hard.review_count, hard.ease),
                         (1.2, 0, 0, 1, 2.35))
        self.assertEqual((good.interval_days, good.streak, good.lapses, good.review_count, good.ease),
                         (3, 1, 0, 1, 2.5))
        self.assertEqual((easy.interval_days, easy.streak, easy.lapses, easy.review_count, easy.ease),
                         (3.9, 1, 0, 1, 2.65))
        self.assertEqual(good.due_at, NOW + timedelta(days=3))
        self.assertEqual(easy.due_at, NOW + timedelta(days=3.9))

    def test_repeated_good_resolves_on_the_third_consecutive_review(self):
        state = first_mistake(NOW)
        for expected_streak in (1, 2):
            state = review(state, "good", NOW)
            self.assertEqual(state.streak, expected_streak)
            self.assertTrue(state.is_mistake)

        resolved = review(state, "good", NOW)
        self.assertEqual(resolved.streak, 3)
        self.assertFalse(resolved.is_mistake)

    def test_again_after_two_good_reviews_restarts_the_streak(self):
        state = review(review(first_mistake(NOW), "good", NOW), "good", NOW)

        retried = review(state, "again", NOW)

        self.assertEqual(retried.streak, 0)
        self.assertEqual(retried.lapses, 1)
        self.assertEqual(retried.review_count, 3)
        self.assertTrue(retried.is_mistake)

    def test_ease_and_interval_are_clamped(self):
        low_ease = ReviewState(interval_days=1, ease=1.3, streak=0, lapses=0, review_count=0,
                               due_at=NOW, is_mistake=True)
        high_ease = ReviewState(interval_days=1, ease=3.0, streak=0, lapses=0, review_count=0,
                                due_at=NOW, is_mistake=True)
        long_interval = ReviewState(interval_days=365, ease=3.0, streak=1, lapses=0, review_count=1,
                                    due_at=NOW, is_mistake=True)

        self.assertEqual(review(low_ease, "again", NOW).ease, 1.3)
        self.assertEqual(review(high_ease, "easy", NOW).ease, 3.0)
        capped = review(long_interval, "good", NOW)
        self.assertEqual(capped.interval_days, 365)
        self.assertEqual(capped.due_at, NOW + timedelta(days=365))

    def test_intervals_are_rounded_to_three_decimal_places(self):
        state = ReviewState(interval_days=1.2345, ease=2.5, streak=1, lapses=0, review_count=1,
                            due_at=NOW, is_mistake=True)

        updated = review(state, "good", NOW)

        self.assertEqual(updated.interval_days, 3.086)


if __name__ == "__main__":
    unittest.main()
