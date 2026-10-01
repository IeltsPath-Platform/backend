"""Daily per-learner counters for LLM-backed actions, kept in PostgreSQL.

A day is a calendar day in the configured IANA time zone. PostgreSQL computes it from its own clock and tz database,
so every API instance agrees on when a day ends and the service needs no Python time zone data. Counting is a single
upsert guarded by the limit, so concurrent requests can never push a learner past it.
"""

from __future__ import annotations

from contextlib import closing
from dataclasses import dataclass
from datetime import date, datetime, timezone
from uuid import UUID

import psycopg2

TUTOR_TURN = "tutor_turn"
MEMORY_SUMMARY = "memory_summary"
KINDS = frozenset({TUTOR_TURN, MEMORY_SUMMARY})

# The local day and the next local midnight, both from the statement's single now().
_CLOCK = """SELECT (now() AT TIME ZONE %(tz)s)::date AS usage_date,
                   (date_trunc('day', now() AT TIME ZONE %(tz)s) + interval '1 day') AT TIME ZONE %(tz)s AS resets_at"""


@dataclass(frozen=True)
class QuotaResult:
    allowed: bool
    used: int
    limit: int  # 0 = unlimited
    usage_date: date  # the local day the unit was counted on; a refund targets this day
    resets_at: datetime  # next local midnight, in UTC


def _user_id(user_id: UUID | str) -> str:
    return str(UUID(str(user_id)))


def _check(kind: str, limit: int) -> None:
    if kind not in KINDS:
        raise ValueError(f"Unknown usage kind: {kind}")
    if limit < 0:
        raise ValueError("limit must be zero (unlimited) or positive")


def _result(used: int, limit: int, usage_date: date, resets_at: datetime, *, allowed: bool) -> QuotaResult:
    return QuotaResult(allowed, used, limit, usage_date, resets_at.astimezone(timezone.utc))


class DailyQuotaStore:
    def __init__(self, database_url: str, timezone_name: str) -> None:
        if not database_url.strip():
            raise ValueError("database_url must not be blank")
        if not timezone_name.strip():
            raise ValueError("timezone must not be blank")
        self._database_url = database_url
        self._timezone = timezone_name

    def _connect(self):
        return closing(psycopg2.connect(self._database_url))

    def check_ready(self) -> None:
        """Fail at start-up, not on a learner's first turn, when the zone or the usage table is missing.

        Only IANA names are accepted: PostgreSQL also parses POSIX strings such as ``UTC+7``, but reads their offset
        with the opposite sign, which would move the day boundary by hours without any error.
        """
        with self._connect() as connection, connection.cursor() as cursor:
            cursor.execute("SELECT 1 FROM pg_timezone_names WHERE name = %s", (self._timezone,))
            known = cursor.fetchone() is not None
        if not known:
            raise ValueError(f"Daily limit time zone is not an IANA name PostgreSQL knows: {self._timezone}")
        self.usage(UUID(int=0), TUTOR_TURN, 0)

    def consume(self, user_id: UUID | str, kind: str, limit: int) -> QuotaResult:
        """Count one unit for today unless the learner already reached ``limit``."""
        _check(kind, limit)
        params = {"tz": self._timezone, "user": _user_id(user_id), "kind": kind, "limit": limit}
        with self._connect() as connection, connection, connection.cursor() as cursor:
            cursor.execute(
                f"""WITH clock AS ({_CLOCK}),
                    counted AS (
                        INSERT INTO llm_daily_usage (user_id, usage_date, kind, used)
                        SELECT %(user)s, clock.usage_date, %(kind)s, 1 FROM clock
                        ON CONFLICT (user_id, usage_date, kind) DO UPDATE SET used = llm_daily_usage.used + 1
                            WHERE %(limit)s = 0 OR llm_daily_usage.used < %(limit)s
                        RETURNING used
                    )
                    SELECT clock.usage_date, clock.resets_at, counted.used FROM clock LEFT JOIN counted ON true""",
                params,
            )
            usage_date, resets_at, used = cursor.fetchone()
            if used is not None:
                return _result(used, limit, usage_date, resets_at, allowed=True)
            # Refused: the guarded update left the row as it was.
            cursor.execute(
                "SELECT used FROM llm_daily_usage WHERE user_id = %(user)s AND usage_date = %(day)s AND kind = %(kind)s",
                {**params, "day": usage_date},
            )
            row = cursor.fetchone()
        return _result(row[0] if row else 0, limit, usage_date, resets_at, allowed=False)

    def refund(self, user_id: UUID | str, kind: str, usage_date: date) -> None:
        """Give back one unit on the day it was counted; never below zero."""
        _check(kind, 0)
        with self._connect() as connection, connection, connection.cursor() as cursor:
            cursor.execute(
                """UPDATE llm_daily_usage SET used = used - 1
                   WHERE user_id = %s AND kind = %s AND usage_date = %s AND used > 0""",
                (_user_id(user_id), kind, usage_date),
            )

    def usage(self, user_id: UUID | str, kind: str, limit: int) -> QuotaResult:
        """Today's count without spending anything."""
        _check(kind, limit)
        with self._connect() as connection, connection.cursor() as cursor:
            cursor.execute(
                f"""WITH clock AS ({_CLOCK})
                    SELECT clock.usage_date, clock.resets_at, COALESCE(u.used, 0) FROM clock
                    LEFT JOIN llm_daily_usage u
                      ON u.user_id = %(user)s AND u.kind = %(kind)s AND u.usage_date = clock.usage_date""",
                {"tz": self._timezone, "user": _user_id(user_id), "kind": kind},
            )
            usage_date, resets_at, used = cursor.fetchone()
        return _result(used, limit, usage_date, resets_at, allowed=limit == 0 or used < limit)


__all__ = ["KINDS", "MEMORY_SUMMARY", "TUTOR_TURN", "DailyQuotaStore", "QuotaResult"]
