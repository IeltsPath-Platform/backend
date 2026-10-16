# Dashboard implementation status

Date: 2026-10-15. The feature code is implemented across User, Learning, Content, Assessment, Library, and FE. It has not been committed or pushed.

## Frontend

- Detailed overview now loads five independent API-backed cards: active study time, lesson completion across four skills, measured band history, vocabulary review, and started courses. It removes Mentor and demo values from that tab; general Overview mocks remain in use elsewhere.
- Filters are stored in the URL; course and band pages have bounded pagination; each card has separate loading, error/retry, empty, and data states.
- Vocabulary can be added by canonical sense and reviewed through a server session. Learners reveal the meaning, self-rate recall, and can retry an identical answer request after a network failure.
- Activity collection is attached to Lesson, Practice, Review, Placement, Topic Test, vocabulary review, and active microphone recording. The browser reports bounded monotonic intervals, stops in hidden tabs, limits idle time, and sends sequential idempotent requests.
- `npm run lint`: passed. `npm run typecheck`: passed. `npm run build`: passed; Vite emitted a >500 kB JS chunk warning. `node --test src/features/study-time/activeIntervals.test.mjs`: 3 passed.
- No browser, end-to-end, keyboard, or multi-tab test ran locally.

## Backend

- User provides session start/heartbeat/close and bounded daily/week/month statistics, with user-scoped leases, receipts, trusted owner verification, additive V6 schema, and goal snapshots.
- Learning provides bounded course/skill completion counts and trusted lesson/practice/review context. Content returns bounded batched lesson metadata. Course activity is additive in Learning V11.
- Assessment freezes measurement provenance per result version and provides owner-scoped history with stable attempt pagination and separate study context. Assessment schema is additive V8.
- Library provides canonical-sense learning state, due queue, self-rated review receipts/scheduling, statistics, and study context. Library schema is additive V3.
- User report: `mvn -q -pl services/user-service -am test -Ddebug=false` passed; User 131/131, common-security 8/8. New PostgreSQL tests skipped without Docker.
- Learning/Content report: Content 255 tests passed. Learning 244 tests had 0 assertion failures and 1 error from the pre-existing missing shared fixture `backend/docs/contracts/answer-spec-v1-vectors.json`; four new focused tests passed. Docker-dependent tests skipped.
- Assessment/Library broad run exposed two new controller parameter-binding failures and the same missing AnswerSpec fixture. Both controller parameter names were fixed explicitly; targeted rerun passed: Assessment controller **3/3**, Library controller **3/3**, and Library use case **6/6**. Do not claim the broad suite passes.
- Docker/Testcontainers, migrations against PostgreSQL, full reactor verify, and live cross-service owner HTTP calls have not been verified.

## Known verification limits

- The missing answer-spec vectors file was not found in the separate docs checkout. The existing test was preserved and not suppressed.
- No retention cleanup is active for time telemetry; policy needs a product decision before long-term operation.
- No real browser E2E has confirmed StrictMode, multi-tab takeover, logout, background/return, or audio/recording behavior.

Use [independent-test-prompt.md](../independent-test-prompt.md) to ask another agent to check the feature and write an independent report.
