"""Pure scheduling rules for a learner's wrong practice questions."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta

DAY = 86_400
AGAIN_INTERVAL = 10 / 1440


@dataclass(frozen=True)
class ReviewState:
    interval_days: float
    ease: float
    streak: int
    lapses: int
    review_count: int
    due_at: datetime
    is_mistake: bool


def first_mistake(now: datetime) -> ReviewState:
    return ReviewState(
        interval_days=1,
        ease=2.5,
        streak=0,
        lapses=0,
        review_count=0,
        due_at=now + timedelta(minutes=10),
        is_mistake=True,
    )


def review(state: ReviewState, rating: str, now: datetime) -> ReviewState:
    """Advance one card's schedule using the fixed again/hard/good/easy rules."""
    interval = state.interval_days
    ease = state.ease
    streak = state.streak
    lapses = state.lapses

    if rating == "again":
        interval = AGAIN_INTERVAL
        streak = 0
        lapses += 1
        ease = max(1.3, ease - 0.20)
    elif rating == "hard":
        interval = max(1, interval * 1.2)
        streak = 0
        ease = max(1.3, ease - 0.15)
    elif rating in {"good", "easy"}:
        interval = 3 if state.review_count == 0 or interval <= 1 else interval * ease
        streak += 1
        if rating == "easy":
            interval *= 1.3
            ease = min(3.0, ease + 0.15)
    else:
        raise ValueError("rating must be one of again, hard, good, or easy")

    interval = min(365, round(interval, 3))
    return ReviewState(
        interval_days=interval,
        ease=ease,
        streak=streak,
        lapses=lapses,
        review_count=state.review_count + 1,
        due_at=now + timedelta(seconds=interval * DAY),
        is_mistake=streak < 3,
    )


__all__ = ["AGAIN_INTERVAL", "DAY", "ReviewState", "first_mistake", "review"]
