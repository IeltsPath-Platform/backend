# User-service study-time implementation

Date: 2026-10-15. Status: implemented; PostgreSQL runtime verification pending Docker.

## Delivered

- Personal start, heartbeat, close and study-time statistics endpoints, documented in
  [User-service README](../../../services/user-service/README.md#recorded-study-time).
- Pure domain interval acceptance with server anchor, 90-second lease/backlog, epoch/sequence checks,
  overlap watermark, bounded payloads, midnight/DST splitting, immutable close, and no offline gap credit.
- Per-user PostgreSQL transaction advisory lock, one open lease unique index, explicit takeover,
  stable start idempotency, payload-hashed heartbeat/close receipts, atomic interval/daily-total writes.
  Sessions always get a new ID for a new measurement lifetime; replay never extends an expired session.
- Source verification port and HTTP adapter, outside the write transaction, preserving bearer and
  correlation ID with bounded timeouts. Learning/Assessment/Library resolve ownership/course;
  User does not accept client-supplied course or user identity.
- Additive V6 migration; daily ALL/course scopes share one ledger. Existing raw activity CRUD unchanged.
- Bounded date-range queries and DAY/WEEK/MONTH buckets; unknown/future/null values remain distinct
  from measured zero, and course scope never copies the global target.
- Goal snapshots seeded at activation; create/status writes schedule the next day transactionally.
  Changing timezone cannot overwrite already recorded days or reactivate a superseded pending target.
  Learner profile updates now reject invalid timezone strings; existing invalid profiles fall back to
  Asia/Ho_Chi_Minh for measurement.
- Config Server User settings for the three existing owner URLs; no Gateway/shared-security changes,
  new dependencies, destructive cleanup, secrets, or commits.

## Contract

Start body: `{clientSessionId: UUID, sourceType: LESSON|PRACTICE|REVIEW|ASSESSMENT|VOCABULARY_REVIEW,
sourceId: UUID, takeover?: boolean}`. HTTP 200 for initial/replayed starts.

Start result: `{sessionId, epoch, serverStartedAt, serverNow, leaseExpiresAt, nextSequence,
heartbeatSeconds:30, idleSeconds:120, leaseSeconds:90, status, totalAcceptedMs}`.

Heartbeat/close body: `{epoch, sequence, intervals:[{startOffsetMs,endOffsetMs}]}`; max 16 intervals.
Result: `{sessionId,epoch,sequence,acceptedMs,totalAcceptedMs,leaseExpiresAt,status}`.
Retry keeps the identical sequence and payload. After expiry/close, start with a fresh client UUID.

Statistics query: required `from`, `to`; optional `grain=DAY|WEEK|MONTH`, `courseId`; maximum inclusive
range 366 days. Result: `{availability,generatedAt,dataFrom,timezonePolicy,scope,days}`; each day is
`{date,timezone,actualSeconds,plannedSeconds,status}`. Statuses: NOT_TRACKED/FUTURE/PARTIAL_DAY/COMPLETE.
Week/month groups are clipped to the requested dates; `date` names the calendar group start.

Error codes in existing `ErrorResponse.message` and `details.code`: SESSION_IN_USE, STALE_EPOCH,
SESSION_CLOSED, LEASE_EXPIRED, INVALID_SEQUENCE, PAYLOAD_CONFLICT (409); SESSION_NOT_FOUND or
SOURCE_NOT_AVAILABLE (404); SOURCE_UNAVAILABLE (503). Malformed/invalid payloads return 400.

## Verification

- Focused domain/usecase/security tests passed after a real MVC parameter binding failure was fixed
  by explicit parameter names. HTTP adapter tests verify owner routing, bearer/correlation forwarding,
  mismatched context rejection, and unavailable-source handling without live network calls.
- Full `mvn -q -pl services/user-service -am test -Ddebug=false`: exit 0; User-service **131 tests,
  0 failures/errors, 8 skips**; common-security **8 tests, 0 failures/errors/skips**.
- New study tests: **25 tests, 22 passed, 3 skipped**. The skipped PostgreSQL cases cover concurrent
  replay, ledger/daily-total rollback, migration uniqueness, and goal queries. Docker was unavailable;
  these integration behaviors have not been executed locally.
- Final focused rerun after the pending-target timezone fix: ScheduleStudyGoalUseCaseTest,
  StudyTimeUseCaseTest, StudyTimePersistenceTest (Docker-dependent skips expected).
- `git diff --check` passed for owned paths. No full-reactor, browser, load, production, or CI run claimed.

## Remaining validation and operating limits

- Run PostgreSQL tests with Docker and exercise actual owner endpoints once all parallel service
  changes are assembled. Browser lifecycle/retry/visibility verification belongs to FE integration.
- Raw receipts and intervals are retained. No purge is enabled; retention deletion needs its approved
  policy and replay watermark preservation. No load benchmark or deployment-specific rate limit is
  claimed; 30-second heartbeat is the client policy, not an abuse-prevention rate limiter.
- Idle/visibility truth is a client signal. Server checks bound and deduplicate elapsed time; they do
  not prove learner attention. No prior elapsed activity/attempt duration is backfilled as active time.
- The initial reporting boundary is the deployment activation timestamp expressed in the profile
  timezone. Existing recorded buckets remain authoritative after timezone changes; days with no rows
  use the current profile timezone. Assessment/vocabulary contexts currently resolve no course.
