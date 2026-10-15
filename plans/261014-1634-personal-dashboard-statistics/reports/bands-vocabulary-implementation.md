# Band history and vocabulary review implementation

Date: 2026-10-15. Status: implemented; database-backed verification remains pending Docker.

## Delivered

- Assessment stores a frozen per-result-version band snapshot, with skill coverage, score source, measurement quality,
  eligibility reason, and scoring version. A pending regrade leaves the last completed result visible; completion of
  a newer version replaces its predecessor for that attempt. Default/fallback and legacy scores do not become measured.
- Assessment history is owner-scoped, bounded to 366 days and 100 results per page. Filters select series and skill;
  baseline/latest/delta cover the whole filtered range. Overall is available only when all four skills qualify in one
  result. An active attempt has a separate owner/status context endpoint for study-time attribution.
- Library records learner state per vocabulary sense, independently of decks, with a versioned self-rated schedule,
  request receipts, unique state-version answers, due-session snapshots, and owner/expiry checks. Adding an existing
  sense is idempotent. The due queue includes NEW senses; the due counter excludes NEW. `FORGOT` schedules a retry in
  ten minutes; remembered stages use 1/3/7/14-day intervals.
- Added additive Assessment V8 and Library V3 migrations. Updated the new controllers to declare query parameter names
  explicitly after MVC tests found Java parameter-name binding depended on compiler metadata.

## API contract

- `GET /api/assessments/me/statistics/bands?from=&to=&seriesKind=&skill=&cursor=&limit=`
- `GET /api/assessments/me/statistics/study-context?sourceType=ASSESSMENT&sourceId=`
- `GET /api/learning-support/flashcards/statistics/vocabulary`
- `POST /api/learning-support/flashcards/vocabulary-study-items`
- `POST /api/learning-support/flashcards/vocabulary-review-sessions`
- `POST /api/learning-support/flashcards/vocabulary-review-sessions/{id}/answers`
- `GET /api/learning-support/flashcards/statistics/study-context?sourceType=VOCABULARY_REVIEW&sourceId=`

The API controllers and result records are the response source of truth; FE uses the same paths and field names.

## Verification

- Focused Assessment and Library tests compiled and ran. The first broad run found two new MVC tests failing because
  their source context query parameters lacked explicit names; both controller annotations were corrected.
- Assessment broad run: **205 tests, 1 new controller failure, 1 unrelated existing error, 17 skips**. The controller
  failure was fixed afterward. The existing `AnswerSpecGraderTest.gradesEverySharedContractVector` cannot find
  `backend/docs/contracts/answer-spec-v1-vectors.json`; that fixture is absent from the BE checkout and the separate
  `docs` repository. It was not changed or suppressed.
- Docker was unavailable; Testcontainers PostgreSQL tests were skipped. Migration execution, regrade SQL against
  PostgreSQL, and concurrent review transactions remain unverified locally.
- Focused rerun passed: `BandStatisticsControllerTest` **3/3**, `VocabularyReviewControllerTest` **3/3**, and
  `VocabularyReviewUseCaseTest` **6/6** (including empty vocabulary returning a null retained percentage).

## Limits

Assessment and vocabulary study contexts currently have no course attribution, so their active time is included only
in the all-course total. Library review uses self-rating and labels its recall source accordingly; it is not objective
test accuracy.
